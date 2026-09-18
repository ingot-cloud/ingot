package com.ingot.framework.commons.model.bff;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.Data;

/**
 * <p>BFF 前端应用注册行，由 Nacos {@code ingot.bff.apps} 绑定。</p>
 *
 * <p>写在 {@code in-bff-apps.yml}，仅 Gateway 与 BFF 加载。Gateway 按 Host 匹配 origin，
 * BFF 按注入的 appId 取回跳地址与 OAuth 客户端。不要把 IP+HTTP 配进 origin。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class BffAppRegistration {
    /**
     * 稳定键。本轮仅 {@code platform-admin}、{@code tenant-admin}，不等于 IAM applicationId。
     */
    private String appId;
    /**
     * 授权域，与入口路径锁定，不能被请求改写。
     */
    private AuthorizationDomain domain;
    /**
     * 管理台精确 origin（scheme + host + 非默认 port）。生产必须 HTTPS。
     */
    private String adminOrigin;
    /**
     * 登录站精确 origin。生产必须 HTTPS，且与 {@link #adminOrigin} 同一主域。
     */
    private String loginOrigin;
    /**
     * 管理台完成页路径。默认 {@code /auth/complete}。
     */
    private String completionPath = "/auth/complete";
    /**
     * 登录站挑战页路径。默认 {@code /oauth2/challenge}。
     */
    private String loginPath = "/oauth2/challenge";
    /**
     * 管理台启动登录路径。默认 {@code /auth/start}。
     */
    private String startPath = "/auth/start";
    /**
     * 完成登录后的落地 path。默认 {@code /}。
     */
    private String defaultReturnTo = "/";
    /**
     * 该应用独立 OAuth 公开客户端 ID。本轮为 {@code in-bff-platform} 或 {@code in-bff-tenant}。
     */
    private String oauthClientId;
    /**
     * OAuth scope。默认 {@code system}。
     */
    private String oauthScope = "system";
    /**
     * Auth 登记的协议回调，与业务 returnTo、完成页独立，必须精确匹配注册值。
     */
    private String oauthRedirectUri;
}
