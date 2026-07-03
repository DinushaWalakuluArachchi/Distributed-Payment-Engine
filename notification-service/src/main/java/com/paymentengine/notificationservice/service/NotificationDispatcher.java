package com.paymentengine.notificationservice.service;

import com.paymentengine.notificationservice.channel.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationDispatcher {

    private final List<NotificationChannel> channels;


    public NotificationDispatcher(List<NotificationChannel> channels) {
        this.channels = channels;
        log.info("🌟 NotificationDispatcher initialized with {} registered channels.", channels.size());
    }

    @KafkaListener(topics = "payment.events", groupId = "notification-service-final-v1")
    public void handledPaymentEvent(Map<String, Object> event){
        log.info("👉 Notification Service Received Event: {}", event);

        String eventType = event.getOrDefault("type", "").toString();

        if (!eventType.equals("PAYMENT_COMPLETED") && !eventType.equals("PAYMENT_FAILED")){
            return;
        }

        Object pId = event.get("paymentId") != null ? event.get("paymentId") : event.get("id");
        if (pId == null) {
            log.error("❌ Cannot dispatch notification. paymentId is missing in event payload!");
            return;
        }

        UUID paymentId = UUID.fromString(pId.toString());
        log.info("Dispatching notifications for paymentId={} eventtype={}",paymentId,eventType);

        for (NotificationChannel channel : channels){
            try {
                channel.send(paymentId, eventType, event);
            }catch (Exception e){
                log.error("Channel {} failed for paymentId={} after retries",
                        channel.getChannelName(),paymentId,e);
            }
        }
    }
}
