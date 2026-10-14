-- 题库表
CREATE TABLE question
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title        VARCHAR(500)    NOT NULL COMMENT '题目',
    category     VARCHAR(32)     NOT NULL COMMENT '分类（QuestionCategory 枚举名）',
    tags         VARCHAR(255)    NULL COMMENT '标签，逗号分隔',
    difficulty   TINYINT         NOT NULL DEFAULT 2 COMMENT '难度 1简单 2中等 3困难',
    std_answer   TEXT            NULL COMMENT '标准答案',
    my_answer    TEXT            NULL COMMENT '个人话术版',
    source       VARCHAR(16)     NOT NULL DEFAULT 'MANUAL' COMMENT '来源 PRESET预置/IMPORT导入/MANUAL手动',
    review_count INT             NOT NULL DEFAULT 0 COMMENT '已复习次数（M3 使用）',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_source (source)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题库表';
