-- L6: 登录强制滑块挑战种子
-- 目标库：ingot_security
-- 依赖：011/015 已存在 group_code=login-auth（POST /bff/auth/login）

USE ingot_security;

INSERT INTO `security_challenge_policy`
  (`code`, `group_code`, `trigger`, `challenge_type`, `pass_token_ttl_sec`, `pass_token_remaining`, `scope`, `enabled`, `priority`, `remark`)
SELECT 'login-always', 'login-auth', 'always', 'SLIDER', 300, 3, 'login', 1, 0, 'L6 登录强制滑块挑战'
WHERE NOT EXISTS (SELECT 1 FROM `security_challenge_policy` WHERE `code` = 'login-always');
