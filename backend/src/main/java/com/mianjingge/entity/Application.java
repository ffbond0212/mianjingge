package com.mianjingge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mianjingge.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("application")
public class Application {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String company;

    private String position;

    /** 投递渠道 */
    private String channel;

    private LocalDate applyDate;

    private String city;

    /** 薪资范围（自由文本） */
    private String salary;

    private String jdUrl;

    private ApplicationStatus status;

    /** 关联面试场次 */
    private Long interviewId;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
