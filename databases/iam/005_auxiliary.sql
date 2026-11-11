-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 自有辅助结构：多作用域字典、社交渠道配置和账号社交绑定。
-- 6张共享框架表由所属library的sql/schema.sql维护，manifest统一投影到完整初始化。
-- 完整部署只执行databases/ingot_iam.sql；依赖与脚本对应关系见databases/README.md。

CREATE TABLE `iam_dictionary` (
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

CREATE TABLE `iam_social_config` (
  `id` bigint unsigned NOT NULL COMMENT '社交登录渠道配置记录标识',
  `tenant_id` bigint unsigned DEFAULT NULL COMMENT '租户归属；NULL 表示平台全局配置',
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

CREATE TABLE `iam_account_social_binding` (
  `id` bigint NOT NULL COMMENT '全局账号社交身份绑定记录标识',
  `tenant_id` bigint NOT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '被绑定的全局Account标识，旧列名保留；不表示租户成员ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '社交身份绑定时间点，UTC',
  PRIMARY KEY (`id`),
  KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全局账号社交身份绑定';
