package com.paymentengine.fraudservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class VelocityCheck implements FraudSignalCheck {

    private final JdbcTemplate jdbcTemplate;

    @Value("${fraud.velocity.max-payments-per-hour}")
    private int maxPaymentsPerHour;

    @Override
    public String getSignalType() {
        return "VELOCITY";
    }

    @Override
    public BigDecimal evaluate(FraudCheckRequest request) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        Integer recentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE sender_id = ? AND created_at > ?",
                Integer.class,
                request.senderId(), java.sql.Timestamp.from(oneHourAgo));

        if (recentCount == null) recentCount = 0;

        if (recentCount <= maxPaymentsPerHour){
            return BigDecimal.ZERO;
        }

        // Scale risk gradually: just over the limit = mild risk, way over = high risk
        double overBy = (double) (recentCount - maxPaymentsPerHour);
        double score = Math.min(1.0,overBy / maxPaymentsPerHour);



        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }
}
