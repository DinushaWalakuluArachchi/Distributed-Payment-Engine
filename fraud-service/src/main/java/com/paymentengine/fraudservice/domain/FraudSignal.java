package com.paymentengine.fraudservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_signals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FraudSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false, length = 100)
    private String signalType;

    @Column(nullable = false, precision = 5, scale = 4)
    private java.math.BigDecimal score;

    @Column(nullable = false)
    private boolean flagged;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static FraudSignal of(
            UUID paymentId, String signalType,
            java.math.BigDecimal score, boolean flagged){

        FraudSignal signal = new FraudSignal();
        signal.paymentId = paymentId;
        signal.signalType = signalType;
        signal.score = score;
        signal.flagged = flagged;
        signal.createdAt = Instant.now();
        return signal;
    }

}
