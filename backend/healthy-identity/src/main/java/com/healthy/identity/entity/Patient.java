package com.healthy.identity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("patient")
public class Patient {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String realName;
    private Integer gender;
    private LocalDate birthday;
    private String idCard;
    private String medicalCardNo;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
