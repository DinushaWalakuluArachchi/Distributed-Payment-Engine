package com.paymentengine.events;

public final class KafkaTopics {

    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String ACCOUNT_EVENTS = "account.events";
    public static final String FRAUD_EVENTS = "fraud.events";
    public static final String NOTIFICATION_EVENTS = "notification.events";

    public KafkaTopics() {  // pure constant holder
    }
}
