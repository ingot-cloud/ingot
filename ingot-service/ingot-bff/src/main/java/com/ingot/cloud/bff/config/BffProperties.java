package com.ingot.cloud.bff.config;

import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>BFF 服务配置属性，统一管理 OAuth2 客户端、Cookie 策略和安全策略</p>
 *
 * <p>通过 {@code ingot.bff.*} 前缀绑定，所有属性均支持环境变量覆盖。
 * Cookie 和安全相关配置需根据部署环境（开发/测试/生产）调整。</p>
 *
 * <h3>使用示例（application.yml）：</h3>
 * <pre>{@code
 * ingot:
 *   bff:
 *     client-id: ingot-bff
 *     redirect-uri: ${BFF_REDIRECT_URI:http://localhost:5400/bff/auth/callback}
 *     cookie:
 *       domain: .ingotcloud.top
 *       secure: true
 *       same-site: Lax
 *     security:
 *       fingerprint-enabled: true
 *       fingerprint-mode: device
 *       default-redirect-uri: https://admin.ingotcloud.top
 *       allowed-frontends:
 *         - https://admin.ingotcloud.top
 *         - https://console.ingotcloud.top
 *         - https://login.ingotcloud.top
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ingot.bff")
public class BffProperties {
    /**
     * BFF 作为 OAuth2 client 的 client_id（公开客户端，使用 PKCE 替代 client_secret）
     */
    private String clientId = "ingot-bff";
    /**
     * OAuth2 scope
     */
    private String scope = "system";
    /**
     * 用户类型（默认管理用户）
     */
    private String userType = "0";
    /**
     * session 有效期（秒），默认 7 天
     */
    private long sessionTtl = 60 * 60 * 24 * 7;
    /**
     * OAuth2 redirect_uri，必须与 oauth2_registered_client 表中注册的值一致。
     * 该地址不会发生真正的 HTTP 重定向（预授权模式直接返回 JSON），
     * 仅用于 Auth 服务的 OAuth2 协议校验。
     */
    private String redirectUri = "http://localhost:5400/bff/auth/callback";

    /**
     * Cookie 配置
     */
    private CookieConfig cookie = new CookieConfig();

    /**
     * 安全配置
     */
    private SecurityConfig security = new SecurityConfig();

    @Data
    public static class CookieConfig {
        /**
         * cookie domain，设置为顶级域名（如 .ingotcloud.top）可实现子域共享。
         * 为空时不设置 domain，cookie 只对当前域有效（开发环境推荐）。
         */
        private String domain;
        /**
         * 是否设置 Secure 标记（仅 HTTPS 传输）。
         * 生产环境必须为 true，开发环境可设为 false。
         */
        private boolean secure = false;
        /**
         * SameSite 策略：Strict / Lax / None。
         * Strict 最安全但可能影响跨站跳转体验，Lax 是合理的默认值。
         * 若设置为 None 则 secure 必须为 true。
         */
        private String sameSite = "Lax";
    }

    @Data
    public static class SecurityConfig {
        /**
         * 是否启用客户端指纹校验。
         * 启用后，session 创建时记录客户端指纹，后续请求指纹不匹配则拒绝。
         */
        private boolean fingerprintEnabled = true;
        /**
         * 指纹模式：{@code device}（前端设备指纹，推荐）或 {@code ip_ua}（服务端 IP+UA，降级方案）。
         * <ul>
         *     <li>{@code device} — 从请求 Header {@code In-Ca-Sig} 读取前端计算的设备指纹，
         *         不受 Docker/代理/IP 变化影响，稳定性最优</li>
         *     <li>{@code ip_ua} — 服务端通过 SHA-256(IP+UA) 计算，适用于无法修改前端的场景</li>
         * </ul>
         */
        private String fingerprintMode = "device";
        /**
         * BFF 信任的前端应用域名列表（统一白名单）。
         * 同时用于两个安全校验场景：
         * <ul>
         *     <li>请求来源校验 — {@code Origin / Referer} 必须匹配此列表（精确匹配 origin 部分）</li>
         *     <li>登录后重定向校验 — 前端传入的 {@code redirectUri} 必须以此列表中的某个条目为前缀</li>
         * </ul>
         * 为空则跳过校验（仅开发环境），生产环境必须配置。
         * 示例: ["https://admin.ingotcloud.top", "https://login.ingotcloud.top"]
         */
        private List<String> allowedFrontends;
        /**
         * 默认的登录后重定向地址，当前端未传 redirectUri 时使用。
         * 为空则 selectTenant 响应不返回 redirectUri 字段。
         * 该值同样受 allowedFrontends 白名单约束。
         */
        private String defaultRedirectUri;
    }
}
