package com.paymentengine.notificationservice.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromAddress;

    @Value("${notification.email.to}")
    private String toAddress;

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void send(UUID paymentId, String eventType, Map<String, Object> payload) {
        log.info("Sending email notification for paymentId={} eventType={}", paymentId, eventType);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toAddress);
        message.setSubject(builSubject(eventType, paymentId));
        message.setText(buildBody(eventType, paymentId, payload));

        mailSender.send(message);

        log.info("Email sent successfully for paymentId={}", paymentId);

    }


    private String builSubject(String eventType, UUID paymentId){
        return switch (eventType){
            case "PAYMENT_COMPLETED" -> "Payment confirmed -- " + paymentId;
            case "PAYMENT_FAILED" -> "Payment failed -- " + paymentId;
            default -> "Payment update -- " + paymentId;
        };
    }

    private String buildBody(String eventType, UUID paymentId,Map<String , Object> payload ){
        return switch (eventType){
            case "PAYMENT_COMPLETED" -> """
                    Your Payment has been processed successfully.
                    
                    Payment ID : %s
                    Amount     : %s %s
                    
                    Thank you for using Payment Engine.
                    """.formatted(paymentId, payload.get("amount"), payload.get("currency"));


            case "PAYMENT_FAILED" -> """
                Unfortunately your payment could not be processed.

                Payment ID : %s
                Reason     : %s

                Please try again or contact support.
                """.formatted(paymentId, payload.getOrDefault("reason", "Unknown"));

            default -> "Payment update for ID: " + paymentId;
        };
    }
}
