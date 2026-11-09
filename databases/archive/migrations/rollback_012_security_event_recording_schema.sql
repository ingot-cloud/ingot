-- 回滚 012：删除 PMS/Member canonical 表；中心库仅删索引/列需手工评估（保留数据）

USE ingot_core;
DROP TABLE IF EXISTS `security_event`;

USE ingot_member;
DROP TABLE IF EXISTS `security_event`;

-- ingot_security：生产环境通常保留 additive 列；如需回滚：
-- ALTER TABLE security_event DROP INDEX uk_event_id;
-- ALTER TABLE security_event DROP INDEX idx_received_id;
-- ALTER TABLE security_event DROP COLUMN priority;
-- ALTER TABLE security_event DROP COLUMN event_id;
