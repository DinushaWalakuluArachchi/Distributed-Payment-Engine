package com.paymentengine.shared.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInitiatedEvent(
        UUID paymentId,
        UUID senderId,
        UUID receiverId,
        BigDecimal amount,
        String currency,
        Instant occurredAt

){
    public static PaymentInitiatedEvent of(
            UUID paymentId, UUID senderId, UUID receiverId,
            BigDecimal amount, String currency){
        return  new PaymentInitiatedEvent(paymentId,senderId,receiverId,amount,currency,Instant.now());
    }

}
