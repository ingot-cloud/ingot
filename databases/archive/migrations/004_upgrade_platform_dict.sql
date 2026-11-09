-- ============================================================
-- 升级 platform_dict 字段为企业级字段方案
-- 版本: V4.0
-- 日期: 2026-04-25
-- 说明: 在 003 已将 meta_dict 重命名为 platform_dict 的基础上，
--       为字典表新增字典项展示、企业作用域、排序、扩展、审计等
--       字段，并补充常用查询索引。
-- ============================================================

USE ingot_core;

-- ============================================================
-- Step 1: 字段扩展（字典项展示、作用域、排序、扩展、审计）
-- ============================================================
ALTER TABLE `platform_dict`
    ADD COLUMN `value`       varchar(128) DEFAULT NULL COMMENT '字典项值（仅字典项有效）'                    AFTER `name`,
    ADD COLUMN `label`       varchar(128) DEFAULT NULL COMMENT '字典项展示文本（仅字典项有效）'              AFTER `value`,
    ADD COLUMN `scope_type`  char(1)      NOT NULL DEFAULT '0' COMMENT '作用域, 0:平台,1:租户,2:应用'        AFTER `type`,
    ADD COLUMN `tenant_id`   bigint       DEFAULT NULL COMMENT '租户ID（scope_type=1时必填）'                AFTER `scope_type`,
    ADD COLUMN `app_id`      bigint       DEFAULT NULL COMMENT '应用ID（scope_type=2时必填）'                AFTER `tenant_id`,
    MODIFY COLUMN `org_type` char(1)      NOT NULL DEFAULT '0' COMMENT '组织类型',
    ADD COLUMN `sort`        int          NOT NULL DEFAULT '0' COMMENT '排序权重'                            AFTER `org_type`,
    ADD COLUMN `system_flag` bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否内置字典'                       AFTER `sort`,
    ADD COLUMN `remark`      varchar(255) DEFAULT NULL COMMENT '备注'                                        AFTER `status`,
    ADD COLUMN `extra`       json         DEFAULT NULL COMMENT '扩展属性'                                    AFTER `remark`,
    ADD COLUMN `created_by`  bigint       DEFAULT NULL COMMENT '创建人'                                      AFTER `extra`,
    ADD COLUMN `updated_by`  bigint       DEFAULT NULL COMMENT '更新人'                                      AFTER `created_by`;

-- ============================================================
-- Step 2: 常用查询索引
-- ============================================================
ALTER TABLE `platform_dict`
    ADD INDEX `idx_dict_pid`         (`pid`)                                  USING BTREE,
    ADD INDEX `idx_dict_code`        (`code`)                                 USING BTREE,
    ADD INDEX `idx_dict_type_status` (`type`, `status`)                       USING BTREE,
    ADD INDEX `idx_dict_scope`       (`scope_type`, `tenant_id`, `app_id`)    USING BTREE;

-- ============================================================
-- 完成
-- ============================================================
SELECT 'Migration 004: upgrade platform_dict to enterprise schema completed.' AS result;
