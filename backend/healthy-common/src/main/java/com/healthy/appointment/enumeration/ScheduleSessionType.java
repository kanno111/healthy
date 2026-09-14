package com.healthy.appointment.enumeration;

/** 出诊班次类型；OTHER 允许管理员填写自定义名称。 */
public enum ScheduleSessionType {
    MORNING("上午门诊"),
    AFTERNOON("下午门诊"),
    OTHER(null);

    private final String fixedName;

    ScheduleSessionType(String fixedName) {
        this.fixedName = fixedName;
    }

    public String fixedName() {
        return fixedName;
    }
}
