package com.ingot.cloud.bff.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.cloud.bff.model.dto.BffTransactionView;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.InJwtClaimNames;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.bff.BffLoginStage;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
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
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 同源双入口认证编排；一次性提交结果并直接签发本域正式 Cookie。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BffAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String AUTH_COOKIE_PREFIX = "JSESSIONID=";

    private static final String PKCE_ALGORITHM = "SHA-256";

    private static final String ACCOUNT_LOCKED_CODE = "ACCOUNT_LOCKED";

    private static final String ACCOUNT_LOCKED_MESSAGE = "账号已被锁定，请联系管理员";

    private static final String ID = "id";

    private static final String SERVICE_FAILURE_PREFIX = "S05";

    private static final String HTTP_FAILURE_PREFIX = "5";

    private static final String NAME = "name";

    private final BffProperties properties;

    private final BffAppRegistry registry;

    private final BffSessionService sessions;

    private final LoginTransactionService store;

    private final RemoteAuthTokenService auth;

    private final BffCleanupService cleanup;

    private final AccountLockSignalPort accountLocks;

    private final AccountLockBffProperties accountLockProperties;

    private final BffMetrics metrics;

    /** 同源自举或复用绑定，不能以新标签页轮换作废其他事务。 */
    public Map<String, Object> issueCsrf(HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = app(request);
        sessions.clearLegacy(request, response);
        AuthBinding binding = store.findBinding(app.getAppId(), sessions.bindingId(request, app));
        if (binding == null) {
            binding = new AuthBinding();
            binding.setAppId(app.getAppId());
            binding.setBindingId(random(BffConstants.TOKEN_BYTES));
            binding.setCsrfToken(random(BffConstants.CSRF_BYTES));
            if (!store.createBinding(binding))
                throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
        long ttl = store.bindingTtl(app.getAppId(), binding.getBindingId());
        sessions.writeBinding(app.getAppId(), binding.getBindingId(), ttl, response);
        return Map.of("csrfToken", binding.getCsrfToken(), "expiresAt", Instant.ofEpochSecond(store.now() + ttl));
    }

    /** 仅用户提交时创建本次事务，原子预留容量并延长绑定。 */
    public BffTransactionView createTransaction(String entry, HttpServletRequest request,
            HttpServletResponse response) {
        BffAppRegistration app = entry(entry, request);
        AuthBinding binding = binding(request, app, true);
        LoginTransaction tx = new LoginTransaction();
        tx.setAppId(app.getAppId());
        tx.setDomain(app.getDomain());
        tx.setBindingId(binding.getBindingId());
        tx.setTransactionId(random(BffConstants.TOKEN_BYTES));
        tx.setStage(BffLoginStage.LOGIN);
        tx.setCodeVerifier(random(BffConstants.PKCE_BYTES));
        tx.setState(random(BffConstants.TOKEN_BYTES));
        tx.setExpiresAt(store.now() + properties.getTransactionTtl());
        store.create(tx);
        metrics.event("prepared", app.getAppId(), tx.getStage().getValue());
        sessions.writeBinding(app.getAppId(), binding.getBindingId(),
                store.bindingTtl(app.getAppId(), binding.getBindingId()), response);
        return view(tx, null);
    }

    /** 只读查询本事务；旧账号的有效 Cookie 不会被当成本次结果。 */
    public BffTransactionView readTransaction(String entry, String id, HttpServletRequest request) {
        BffAppRegistration app = entry(entry, request);
        LoginTransaction tx = transaction(id, request, app, false);
        Boolean established = null;
        if (tx.getStage() == BffLoginStage.COMPLETED) {
            BffSession current = sessions.getSession(request, false);
            established = current != null && tx.getResultSessionId().equals(current.getSessionId());
        }
        return view(tx, established);
    }

    /** 校验密码，平台/单组织直接完成，多组织进入当前页选择。 */
    public R<?> login(String entry, BffLoginDTO dto, HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = entry(entry, request);
        LoginTransaction tx = transaction(dto.getTransactionId(), request, app, true);
        if (StrUtil.hasBlank(dto.getUsername(), dto.getPassword()))
            throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
        sessions.fingerprint(request);
        store.claim(tx, BffLoginStage.LOGIN);
        long started = System.nanoTime();
        if (accountLockProperties.isEnabled() && !accountLockProperties.isEmitLoginFailureOnBffBlock()
                && accountLocks.isLockedByUsername(UserTypeEnum.getEnum(properties.getUserType()), dto.getUsername())) {
            store.transition(tx, BffLoginStage.FAILED, null);
            return R.error(ACCOUNT_LOCKED_CODE, ACCOUNT_LOCKED_MESSAGE);
        }
        Map<String, String> form = new HashMap<>();
        form.put(OAuth2ParameterNames.USERNAME, dto.getUsername());
        form.put(OAuth2ParameterNames.PASSWORD, dto.getPassword());
        form.put(InOAuth2ParameterNames.DOMAIN, app.getDomain().getValue());
        try {
            ResponseEntity<R<Map<String, Object>>> responseEntity = auth.preAuthorize(null, properties.getUserType(),
                    PreAuthorizationGrantType.PASSWORD.value(), app.getOauthClientId(), challenge(tx.getCodeVerifier()),
                    OAuth2AuthorizationResponseType.CODE.getValue(), app.getOauthRedirectUri(), app.getOauthScope(),
                    tx.getState(), app.getDomain().getValue(), form);
            R<Map<String, Object>> result = responseEntity == null ? null : responseEntity.getBody();
            if (result == null || !result.isSuccess()) {
                if (result == null || serverFailure(result.getCode()))
                    throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
                store.transition(tx, BffLoginStage.FAILED, null);
                return result;
            }
            Map<String, Object> data = result.getData();
            tx.setAuthenticationExpiresAt(authenticationDeadline(data));
            tx.setLeaseUntil(Math.min(tx.getLeaseUntil(), store.deadline(tx)));
            if (store.deadline(tx) <= store.now())
                throw new BffAuthException(BffErrorCode.TRANSACTION_EXPIRED);
            tx.setAuthCookie(authCookie(responseEntity.getHeaders()));
            if (StrUtil.isBlank(tx.getAuthCookie()))
                throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
            List<TenantMainDTO> allows = allows(data);
            if (app.getDomain() == AuthorizationDomain.PLATFORM)
                return authorize(tx, app, null, request, response);
            if (allows.isEmpty())
                throw new BffAuthException(BffErrorCode.IDENTITY_UNAVAILABLE);
            if (allows.size() == 1)
                return authorize(tx, app, allows.get(0).getId(), request, response);
            tx.setAllows(allows);
            store.transition(tx, BffLoginStage.SELECT_TENANT, null);
            return R.ok(view(tx, null));
        }
        catch (InFeignException exception) {
            if (!serverFailure(exception.getCode())) {
                fail(tx, BffLoginStage.FAILED);
                return R.error(exception.getCode(), exception.getMessage());
            }
            fail(tx, BffLoginStage.UNKNOWN);
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
        catch (BffAuthException exception) {
            fail(tx, exception.getErrorCode() == BffErrorCode.AUTH_UNAVAILABLE ? BffLoginStage.UNKNOWN
                    : BffLoginStage.FAILED);
            throw exception;
        }
        catch (RuntimeException exception) {
            fail(tx, BffLoginStage.UNKNOWN);
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
        finally {
            form.clear();
            metrics.authentication(app.getAppId(), started);
        }
    }

    /** 选择当前事务允许的组织，仍由 Auth 重验实时资格。 */
    public R<?> selectTenant(String tenantId, String id, HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = entry(BffConstants.ENTRY_TENANT, request);
        LoginTransaction tx = transaction(id, request, app, true);
        if (StrUtil.isBlank(tenantId) || tx.getAllows() == null
                || tx.getAllows().stream().noneMatch(t -> tenantId.equals(t.getId())))
            throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
        store.claim(tx, BffLoginStage.SELECT_TENANT);
        try {
            return authorize(tx, app, tenantId, request, response);
        }
        catch (BffAuthException exception) {
            fail(tx, exception.getErrorCode() == BffErrorCode.AUTH_UNAVAILABLE ? BffLoginStage.UNKNOWN
                    : BffLoginStage.FAILED);
            throw exception;
        }
        catch (RuntimeException exception) {
            fail(tx, BffLoginStage.UNKNOWN);
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
    }

    /** 只为原浏览器重新下发未被撤销/替换的本次会话，不再换码。 */
    public BffTransactionView resume(String entry, String id, HttpServletRequest request,
            HttpServletResponse response) {
        BffAppRegistration app = entry(entry, request);
        LoginTransaction tx = transaction(id, request, app, true);
        if (tx.getStage() != BffLoginStage.COMPLETED)
            throw new BffAuthException(BffErrorCode.RESULT_UNAVAILABLE);
        BffSession session = store.readSession(app.getAppId(), tx.getResultSessionId());
        if (session == null || !tx.getBindingId().equals(session.getBindingId())
                || !sessions.verifyFingerprint(session, request) || !sessions.validSid(session, app))
            throw new BffAuthException(BffErrorCode.RESULT_UNAVAILABLE);
        sessions.writeSession(session, response);
        metrics.event("resume", app.getAppId(), BffLoginStage.COMPLETED.getValue());
        return view(tx, null);
    }

    /** 返回当前真实 Cookie 的有效身份并确认交付。 */
    public R<Map<String, Object>> me(HttpServletRequest request) {
        app(request);
        BffSession session = sessions.getSession(request, true);
        if (session == null)
            return R.error(BaseErrorCode.UNAUTHORIZED);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appId", session.getAppId());
        data.put("domain", session.getDomain());
        data.put("tenantId", session.getTenantId());
        data.put("userId", session.getUserId());
        data.put("clientId", session.getClientId());
        return R.ok(data);
    }

    /** 同源退出保留 CSRF 失效仍清当前应用的兜底，不清另一应用。 */
    public R<?> logout(HttpServletRequest request, HttpServletResponse response) {
        BffAppRegistration app = app(request);
        String bindingId = sessions.bindingId(request, app);
        BffSession session = store.readSession(app.getAppId(), sessions.sessionId(request, app));
        if (session != null)
            store.invalidate(session);
        store.deleteBinding(app.getAppId(), bindingId);
        sessions.clear(app, response);
        cleanup.cleanupApp(app.getAppId());
        return R.ok();
    }

    private R<?> authorize(LoginTransaction tx, BffAppRegistration app, String tenantId, HttpServletRequest request,
            HttpServletResponse response) {
        Map<String, Object> authorization;
        try {
            authorization = authData(auth.authorize(tx.getAuthCookie(), PreAuthorizationGrantType.PASSWORD.value(),
                    tenantId, app.getDomain().getValue(), app.getOauthClientId(), challenge(tx.getCodeVerifier()),
                    OAuth2AuthorizationResponseType.CODE.getValue(), app.getOauthRedirectUri(), app.getOauthScope(),
                    tx.getState()));
        }
        catch (InFeignException exception) {
            throw new BffAuthException(classify(exception.getCode()));
        }
        Map<String, String> form = new HashMap<>();
        form.put(OAuth2ParameterNames.CODE, String.valueOf(authorization.get(OAuth2ParameterNames.CODE)));
        form.put(OAuth2ParameterNames.GRANT_TYPE, SecurityConstants.GrantType.AUTHORIZATION_CODE);
        form.put(PkceParameterNames.CODE_VERIFIER, tx.getCodeVerifier());
        form.put(OAuth2ParameterNames.CLIENT_ID, app.getOauthClientId());
        form.put(OAuth2ParameterNames.REDIRECT_URI, app.getOauthRedirectUri());
        Map<String, Object> token;
        try {
            token = authData(auth.token(form));
        }
        catch (InFeignException exception) {
            throw new BffAuthException(classify(exception.getCode()));
        }
        finally {
            form.clear();
        }
        BffSession session = new BffSession();
        session.setSessionId(random(BffConstants.TOKEN_BYTES));
        session.setAppId(app.getAppId());
        session.setDomain(app.getDomain().getValue());
        session.setBindingId(tx.getBindingId());
        session.setTransactionId(tx.getTransactionId());
        session.setClientId(app.getOauthClientId());
        session.setTenantId(tenantId);
        session.setAuthCookie(tx.getAuthCookie());
        session.setCreatedAt(store.now());
        session.setExpiresAt(store.now() + properties.getSessionTtl());
        session.setRecoveryExpiresAt(
                Math.min(session.getExpiresAt(), store.now() + properties.getCompletionRecoveryTtl()));
        session.setAccessToken((String) token.get(InOAuth2ParameterNames.ACCESS_TOKEN));
        session.setRefreshToken((String) token.get(InOAuth2ParameterNames.REFRESH_TOKEN));
        session.setSid(JwtPayloadUtil.readClaim(session.getAccessToken(), InJwtClaimNames.SID));
        boolean committing = false;
        try {
            session.setFingerprint(sessions.fingerprint(request));
            session.setFingerprintMode(
                    session.getFingerprint() == null ? null : properties.getSecurity().getFingerprintMode());
            if (StrUtil.hasBlank(session.getAccessToken(), session.getSid()))
                throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
            sessions.bindIdentity(session, app);
            store.transition(tx, BffLoginStage.COMMITTING, session);
            committing = true;
            store.commit(tx, session, sessions.sessionId(request, app));
            metrics.event("commit", app.getAppId(), BffLoginStage.COMPLETED.getValue());
        }
        catch (RuntimeException exception) {
            try {
                store.enqueueRevoke(session);
            }
            catch (RuntimeException unavailable) {
                if (!committing)
                    cleanup.revokeUncommitted(session);
                // commit前已有原子清理记录；commit回包不明时绝不误撤可能已交付的身份。
                else
                    log.warn("[BffAuth] commit reply uncertain; durable cleanup record retained, app={}",
                            app.getAppId());
            }
            throw exception;
        }
        sessions.writeSession(session, response);
        sessions.writeBinding(app.getAppId(), tx.getBindingId(), store.bindingTtl(app.getAppId(), tx.getBindingId()),
                response);
        return R.ok(view(tx, null));
    }

    private Map<String, Object> authData(R<Map<String, Object>> result) {
        if (result == null || result.getData() == null || !result.isSuccess())
            throw new BffAuthException(result == null ? BffErrorCode.AUTH_UNAVAILABLE : classify(result.getCode()));
        return result.getData();
    }

    private BffErrorCode classify(String code) {
        if (OAuth2ErrorCodes.INVALID_GRANT.equals(code) || OAuth2ErrorCodes.INVALID_TOKEN.equals(code)
                || BaseErrorCode.UNAUTHORIZED.getCode().equals(code))
            return BffErrorCode.TRANSACTION_EXPIRED;
        if (BaseErrorCode.FORBIDDEN.getCode().equals(code) || OAuth2ErrorCodes.INVALID_REQUEST.equals(code)
                || OAuth2ErrorCodes.ACCESS_DENIED.equals(code))
            return BffErrorCode.IDENTITY_UNAVAILABLE;
        return BffErrorCode.AUTH_UNAVAILABLE;
    }

    private boolean serverFailure(String code) {
        return code == null || BaseErrorCode.REQUEST_FALLBACK.getCode().equals(code)
                || code.startsWith(SERVICE_FAILURE_PREFIX) || code.startsWith(HTTP_FAILURE_PREFIX);
    }

    private void fail(LoginTransaction tx, BffLoginStage stage) {
        if (tx.getStage() == BffLoginStage.COMPLETED || tx.getStage() == BffLoginStage.FAILED)
            return;
        metrics.event("authentication.result", tx.getAppId(), stage.getValue());
        if (stage == BffLoginStage.UNKNOWN)
            log.warn("[BffAuth] result UNKNOWN; no Auth replay, app={}, correlation={}", tx.getAppId(),
                    metrics.correlation(tx.getTransactionId()));
        try {
            store.transition(tx, stage, null);
        }
        catch (RuntimeException exception) {
            log.warn("[BffAuth] state uncertain; no automatic Auth retry, app={}, correlation={}", tx.getAppId(),
                    metrics.correlation(tx.getTransactionId()));
        }
    }

    private BffAppRegistration app(HttpServletRequest request) {
        BffAppRegistration app = registry.requireFromRequest(request);
        registry.requireSource(request, app);
        return app;
    }

    private BffAppRegistration entry(String entry, HttpServletRequest request) {
        BffAppRegistration app = app(request);
        registry.requireEntry(entry, app);
        return app;
    }

    private AuthBinding binding(HttpServletRequest request, BffAppRegistration app, boolean csrf) {
        AuthBinding binding = store.findBinding(app.getAppId(), sessions.bindingId(request, app));
        if (binding == null)
            throw new BffAuthException(BffErrorCode.BINDING_MISMATCH);
        if (csrf && !binding.getCsrfToken().equals(request.getHeader(BffConstants.CSRF_HEADER)))
            throw new BffAuthException(BffErrorCode.CSRF_INVALID);
        return binding;
    }

    private LoginTransaction transaction(String id, HttpServletRequest request, BffAppRegistration app, boolean csrf) {
        return store.require(app.getAppId(), id, binding(request, app, csrf).getBindingId());
    }

    private long authenticationDeadline(Map<String, Object> data) {
        try {
            Object value = data == null ? null : data.get(BffConstants.AUTHENTICATION_EXPIRES_AT);
            if (!(value instanceof String text))
                throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
            return Instant.parse(text).getEpochSecond();
        }
        catch (DateTimeParseException exception) {
            throw new BffAuthException(BffErrorCode.AUTH_UNAVAILABLE);
        }
    }

    private BffTransactionView view(LoginTransaction tx, Boolean established) {
        boolean completed = tx.getStage() == BffLoginStage.COMPLETED;
        boolean busy = tx.getStage() == BffLoginStage.AUTHENTICATING || tx.getStage() == BffLoginStage.COMMITTING
                || tx.getStage() == BffLoginStage.UNKNOWN;
        List<Map<String, String>> candidates = tx.getStage() == BffLoginStage.SELECT_TENANT ? tx.getAllows()
            .stream()
            .map(t -> Map.of(ID, t.getId(), NAME, StrUtil.blankToDefault(t.getName(), t.getId())))
            .toList() : null;
        return new BffTransactionView(tx.getTransactionId(), tx.getStage(),
                completed ? null : Instant.ofEpochSecond(store.deadline(tx)), candidates,
                completed ? BffConstants.DEFAULT_RETURN_TO : null, established,
                completed ? Instant.ofEpochSecond(tx.getRecoveryExpiresAt()) : null,
                busy ? BffConstants.RETRY_AFTER_MILLIS : null);
    }

    private List<TenantMainDTO> allows(Map<String, Object> data) {
        List<TenantMainDTO> result = new ArrayList<>();
        Object raw = data == null ? null : data.get(InOAuth2ParameterNames.PRE_ALLOW_LIST);
        if (raw instanceof List<?> list)
            for (Object item : list) {
                if (item instanceof TenantMainDTO dto && StrUtil.isNotBlank(dto.getId()))
                    result.add(dto);
                else if (item instanceof Map<?, ?> map && map.get(ID) != null) {
                    TenantMainDTO dto = new TenantMainDTO();
                    dto.setId(String.valueOf(map.get(ID)));
                    dto.setName(map.get(NAME) == null ? dto.getId() : String.valueOf(map.get(NAME)));
                    result.add(dto);
                }
            }
        return result;
    }

    private String authCookie(HttpHeaders headers) {
        List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);
        if (cookies != null)
            for (String cookie : cookies)
                if (cookie.startsWith(AUTH_COOKIE_PREFIX))
                    return StrUtil.subBefore(cookie, ";", false);
        return null;
    }

    private String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        MessageDigest.getInstance(PKCE_ALGORITHM).digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("PKCE algorithm unavailable", exception);
        }
    }

    private String random(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

}
