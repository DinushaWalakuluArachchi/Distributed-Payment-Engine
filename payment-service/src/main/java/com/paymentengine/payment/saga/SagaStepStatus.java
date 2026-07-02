package com.paymentengine.payment.saga;

public enum SagaStepStatus {
    PENDING,
    COMPLETED,
    FAILED,
    COMPENSATED
}
