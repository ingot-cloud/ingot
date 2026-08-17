package com.ingot.cloud.bff.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.bff.client.AuthClient;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.oauth2.core.endpoint.PreAuthorizationGrantType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * <p>BFF OAuth2 登录编排服务，封装完整的预授权→授权码→Token 流程</p>
 *
 * <p>前端只需提交业务参数（账号密码 / 租户选择），所有 OAuth2 参数（PKCE、state、
 * redirect_uri）均由本服务内部生成和管理，前端不接触任何 OAuth2 细节。
 * BFF 作为 PKCE 公开客户端（{@code client_authentication_method=none}），不需要 client_secret。</p>
 *
 * <p>三段式登录流程：</p>
 * <ol>
 *     <li>{@link #login} — 预授权：账号密码认证，返回可选租户列表</li>
 *     <li>{@link #selectTenant} — 选租户：获取授权码 + 换取 Token，Token 存 Redis</li>
 *     <li>{@link #logout} — 登出：撤销 auth Token + 清除 BFF session</li>
 * </ol>
 *
 * @author jy
 * @implNote redirect_uri 通过 {@link BffProperties#getRedirectUri()} 配置，
 * 必须与 oauth2_registered_client 表中注册的值一致。
 * PKCE code_verifier 暂存在 BffSession 的 accessToken 字段中
 * （登录成功后会被真正的 JWT 覆盖）。
 * Auth 服务的 JSESSIONID 暂存在 BffSession 的 authCookie 字段中，
 * 用于 authorize 调用时恢复 Auth 的 SecurityContext。
 * @see AuthClient
 * @see BffSessionService
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BffAuthService {
    private static final String CODE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    private static final String MSG_ACCOUNT_LOCKED = "账号已被锁定，请联系管理员";

    private final BffProperties properties;
    private final BffSessionService sessionService;
    private final AuthClient authClient;
    private final AccountLockSignalPort accountLockSignalPort;
    private final AccountLockBffProperties accountLockBffProperties;

    /**
     * 第一步：登录（预授权），返回可选租户列表。
     * <p>
     * 前端只传 username/password/vcCode，BFF 自动生成 PKCE、state 等 OAuth2 参数，
     * 并通过 Feign 调用 auth 服务的 pre_authorize 接口。
     * Auth 返回的 JSESSIONID cookie 会被捕获并暂存到 BFF Session，
     * 供后续 selectTenant 调用时转发给 Auth 恢复 SecurityContext。
     */
    public R<?> login(BffLoginDTO dto, HttpServletRequest request, HttpServletResponse response) {
        if (accountLockBffProperties.isEnabled()
                && !accountLockBffProperties.isEmitLoginFailureOnBffBlock()
                && isAccountLocked(dto.getUsername())) {
            log.info("[BffAuth] account locked, skip pre_authorize username={}", dto.getUsername());
            return R.error(CODE_ACCOUNT_LOCKED, MSG_ACCOUNT_LOCKED);
        }

        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state = generateState();
        String redirectUri = properties.getRedirectUri();

        Map<String, String> formData = new HashMap<>();
        formData.put("username", dto.getUsername());
        formData.put("password", dto.getPassword());

        ResponseEntity<R<Map<String, Object>>> responseEntity;
        try {
            responseEntity = authClient.preAuthorize(null,
                    properties.getUserType(), PreAuthorizationGrantType.PASSWORD.value(),
                    properties.getClientId(), codeChallenge,
                    "code", redirectUri, properties.getScope(), state,
                    formData);
        } catch (Exception e) {
            log.error("[BffAuth] pre_authorize failed", e);
            return R.error500(e.getMessage());
        }

        R<Map<String, Object>> result = responseEntity.getBody();
        if (result == null) {
            log.debug("[BffAuth] pre_authorize returned empty body");
            return R.error500();
        }

        if (result.isSuccess()) {
            String authCookie = extractAuthCookie(responseEntity.getHeaders());

            BffSession session = new BffSession();
            session.setClientId(properties.getClientId());
            session.setAccessToken(codeVerifier);
            session.setRefreshToken(state + "|" + redirectUri);
            session.setAuthCookie(authCookie);

            String sessionId = sessionService.createSession(session, request, response);
            log.info("[BffAuth] login success, sessionId={}, authCookie={}", sessionId,
                    StrUtil.isNotEmpty(authCookie) ? "captured" : "missing");
        }

        return result;
    }

    /**
     * 第二步：选择租户，完成 authorize + token 换取。
     * <p>
     * 前端只传 tenantId 和可选的 redirectUri，BFF 从 session 中恢复 PKCE 参数和 Auth 的 session cookie，
     * 依次完成授权码获取和 Token 换取。成功后响应中返回校验过的 redirectUri 供前端跳转。
     *
     * @param frontRedirectUri 前端传入的登录后跳转地址（可选），后端校验白名单后原样返回
     */
    public R<?> selectTenant(String tenantId, String frontRedirectUri, HttpServletRequest request, HttpServletResponse response) {
        BffSession session = sessionService.getSession(request);
        if (session == null) {
            return R.error("S0401", "session not found, please login first");
        }

        String sessionId = sessionService.getSessionIdFromCookie(request);
        String codeVerifier = session.getAccessToken();
        String[] stateAndUri = session.getRefreshToken().split("\\|", 2);
        String state = stateAndUri[0];
        String redirectUri = stateAndUri.length > 1 ? stateAndUri[1] : properties.getRedirectUri();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        // 使用 Auth 服务的 session cookie（预授权阶段捕获），而非浏览器的 cookie
        String authCookie = session.getAuthCookie();

        R<Map<String, Object>> authorizeResult;
        try {
            authorizeResult = authClient.authorize(
                    authCookie, PreAuthorizationGrantType.PASSWORD.value(),
                    tenantId, properties.getClientId(), codeChallenge,
                    "code", redirectUri, properties.getScope(), state);
        } catch (Exception e) {
            log.error("[BffAuth] authorize failed", e);
            return R.error500(e.getMessage());
        }

        if (!authorizeResult.isSuccess()) {
            return authorizeResult;
        }

        String code = String.valueOf(authorizeResult.getData().get("code"));

        Map<String, String> tokenForm = new HashMap<>();
        tokenForm.put("code", code);
        tokenForm.put("grant_type", "authorization_code");
        tokenForm.put("code_verifier", codeVerifier);
        tokenForm.put("client_id", properties.getClientId());
        tokenForm.put("redirect_uri", redirectUri);

        R<Map<String, Object>> tokenResult;
        try {
            tokenResult = authClient.token(tokenForm);
        } catch (Exception e) {
            log.error("[BffAuth] token exchange failed", e);
            return R.error500(e.getMessage());
        }

        if (!tokenResult.isSuccess()) {
            return tokenResult;
        }

        Map<String, Object> tokenData = tokenResult.getData();
        String accessToken = (String) tokenData.get("accessToken");
        String refreshToken = tokenData.get("refreshToken") != null ? (String) tokenData.get("refreshToken") : "";
        long expiresIn = Long.parseLong(String.valueOf(tokenData.get("expiresIn")));

        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setExpiresAt(Instant.now().getEpochSecond() + expiresIn);
        session.setTenantId(tenantId);
        session.setAuthCookie(authCookie);
        sessionService.updateSession(sessionId, session, expiresIn, response);

        log.info("[BffAuth] selectTenant success, sessionId={}, tenantId={}", sessionId, tenantId);

        String validatedRedirectUri = resolveAndValidateRedirectUri(frontRedirectUri);
        if (validatedRedirectUri != null) {
            return R.ok(Map.of("redirectUri", validatedRedirectUri));
        }
        return R.ok();
    }


    /**
     * 登出：撤销 auth token + 清除 BFF session。
     */
    public R<?> logout(HttpServletRequest request, HttpServletResponse response) {
        BffSession session = sessionService.getSession(request);
        if (session != null && StrUtil.isNotEmpty(session.getAccessToken())) {
            try {
                authClient.revokeToken(session.getAuthCookie(), "Bearer " + session.getAccessToken());
            } catch (Exception e) {
                log.warn("[BffAuth] token revoke failed on auth service", e);
            }
        }
        sessionService.removeSession(request, response);
        return R.ok();
    }

    // ---- helpers ----

    /**
     * 解析并校验前端传入的登录后重定向 URI。
     * 优先使用前端传入值，未传时使用 defaultRedirectUri 配置。
     * 如果配置了白名单，则验证 URI 必须以白名单中的某个条目为前缀。
     *
     * @return 校验通过的 URI；无 URI 或校验失败返回 null
     */
    private String resolveAndValidateRedirectUri(String frontRedirectUri) {
        String uri = StrUtil.isNotEmpty(frontRedirectUri)
                ? frontRedirectUri
                : properties.getSecurity().getDefaultRedirectUri();

        if (StrUtil.isEmpty(uri)) {
            return null;
        }

        List<String> allowedFrontends = properties.getSecurity().getAllowedFrontends();
        if (allowedFrontends == null || allowedFrontends.isEmpty()) {
            return uri;
        }

        for (String allowed : allowedFrontends) {
            if (uri.startsWith(allowed)) {
                return uri;
            }
        }

        log.warn("[BffAuth] redirect_uri rejected by whitelist: {}", uri);
        return null;
    }

    private boolean isAccountLocked(String username) {
        if (StrUtil.isBlank(username)) {
            return false;
        }
        UserTypeEnum userType = UserTypeEnum.getEnum(properties.getUserType());
        if (userType == null) {
            userType = UserTypeEnum.ADMIN;
        }
        return accountLockSignalPort.isLockedByUsername(userType, username);
    }

    /**
     * 从 Auth 服务响应头中提取 JSESSIONID cookie，
     * 格式化为可直接用作 Cookie 请求头的字符串。
     */
    private String extractAuthCookie(HttpHeaders headers) {
        List<String> setCookies = headers.get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) {
            return null;
        }
        for (String setCookie : setCookies) {
            if (setCookie.startsWith("JSESSIONID=")) {
                String value = setCookie.split(";")[0];
                return value;
            }
        }
        return null;
    }

    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
