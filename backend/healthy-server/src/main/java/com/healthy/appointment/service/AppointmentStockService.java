package com.healthy.appointment.service;

import java.time.Duration;

/** Redis 班次库存的原子预扣与同步操作。 */
public interface AppointmentStockService {
    enum PreDeductResult {
        SUCCESS,
        EMPTY,
        MISSING
    }

    /**
     * 使用 Lua 原子预扣一个号源。
     *
     * @return {@code true} 表示预扣成功，{@code false} 表示库存不足
     */
    PreDeductResult preDeduct(Long slotId);

    /** 在数据库事务未提交时，归还此前成功预扣的一个号源。 */
    void restore(Long slotId);

    /** 以 MySQL 中的剩余号源覆盖 Redis 库存，用于放号、调号源等管理操作提交后。 */
    void initialize(Long slotId, int remainingCapacity);

    /** 仅在库存键不存在时，以 MySQL 数据填充库存。 */
    void initializeIfAbsent(Long slotId, int remainingCapacity);

    /**
     * 仅当库存 Key 已存在时，使用 Lua 原子执行 INCRBY。
     * Key 缺失时不创建，保持 MISSING 状态交由下一次挂号从 MySQL 懒加载。
     *
     * @return {@code true} 表示已完成调整；{@code false} 表示 Key 缺失、值异常或缩容后会小于零
     */
    boolean adjustByDeltaIfPresent(Long slotId, int delta);

    /** 删除已关闭班次的库存键。 */
    void remove(Long slotId);

    /** 仅当库存仍为 0 时原子删除库存键，供延迟自愈使用。 */
    boolean removeIfZero(Long slotId);

    /** 仅当 Redis 当前库存仍等于 expectedStock 时原子删除库存 Key。 */
    boolean removeIfValue(Long slotId, int expectedStock);

    /** 返回当前 Redis 库存；Key 缺失或值异常时返回 {@code null}。 */
    Integer getStock(Long slotId);

    /** 当前库存是否仍为 0；键缺失、异常值均不视为 0。 */
    boolean isZero(Long slotId);

    /** 为同一班次的延迟校验抢占一个带 TTL 的标记。 */
    boolean tryAcquireRepairCheck(Long slotId, Duration ttl);
}
