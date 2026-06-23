package com.paymentengine.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID receiverId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void onCreate(){
        createdAt = updatedAt = Instant.now();
    }

    @PrePersist
    private  void onUpdate(){
        updatedAt= Instant.now();

    }
    public static Payment initiate(
            String idempotencyKey, UUID senderId, UUID receiverId,
            BigDecimal amount, String currency){


        Payment  payment = new Payment();
        payment.idempotencyKey = idempotencyKey;
        payment.senderId = senderId;
        payment.receiverId = receiverId;
        payment.amount = amount;
        payment.currency = currency;
        payment.status = PaymentStatus.INITIATED;
        return payment;
    }

    public void transitionTo(PaymentStatus target){
        if (!this.status.canTransitionTo(target)){
            throw new IllegalStatusTransitionException(this.status, target);
        }
        this.status = target;
    }

}
