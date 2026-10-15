package com.mianjingge.vo;

import com.mianjingge.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationTimelineVO {

    private Long id;

    private ApplicationStatus fromStatus;

    private String fromStatusLabel;

    private ApplicationStatus toStatus;

    private String toStatusLabel;

    private String remark;

    private LocalDateTime createdAt;
}
