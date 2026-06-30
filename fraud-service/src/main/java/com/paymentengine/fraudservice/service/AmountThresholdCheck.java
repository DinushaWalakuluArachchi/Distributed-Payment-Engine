package com.paymentengine.fraudservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AmountThresholdCheck implements FraudSignalCheck {

    @Value("${fraud.amount.high-value-threshold}")
    private BigDecimal highValueThreshold;

    @Override
    public String getSignalType() {
        return "AMOUNT_THRESHOLD";
    }

    @Override
    public BigDecimal evaluate(FraudCheckRequest request) {

        BigDecimal amount = request.amount();

        if (amount.compareTo(highValueThreshold)<= 0){
            return BigDecimal.ZERO;
        }


        // Risk grows with how far above the threshold the payment is
        BigDecimal ratio = amount.divide(highValueThreshold, 4, RoundingMode.HALF_UP);
        double score= Math.min(1.0, (ratio.doubleValue()- 1.0) * 0.5);


        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }
}
