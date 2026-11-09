-- ingot-id MySQL 建表脚本（MySQL 8.0.16+）。
-- 在消费服务DataSource对应的数据库执行；不选库、不删除表、不写入业务数据。
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `fw_id_leaf_alloc` (
  `biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'Leaf业务号段名称，不同业务独立分配',
  `max_id` bigint NOT NULL DEFAULT '1' COMMENT '已分配号段的最大ID高水位，更新必须保持发号原子性',
  `step` int NOT NULL COMMENT '每次申请的号段长度',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务说明，可空',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '号段高水位最近更新时间点，UTC',
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Leaf业务号段分配高水位；所属模块：ingot-id';
