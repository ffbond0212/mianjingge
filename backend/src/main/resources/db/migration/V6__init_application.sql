-- M3-2 投递看板：投递表 + 状态流转时间线表
CREATE TABLE application (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    company      VARCHAR(100) NOT NULL COMMENT '公司（必填）',
    position     VARCHAR(100) NOT NULL COMMENT '岗位（必填）',
    channel      VARCHAR(50) NULL COMMENT '投递渠道',
    apply_date   DATE NULL COMMENT '投递日期',
    city         VARCHAR(50) NULL COMMENT '城市',
    salary       VARCHAR(100) NULL COMMENT '薪资范围',
    jd_url       VARCHAR(500) NULL COMMENT 'JD 链接',
    status       VARCHAR(20) NOT NULL DEFAULT 'APPLIED' COMMENT 'APPLIED/WRITTEN/FIRST/SECOND/THIRD/HR/OFFER/REJECTED',
    interview_id BIGINT NULL COMMENT '关联面试场次（interview.id）',
    remark       VARCHAR(500) NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '投递记录';

CREATE TABLE application_timeline (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    from_status    VARCHAR(20) NULL COMMENT '变更前状态（创建时为空）',
    to_status      VARCHAR(20) NOT NULL COMMENT '变更后状态',
    remark         VARCHAR(200) NULL COMMENT '备注',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_application_id (application_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '投递状态时间线';
