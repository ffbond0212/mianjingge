-- 面试场次表
CREATE TABLE interview
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    company        VARCHAR(100)    NOT NULL COMMENT '公司',
    position       VARCHAR(100)    NOT NULL COMMENT '岗位',
    round          VARCHAR(50)     NULL COMMENT '轮次，如 一面/二面/HR面',
    interview_date DATE            NULL COMMENT '面试日期',
    summary        TEXT            NULL COMMENT '整体总结',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='面试场次表';

-- 面试题目记录表（一场面试中每道题的作答情况）
CREATE TABLE interview_question
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    interview_id       BIGINT UNSIGNED NOT NULL COMMENT '场次 id',
    question_id        BIGINT UNSIGNED NULL COMMENT '关联题库题目 id，现场新题未关联时为空',
    question_snapshot  VARCHAR(500)    NOT NULL COMMENT '现场题目原文快照',
    score              TINYINT         NULL COMMENT '得分 0-10，未答为空',
    answered           TINYINT         NOT NULL COMMENT '1 答上 2 部分答上 3 未答上',
    deduction          TEXT            NULL COMMENT '扣分点',
    my_answer_snapshot TEXT            NULL COMMENT '现场回答快照',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_interview_id (interview_id),
    KEY idx_question_id (question_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='面试题目记录表';
