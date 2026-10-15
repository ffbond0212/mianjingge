package com.mianjingge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("review_plan")
public class ReviewPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    /** 题目快照（题目删除后仍可显示） */
    private String questionSnapshot;

    /** 当前档位，对应间隔数组下标 */
    private Integer stage;

    /** 当前间隔天数快照 */
    private Integer intervalDays;

    private LocalDate nextReviewDate;

    /** ACTIVE 复习中 / COMPLETED 已毕业归档 */
    private String status;

    private Integer reviewCount;

    private Integer lastScore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
