package com.healthy.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.healthy.appointment.entity.OutboxMessage;
import com.healthy.appointment.mapper.OutboxMessageMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxTransactionIntegrationTest {
    @Autowired
    private OutboxMessageService outboxMessageService;
    @Autowired
    private OutboxMessageMapper outboxMessageMapper;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long aggregateId;

    @AfterEach
    void cleanUp() {
        if (aggregateId != null) {
            outboxMessageMapper.delete(new LambdaQueryWrapper<>(OutboxMessage.class)
                    .eq(OutboxMessage::getAggregateType, "WAITLIST")
                    .eq(OutboxMessage::getAggregateId, aggregateId));
        }
    }

    @Test
    void committedBusinessTransactionKeepsOutboxMessage() {
        aggregateId = testAggregateId();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> outboxMessageService.recordWaitlistTimeout(
                aggregateId, 10L, LocalDateTime.now().plusMinutes(1)));

        assertThat(countMessages()).isOne();
    }

    @Test
    void rolledBackBusinessTransactionAlsoRollsBackOutboxMessage() {
        aggregateId = testAggregateId();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            outboxMessageService.recordWaitlistTimeout(
                    aggregateId, 10L, LocalDateTime.now().plusMinutes(1));
            status.setRollbackOnly();
        });

        assertThat(countMessages()).isZero();
    }

    private Long countMessages() {
        return outboxMessageMapper.selectCount(new LambdaQueryWrapper<>(OutboxMessage.class)
                .eq(OutboxMessage::getAggregateType, "WAITLIST")
                .eq(OutboxMessage::getAggregateId, aggregateId));
    }

    private long testAggregateId() {
        return 8_000_000_000_000_000_000L
                + ThreadLocalRandom.current().nextLong(100_000_000L);
    }
}
