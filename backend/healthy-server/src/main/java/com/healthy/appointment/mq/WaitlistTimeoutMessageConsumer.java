package com.healthy.appointment.mq;

import com.healthy.appointment.config.WaitlistRabbitMqConfig;
import com.healthy.appointment.service.AppointmentWaitlistService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class WaitlistTimeoutMessageConsumer {
    private final AppointmentWaitlistService appointmentWaitlistService;

    @RabbitListener(queues = WaitlistRabbitMqConfig.CONSUME_QUEUE,
            containerFactory = "waitlistRabbitListenerContainerFactory")
    public void consume(WaitlistTimeoutMessage message, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // A runtime exception is deliberately propagated to the listener container. Spring AMQP
        // performs two retries; after all three attempts fail it rejects without requeue, and the
        // low-frequency database recovery scan remains the final recovery path.
        appointmentWaitlistService.expireOffered(message.waitlistId());
        channel.basicAck(deliveryTag, false);
    }
}
