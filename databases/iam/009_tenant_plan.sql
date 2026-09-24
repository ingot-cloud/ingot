ALTER TABLE iam_tenant
    ADD COLUMN plan_id BIGINT UNSIGNED NULL COMMENT '最近一次提交的套餐；修改套餐目录不自动回写' AFTER owner_member_id;
