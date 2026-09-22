package com.healthy.appointment.mq;

import com.healthy.appointment.config.AppointmentWaitlistProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

class WaitlistTimeoutMessagePublisherTest {
    @Test
    void doesNotSendWithoutTransactionSynchronization() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        WaitlistTimeoutMessagePublisher publisher = new WaitlistTimeoutMessagePublisher(
                rabbitTemplate, new AppointmentWaitlistProperties());

        publisher.publishAfterCommit(20L, 10L);

        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void sendsOnlyAfterTransactionCommitWithConfiguredTtl() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        AppointmentWaitlistProperties properties = new AppointmentWaitlistProperties();
        WaitlistTimeoutMessagePublisher publisher = new WaitlistTimeoutMessagePublisher(rabbitTemplate, properties);
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishAfterCommit(20L, 10L);
            verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(),
                    any(MessagePostProcessor.class), any(CorrelationData.class));

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

            ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
            ArgumentCaptor<CorrelationData> correlationData = ArgumentCaptor.forClass(CorrelationData.class);
            verify(rabbitTemplate).convertAndSend(eq("appointment.waitlist.timeout.exchange"),
                    eq("waitlist.timeout.delay"), eq(new WaitlistTimeoutMessage(20L, 10L)),
                    processor.capture(), correlationData.capture());
            org.springframework.amqp.core.Message message = processor.getValue().postProcessMessage(
                    new org.springframework.amqp.core.Message(new byte[0], new org.springframework.amqp.core.MessageProperties()));
            assertThat(message.getMessageProperties().getExpiration()).isEqualTo("10000");
            assertThat(correlationData.getValue().getId()).startsWith("waitlist-timeout-20-");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
