package com.paymentengine.notificationservice.channel;

import java.util.Map;
import java.util.UUID;

public interface NotificationChannel {
    String getChannelName();
    void send(UUID paymentId, String eventType, Map<String , Object> payload);
}
