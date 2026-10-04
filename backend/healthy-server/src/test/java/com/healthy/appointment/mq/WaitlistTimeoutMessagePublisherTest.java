package com.healthy.appointment.mq;

import com.healthy.appointment.config.WaitlistRabbitMqConfig;
import com.healthy.appointment.entity.OutboxMessage;
import com.healthy.appointment.enumeration.OutboxEventType;
import com.healthy.appointment.service.OutboxMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WaitlistTimeoutMessagePublisherTest {
    @Test
    void publishesClaimedOutboxMessageWithRemainingTtlAndMarksAckAsSent() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        WaitlistTimeoutMessagePublisher publisher = new WaitlistTimeoutMessagePublisher(
                rabbitTemplate, new ObjectMapper(), outboxMessageService);
        OutboxMessage outbox = outbox(LocalDateTime.now().plusSeconds(10));

        publisher.publish(outbox);

        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlation = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(eq(WaitlistRabbitMqConfig.DELAY_EXCHANGE),
                eq(WaitlistRabbitMqConfig.DELAY_ROUTING_KEY),
                eq(new WaitlistTimeoutMessage(20L, 10L)), processor.capture(), correlation.capture());
        Message message = processor.getValue().postProcessMessage(
                new Message(new byte[0], new MessageProperties()));
        long ttl = Long.parseLong(message.getMessageProperties().getExpiration());
        assertThat(ttl).isPositive().isLessThanOrEqualTo(10_000L);
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("event-1");
        assertThat(message.getMessageProperties().getHeaders())
                .containsEntry(WaitlistTimeoutMessagePublisher.OUTBOX_ID_HEADER, 1L)
                .containsEntry(WaitlistTimeoutMessagePublisher.OUTBOX_ATTEMPT_HEADER, 1);

        correlation.getValue().getFuture().complete(new CorrelationData.Confirm(true, null));

        verify(outboxMessageService).markSent(1L, 1);
        verify(outboxMessageService, never()).markFailed(any(), any(Integer.class), any());
    }

    @Test
    void publishesAlreadyDueMessageDirectlyToConsumeExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        WaitlistTimeoutMessagePublisher publisher = new WaitlistTimeoutMessagePublisher(
                rabbitTemplate, new ObjectMapper(), outboxMessageService);
        OutboxMessage outbox = outbox(LocalDateTime.now().minusSeconds(1));

        publisher.publish(outbox);

        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlation = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(eq(WaitlistRabbitMqConfig.DEAD_LETTER_EXCHANGE),
                eq(WaitlistRabbitMqConfig.CONSUME_ROUTING_KEY),
                eq(new WaitlistTimeoutMessage(20L, 10L)), processor.capture(), correlation.capture());
        Message message = processor.getValue().postProcessMessage(
                new Message(new byte[0], new MessageProperties()));
        assertThat(message.getMessageProperties().getExpiration()).isNull();
    }

    @Test
    void marksPublisherNackForRetry() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        WaitlistTimeoutMessagePublisher publisher = new WaitlistTimeoutMessagePublisher(
                rabbitTemplate, new ObjectMapper(), outboxMessageService);
        OutboxMessage outbox = outbox(LocalDateTime.now().plusSeconds(10));
        ArgumentCaptor<CorrelationData> correlation = ArgumentCaptor.forClass(CorrelationData.class);

        publisher.publish(outbox);
        verify(rabbitTemplate).convertAndSend(any(), any(), any(),
                any(MessagePostProcessor.class), correlation.capture());
        correlation.getValue().getFuture().complete(new CorrelationData.Confirm(false, "broker unavailable"));

        verify(outboxMessageService).markFailed(eq(1L), eq(1),
                org.mockito.ArgumentMatchers.contains("NACK"));
        verify(outboxMessageService, never()).markSent(any(), any(Integer.class));
    }

    @Test
    void rejectsUnsupportedEventTypeWithoutSending() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        WaitlistTimeoutMessagePublisher publisher = new WaitlistTimeoutMessagePublisher(
                rabbitTemplate, new ObjectMapper(), outboxMessageService);
        OutboxMessage outbox = outbox(LocalDateTime.now().plusSeconds(10));
        outbox.setEventType("UNKNOWN_EVENT");

        publisher.publish(outbox);

        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        verify(outboxMessageService).markFailed(eq(1L), eq(1),
                org.mockito.ArgumentMatchers.contains("Unsupported outbox event type"));
    }

    private OutboxMessage outbox(LocalDateTime scheduledAt) {
        OutboxMessage message = new OutboxMessage();
        message.setId(1L);
        message.setEventId("event-1");
        message.setEventType(OutboxEventType.WAITLIST_OFFER_TIMEOUT.name());
        message.setPayload("{\"waitlistId\":20,\"scheduleSlotId\":10}");
        message.setExchangeName(WaitlistRabbitMqConfig.DELAY_EXCHANGE);
        message.setRoutingKey(WaitlistRabbitMqConfig.DELAY_ROUTING_KEY);
        message.setScheduledAt(scheduledAt);
        message.setAttemptCount(1);
        return message;
    }
}
