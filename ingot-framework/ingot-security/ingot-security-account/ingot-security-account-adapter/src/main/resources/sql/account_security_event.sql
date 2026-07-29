-- ============================================================
-- account_security_event 表 DDL
-- 模块: ingot-security-account-adapter
-- 说明: 账号安全事件审计表，统一记录认证、账号状态变更、凭证变更等安全事件。
--       引入 ingot-security-account-adapter 模块时需执行此脚本。
--
-- event_type / event_category 枚举值：
--   AUTH（认证事件）:
--     LOGIN_SUCCESS   - 登录成功
--     LOGIN_FAILURE   - 登录失败
--     LOGOUT          - 登出
--     TOKEN_REFRESH   - Token 刷新
--   ACCOUNT（账号事件）:
--     ACCOUNT_CREATED  - 账号创建
--     ACCOUNT_ENABLED  - 账号启用
--     ACCOUNT_DISABLED - 账号禁用
--     ACCOUNT_LOCKED   - 账号锁定
--     ACCOUNT_UNLOCKED - 账号解锁
--     ACCOUNT_DELETED  - 账号注销
--   CREDENTIAL（凭证事件）:
--     PASSWORD_CHANGED       - 密码修改
--     PASSWORD_RESET         - 密码重置
--     PASSWORD_EXPIRED       - 密码过期
--     FORCE_CHANGE_PASSWORD  - 强制修改密码
--
-- source 枚举值（同 EventSource）：
--   AUTH / PMS / MEMBER / SYSTEM
-- ============================================================

CREATE TABLE IF NOT EXISTS account_security_event (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id        BIGINT       NOT NULL               COMMENT '用户ID',
  user_type      VARCHAR(20)  NOT NULL               COMMENT '用户类型（同 UserTypeEnum.value：0-系统用户 1-C端用户）',

  -- 事件分类
  event_type     VARCHAR(50)  NOT NULL               COMMENT '事件类型（如 LOGIN_SUCCESS、ACCOUNT_LOCKED）',
  event_category VARCHAR(20)  NOT NULL               COMMENT '事件分类（AUTH / ACCOUNT / CREDENTIAL）',

  -- 事件详情
  reason_code    VARCHAR(50)  DEFAULT NULL            COMMENT '原因代码',
  reason_detail  VARCHAR(500) DEFAULT NULL            COMMENT '详细描述',
  result         VARCHAR(20)  DEFAULT NULL            COMMENT '结果（SUCCESS-成功 FAILURE-失败，状态变更类事件为 NULL）',

  -- 上下文信息
  source         VARCHAR(50)  DEFAULT NULL            COMMENT '来源（AUTH / PMS / MEMBER / SYSTEM）',
  operator_id    BIGINT       DEFAULT NULL            COMMENT '操作人ID（系统触发时为 NULL）',
  operator_name  VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名（系统触发时为 NULL）',
  client_ip      VARCHAR(64)  DEFAULT NULL            COMMENT '客户端IP',
  user_agent     VARCHAR(500) DEFAULT NULL            COMMENT '客户端 User-Agent',
  tenant_id      BIGINT       DEFAULT NULL            COMMENT '租户ID',

  -- 扩展字段
  extra_data     JSON         DEFAULT NULL            COMMENT '扩展数据（JSON）',

  -- 时间戳
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',

  PRIMARY KEY (id),
  KEY idx_user_event   (user_id, user_type, event_type, created_at) USING BTREE COMMENT '用户事件查询索引',
  KEY idx_created_at   (created_at)                                 USING BTREE COMMENT '时间范围查询索引',
  KEY idx_event_type   (event_type, created_at)                     USING BTREE COMMENT '事件类型聚合索引',
  KEY idx_tenant       (tenant_id, created_at)                      USING BTREE COMMENT '租户维度查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号安全事件表';
