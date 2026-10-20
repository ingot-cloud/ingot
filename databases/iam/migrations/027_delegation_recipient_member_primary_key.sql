-- 仅由 tools/iam/schema_key_upgrade.py 全量预检后执行；停止全部写入并备份。
-- Leaf max_id为排他上界；回填与预留同事务，高水位绝不回退。
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';
DROP PROCEDURE IF EXISTS iam_upgrade_027;
DELIMITER $$
CREATE PROCEDURE iam_upgrade_027()
BEGIN
    DECLARE pending_rows BIGINT DEFAULT 0;
    DECLARE reserved_start BIGINT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS iam_027_ids;
        RESIGNAL;
    END;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
                   AND table_name='iam_delegation_recipient_member' AND column_name='id') THEN
        ALTER TABLE iam_delegation_recipient_member
            ADD COLUMN id BIGINT UNSIGNED NULL COMMENT '委派接收成员关系记录标识；由IAM的Leaf发号器生成，不承载业务顺序';
    END IF;
    CREATE TEMPORARY TABLE iam_027_ids (
        delegation_id BIGINT UNSIGNED NOT NULL,
        member_id BIGINT UNSIGNED NOT NULL,
        row_id BIGINT UNSIGNED NOT NULL,
        PRIMARY KEY(delegation_id,member_id)
    );
    START TRANSACTION;
    SELECT max_id INTO reserved_start FROM biz_leaf_alloc WHERE biz_tag='iam' FOR UPDATE;
    SELECT COUNT(*) INTO pending_rows FROM iam_delegation_recipient_member WHERE id IS NULL;
    IF reserved_start <= 0 OR pending_rows > 9223372036854775807-reserved_start THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='027: invalid or exhausted IAM Leaf high watermark';
    END IF;
    IF pending_rows > 0 THEN
        UPDATE biz_leaf_alloc SET max_id=reserved_start+pending_rows WHERE biz_tag='iam';
        INSERT INTO iam_027_ids(delegation_id,member_id,row_id)
            SELECT delegation_id,member_id,reserved_start+ROW_NUMBER() OVER (ORDER BY delegation_id,member_id)-1
            FROM iam_delegation_recipient_member WHERE id IS NULL;
        UPDATE iam_delegation_recipient_member r JOIN iam_027_ids m
            ON r.delegation_id=m.delegation_id AND r.member_id=m.member_id SET r.id=m.row_id;
    END IF;
    COMMIT;
    DROP TEMPORARY TABLE iam_027_ids;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
                   AND table_name='iam_delegation_recipient_member' AND index_name='PRIMARY') THEN
        ALTER TABLE iam_delegation_recipient_member
            MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '委派接收成员关系记录标识；由IAM的Leaf发号器生成，不承载业务顺序',
            ADD PRIMARY KEY(id);
    END IF;
END$$
DELIMITER ;
CALL iam_upgrade_027();
DROP PROCEDURE iam_upgrade_027;
