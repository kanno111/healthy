package com.healthy.appointment.constant;

/**
 * 项目公共常量。
 */
public final class Constant {
    private Constant() {
    }

    /** 当前有效登录 Token 的 Redis 键前缀。 */
    public static final String AUTH_TOKEN_PREFIX = "auth:token:";

    /** Redis 中班次剩余号源的键前缀。 */
    public static final String APPOINTMENT_STOCK_PREFIX = "appointment:stock:";

    /** 排班处于开放预约状态。 */
    public static final String SCHEDULE_STATUS_OPEN = "OPEN";

    /** 排班已由管理员关闭预约。 */
    public static final String SCHEDULE_STATUS_CLOSED = "CLOSED";

    /** 单次排班或查询允许覆盖的最大天数。 */
    public static final int MAX_SCHEDULE_DAYS = 31;

    /** 患者端单次查看号源允许覆盖的最大天数。 */
    public static final int MAX_PATIENT_SCHEDULE_QUERY_DAYS = 14;

    /** 患者已成功预约，等待到诊。 */
    public static final String APPOINTMENT_STATUS_BOOKED = "BOOKED";

    /** 患者已取消预约。 */
    public static final String APPOINTMENT_STATUS_CANCELLED = "CANCELLED";

    /** 患者已完成就诊。 */
    public static final String APPOINTMENT_STATUS_COMPLETED = "COMPLETED";

    /** 患者正在等待班次释放号源。 */
    public static final String WAITLIST_STATUS_WAITING = "WAITING";

    /** 患者已获得限时确认名额。 */
    public static final String WAITLIST_STATUS_OFFERED = "OFFERED";

    /** 候补名额已确认并完成挂号。 */
    public static final String WAITLIST_STATUS_CONFIRMED = "CONFIRMED";

    /** 候补 offer 已超时。 */
    public static final String WAITLIST_STATUS_EXPIRED = "EXPIRED";

    /** 患者主动取消候补。 */
    public static final String WAITLIST_STATUS_CANCELLED = "CANCELLED";
}
