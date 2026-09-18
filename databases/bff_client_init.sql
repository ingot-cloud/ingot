-- ============================================================
-- BFF 双入口 OAuth 公开客户端
-- 库：ingot_auth
-- 日期：2026-09-17
--
-- 与 Nacos in-bff-apps.yml 的 oauth-client-id 对齐：
--   in-bff-platform / in-bff-tenant
-- PKCE 公开客户端，无 client_secret。
-- 可重复执行：已存在同 client_id 且未删除的行则跳过。
-- 内网自定义 BFF 回调时，把对应 https origin 追加进 redirect_uris。
-- ============================================================

USE ingot_auth;

SET NAMES utf8mb4;

INSERT INTO `oauth2_registered_client` (
    `id`,
    `client_id`,
    `client_id_issued_at`,
    `client_secret`,
    `client_secret_expires_at`,
    `client_name`,
    `client_authentication_methods`,
    `authorization_grant_types`,
    `redirect_uris`,
    `post_logout_redirect_uris`,
    `scopes`,
    `client_settings`,
    `token_settings`,
    `updated_at`,
    `deleted_at`
)
SELECT
    'in-bff-platform',
    'in-bff-platform',
    NOW(),
    NULL,
    NULL,
    'BFF平台入口',
    'none,pre_auth_none',
    'pre_authorization_code,authorization_code',
    'http://localhost:5400/bff/auth/platform/callback,https://bff.ingotcloud.top/bff/auth/platform/callback',
    NULL,
    'system',
    '{"@class":"java.util.Collections$UnmodifiableMap","ingot.settings.client.status":"0","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000],"ingot.settings.token.auth-type":"1","settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}',
    NOW(),
    NULL
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `oauth2_registered_client`
    WHERE `client_id` = 'in-bff-platform'
      AND `deleted_at` IS NULL
);

INSERT INTO `oauth2_registered_client` (
    `id`,
    `client_id`,
    `client_id_issued_at`,
    `client_secret`,
    `client_secret_expires_at`,
    `client_name`,
    `client_authentication_methods`,
    `authorization_grant_types`,
    `redirect_uris`,
    `post_logout_redirect_uris`,
    `scopes`,
    `client_settings`,
    `token_settings`,
    `updated_at`,
    `deleted_at`
)
SELECT
    'in-bff-tenant',
    'in-bff-tenant',
    NOW(),
    NULL,
    NULL,
    'BFF租户入口',
    'none,pre_auth_none',
    'pre_authorization_code,authorization_code',
    'http://localhost:5400/bff/auth/tenant/callback,https://bff.ingotcloud.top/bff/auth/tenant/callback',
    NULL,
    'system',
    '{"@class":"java.util.Collections$UnmodifiableMap","ingot.settings.client.status":"0","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000],"ingot.settings.token.auth-type":"1","settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000]}',
    NOW(),
    NULL
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `oauth2_registered_client`
    WHERE `client_id` = 'in-bff-tenant'
      AND `deleted_at` IS NULL
);
