package com.ingot.cloud.auth.api.rpc;

import java.util.Map;

import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.constants.ServiceNameConstants;
import com.ingot.framework.commons.model.support.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>Auth 服务 OAuth2 协议端点的 Feign 契约，供边缘代理（BFF 及将来的 App BFF）编排三段式登录。</p>
 *
 * <p>本接口只覆盖 {@code /oauth2/**} 协议面，不承担会话查询与撤销职责 —— 后者属于
 * {@link RemoteAuthSessionService} 的 {@code /inner/session/**} 执行面。调用不经网关，
 * 由 Nacos 按 {@link ServiceNameConstants#AUTH_SERVICE} 直连。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see RemoteAuthSessionService
 * @apiNote 预授权与选租户之间需要沿用同一个 Auth 侧 {@code JSESSIONID}，因此
 * {@link #preAuthorize} 返回 {@link ResponseEntity} 以便调用方读取 {@code Set-Cookie}，
 * 并在 {@link #authorize} 时回传。
 */
@FeignClient(contextId = "RemoteAuthTokenService", value = ServiceNameConstants.AUTH_SERVICE)
public interface RemoteAuthTokenService {

    /**
     * 预授权：校验账号凭据并返回可选租户列表。
     *
     * @param cookie        回传的 Auth 会话 Cookie，首次调用可为空
     * @param userType      用户类型
     * @param preGrantType  预授权类型
     * @param clientId      客户端 ID
     * @param codeChallenge PKCE code_challenge
     * @param responseType  响应类型
     * @param redirectUri   重定向 URI
     * @param scope         授权范围
     * @param state         CSRF 状态值
     * @param formData      表单参数（username / password 等）
     * @return 预授权结果，data 中含可选租户列表
     */
    @PostMapping(value = SecurityConstants.PRE_AUTHORIZE_URI,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<R<Map<String, Object>>> preAuthorize(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestParam(InOAuth2ParameterNames.USER_TYPE) String userType,
            @RequestParam(InOAuth2ParameterNames.PRE_GRANT_TYPE) String preGrantType,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(PkceParameterNames.CODE_CHALLENGE) String codeChallenge,
            @RequestParam(OAuth2ParameterNames.RESPONSE_TYPE) String responseType,
            @RequestParam(OAuth2ParameterNames.REDIRECT_URI) String redirectUri,
            @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
            @RequestParam(OAuth2ParameterNames.STATE) String state,
            @RequestParam(value = InOAuth2ParameterNames.DOMAIN, required = false) String domain,
            Map<String, ?> formData);

    /**
     * 选定租户并获取授权码。
     *
     * @param cookie        预授权阶段捕获的 Auth 会话 Cookie
     * @param preGrantType  预授权类型
     * @param tenantId      选定的租户 ID
     * @param clientId      客户端 ID
     * @param codeChallenge PKCE code_challenge
     * @param responseType  响应类型
     * @param redirectUri   重定向 URI
     * @param scope         授权范围
     * @param state         CSRF 状态值
     * @return 授权码结果，data 中含 {@code code}
     */
    @GetMapping(SecurityConstants.AUTHORIZE_URI)
    R<Map<String, Object>> authorize(
            @RequestHeader(value = HttpHeaders.COOKIE, required = false) String cookie,
            @RequestParam(InOAuth2ParameterNames.PRE_GRANT_TYPE) String preGrantType,
            @RequestParam(value = InOAuth2ParameterNames.TENANT, required = false) String tenantId,
            @RequestParam(value = InOAuth2ParameterNames.DOMAIN, required = false) String domain,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(PkceParameterNames.CODE_CHALLENGE) String codeChallenge,
            @RequestParam(OAuth2ParameterNames.RESPONSE_TYPE) String responseType,
            @RequestParam(OAuth2ParameterNames.REDIRECT_URI) String redirectUri,
            @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
            @RequestParam(OAuth2ParameterNames.STATE) String state);

    /**
     * 授权码换取 Token（PKCE 公开客户端，无需 client_secret）。
     *
     * @param formData 表单参数（code / grant_type / code_verifier / client_id / redirect_uri）
     * @return Token 结果
     */
    @PostMapping(value = SecurityConstants.TOKEN_ENDPOINT_URI,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    R<Map<String, Object>> token(Map<String, ?> formData);
}
