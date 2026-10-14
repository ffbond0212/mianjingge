package com.mianjingge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mianjingge.enums.QuestionCategory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 数据库存枚举名 */
    private QuestionCategory category;

    /** 逗号分隔 */
    private String tags;

    /** 1 简单 2 中等 3 困难 */
    private Integer difficulty;

    private String stdAnswer;

    private String myAnswer;

    /** PRESET 预置 / IMPORT 导入 / MANUAL 手动 */
    private String source;

    private Integer reviewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
