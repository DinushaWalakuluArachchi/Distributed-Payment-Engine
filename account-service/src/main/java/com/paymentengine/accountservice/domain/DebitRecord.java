package com.paymentengine.accountservice.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "debit_records")
@Getter
@NoArgsConstructor
public class DebitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static DebitRecord of(UUID paymentId, UUID accountId, BigDecimal amount){
        DebitRecord record = new DebitRecord();
        record.paymentId = paymentId;
        record.accountId = accountId;
        record.amount = amount;
        record.status = DebitStatus.DEBITED;
        record.createdAt = Instant.now();
        return record;
    }

    public void markCompensated(){
        this.status = DebitStatus.COMPENSATED;
    }


}
