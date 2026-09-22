package com.healthy.appointment.mq;

import com.healthy.appointment.service.AppointmentWaitlistService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WaitlistTimeoutMessageConsumerTest {
    @Test
    void acknowledgesIdempotentMessageAfterBusinessHandling() throws Exception {
        AppointmentWaitlistService service = mock(AppointmentWaitlistService.class);
        Channel channel = mock(Channel.class);
        WaitlistTimeoutMessageConsumer consumer = new WaitlistTimeoutMessageConsumer(service);

        consumer.consume(new WaitlistTimeoutMessage(20L, 10L), channel, 7L);

        verify(service).expireOffered(20L);
        verify(channel).basicAck(7L, false);
    }

    @Test
    void propagatesSystemFailureForContainerRetryWithoutAcknowledging() throws Exception {
        AppointmentWaitlistService service = mock(AppointmentWaitlistService.class);
        Channel channel = mock(Channel.class);
        doThrow(new IllegalStateException("database unavailable")).when(service).expireOffered(20L);
        WaitlistTimeoutMessageConsumer consumer = new WaitlistTimeoutMessageConsumer(service);

        assertThatThrownBy(() -> consumer.consume(new WaitlistTimeoutMessage(20L, 10L), channel, 7L))
                .isInstanceOf(IllegalStateException.class);

        verify(channel, org.mockito.Mockito.never()).basicAck(7L, false);
    }
}
