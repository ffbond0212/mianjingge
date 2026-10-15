package com.mianjingge.vo;

import com.mianjingge.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ApplicationVO {

    private Long id;

    private String company;

    private String position;

    private String channel;

    private LocalDate applyDate;

    private String city;

    private String salary;

    private String jdUrl;

    private ApplicationStatus status;

    private String statusLabel;

    /** 关联面试场次 id（用于直达复盘） */
    private Long interviewId;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
