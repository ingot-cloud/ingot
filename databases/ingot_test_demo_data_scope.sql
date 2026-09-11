-- 数据权限联调示例表（ingot-test / ingot_core_test）
CREATE TABLE IF NOT EXISTS `t_demo_order` (
    `id`            bigint       NOT NULL COMMENT '主键',
    `title`         varchar(128) NOT NULL COMMENT '标题',
    `dept_id`       bigint                DEFAULT NULL COMMENT '所属部门',
    `owner_user_id` bigint                DEFAULT NULL COMMENT '归属用户',
    `tenant_id`     bigint                DEFAULT NULL COMMENT '租户',
    PRIMARY KEY (`id`)
) COMMENT = '示例订单';

CREATE TABLE IF NOT EXISTS `t_demo_announcement` (
    `id`            bigint       NOT NULL COMMENT '主键',
    `title`         varchar(128) NOT NULL COMMENT '标题',
    `dept_id`       bigint                DEFAULT NULL COMMENT '所属部门',
    `owner_user_id` bigint                DEFAULT NULL COMMENT '归属用户',
    `tenant_id`     bigint                DEFAULT NULL COMMENT '租户',
    PRIMARY KEY (`id`)
) COMMENT = '示例公告';
