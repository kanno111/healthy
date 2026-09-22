package com.healthy.appointment.mq;

import com.healthy.appointment.config.WaitlistRabbitMqConfig;
import com.healthy.appointment.service.AppointmentWaitlistService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class WaitlistTimeoutMessageConsumer {
    private final AppointmentWaitlistService appointmentWaitlistService;

    @RabbitListener(queues = WaitlistRabbitMqConfig.CONSUME_QUEUE,
            containerFactory = "waitlistRabbitListenerContainerFactory")
    public void consume(WaitlistTimeoutMessage message, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                        @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) throws IOException {
        handle(message, channel, deliveryTag, messageId);
    }

    public void consume(WaitlistTimeoutMessage message, Channel channel, long deliveryTag) throws IOException {
        handle(message, channel, deliveryTag, null);
    }

    private void handle(WaitlistTimeoutMessage message, Channel channel, long deliveryTag,
                        String messageId) throws IOException {
        // A runtime exception is deliberately propagated to the listener container. Spring AMQP
        // performs two retries; after all three attempts fail it rejects without requeue, and the
        // low-frequency database recovery scan remains the final recovery path.
        try {
            boolean expired = appointmentWaitlistService.expireOffered(message.waitlistId());
            if (expired) {
                log.info("RabbitMQ message consumed successfully: messageId={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, consumeResult=EXPIRED",
                        messageId, message.waitlistId(), message.scheduleSlotId(),
                        WaitlistRabbitMqConfig.DEAD_LETTER_EXCHANGE, WaitlistRabbitMqConfig.CONSUME_ROUTING_KEY);
            } else {
                log.warn("RabbitMQ message ignored because business state already changed: messageId={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, affectedRows=0, consumeResult=IDEMPOTENT_SKIP",
                        messageId, message.waitlistId(), message.scheduleSlotId(),
                        WaitlistRabbitMqConfig.DEAD_LETTER_EXCHANGE, WaitlistRabbitMqConfig.CONSUME_ROUTING_KEY);
            }
        } catch (RuntimeException exception) {
            log.error("RabbitMQ consumer business execution failed: messageId={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, consumeResult=FAILED",
                    messageId, message.waitlistId(), message.scheduleSlotId(),
                    WaitlistRabbitMqConfig.DEAD_LETTER_EXCHANGE, WaitlistRabbitMqConfig.CONSUME_ROUTING_KEY,
                    exception);
            throw exception;
        }
        channel.basicAck(deliveryTag, false);
    }
}
