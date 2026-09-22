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
    private final RabbitTemplate rabbitTemplate;
    private final AppointmentWaitlistProperties appointmentWaitlistProperties;

    @PostConstruct
    void configureReturnedMessageLogging() {
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "Waitlist timeout message was returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText()));
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
        try {
            rabbitTemplate.convertAndSend(WaitlistRabbitMqConfig.DELAY_EXCHANGE,
                    WaitlistRabbitMqConfig.DELAY_ROUTING_KEY,
                    new WaitlistTimeoutMessage(waitlistId, scheduleSlotId),
                    message -> {
                        message.getMessageProperties().setExpiration(String.valueOf(
                                appointmentWaitlistProperties.getOfferDuration().toMillis()));
                        return message;
                    }, correlationData);
            correlationData.getFuture().whenComplete((confirm, exception) -> {
                if (exception != null) {
                    log.error("Waitlist timeout publisher confirm failed: waitlistId={}, scheduleSlotId={}, correlationId={}",
                            waitlistId, scheduleSlotId, correlationData.getId(), exception);
                    return;
                }
                if (!confirm.isAck()) {
                    log.error("Waitlist timeout message was nacked: waitlistId={}, scheduleSlotId={}, correlationId={}, reason={}",
                            waitlistId, scheduleSlotId, correlationData.getId(), confirm.getReason());
                    return;
                }
                log.debug("Waitlist timeout message confirmed: waitlistId={}, scheduleSlotId={}, correlationId={}",
                        waitlistId, scheduleSlotId, correlationData.getId());
            });
        } catch (RuntimeException exception) {
            // The committed OFFERED record is recovered later by the low-frequency database scan.
            log.error("Unable to publish waitlist timeout message: waitlistId={}, scheduleSlotId={}, reason={}",
                    waitlistId, scheduleSlotId, exception.getMessage(), exception);
        }
    }
}
