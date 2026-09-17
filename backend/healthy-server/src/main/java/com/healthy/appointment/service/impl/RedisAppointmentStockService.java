package com.healthy.appointment.service.impl;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.service.AppointmentStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.healthy.appointment.constant.Constant.APPOINTMENT_STOCK_PREFIX;

/** Redis 班次库存实现；Redis 异常或库存键缺失时失败关闭，绝不绕过 MySQL 最终扣减。 */
@Service
@RequiredArgsConstructor
public class RedisAppointmentStockService implements AppointmentStockService {
    private static final DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return -1
            end
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil then
                return -1
            end
            if stock <= 0 then
                return 0
            end
            redis.call('DECR', KEYS[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RESTORE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            return redis.call('INCR', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preDeduct(Long slotId) {
        Long result = stringRedisTemplate.execute(PRE_DEDUCT_SCRIPT, List.of(stockKey(slotId)));
        if (result == null || result < 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return result == 1;
    }

    @Override
    public void restore(Long slotId) {
        stringRedisTemplate.execute(RESTORE_SCRIPT, List.of(stockKey(slotId)));
    }

    @Override
    public void initialize(Long slotId, int remainingCapacity) {
        stringRedisTemplate.opsForValue().set(stockKey(slotId), String.valueOf(remainingCapacity));
    }

    @Override
    public void initializeIfAbsent(Long slotId, int remainingCapacity) {
        stringRedisTemplate.opsForValue().setIfAbsent(stockKey(slotId), String.valueOf(remainingCapacity));
    }

    @Override
    public void remove(Long slotId) {
        stringRedisTemplate.delete(stockKey(slotId));
    }

    private String stockKey(Long slotId) {
        return APPOINTMENT_STOCK_PREFIX + slotId;
    }
}
