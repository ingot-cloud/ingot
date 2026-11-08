-- 可选历史结构：仅供明确执行历史迁移/核对的环境使用，不属于新项目默认初始化。
-- 从 005_auxiliary.sql 原样迁入，不包含数据、删表、选库或自动升级。
SET time_zone = '+00:00';

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
