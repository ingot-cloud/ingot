-- ingot-oss-common MySQL 建表脚本（MySQL 8.0.16+）。
-- 在消费服务DataSource对应的数据库执行；不选库、不删除表、不写入业务数据。
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS fw_oss_asset_upload (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '一次性上传凭证 ID',
    owner_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '服务端恢复的域、组织和操作者',
    purpose VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '固定业务用途编码',
    target_key VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '既有目标 ID 或空字符串',
    bucket_name VARCHAR(63) NOT NULL COMMENT '服务端选定的私有 bucket',
    object_name VARCHAR(255) NOT NULL COMMENT '服务端生成的对象名',
    state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '暂存状态，消费成功随业务事务移除',
    expires_at DATETIME(6) NOT NULL COMMENT '凭证到期时间，UTC',
    PRIMARY KEY (id),
    KEY idx_asset_upload_expires (expires_at, id),
    CONSTRAINT ck_asset_upload_state CHECK (state IN ('PENDING','READY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务图片暂存与一次性绑定凭证；所属模块：ingot-oss-common';
