package com.paymentengine.shared.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        String failedAtStep,
        String reason,
        Instant occurredAt) {

    public static PaymentFailedEvent of(UUID paymentId, String failedAtStep, String reason){
        return new PaymentFailedEvent(paymentId, failedAtStep, reason, Instant.now());
    }
}
