-- M3-1 复习计划：任务表 + 全局间隔配置表
CREATE TABLE review_plan (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id    BIGINT NOT NULL,
    question_snapshot VARCHAR(500) NOT NULL COMMENT '题目快照（题目删除后仍可显示）',
    stage          INT NOT NULL DEFAULT 0 COMMENT '当前档位 0..4，对应间隔数组下标',
    interval_days  INT NOT NULL COMMENT '当前间隔天数快照',
    next_review_date DATE NOT NULL COMMENT '下次复习日期',
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 复习中 / COMPLETED 已毕业归档',
    review_count   INT NOT NULL DEFAULT 0 COMMENT '已复习次数',
    last_score     TINYINT NULL COMMENT '最近一次打卡得分',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_next_review (next_review_date),
    KEY idx_question_id (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '复习计划';

CREATE TABLE review_config (
    id             INT PRIMARY KEY,
    intervals_json VARCHAR(200) NOT NULL COMMENT '遗忘曲线间隔（天）数组，如 [1,3,7,15,30]',
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '复习配置（单行 id=1）';

INSERT INTO review_config (id, intervals_json) VALUES (1, '[1,3,7,15,30]');
