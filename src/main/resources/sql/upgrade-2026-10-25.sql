-- =====================================================================
-- 增量升级脚本（2026-10-25）
-- 适用于已经执行过 schema.sql 的数据库，只需执行一次；
-- 全新安装直接执行 schema.sql 即可（其中已包含以下字段）。
-- =====================================================================
USE association_archive;

-- 1. 档案条目增加创建人：草稿状态仅创建人本人可见
ALTER TABLE biz_archive
    ADD COLUMN create_by VARCHAR(32) NULL COMMENT '创建人用户 id（草稿仅本人可见）' AFTER task_code;

ALTER TABLE biz_archive ADD KEY idx_create_by (create_by);

-- 2. 历史数据回填：按协会归属取该会第一个账号，避免存量草稿变为不可见
UPDATE biz_archive a
JOIN (SELECT assoc_code, CAST(MIN(id) AS CHAR) AS uid
      FROM sys_user
      WHERE role_code = 'ASSOC'
      GROUP BY assoc_code) u
  ON a.assoc_code = u.assoc_code
SET a.create_by = u.uid
WHERE a.create_by IS NULL;

-- 3. 附件改为内容入库：数据库迁移/换机后附件仍可打开，不再依赖服务器本地目录
ALTER TABLE biz_attachment
    ADD COLUMN file_content LONGBLOB NULL COMMENT '附件二进制内容（随数据库迁移）' AFTER store_path;

-- 历史附件的磁盘路径保留，但允许为空（新附件不再写磁盘）
ALTER TABLE biz_attachment
    MODIFY COLUMN store_path VARCHAR(255) NULL COMMENT '旧版磁盘相对路径，仅历史数据使用';

-- 注意：单文件上限受 MySQL max_allowed_packet 限制（默认 64MB），
-- 如需更大附件可执行：SET GLOBAL max_allowed_packet = 128 * 1024 * 1024;

-- 4. 注册邀请码：主管部门发放，绑定角色与协会，12 小时内有效
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

