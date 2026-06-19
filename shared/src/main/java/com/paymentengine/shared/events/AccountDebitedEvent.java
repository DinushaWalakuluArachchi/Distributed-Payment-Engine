package com.paymentengine.shared.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountDebitedEvent(
        UUID paymentId,
        UUID ownerId,
        BigDecimal amount,
        Instant occurredAt
) {

    public AccountDebitedEvent(UUID paymentId, UUID ownerId, BigDecimal amount){
        this(paymentId, ownerId, amount, Instant.now());
    }
}
