-- 回滚 015_login_auth_group_pattern_fix.sql
-- 目标库：ingot_security
--
-- 还原 011 的分组内容。仅在需要回到 Phase 02 之前（TokenEndpoint 仍在、网关仍注册
-- /auth/token/**）时执行；否则该 pattern 没有保护对象。

USE ingot_security;

UPDATE `gateway_endpoint_group`
SET `pattern_list` = JSON_ARRAY(
      JSON_OBJECT('path', '/auth/token/**', 'method', 'POST'),
      JSON_OBJECT('path', '/bff/auth/login', 'method', 'POST')
    ),
    `remark` = 'L4 登录路径分组'
WHERE `code` = 'login-auth';
