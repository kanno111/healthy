package com.healthy.appointment.service.impl;

import com.healthy.appointment.enumeration.ErrorCode;
import com.healthy.appointment.exception.BusinessException;
import com.healthy.appointment.service.AppointmentStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static com.healthy.appointment.constant.Constant.APPOINTMENT_STOCK_PREFIX;

/** Redis 班次库存实现；Redis 异常或库存键缺失时失败关闭，绝不绕过 MySQL 最终扣减。 */
@Service
@RequiredArgsConstructor
public class RedisAppointmentStockService implements AppointmentStockService {
    private static final Logger log = LoggerFactory.getLogger(RedisAppointmentStockService.class);
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
            redis.call('INCR', KEYS[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> ADJUST_BY_DELTA_IF_PRESENT_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            local stock = tonumber(redis.call('GET', KEYS[1]))
            local delta = tonumber(ARGV[1])
            if stock == nil or delta == nil or stock + delta < 0 then
                return -1
            end
            redis.call('INCRBY', KEYS[1], delta)
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> REMOVE_IF_VALUE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return 0
            end
            local stock = tonumber(redis.call('GET', KEYS[1]))
            local expectedStock = tonumber(ARGV[1])
            if stock == nil or expectedStock == nil or stock ~= expectedStock then
                return 0
            end
            return redis.call('DEL', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PreDeductResult preDeduct(Long slotId) {
        Long result = stringRedisTemplate.execute(PRE_DEDUCT_SCRIPT, List.of(stockKey(slotId)));
        if (result == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        if (result == 1) {
            return PreDeductResult.SUCCESS;
        }
        if (result == 0) {
            return PreDeductResult.EMPTY;
        }
        if (result == -1) {
            return PreDeductResult.MISSING;
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    public void restore(Long slotId) {
        try {
            Long result = stringRedisTemplate.execute(RESTORE_SCRIPT, List.of(stockKey(slotId)));
            if (result == null || result != 1) {
                log.error("Redis stock restore failed: operation=RESTORE, slotId={}, reason=unexpected Lua result {}", slotId, result);
            }
        } catch (RuntimeException exception) {
            // Redis 补偿失败不能再改变已决定的 MySQL 事务结果；后续 EMPTY 自愈会处理库存偏小。
            log.error("Redis stock restore failed: operation=RESTORE, slotId={}, reason={}",
                    slotId, exception.getMessage(), exception);
        }
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
    public boolean adjustByDeltaIfPresent(Long slotId, int delta) {
        Long result = stringRedisTemplate.execute(ADJUST_BY_DELTA_IF_PRESENT_SCRIPT,
                List.of(stockKey(slotId)), String.valueOf(delta));
        if (result != null && result == 1) {
            return true;
        }
        if (result != null && result == -1) {
            log.warn("Redis stock capacity adjustment skipped: operation=ADJUST_BY_DELTA, slotId={}, delta={}, reason=invalid stock or negative result",
                    slotId, delta);
        }
        return false;
    }

    @Override
    public void remove(Long slotId) {
        stringRedisTemplate.delete(stockKey(slotId));
    }

    @Override
    public boolean removeIfZero(Long slotId) {
        return removeIfValue(slotId, 0);
    }

    @Override
    public boolean removeIfValue(Long slotId, int expectedStock) {
        Long result = stringRedisTemplate.execute(REMOVE_IF_VALUE_SCRIPT,
                List.of(stockKey(slotId)), String.valueOf(expectedStock));
        return result != null && result == 1;
    }

    @Override
    public Integer getStock(Long slotId) {
        String value = stringRedisTemplate.opsForValue().get(stockKey(slotId));
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            log.warn("Redis stock value is invalid: operation=GET_STOCK, slotId={}, value={}", slotId, value);
            return null;
        }
    }

    @Override
    public boolean isZero(Long slotId) {
        return Integer.valueOf(0).equals(getStock(slotId));
    }

    @Override
    public boolean tryAcquireRepairCheck(Long slotId, Duration ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(repairCheckKey(slotId), "1", ttl));
    }

    private String stockKey(Long slotId) {
        return APPOINTMENT_STOCK_PREFIX + slotId;
    }

    private String repairCheckKey(Long slotId) {
        return APPOINTMENT_STOCK_PREFIX + "check:" + slotId;
    }
}
