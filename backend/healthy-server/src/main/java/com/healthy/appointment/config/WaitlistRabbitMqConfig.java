package com.healthy.appointment.config;

import com.healthy.appointment.mq.WaitlistTimeoutMessagePublisher;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecovererWithConfirms;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Configuration
@Slf4j
public class WaitlistRabbitMqConfig {
    public static final String DELAY_EXCHANGE = "appointment.waitlist.timeout.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "appointment.waitlist.timeout.dlx";
    public static final String DELAY_QUEUE = "appointment.waitlist.timeout.delay.queue";
    public static final String CONSUME_QUEUE = "appointment.waitlist.timeout.queue";
    public static final String FAILURE_EXCHANGE = "appointment.waitlist.timeout.failure.exchange";
    public static final String FAILURE_QUEUE = "appointment.waitlist.timeout.failure.queue";
    public static final String DELAY_ROUTING_KEY = "waitlist.timeout.delay";
    public static final String CONSUME_ROUTING_KEY = "waitlist.timeout.consume";
    public static final String FAILURE_ROUTING_KEY = "waitlist.timeout.failure";

    @Bean
    public DirectExchange waitlistTimeoutDelayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange waitlistTimeoutDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue waitlistTimeoutDelayQueue() {
        return new Queue(DELAY_QUEUE, true, false, false, Map.of(
                "x-dead-letter-exchange", DEAD_LETTER_EXCHANGE,
                "x-dead-letter-routing-key", CONSUME_ROUTING_KEY));
    }

    @Bean
    public Queue waitlistTimeoutConsumeQueue() {
        return new Queue(CONSUME_QUEUE, true);
    }

    @Bean
    public DirectExchange waitlistTimeoutFailureExchange() {
        return new DirectExchange(FAILURE_EXCHANGE, true, false);
    }

    @Bean
    public Queue waitlistTimeoutFailureQueue() {
        return new Queue(FAILURE_QUEUE, true);
    }

    @Bean
    public Binding waitlistTimeoutDelayBinding() {
        return BindingBuilder.bind(waitlistTimeoutDelayQueue())
                .to(waitlistTimeoutDelayExchange())
                .with(DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding waitlistTimeoutConsumeBinding() {
        return BindingBuilder.bind(waitlistTimeoutConsumeQueue())
                .to(waitlistTimeoutDeadLetterExchange())
                .with(CONSUME_ROUTING_KEY);
    }

    @Bean
    public Binding waitlistTimeoutFailureBinding() {
        return BindingBuilder.bind(waitlistTimeoutFailureQueue())
                .to(waitlistTimeoutFailureExchange())
                .with(FAILURE_ROUTING_KEY);
    }

    @Bean("waitlistRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory waitlistRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        RabbitProperties.ListenerRetry retry = rabbitProperties.getListener().getSimple().getRetry();
        RepublishMessageRecovererWithConfirms republisher = new RepublishMessageRecovererWithConfirms(
                rabbitTemplate, FAILURE_EXCHANGE, FAILURE_ROUTING_KEY,
                CachingConnectionFactory.ConfirmType.CORRELATED);
        MessageRecoverer parkingRecoverer = (message, cause) -> {
            String messageId = message.getMessageProperties().getMessageId();
            Object waitlistId = message.getMessageProperties().getHeaders()
                    .get(WaitlistTimeoutMessagePublisher.WAITLIST_ID_HEADER);
            Object slotId = message.getMessageProperties().getHeaders()
                    .get(WaitlistTimeoutMessagePublisher.SLOT_ID_HEADER);
            try {
                republisher.recover(message, cause);
                log.error("RabbitMQ consumer retries exhausted; message moved to failure queue: messageId={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, retryResult=PARKED",
                        messageId, waitlistId, slotId, FAILURE_EXCHANGE, FAILURE_ROUTING_KEY, cause);
            } catch (RuntimeException exception) {
                log.error("RabbitMQ consumer retries exhausted and failure-queue publish failed: messageId={}, waitlistId={}, slotId={}, exchange={}, routingKey={}, retryResult=PARK_FAILED",
                        messageId, waitlistId, slotId, FAILURE_EXCHANGE, FAILURE_ROUTING_KEY, exception);
                throw exception;
            }
            // Manual acknowledgement mode requires the original failed delivery to be settled.
            // It has already been safely parked, so reject it without putting it back in a loop.
            throw new AmqpRejectAndDontRequeueException(
                    "Waitlist timeout message parked after retry exhaustion", true, cause);
        };
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(retry.getMaxAttempts())
                .backOffOptions(retry.getInitialInterval().toMillis(), retry.getMultiplier(),
                        retry.getMaxInterval().toMillis())
                .recoverer(parkingRecoverer)
                .build());
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
