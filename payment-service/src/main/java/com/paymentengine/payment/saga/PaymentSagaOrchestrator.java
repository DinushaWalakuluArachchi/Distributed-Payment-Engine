package com.paymentengine.payment.saga;

import com.paymentengine.payment.domain.Payment;
import com.paymentengine.payment.domain.PaymentStatus;
import com.paymentengine.payment.domain.SagaStep;
import com.paymentengine.payment.repository.PaymentRepository;
import com.paymentengine.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSagaOrchestrator {


    private final PaymentRepository paymentRepo;
    private final PaymentService paymentService;
    private final SagaStepTracker stepTracker;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafka;

    @Value("${services.fraud-service}")
    private String fraudServiceUrl;

    @Value("${services.account-service}")
    private String accountServiceUrl;

    // Listens for PaymentInitiatedEvent published by PaymentService
    @KafkaListener(topics = "payment.events", groupId = "saga-orchestrator")
    @Transactional
    public void handlePaymentInitiated(Map<String, Object> event){

        log.info("👉 Received Kafka event payload: {}", event);

        // 🌟 1. Infinite Loop එක නවත්වන්න: PAYMENT_COMPLETED ඉවෙන්ට් එකක් ආවොත් සගා එක ආයේ දුවන්නැතුව මෙතනින්ම අයින් වෙනවා
        if (event.containsKey("type") && "PAYMENT_COMPLETED".equals(event.get("type"))) {
            log.info("Skipping completion event to prevent infinite loop.");
            return;
        }

        // 🌟 2. ආරක්ෂිතව Keys තියෙනවද කියලා පරීක්ෂා කිරීම (NullPointer / Invalid UUID වැළැක්වීමට)
        Object pId = event.get("id") != null ? event.get("id") : event.get("paymentId");
        Object sId = event.get("senderId");
        Object rId = event.get("receiverId");
        Object amt = event.get("amount");

        if (pId == null || sId == null || rId == null || amt == null) {
            log.warn("⚠️ Missing required fields in Kafka event. Skipping execution. (Probably a non-initiation event)");
            return;
        }

        UUID paymentId = UUID.fromString(pId.toString());
        UUID senderId = UUID.fromString(sId.toString());
        UUID receiverId = UUID.fromString(rId.toString());
        BigDecimal amount = new BigDecimal(amt.toString());
        String currency = event.get("currency") != null ? event.get("currency").toString() : "USD";

        log.info("Saga starting for paymentId={}", paymentId);

        Payment payment = paymentRepo.findById(paymentId).orElseThrow();

        // 1st step - Fraud check
        SagaStep fraudStep = stepTracker.save(
                SagaStep.create(paymentId, "FRAUD_CHECK"));

        try {
            ResponseEntity<Map> fraudResponse = restTemplate.postForEntity(
                    fraudServiceUrl + "/fraud/check",
                    Map.of(
                            "paymentId" , paymentId,
                            "senderId", senderId,
                            "receiverId", receiverId,
                            "amount", amount,
                            "currency", currency

                    ),
                    Map.class
            );

            boolean approved = Boolean.TRUE.equals(
                    fraudResponse.getBody().get("approved"));

            if (!approved){
                fraudStep.fail();
                stepTracker.save(fraudStep);
                paymentService.updateStatus(paymentId, PaymentStatus.FRAUD_REJECTED);
                log.warn("Payment {} rejected by fraud check", paymentId);
                return;
            }

            fraudStep.completed();
            stepTracker.save(fraudStep);
            paymentService.updateStatus(paymentId, PaymentStatus.FRAUD_APPROVED);
        }catch (Exception e){
            fraudStep.fail();;
            stepTracker.save(fraudStep);
            failPayment(paymentId, "FRAUD_CHECK", e.getMessage());
            return;
        }

        // 2nd step = Debit sender
        SagaStep debitStep = stepTracker.save(
                SagaStep.create(paymentId, "DEBIT_SENDER"));

        try {
            paymentService.updateStatus(paymentId, PaymentStatus.DEBITING);

            restTemplate.postForEntity(
                    accountServiceUrl + "/accounts/" + senderId + "/debit",
                    Map.of("paymentId", paymentId, "amount", amount),
                    Void.class
            );

            debitStep.completed();
            stepTracker.save(debitStep);

        }catch (HttpClientErrorException e){
            debitStep.fail();
            stepTracker.save(debitStep);
            // debit failed - no money moved yet, jus mark failed
            failPayment(paymentId, "DEBIT_SENDER", e.getMessage());
            return;
        }

        // 3rd step: Credit Receiver
        SagaStep creditStep = stepTracker.save(
                SagaStep.create(paymentId, "CREDIT_RECEIVER"));

        try {
            restTemplate.postForEntity(
                    accountServiceUrl + "/accounts/" + receiverId + "/credit",
                    Map.of("paymentId", paymentId, "amount", amount),
                    Void.class
            );

            creditStep.completed();
            stepTracker.save(creditStep);
            paymentService.updateStatus(paymentId, PaymentStatus.CREDITED);

        }catch (HttpClientErrorException e){
            creditStep.fail();
            stepTracker.save(creditStep);

            //credit failed - must compensate the debit
            log.error("Credited failed for paymentId={}, running compensation",paymentId);
            failPayment(paymentId, "CREDIT_RECEIVER", e.getMessage());
            return;
        }

        //4th step: Complete
        paymentService.updateStatus(paymentId, PaymentStatus.COMPLETED);

        kafka.send("payment.events", paymentId.toString(),
                Map.of("type", "PAYMENT_COMPLETED", "paymentId",paymentId));

        log.info("Saga completed successfully for paymentId={}", paymentId);

    }

    private void compensateDebit(UUID paymentId, UUID senderId, SagaStep debitStep){
        try {
            restTemplate.postForEntity(
                    accountServiceUrl + "/accounts/" + senderId  + "/compensate",
                    Map.of("paymentId", paymentId),
                    Void.class
            );
            debitStep.compensate();
            stepTracker.save(debitStep);
            log.info("Debit compensated for paymentId={}", paymentId);
        }catch (Exception e){
            log.error("CRITICAL: compensation failed for paymentId={} - manual intervention needed", paymentId, e);
        }
    }

    private void failPayment(UUID paymentId, String failedStep, String reason){
        try {
            paymentService.updateStatus(paymentId, PaymentStatus.COMPENSATING);
            paymentService.updateStatus(paymentId, PaymentStatus.FAILED);
        }catch (Exception e){
            log.error("Could not transition payment {} to FAILED", paymentId, e);
        }
        log.error("Payment {} FAILED at step {} - reason: {}", paymentId, failedStep, reason);
    }


}
