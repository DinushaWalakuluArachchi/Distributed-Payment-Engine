package com.paymentengine.payment.service;

public class DuplicatepaymentException extends RuntimeException {
    public DuplicatepaymentException(String message) {
        super(message);
    }
}
