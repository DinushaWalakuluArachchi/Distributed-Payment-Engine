package com.paymentengine.fraudservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class NewAccountCheck implements FraudSignalCheck{

    private final JdbcTemplate jdbcTemplate;

    @Value("${fraud.account.new-account-days}")
    private int newAccountDays;

    @Override
    public String getSignalType() {
        return "NEW_ACCOUNT";
    }

    @Override
    public BigDecimal evaluate(FraudCheckRequest request) {
       try {


           Timestamp createdAt = jdbcTemplate.queryForObject(
                   "SELECT created_at FROM accounts WHERE owner_id =?",
                   Timestamp.class,
                   request.senderId());

           if (createdAt == null) {
               return BigDecimal.ZERO;
           }

           long accountAgeDays = java.time.Duration.between(
                   createdAt.toInstant(), Instant.now()
           ).toDays();

           if (accountAgeDays >= newAccountDays) {
               return BigDecimal.ZERO;
           }


           // Brand new account (0 days) = highest risk, approaching the threshold = lower risk
           double score = 1.0 - ((double) accountAgeDays / newAccountDays);
           return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);

       }catch (EmptyResultDataAccessException e){
        return BigDecimal.ZERO;
       }
    }
}
