-- 仅由 tools/iam/schema_key_upgrade.py 全量预检后执行；真实MySQL索引对照证据见对应SDD变更。
-- 保留uk_user_sequence(user_id,sequence_number)、PRIMARY及idx_created_at。
ALTER TABLE password_history DROP INDEX idx_user_id;
