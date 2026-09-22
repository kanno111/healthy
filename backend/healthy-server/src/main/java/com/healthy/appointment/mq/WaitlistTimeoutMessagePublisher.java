package com.healthy.appointment.mq;

import com.healthy.appointment.config.AppointmentWaitlistProperties;
import com.healthy.appointment.config.WaitlistRabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WaitlistTimeoutMessagePublisher {
    public static final String WAITLIST_ID_HEADER = "x-waitlist-id";
    public static final String SLOT_ID_HEADER = "x-slot-id";

    private final RabbitTemplate rabbitTemplate;
    private final AppointmentWaitlistProperties appointmentWaitlistProperties;

    @PostConstruct
    void configureReturnedMessageLogging() {
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();
            Object waitlistId = returned.getMessage().getMessageProperties().getHeaders().get(WAITLIST_ID_HEADER);
            Object slotId = returned.getMessage().getMessageProperties().getHeaders().get(SLOT_ID_HEADER);
            log.error("RabbitMQ message could not be routed: messageId={}, correlationData={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, replyCode={}, replyText={}, sendResult=RETURNED",
                    messageId, messageId, waitlistId, slotId, returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText());
        });
    }

    public void publishAfterCommit(Long waitlistId, Long scheduleSlotId) {
        Runnable publishTask = () -> publish(waitlistId, scheduleSlotId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // An OFFERED transition must always be wrapped in a database transaction. Sending
            // here would break the "commit first, publish second" guarantee; the recovery scan
            // will handle a record if an unexpected caller violates that contract.
            log.error("Skipped waitlist timeout message without transaction synchronization: waitlistId={}, scheduleSlotId={}",
                    waitlistId, scheduleSlotId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishTask.run();
            }
        });
    }

    private void publish(Long waitlistId, Long scheduleSlotId) {
        CorrelationData correlationData = new CorrelationData(
                "waitlist-timeout-" + waitlistId + "-" + UUID.randomUUID());
        String messageId = correlationData.getId();
        try {
            rabbitTemplate.convertAndSend(WaitlistRabbitMqConfig.DELAY_EXCHANGE,
                    WaitlistRabbitMqConfig.DELAY_ROUTING_KEY,
                    new WaitlistTimeoutMessage(waitlistId, scheduleSlotId),
                    message -> {
                        message.getMessageProperties().setExpiration(String.valueOf(
                                appointmentWaitlistProperties.getOfferDuration().toMillis()));
                        message.getMessageProperties().setMessageId(messageId);
                        message.getMessageProperties().setHeader(WAITLIST_ID_HEADER, waitlistId);
                        message.getMessageProperties().setHeader(SLOT_ID_HEADER, scheduleSlotId);
                        return message;
                    }, correlationData);
            correlationData.getFuture().whenComplete((confirm, exception) -> {
                if (exception != null) {
                    log.error("RabbitMQ publisher confirm failed: messageId={}, correlationData={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, sendResult=CONFIRM_ERROR",
                            messageId, correlationData.getId(), waitlistId, scheduleSlotId,
                            WaitlistRabbitMqConfig.DELAY_EXCHANGE, WaitlistRabbitMqConfig.DELAY_ROUTING_KEY, exception);
                    return;
                }
                if (!confirm.isAck()) {
                    log.error("RabbitMQ publisher confirm NACK: messageId={}, correlationData={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, reason={}, sendResult=NACK",
                            messageId, correlationData.getId(), waitlistId, scheduleSlotId,
                            WaitlistRabbitMqConfig.DELAY_EXCHANGE, WaitlistRabbitMqConfig.DELAY_ROUTING_KEY,
                            confirm.getReason());
                    return;
                }
                if (correlationData.getReturned() != null) {
                    return;
                }
                log.info("RabbitMQ message sent successfully: messageId={}, correlationData={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, sendResult=ACK",
                        messageId, correlationData.getId(), waitlistId, scheduleSlotId,
                        WaitlistRabbitMqConfig.DELAY_EXCHANGE, WaitlistRabbitMqConfig.DELAY_ROUTING_KEY);
            });
        } catch (RuntimeException exception) {
            // The committed OFFERED record is recovered later by the low-frequency database scan.
            log.error("Unable to publish RabbitMQ message: messageId={}, correlationData={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, sendResult=FAILED",
                    messageId, correlationData.getId(), waitlistId, scheduleSlotId,
                    WaitlistRabbitMqConfig.DELAY_EXCHANGE, WaitlistRabbitMqConfig.DELAY_ROUTING_KEY, exception);
        }
    }
}
