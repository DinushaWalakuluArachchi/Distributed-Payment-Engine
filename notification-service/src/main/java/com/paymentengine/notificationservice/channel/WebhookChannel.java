package com.paymentengine.notificationservice.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookChannel implements NotificationChannel {

    private final RestTemplate restTemplate;

    @Value("${notification.webhook.url}")
    private String webhookUrl;

    @Override
    public String getChannelName() {
        return "WEBHOOK";
    }

    @Override
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public void send(UUID paymentId, String eventType, Map<String, Object> payload) {
        log.info("Sending webhook for paymentId={} to {}", paymentId, webhookUrl);

        Map<String, Object> webhookBody = new HashMap<>(payload);
        webhookBody.put("paymentId", paymentId);
        webhookBody.put("eventType", eventType);
        webhookBody.put("timestamp", Instant.now().toString());

        ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl, webhookBody, String.class);

        if (!response.getStatusCode().is2xxSuccessful()){
            throw new RestClientException(
                    "Webhook returned non-2xx: " + response.getStatusCode());
        }

        log.info("Webhook delivered successfully for paymentid={}", paymentId);
    }
}
