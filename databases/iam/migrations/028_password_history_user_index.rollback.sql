-- 恢复原索引，不修改密码历史数据。
ALTER TABLE password_history ADD INDEX idx_user_id(user_id);
