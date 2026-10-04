package com.healthy.appointment.mq;

import com.healthy.appointment.config.WaitlistRabbitMqConfig;
import com.healthy.appointment.entity.OutboxMessage;
import com.healthy.appointment.enumeration.OutboxEventType;
import com.healthy.appointment.service.OutboxMessageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class WaitlistTimeoutMessagePublisher {
    public static final String WAITLIST_ID_HEADER = "x-waitlist-id";
    public static final String SLOT_ID_HEADER = "x-slot-id";
    public static final String OUTBOX_ID_HEADER = "x-outbox-id";
    public static final String OUTBOX_ATTEMPT_HEADER = "x-outbox-attempt";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxMessageService outboxMessageService;

    @PostConstruct
    void configureReturnedMessageLogging() {
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();
            Object outboxId = returned.getMessage().getMessageProperties().getHeaders().get(OUTBOX_ID_HEADER);
            Object waitlistId = returned.getMessage().getMessageProperties().getHeaders().get(WAITLIST_ID_HEADER);
            Object slotId = returned.getMessage().getMessageProperties().getHeaders().get(SLOT_ID_HEADER);
            log.error("RabbitMQ message could not be routed: outboxId={}, messageId={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, replyCode={}, replyText={}, sendResult=RETURNED",
                    outboxId, messageId, waitlistId, slotId, returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText());
        });
    }

    public void publish(OutboxMessage outboxMessage) {
        int attemptCount = outboxMessage.getAttemptCount();
        try {
            if (!OutboxEventType.WAITLIST_OFFER_TIMEOUT.name().equals(outboxMessage.getEventType())) {
                throw new IllegalArgumentException("Unsupported outbox event type: " + outboxMessage.getEventType());
            }
            WaitlistTimeoutMessage payload = objectMapper.readValue(
                    outboxMessage.getPayload(), WaitlistTimeoutMessage.class);
            Destination destination = resolveDestination(outboxMessage);
            CorrelationData correlationData = new CorrelationData(outboxMessage.getEventId());

            rabbitTemplate.convertAndSend(destination.exchange(), destination.routingKey(), payload,
                    message -> {
                        if (destination.ttlMillis() != null) {
                            message.getMessageProperties().setExpiration(
                                    String.valueOf(destination.ttlMillis()));
                        }
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setMessageId(outboxMessage.getEventId());
                        message.getMessageProperties().setHeader(OUTBOX_ID_HEADER, outboxMessage.getId());
                        message.getMessageProperties().setHeader(OUTBOX_ATTEMPT_HEADER, attemptCount);
                        message.getMessageProperties().setHeader(WAITLIST_ID_HEADER, payload.waitlistId());
                        message.getMessageProperties().setHeader(SLOT_ID_HEADER, payload.scheduleSlotId());
                        return message;
                    }, correlationData);

            correlationData.getFuture().whenComplete((confirm, exception) -> {
                if (exception != null) {
                    failSafely(outboxMessage, "Publisher confirm error: " + failureMessage(exception));
                    return;
                }
                if (!confirm.ack()) {
                    failSafely(outboxMessage, "Publisher confirm NACK: " + confirm.reason());
                    return;
                }
                if (correlationData.getReturned() != null) {
                    failSafely(outboxMessage, "Message returned as unroutable: "
                            + correlationData.getReturned().getReplyText());
                    return;
                }
                try {
                    outboxMessageService.markSent(outboxMessage.getId(), attemptCount);
                    log.info("RabbitMQ outbox message sent successfully: outboxId={}, eventId={}, attemptCount={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, sendResult=ACK",
                            outboxMessage.getId(), outboxMessage.getEventId(), attemptCount,
                            payload.waitlistId(), payload.scheduleSlotId(),
                            destination.exchange(), destination.routingKey());
                } catch (RuntimeException stateException) {
                    // The PROCESSING timeout recovery will make this message dispatchable again.
                    log.error("RabbitMQ ACK received but outbox SENT update failed: outboxId={}, eventId={}, attemptCount={}",
                            outboxMessage.getId(), outboxMessage.getEventId(), attemptCount, stateException);
                }
            });
        } catch (RuntimeException exception) {
            failSafely(outboxMessage, "Publish failed: " + failureMessage(exception));
        }
    }

    private Destination resolveDestination(OutboxMessage outboxMessage) {
        if (outboxMessage.getScheduledAt() == null) {
            throw new IllegalArgumentException("Outbox scheduledAt must not be null");
        }
        long remainingMillis = Duration.between(LocalDateTime.now(),
                outboxMessage.getScheduledAt()).toMillis();
        if (remainingMillis <= 0) {
            return new Destination(WaitlistRabbitMqConfig.DEAD_LETTER_EXCHANGE,
                    WaitlistRabbitMqConfig.CONSUME_ROUTING_KEY, null);
        }
        return new Destination(outboxMessage.getExchangeName(), outboxMessage.getRoutingKey(),
                Math.max(1, remainingMillis));
    }

    private void failSafely(OutboxMessage outboxMessage, String failure) {
        try {
            outboxMessageService.markFailed(outboxMessage.getId(),
                    outboxMessage.getAttemptCount(), failure);
        } catch (RuntimeException stateException) {
            // Leave PROCESSING in place. The relay recovers it after processing-timeout.
            log.error("Unable to persist outbox publisher failure: outboxId={}, eventId={}, attemptCount={}, failure={}",
                    outboxMessage.getId(), outboxMessage.getEventId(),
                    outboxMessage.getAttemptCount(), failure, stateException);
        }
    }

    private String failureMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record Destination(String exchange, String routingKey, Long ttlMillis) {
    }
}
