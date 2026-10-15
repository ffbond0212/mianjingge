package com.mianjingge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("interview")
public class Interview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String company;

    private String position;

    /** 轮次，如 一面/二面/HR面 */
    private String round;

    private LocalDate interviewDate;

    /** 整体总结 */
    private String summary;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
