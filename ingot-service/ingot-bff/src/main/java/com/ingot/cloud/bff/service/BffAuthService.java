package com.ingot.cloud.bff.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.InJwtClaimNames;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.JwtPayloadUtil;
import com.ingot.framework.feign.exception.InFeignException;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.oauth2.core.endpoint.PreAuthorizationGrantType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.stereotype.Service;

/**
 * 双入口 BFF 登录编排：事务、凭证、交接与正式会话。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BffAuthService {
    private static final String STAGE_LOGIN = "LOGIN";
    private static final String STAGE_SELECT_TENANT = "SELECT_TENANT";
    private static final String STAGE_READY = "READY";
    private static final String CODE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    private static final String MSG_ACCOUNT_LOCKED = "账号已被锁定，请联系管理员";
    private static final String AUTH_COOKIE_NAME = "JSESSIONID";
    private static final String CODE_CHALLENGE_ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BffProperties properties;
    private final BffAppRegistry appRegistry;
    private final BffSessionService sessionService;
    private final LoginTransactionService transactionService;
    private final RemoteAuthTokenService remoteAuthTokenService;
    private final RemoteAuthSessionService remoteAuthSessionService;
    private final AccountLockSignalPort accountLockSignalPort;
    private final AccountLockBffProperties accountLockBffProperties;

    /**
     * 签发本 host 的 CSRF，并轮换浏览器绑定。
     *
     * <p>请求若已携带绑定 Cookie，先删除 Redis 中该绑定，再写入新绑定与 Cookie。
     * 旧 token 不得继续用于后续状态修改请求。</p>
     *
     * @param request  当前请求，用于校验入口及读取旧绑定 Cookie
     * @param response 写入新的绑定 Cookie
     * @return 仅含 {@code csrfToken} 的 data
     */
    public Map<String, Object> issueCsrf(HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = appRegistry.requireFromRequest(request);
        if (BffConstants.ENTRY_ADMIN.equals(appRegistry.requireEntryRole(request))) {
            appRegistry.requireAdmin(request, app);
        } else {
            appRegistry.requireLogin(request, app);
        }
        transactionService.deleteBinding(sessionService.getBindingIdFromCookie(request));
        AuthBinding binding = new AuthBinding();
        binding.setBindingId(randomToken(16));
        binding.setAppId(app.getAppId());
        binding.setCsrfToken(randomToken(24));
        transactionService.saveBinding(binding, transactionService.ttlSeconds());
        sessionService.writeBindingCookie(binding.getBindingId(), transactionService.ttlSeconds(), response);
        return Map.of("csrfToken", binding.getCsrfToken());
    }

    public Map<String, Object> createTransaction(String entry, HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = requireEntryApp(entry, request);
        appRegistry.requireAdmin(request, app);
        AuthBinding binding = requireCsrf(request, app);
        LoginTransaction transaction = new LoginTransaction();
        transaction.setTransactionId(randomToken(16));
        transaction.setAppId(app.getAppId());
        transaction.setDomain(app.getDomain());
        transaction.setEntry(entry);
        transaction.setStage(STAGE_LOGIN);
        transaction.setAdminBindingId(binding.getBindingId());
        transaction.setCsrfToken(binding.getCsrfToken());
        transaction.setCodeVerifier(randomToken(32));
        transaction.setState(randomToken(8));
        transaction.setExpiresAt(Instant.now().getEpochSecond() + transactionService.ttlSeconds());
        transactionService.save(transaction);
        return Map.of(
                "transactionId", transaction.getTransactionId(),
                "loginUrl", appRegistry.loginUrl(app, transaction.getTransactionId()));
    }

    public Map<String, Object> readTransaction(String entry, String transactionId, HttpServletRequest request) {
        BffAppRegistration app = requireEntryApp(entry, request);
        appRegistry.requireLogin(request, app);
        LoginTransaction transaction = requireTransaction(transactionId, app);
        bindLoginBrowser(transaction, request, app);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionId", transaction.getTransactionId());
        data.put("stage", transaction.getStage());
        data.put("expiresAt", transaction.getExpiresAt());
        if (STAGE_SELECT_TENANT.equals(transaction.getStage()) && transaction.getAllows() != null) {
            data.put("allows", summarizeAllows(transaction.getAllows()));
        }
        if (STAGE_READY.equals(transaction.getStage()) && StrUtil.isNotBlank(transaction.getTicket())) {
            data.put("completionUrl", appRegistry.completionUrl(app, transaction.getTicket()));
        }
        return data;
    }

    public R<?> login(String entry, BffLoginDTO dto, HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = requireEntryApp(entry, request);
        appRegistry.requireLogin(request, app);
        LoginTransaction transaction = requireTransaction(dto.getTransactionId(), app);
        requireCsrf(request, app);
        requireLoginBinding(transaction, request, app);
        if (!STAGE_LOGIN.equals(transaction.getStage())) {
            throw new BffAuthException(BffErrorCode.TRANSACTION_CONFLICT);
        }
        if (accountLockBffProperties.isEnabled()
                && !accountLockBffProperties.isEmitLoginFailureOnBffBlock()
                && isAccountLocked(dto.getUsername())) {
            return R.error(CODE_ACCOUNT_LOCKED, MSG_ACCOUNT_LOCKED);
        }

        AuthorizationDomain domain = app.getDomain();
        String codeChallenge = generateCodeChallenge(transaction.getCodeVerifier());
        Map<String, String> formData = new HashMap<>();
        formData.put(OAuth2ParameterNames.USERNAME, dto.getUsername());
        formData.put(OAuth2ParameterNames.PASSWORD, dto.getPassword());
        formData.put(InOAuth2ParameterNames.DOMAIN, domain.getValue());

        ResponseEntity<R<Map<String, Object>>> responseEntity;
        try {
            responseEntity = remoteAuthTokenService.preAuthorize(null,
                    properties.getUserType(), PreAuthorizationGrantType.PASSWORD.value(),
                    app.getOauthClientId(), codeChallenge,
                    OAuth2AuthorizationResponseType.CODE.getValue(),
                    app.getOauthRedirectUri(), app.getOauthScope(), transaction.getState(),
                    domain.getValue(), formData);
        } catch (Exception exception) {
            return authRpcFailure(exception, "pre_authorize");
        }
        R<Map<String, Object>> result = responseEntity.getBody();
        if (result == null || !result.isSuccess()) {
            return result == null ? R.error500() : result;
        }
        transaction.setAuthCookie(extractAuthCookie(responseEntity.getHeaders()));
        List<TenantMainDTO> allows = parseAllows(result.getData());
        if (domain == AuthorizationDomain.PLATFORM) {
            return completeAuthorize(transaction, app, null, request, response);
        }
        if (CollUtil.isEmpty(allows)) {
            throw new BffAuthException(BffErrorCode.IDENTITY_UNAVAILABLE);
        }
        if (allows.size() == 1) {
            return completeAuthorize(transaction, app, allows.get(0).getId(), request, response);
        }
        transaction.setStage(STAGE_SELECT_TENANT);
        transaction.setAllows(allows);
        transactionService.save(transaction);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", STAGE_SELECT_TENANT);
        data.put("transactionId", transaction.getTransactionId());
        data.put("allows", summarizeAllows(allows));
        return R.ok(data);
    }

    public R<?> selectTenant(String tenantId, String transactionId, HttpServletRequest request,
            HttpServletResponse response) {
        BffAppRegistration app = requireEntryApp(BffConstants.ENTRY_TENANT, request);
        appRegistry.requireLogin(request, app);
        LoginTransaction transaction = requireTransaction(transactionId, app);
        requireCsrf(request, app);
        requireLoginBinding(transaction, request, app);
        if (!STAGE_SELECT_TENANT.equals(transaction.getStage())) {
            throw new BffAuthException(BffErrorCode.TRANSACTION_CONFLICT);
        }
        if (StrUtil.isBlank(tenantId) || transaction.getAllows() == null
                || transaction.getAllows().stream().noneMatch(item -> StrUtil.equals(item.getId(), tenantId))) {
            throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
        }
        return completeAuthorize(transaction, app, tenantId, request, response);
    }

    public Map<String, Object> complete(String entry, String ticket, HttpServletRequest request,
            HttpServletResponse response) {
        BffAppRegistration app = requireEntryApp(entry, request);
        appRegistry.requireAdmin(request, app);
        requireCsrf(request, app);
        if (StrUtil.isBlank(ticket)) {
            throw new BffAuthException(BffErrorCode.TICKET_INVALID);
        }
        LoginTransaction transaction = transactionService.require(
                transactionService.requireTransactionIdByTicket(ticket));
        if (transaction == null || !STAGE_READY.equals(transaction.getStage())
                || !StrUtil.equals(transaction.getAppId(), app.getAppId())
                || transaction.getTicketExpiresAt() <= Instant.now().getEpochSecond()) {
            throw new BffAuthException(BffErrorCode.TICKET_INVALID);
        }
        AuthBinding binding = transactionService.requireBinding(sessionService.getBindingIdFromCookie(request));
        if (!StrUtil.equals(binding.getBindingId(), transaction.getAdminBindingId())) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
        BffSession session = new BffSession();
        session.setAccessToken(transaction.getAccessToken());
        session.setRefreshToken(transaction.getRefreshToken());
        session.setSid(transaction.getSid());
        session.setUserId(transaction.getUserId());
        session.setTenantId(transaction.getTenantId());
        session.setClientId(app.getOauthClientId());
        session.setAppId(app.getAppId());
        session.setDomain(app.getDomain().getValue());
        session.setAuthCookie(transaction.getAuthCookie());
        long ttl = Math.max(60, properties.getSessionTtl());
        sessionService.createSession(session, request, response);
        transactionService.saveBinding(binding, ttl);
        sessionService.writeBindingCookie(binding.getBindingId(), ttl, response);
        transactionService.deleteTicket(ticket);
        transactionService.delete(transaction.getTransactionId());
        return Map.of("returnTo", app.getDefaultReturnTo());
    }

    /**
     * 撤销当前应用会话并清除本 host 的正式 Cookie 与绑定 Cookie。
     *
     * <p>CSRF 不匹配时仍清除本机会话，避免前端无法离开；只要还能读到会话就尽量撤销 Auth sid。</p>
     *
     * @param request  当前管理台请求
     * @param response 用于清除 Cookie
     * @return 空成功 data
     */
    public R<?> logout(HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = appRegistry.requireFromRequest(request);
        appRegistry.requireAdmin(request, app);
        try {
            requireCsrf(request, app);
        } catch (BffAuthException exception) {
            if (exception.getErrorCode() != BffErrorCode.BINDING_MISMATCH) {
                throw exception;
            }
            log.warn("[BffAuth] logout csrf mismatch, still clearing local session");
        }
        BffSession session = sessionService.getSession(request);
        if (session != null && StrUtil.isNotEmpty(session.getSid())) {
            InnerSessionRevokeDTO params = InnerSessionRevokeDTO.builder()
                    .reason(SessionRevokeReason.USER_LOGOUT)
                    .build();
            try {
                remoteAuthSessionService.revokeBySid(session.getAuthCookie(), session.getSid(), params);
            } catch (Exception exception) {
                log.warn("[BffAuth] session revoke failed on auth service, sid={}", session.getSid(), exception);
            }
        }
        sessionService.removeSession(request, response);
        transactionService.deleteBinding(sessionService.getBindingIdFromCookie(request));
        sessionService.clearBindingCookie(response);
        return R.ok();
    }

    public R<Map<String, Object>> me(HttpServletRequest request) {
        BffSession session = sessionService.getSession(request);
        if (session == null || StrUtil.isBlank(session.getAccessToken())) {
            return R.error(com.ingot.framework.commons.model.status.BaseErrorCode.UNAUTHORIZED);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appId", session.getAppId());
        data.put("domain", session.getDomain());
        data.put("tenantId", session.getTenantId());
        data.put("userId", session.getUserId() != null ? session.getUserId() : 0);
        data.put("clientId", session.getClientId());
        return R.ok(data);
    }

    private R<?> completeAuthorize(LoginTransaction transaction, BffAppRegistration app, String tenantId,
            HttpServletRequest request, HttpServletResponse response) {
        String codeChallenge = generateCodeChallenge(transaction.getCodeVerifier());
        R<Map<String, Object>> authorizeResult;
        try {
            authorizeResult = remoteAuthTokenService.authorize(
                    transaction.getAuthCookie(), PreAuthorizationGrantType.PASSWORD.value(),
                    tenantId, app.getDomain().getValue(), app.getOauthClientId(), codeChallenge,
                    OAuth2AuthorizationResponseType.CODE.getValue(),
                    app.getOauthRedirectUri(), app.getOauthScope(), transaction.getState());
        } catch (Exception exception) {
            throw new BffAuthException(authorizeWindowError(exception, "authorize"));
        }
        if (authorizeResult == null || !authorizeResult.isSuccess() || authorizeResult.getData() == null) {
            throw new BffAuthException(authorizeWindowError(null, "authorize"));
        }
        Map<String, String> tokenForm = new HashMap<>();
        tokenForm.put(OAuth2ParameterNames.CODE, String.valueOf(authorizeResult.getData().get(OAuth2ParameterNames.CODE)));
        tokenForm.put(OAuth2ParameterNames.GRANT_TYPE, SecurityConstants.GrantType.AUTHORIZATION_CODE);
        tokenForm.put(PkceParameterNames.CODE_VERIFIER, transaction.getCodeVerifier());
        tokenForm.put(OAuth2ParameterNames.CLIENT_ID, app.getOauthClientId());
        tokenForm.put(OAuth2ParameterNames.REDIRECT_URI, app.getOauthRedirectUri());
        R<Map<String, Object>> tokenResult;
        try {
            tokenResult = remoteAuthTokenService.token(tokenForm);
        } catch (Exception exception) {
            throw new BffAuthException(authorizeWindowError(exception, "token"));
        }
        if (tokenResult == null || !tokenResult.isSuccess() || tokenResult.getData() == null) {
            throw new BffAuthException(authorizeWindowError(null, "token"));
        }
        Map<String, Object> tokenData = tokenResult.getData();
        transaction.setAccessToken((String) tokenData.get(InOAuth2ParameterNames.ACCESS_TOKEN));
        transaction.setRefreshToken(StrUtil.emptyIfNull((String) tokenData.get(InOAuth2ParameterNames.REFRESH_TOKEN)));
        transaction.setSid(JwtPayloadUtil.readClaim(transaction.getAccessToken(), InJwtClaimNames.SID));
        Object userId = tokenData.get("user_id");
        if (userId instanceof Number number) {
            transaction.setUserId(number.longValue());
        }
        transaction.setTenantId(tenantId);
        transaction.setStage(STAGE_READY);
        transaction.setTicket(randomToken(24));
        long now = Instant.now().getEpochSecond();
        transaction.setTicketExpiresAt(Math.min(now + properties.getTicketTtl(), transaction.getExpiresAt()));
        transactionService.save(transaction);
        transactionService.saveTicket(transaction.getTicket(), transaction.getTransactionId(),
                Math.max(1, transaction.getTicketExpiresAt() - now));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stage", STAGE_READY);
        data.put("transactionId", transaction.getTransactionId());
        data.put("completionUrl", appRegistry.completionUrl(app, transaction.getTicket()));
        return R.ok(data);
    }

    private BffAppRegistration requireEntryApp(String entry, HttpServletRequest request) {
        BffAppRegistration app = appRegistry.requireFromRequest(request);
        AuthorizationDomain expected = BffConstants.ENTRY_PLATFORM.equals(entry)
                ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT;
        appRegistry.requireDomain(app, expected);
        return app;
    }

    private LoginTransaction requireTransaction(String transactionId, BffAppRegistration app) {
        LoginTransaction transaction = transactionService.require(transactionId);
        if (!StrUtil.equals(transaction.getAppId(), app.getAppId())) {
            throw new BffAuthException(BffErrorCode.ENTRY_MISMATCH);
        }
        return transaction;
    }

    private AuthBinding requireCsrf(HttpServletRequest request, BffAppRegistration app) {
        AuthBinding binding = transactionService.requireBinding(sessionService.getBindingIdFromCookie(request));
        if (!StrUtil.equals(binding.getAppId(), app.getAppId())) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
        String header = request.getHeader(BffConstants.CSRF_HEADER);
        if (StrUtil.isBlank(header) || !StrUtil.equals(header, binding.getCsrfToken())) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
        return binding;
    }

    private void requireLoginBinding(LoginTransaction transaction, HttpServletRequest request, BffAppRegistration app) {
        AuthBinding binding = requireCsrf(request, app);
        bindLoginBrowser(transaction, binding);
    }

    private void bindLoginBrowser(LoginTransaction transaction, HttpServletRequest request, BffAppRegistration app) {
        AuthBinding binding = transactionService.requireBinding(sessionService.getBindingIdFromCookie(request));
        if (!StrUtil.equals(binding.getAppId(), app.getAppId())) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
        bindLoginBrowser(transaction, binding);
    }

    private void bindLoginBrowser(LoginTransaction transaction, AuthBinding binding) {
        if (StrUtil.isNotBlank(transaction.getLoginBindingId())
                && !StrUtil.equals(transaction.getLoginBindingId(), binding.getBindingId())) {
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        }
        if (StrUtil.isBlank(transaction.getLoginBindingId())) {
            transaction.setLoginBindingId(binding.getBindingId());
            transactionService.save(transaction);
        }
    }

    private List<TenantMainDTO> parseAllows(Map<String, Object> data) {
        if (data == null) {
            return List.of();
        }
        Object raw = data.get(InOAuth2ParameterNames.PRE_ALLOW_LIST);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<TenantMainDTO> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                TenantMainDTO dto = new TenantMainDTO();
                Object id = map.get("id");
                Object name = map.get("name");
                dto.setId(id == null ? null : String.valueOf(id));
                dto.setName(name == null ? null : String.valueOf(name));
                if (StrUtil.isNotBlank(dto.getId())) {
                    result.add(dto);
                }
            } else if (item instanceof TenantMainDTO dto && StrUtil.isNotBlank(dto.getId())) {
                result.add(dto);
            }
        }
        return result;
    }

    private List<Map<String, String>> summarizeAllows(List<TenantMainDTO> allows) {
        return allows.stream()
                .map(item -> Map.of("id", item.getId(), "name", StrUtil.blankToDefault(item.getName(), item.getId())))
                .toList();
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

    private BffErrorCode authorizeWindowError(Exception exception, String stage) {
        if (exception != null) {
            log.warn("[BffAuth] {} failed, mapping to transaction expired", stage, exception);
        } else {
            log.warn("[BffAuth] {} returned unsuccessful result, mapping to transaction expired", stage);
        }
        return BffErrorCode.TRANSACTION_EXPIRED;
    }

    private R<?> authRpcFailure(Exception exception, String stage) {
        if (exception instanceof InFeignException feignException) {
            log.warn("[BffAuth] {} failed, code={}", stage, feignException.getCode());
            String code = StrUtil.blankToDefault(feignException.getCode(),
                    BaseErrorCode.BAD_REQUEST.getCode());
            String message = StrUtil.blankToDefault(feignException.getMessage(),
                    BaseErrorCode.BAD_REQUEST.getText());
            return R.error(code, message);
        }
        log.error("[BffAuth] {} failed", stage, exception);
        return R.error500();
    }

    private String extractAuthCookie(HttpHeaders headers) {
        List<String> setCookies = headers.get(HttpHeaders.SET_COOKIE);
        if (setCookies == null) {
            return null;
        }
        String cookiePrefix = AUTH_COOKIE_NAME + "=";
        for (String setCookie : setCookies) {
            if (setCookie.startsWith(cookiePrefix)) {
                return StrUtil.subBefore(setCookie, ";", false);
            }
        }
        return null;
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance(CODE_CHALLENGE_ALGORITHM);
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to generate PKCE code challenge", exception);
        }
    }

    private String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
