package com.healthy.appointment.constant;

/**
 * 项目公共常量。
 */
public final class Constant {
    private Constant() {
    }

    /** 当前有效登录 Token 的 Redis 键前缀。 */
    public static final String AUTH_TOKEN_PREFIX = "auth:token:";

    /** 排班处于开放预约状态。 */
    public static final String SCHEDULE_STATUS_OPEN = "OPEN";

    /** 排班已由管理员关闭预约。 */
    public static final String SCHEDULE_STATUS_CLOSED = "CLOSED";

    /** 单次排班或查询允许覆盖的最大天数。 */
    public static final int MAX_SCHEDULE_DAYS = 31;

    /** 患者端单次查看号源允许覆盖的最大天数。 */
    public static final int MAX_PATIENT_SCHEDULE_QUERY_DAYS = 14;
}
