package com.healthy.appointment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.healthy.appointment.entity.OutboxMessage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxMessageMapper extends BaseMapper<OutboxMessage> {
    List<OutboxMessage> selectDispatchable(@Param("now") LocalDateTime now,
                                           @Param("limit") int limit);

    int markProcessing(@Param("id") Long id,
                       @Param("now") LocalDateTime now);

    int markSent(@Param("id") Long id,
                 @Param("attemptCount") int attemptCount,
                 @Param("now") LocalDateTime now);

    int markFailed(@Param("id") Long id,
                   @Param("attemptCount") int attemptCount,
                   @Param("status") String status,
                   @Param("nextRetryAt") LocalDateTime nextRetryAt,
                   @Param("lastError") String lastError);

    int recoverTimedOutProcessing(@Param("deadline") LocalDateTime deadline,
                                  @Param("now") LocalDateTime now,
                                  @Param("maxAttempts") int maxAttempts);
}
