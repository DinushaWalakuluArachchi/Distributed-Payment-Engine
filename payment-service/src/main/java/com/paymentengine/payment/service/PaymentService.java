package com.paymentengine.payment.service;

import com.paymentengine.payment.domain.Payment;
import com.paymentengine.payment.domain.PaymentStatus;
import com.paymentengine.payment.repository.PaymentRepository;
import com.paymentengine.shared.events.KafkaTopics;
import com.paymentengine.shared.events.PaymentInitiatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    private final MeterRegistry meterRegistry;


    @Transactional
    public Payment initiatePayment(
            String idempotencyKey, UUID senderId, UUID receiverId,
            BigDecimal amount, String currency){


        // Defensive check — gateway should have already caught duplicates,
        // but the service layer never trusts an upstream guarantee blindly
        paymentRepo.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            throw new DuplicatepaymentException(
                    "Payment already exist for idempotency Key: " + idempotencyKey);
        });

        Payment payment = Payment.initiate(
                idempotencyKey,senderId,receiverId,amount,currency);
        paymentRepo.save(payment);

        log.info("Payment initiated: id={}, amount{}", payment.getId(), amount);

        //move to fraud_checking immediately and publish the event
        payment.transitionTo(PaymentStatus.FRAUD_CHECKING);
        paymentRepo.save(payment);

        meterRegistry.counter("payments.initiated", "currency", currency).increment();

        kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS,
                payment.getId().toString(),
                PaymentInitiatedEvent.of(
                        payment.getId(), senderId,receiverId,amount,currency));
        return payment;
    }

    @Transactional
    public void updateStatus(UUID paymentId, PaymentStatus newStatus){
        Payment payment = paymentRepo.findById(paymentId).orElseThrow(()-> new PaymentNotFoundException(
                "Payment not found: " + paymentId));

        PaymentStatus oldStatus = payment.getStatus();
        payment.transitionTo(newStatus);
        paymentRepo.save(payment);

        if (newStatus == PaymentStatus.COMPLETED){
            meterRegistry.counter("payments.completed").increment();
        }
        if (newStatus == PaymentStatus.FAILED){
            meterRegistry.counter("payments.failed").increment();
        }

        log.info("Payment {} transitioned {} -> {}", paymentId, oldStatus, newStatus);
    }

    @Transactional
    public Payment getPayment(UUID paymentId){
        return paymentRepo.findById(paymentId).orElseThrow(()-> new PaymentNotFoundException(
                "Payment not found: " +paymentId));
    }
}
