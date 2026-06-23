package com.paymentengine.paymentservice.domain;

public class IllegalStatusTransitionException extends RuntimeException{
    public IllegalStatusTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Cannot transition payment from %s to %s".formatted(from,to));
    }
}
