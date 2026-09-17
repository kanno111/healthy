package com.healthy.appointment.service;

/** Redis 班次库存的原子预扣与同步操作。 */
public interface AppointmentStockService {
    /**
     * 使用 Lua 原子预扣一个号源。
     *
     * @return {@code true} 表示预扣成功，{@code false} 表示库存不足
     */
    boolean preDeduct(Long slotId);

    /** 在数据库事务未提交时，归还此前成功预扣的一个号源。 */
    void restore(Long slotId);

    /** 以 MySQL 中的剩余号源覆盖 Redis 库存，用于放号、调号源等管理操作提交后。 */
    void initialize(Long slotId, int remainingCapacity);

    /** 仅在库存键不存在时，以 MySQL 数据填充库存。 */
    void initializeIfAbsent(Long slotId, int remainingCapacity);

    /** 删除已关闭班次的库存键。 */
    void remove(Long slotId);
}
