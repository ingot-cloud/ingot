-- L5: 会话安全 — 修正登录路径分组中已摘除的 /auth/token/**
-- 目标库：ingot_security
--
-- 011 的 login-auth 分组把 /auth/token/** 与 /bff/auth/login 一起纳入限流，
-- 但 TokenEndpoint 已随 Phase 02 删除、网关也不再注册该路由，规则护不到任何入口。
-- 只改仍含旧路径的行，已在控制台修过的环境不受影响。

USE ingot_security;

UPDATE `gateway_endpoint_group`
SET `pattern_list` = JSON_ARRAY(JSON_OBJECT('path', '/bff/auth/login', 'method', 'POST')),
    `remark` = '登录路径分组；登录入口统一在 BFF，/auth/token/** 已随 TokenEndpoint 摘除'
WHERE `code` = 'login-auth'
  AND JSON_SEARCH(`pattern_list`, 'one', '/auth/token/**') IS NOT NULL;
