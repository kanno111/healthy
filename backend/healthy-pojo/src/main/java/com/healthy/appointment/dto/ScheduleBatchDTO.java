package com.healthy.appointment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 管理端批量生成医生号源的输入参数。 */
@Data
public class ScheduleBatchDTO {
    @NotNull(message = "医生不能为空")
    private Long doctorId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotEmpty(message = "至少选择一个出诊星期")
    private List<@NotNull @Min(1) @Max(7) Integer> weekdays;

    @NotEmpty(message = "至少配置一个出诊时段")
    private List<@Valid Session> sessions;

    /** 一个连续出诊班次，例如上午门诊或下午门诊。 */
    @Data
    public static class Session {
        @NotBlank(message = "班次类型不能为空")
        private String sessionType;

        @Size(max = 20, message = "自定义班次名称不能超过20个字符")
        private String customSessionName;

        @NotNull(message = "开始时间不能为空")
        private LocalTime startTime;

        @NotNull(message = "结束时间不能为空")
        private LocalTime endTime;

        @NotNull(message = "班次总号源不能为空")
        @Min(value = 1, message = "班次总号源必须大于 0")
        private Integer capacity;

        @NotNull(message = "平均接诊时长不能为空")
        @Min(value = 1, message = "平均接诊时长必须大于 0")
        @Max(value = 60, message = "平均接诊时长不能超过60分钟")
        private Integer averageConsultationMinutes;
    }
}
