package com.healthy.appointment.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** MyBatis 查询结果对象，包含科室关联的医生数量。 */
@Data
public class DepartmentVO {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private Long doctorCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
