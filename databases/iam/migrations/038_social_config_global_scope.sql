-- 已有库允许平台全局社会化配置。显式选中已核对的 IAM 库后执行。
ALTER TABLE `iam_social_config`
    MODIFY COLUMN `tenant_id` bigint unsigned DEFAULT NULL COMMENT '租户归属；NULL 表示平台全局配置';
