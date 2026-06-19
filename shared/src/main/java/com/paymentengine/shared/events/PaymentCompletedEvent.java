package com.paymentengine.shared.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(UUID paymentId,
                                    Instant occurredAt) {

    public static PaymentCompletedEvent of(UUID paymentId){
        return new PaymentCompletedEvent(paymentId, Instant.now());
    }
}
