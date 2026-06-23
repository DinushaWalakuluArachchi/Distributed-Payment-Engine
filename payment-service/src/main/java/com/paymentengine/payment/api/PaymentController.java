package com.paymentengine.payment.api;

import com.paymentengine.payment.domain.Payment;
import com.paymentengine.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {


    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse>initiate(
            @RequestHeader("Idempotency-key") String idempotencyKey,
            @RequestBody InitiatePaymentRequest request){
        Payment payment = paymentService.initiatePayment(
                idempotencyKey,
                request.senderId(),
                request.receiverId(),
                request.amount(),
                request.currency()

        );
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable UUID id){
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    record InitiatePaymentRequest(
            UUID senderId, UUID receiverId, BigDecimal amount, String currency) {}

    record PaymentResponse(
            UUID id, String status, BigDecimal amount, String currency) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(
                    p.getId(), p.getStatus().name(), p.getAmount(), p.getCurrency());
        }
    }

}
