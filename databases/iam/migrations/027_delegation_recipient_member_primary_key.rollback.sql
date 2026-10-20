-- 仅由工具预检且停止全部写入后执行。保留业务关系/唯一键/外键/CHECK，不回退Leaf高水位。
ALTER TABLE iam_delegation_recipient_member DROP PRIMARY KEY, DROP COLUMN id;
