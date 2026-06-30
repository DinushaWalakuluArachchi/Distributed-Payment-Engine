package com.paymentengine.fraudservice.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface FraudSignalCheck {

    String getSignalType();

    BigDecimal evaluate(FraudCheckRequest request);

    record FraudCheckRequest(
            UUID paymentId,
            UUID senderId,
            UUID receiverId,
            BigDecimal amount,
            String currency
    ){

    }
}
