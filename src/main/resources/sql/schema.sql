-- =====================================================================
-- 中山商协会档案资料收集与服务系统 数据库脚本
-- 依据《需求规格说明书》5.2 核心数据表设计 / 5.3 编码规则
-- MySQL 8.0，库名 association_archive
-- =====================================================================
CREATE DATABASE IF NOT EXISTS association_archive
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE association_archive;

-- 用户账号（SRS 表 5-5 sys_user）
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 口令哈希',
    real_name     VARCHAR(32)  NULL,
    role_code     VARCHAR(16)  NOT NULL COMMENT 'ADMIN/LEADER/ASSOC',
    assoc_code    VARCHAR(16)  NULL COMMENT '协会角色所属协会编号',
    phone         VARCHAR(20)  NULL,
    email         VARCHAR(64)  NULL,
    wx_openid     VARCHAR(64)  NULL COMMENT '微信 openid',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 停用',
    fail_count    INT          NOT NULL DEFAULT 0,
    lock_until    DATETIME     NULL,
    last_login_at DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username),
    KEY idx_openid (wx_openid)
) COMMENT '用户账号';

-- 商协会主档（SRS 表 5-2 biz_association）
CREATE TABLE IF NOT EXISTS biz_association (
    assoc_code     VARCHAR(16) PRIMARY KEY COMMENT '协会编号',
    assoc_name     VARCHAR(100) NOT NULL,
    credit_code    CHAR(18)     NULL,
    industry_type  VARCHAR(32)  NULL COMMENT '行业类别：灯饰/五金/家电等',
    register_org   VARCHAR(64)  NULL,
    register_date  DATE         NULL,
    legal_person   VARCHAR(32)  NULL,
    contact_phone  VARCHAR(20)  NULL,
    activity_level TINYINT      NULL COMMENT '1 示范 2 成长 3 休眠',
    activity_score DECIMAL(5,2) NULL,
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 0 注销 2 异常',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    KEY idx_industry (industry_type)
) COMMENT '商协会主档（一协一档）';

-- 档案分类（SRS 表 5-5 biz_category，六大类两级）
CREATE TABLE IF NOT EXISTS biz_category (
    category_code     VARCHAR(16) PRIMARY KEY COMMENT '分类编码，如 B03',
    major_code        CHAR(1)  NOT NULL COMMENT '大类码 A-F',
    major_name        VARCHAR(20) NOT NULL,
    sub_code          VARCHAR(2)  NOT NULL,
    sub_name          VARCHAR(50) NOT NULL,
    default_retention VARCHAR(16) NULL COMMENT '永久/定期30年/定期10年',
    default_security_level TINYINT NULL COMMENT '1 公开 2 内部',
    required_meta_keys VARCHAR(255) NULL COMMENT '必填元数据项 JSON 数组',
    status            TINYINT NOT NULL DEFAULT 1,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT NOT NULL DEFAULT 0,
    KEY idx_major (major_code)
) COMMENT '档案分类';

-- 档案条目（SRS 表 5-1 biz_archive）
CREATE TABLE IF NOT EXISTS biz_archive (
    biz_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_id    VARCHAR(32)  NULL COMMENT 'ZSA-… 唯一标识，归档时生成',
    assoc_code    VARCHAR(16)  NOT NULL,
    category_code VARCHAR(16)  NOT NULL,
    major_code    CHAR(1)      NOT NULL,
    title         VARCHAR(200) NOT NULL,
    archive_year  CHAR(4)      NOT NULL,
    responsibility VARCHAR(64) NULL,
    security_level TINYINT     NOT NULL DEFAULT 1,
    retention     VARCHAR(16)  NULL,
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 1 待审核 2 退回 3 已归档',
    reject_reason VARCHAR(500) NULL,
    submit_by     VARCHAR(32)  NULL,
    submit_at     DATETIME     NULL,
    archived_by   VARCHAR(32)  NULL,
    archived_at   DATETIME     NULL,
    task_code     VARCHAR(16)  NULL COMMENT '来源补录任务',
    create_by     VARCHAR(32)  NULL COMMENT '创建人用户 id（草稿仅本人可见）',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_archive_id (archive_id),
    KEY idx_assoc_status (assoc_code, status),
    KEY idx_create_by (create_by),
    KEY idx_major_year (major_code, archive_year)
) COMMENT '档案条目';

-- 附件（SRS 表 5-3 biz_attachment）
CREATE TABLE IF NOT EXISTS biz_attachment (
    attach_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_id     BIGINT       NULL COMMENT '所属档案条目',
    file_name  VARCHAR(200) NOT NULL,
    store_path VARCHAR(255) NULL COMMENT '旧版磁盘相对路径，仅历史数据使用',
    file_content LONGBLOB   NULL COMMENT '附件二进制内容（随数据库迁移，换机后仍可打开）',
    file_hash  CHAR(64)     NOT NULL COMMENT 'SHA-256',
    file_type  VARCHAR(16)  NULL,
    file_size  BIGINT       NULL,
    upload_by  VARCHAR(32)  NULL,
    upload_at  DATETIME     NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    KEY idx_biz (biz_id)
) COMMENT '附件（哈希固证）';

-- 审核记录（SRS 表 5-5 biz_review）
CREATE TABLE IF NOT EXISTS biz_review (
    review_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_id        BIGINT      NOT NULL,
    archive_id    VARCHAR(32) NULL,
    node_level    TINYINT     NOT NULL DEFAULT 1 COMMENT '1 初审 2 复审',
    reviewer_id   BIGINT      NULL,
    reviewer_name VARCHAR(32) NULL,
    review_result TINYINT     NOT NULL COMMENT '1 通过 2 退回',
    review_opinion VARCHAR(500) NULL,
    review_at     DATETIME    NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    KEY idx_biz (biz_id)
) COMMENT '审核记录';

-- 借阅记录（SRS 表 5-4 biz_borrow）
CREATE TABLE IF NOT EXISTS biz_borrow (
    borrow_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_id          BIGINT      NOT NULL,
    archive_id      VARCHAR(32) NULL,
    applicant_id    BIGINT      NULL,
    applicant_name  VARCHAR(32) NULL,
    applicant_assoc VARCHAR(16) NULL,
    reason          VARCHAR(255) NOT NULL,
    borrow_at       DATETIME    NULL,
    due_at          DATETIME    NOT NULL,
    return_at       DATETIME    NULL,
    approve_by      BIGINT      NULL,
    approve_at      DATETIME    NULL,
    approve_result  TINYINT     NULL COMMENT '1 通过 2 拒绝',
    approve_opinion VARCHAR(255) NULL,
    borrow_status   TINYINT     NOT NULL DEFAULT 0 COMMENT '0 待审批 1 借阅中 2 已归还 3 逾期 4 已拒绝',
    trace_log       TEXT        NULL COMMENT '查阅留痕 JSON',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT     NOT NULL DEFAULT 0,
    KEY idx_applicant (applicant_assoc),
    KEY idx_status (borrow_status)
) COMMENT '借阅记录（全流程留痕）';

-- 专项补录任务（SRS 3.6 biz_task）
CREATE TABLE IF NOT EXISTS biz_task (
    task_code    VARCHAR(16) PRIMARY KEY,
    task_name    VARCHAR(100) NOT NULL,
    task_desc    VARCHAR(500) NULL,
    major_codes  VARCHAR(32)  NULL COMMENT '涉及大类码，逗号分隔',
    target_scope VARCHAR(16)  NOT NULL COMMENT 'ALL/INDUSTRY/LIST',
    target_value VARCHAR(500) NULL COMMENT '范围参数 JSON',
    start_time   DATETIME     NULL,
    end_time     DATETIME     NULL,
    created_by   VARCHAR(32)  NULL,
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1 进行中 2 结束 0 撤回',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0
) COMMENT '专项补录任务';

-- 操作审计日志（SRS 表 5-5 sys_audit_log，仅查不改删）
CREATE TABLE IF NOT EXISTS sys_audit_log (
    log_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    operate_by   VARCHAR(32)  NOT NULL,
    action       VARCHAR(64)  NOT NULL,
    params_brief VARCHAR(500) NULL,
    ip           VARCHAR(45)  NULL,
    operate_at   DATETIME     NOT NULL,
    result       VARCHAR(255) NULL,
    cost_ms      BIGINT       NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    KEY idx_operate_at (operate_at)
) COMMENT '操作审计日志';

-- 政策推送（SRS 表 5-5 biz_policy_push）
CREATE TABLE IF NOT EXISTS biz_policy_push (
    push_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    content      TEXT         NULL,
    attach_path  VARCHAR(255) NULL,
    target_scope VARCHAR(16)  NOT NULL COMMENT 'ALL/INDUSTRY/LEVEL/LIST',
    target_value VARCHAR(500) NULL,
    created_by   VARCHAR(32)  NULL,
    published_at DATETIME     NULL,
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 1 已发布 2 已撤回',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0
) COMMENT '政策推送';

-- 注册邀请码（SRS FR-01）：主管部门发放，绑定角色与协会，12 小时内有效
CREATE TABLE IF NOT EXISTS sys_invite_code (
    invite_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(16)  NOT NULL COMMENT '8 位邀请码，全局唯一',
    role_code   VARCHAR(16)  NOT NULL COMMENT 'ADMIN/LEADER/ASSOC',
    assoc_code  VARCHAR(16)  NULL COMMENT '商协会管理员注册后加入的协会编号',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未使用 1 已使用',
    expires_at  DATETIME     NOT NULL COMMENT '创建后 12 小时过期',
    used_by     BIGINT       NULL COMMENT '使用人用户 id',
    used_at     DATETIME     NULL,
    created_by  VARCHAR(32)  NULL COMMENT '发放人用户 id',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_invite_code (code),
    KEY idx_expires (expires_at)
) COMMENT '注册邀请码';
