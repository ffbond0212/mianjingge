package com.mianjingge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_config")
public class ReviewConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 遗忘曲线间隔（天）JSON 数组，如 [1,3,7,15,30] */
    private String intervalsJson;

    private LocalDateTime updatedAt;
}
