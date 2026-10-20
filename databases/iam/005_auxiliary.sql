-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：保留辅助领域结构，仅提取源结构定义，不包含数据、DROP 或 USE。
-- 这些旧列名继续表示新 Account/Tenant/Application/Plan 的映射 ID；真实映射在迁移阶段验证。
--
-- 归属说明：account_lock_state、password_history、password_expiration 的权威 DDL 由安全框架自带，
-- 不在本文件重复定义。部署 IAM 数据库时另行执行：
--   ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql
--   ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql
-- 表可以部署在 IAM 库，但持久化职责仍归框架适配器，IAM 不得新增同表实体或 Mapper。


CREATE TABLE `biz_leaf_alloc` (
  `biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'Leaf业务号段名称，不同业务独立分配',
  `max_id` bigint NOT NULL DEFAULT '1' COMMENT '已分配号段的最大ID高水位，更新必须保持发号原子性',
  `step` int NOT NULL COMMENT '每次申请的号段长度',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务说明，可空',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '号段高水位最近更新时间点，UTC',
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Leaf业务号段分配高水位';

CREATE TABLE `platform_dict` (
  `id` bigint NOT NULL COMMENT '多作用域字典及字典项记录标识',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父ID',
  `code` varchar(64) NOT NULL COMMENT '编码',
  `name` varchar(128) NOT NULL COMMENT '名称',
  `value` varchar(128) DEFAULT NULL COMMENT '字典项值（仅字典项有效）',
  `label` varchar(128) DEFAULT NULL COMMENT '字典项展示文本（仅字典项有效）',
  `type` char(1) NOT NULL COMMENT '字典节点类别：0字典类型、1字典项',
  `scope_type` char(1) NOT NULL DEFAULT '0' COMMENT '作用域, 0:平台,1:租户,2:应用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（scope_type=1时必填）',
  `app_id` bigint DEFAULT NULL COMMENT '作用域为应用时的IAM应用目录ID；其他作用域可空',
  `org_type` char(1) NOT NULL DEFAULT '0' COMMENT '组织类型：0平台、1组织',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `system_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置字典',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `extra` json DEFAULT NULL COMMENT '扩展属性',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime DEFAULT NULL COMMENT '记录创建时间点，UTC',
  `updated_at` datetime DEFAULT NULL COMMENT '记录最后更新时间点，UTC',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
  PRIMARY KEY (`id`),
  KEY `idx_dict_pid` (`pid`) USING BTREE,
  KEY `idx_dict_code` (`code`) USING BTREE,
  KEY `idx_dict_type_status` (`type`,`status`) USING BTREE,
  KEY `idx_dict_scope` (`scope_type`,`tenant_id`,`app_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多作用域字典及字典项';

CREATE TABLE `security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统一安全事件持久记录记录标识；数据库自增',
  `event_id` varchar(32) DEFAULT NULL COMMENT 'producer 幂等 ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `priority` varchar(16) NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
  `occurred_at` datetime DEFAULT NULL COMMENT '业务发生时间点，UTC',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端接收安全事件的时间点，UTC',
  `tenant_id` bigint DEFAULT NULL COMMENT '事件所属租户ID；NULL表示平台事件或未提供租户上下文',
  `user_id` bigint DEFAULT NULL COMMENT '安全事件关联的用户标识，结合user_type解释；可空',
  `user_type` varchar(20) DEFAULT NULL COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',
  `account` varchar(128) DEFAULT NULL COMMENT '事件涉及的账号显示标识，可空',
  `client_id` varchar(64) DEFAULT NULL COMMENT '认证客户端标识，可空',
  `app_id` varchar(64) DEFAULT NULL COMMENT '上报方提供的应用标识，可空',
  `session_id` varchar(64) DEFAULT NULL COMMENT '认证会话标识，可空',
  `device_id` varchar(128) DEFAULT NULL COMMENT '设备标识，可空',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP地址，可空',
  `request_uri` varchar(512) DEFAULT NULL COMMENT '触发事件的请求路径，可空',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '客户端User-Agent，可空',
  `result` varchar(20) DEFAULT NULL COMMENT '安全操作结果稳定标识，可空',
  `reason_code` varchar(50) DEFAULT NULL COMMENT '安全事件原因代码，可空',
  `reason_detail` varchar(500) DEFAULT NULL COMMENT '安全事件原因说明，不保存凭证原文',
  `source_module` varchar(64) NOT NULL COMMENT '上报模块',
  `source` varchar(50) DEFAULT NULL COMMENT '安全事件来源渠道或触发来源，可空',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人标识，可空',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名快照，可空',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '请求链路追踪标识，可空',
  `extension` json DEFAULT NULL COMMENT '安全事件扩展JSON，须遵守脱敏白名单',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_received_id` (`received_at`,`id`),
  KEY `idx_event_type` (`event_type`,`received_at`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB AUTO_INCREMENT=93243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件（canonical）';

CREATE TABLE `sys_social_details` (
  `id` bigint unsigned NOT NULL COMMENT '社交登录渠道配置记录标识',
  `tenant_id` bigint unsigned NOT NULL COMMENT '租户ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方平台分配的客户端App ID，不是IAM应用目录数字ID',
  `app_secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方应用密钥，可空；禁止在审计及字典示例中泄露',
  `redirect_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重定向地址',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '社交名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方登录渠道类型，对应社交登录渠道配置',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '记录创建时间点，UTC',
  `updated_at` datetime DEFAULT NULL COMMENT '记录最后更新时间点，UTC',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社交登录渠道配置';

CREATE TABLE `sys_user_social` (
  `id` bigint NOT NULL COMMENT '全局账号社交身份绑定记录标识',
  `tenant_id` bigint NOT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '被绑定的全局Account标识，旧列名保留；不表示租户成员ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '社交身份绑定时间点，UTC',
  PRIMARY KEY (`id`),
  KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全局账号社交身份绑定';

CREATE TABLE `sys_tenant_plan_record` (
  `id` bigint NOT NULL COMMENT '历史组织套餐购买记录记录标识',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `plan_id` int NOT NULL COMMENT '历史套餐标识，保留INT库存类型；不自动关联当前套餐目录',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历史套餐类别：0长期、1短期',
  `duration` int NOT NULL COMMENT '历史套餐购买时长数值，与unit共同表示有效时长',
  `unit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历史套餐时长单位：1月、2年',
  `created_at` datetime NOT NULL COMMENT '记录创建时间点，UTC',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史组织套餐购买记录';
