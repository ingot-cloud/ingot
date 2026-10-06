-- 仅示例独立业务数据库，生产迁移由接入服务管理，不放入IAM库。
CREATE TABLE ops_incident (
  id VARCHAR(36) PRIMARY KEY,
  owner_member_id VARCHAR(32) NOT NULL,
  title VARCHAR(200) NOT NULL,
  contact VARCHAR(128) NULL,
  INDEX idx_incident_owner(owner_member_id)
);
