package com.mianjingge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mianjingge.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application_timeline")
public class ApplicationTimeline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    /** 变更前状态（创建时为 null） */
    private ApplicationStatus fromStatus;

    private ApplicationStatus toStatus;

    private String remark;

    private LocalDateTime createdAt;
}
