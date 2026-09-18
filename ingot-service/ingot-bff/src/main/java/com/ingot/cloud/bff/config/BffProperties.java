package com.ingot.cloud.bff.config;

import java.util.ArrayList;
import java.util.List;

import com.ingot.cloud.bff.model.enums.FingerprintMode;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.utils.BffCookiePolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>BFF 服务配置，绑定 {@code ingot.bff.*}。</p>
 *
 * <p>{@code require-https} 与 {@code apps} 来自 Nacos {@code in-bff-apps.yml}（仅 BFF/Gateway 加载）。
 * TTL 与指纹来自 {@code in-service-bff.yml}。Cookie 名称与 Secure 由 {@link BffCookiePolicy} 按
 * {@code require-https} 决定，没有可配的 Domain。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * ingot:
 *   bff:
 *     require-https: true
 *     user-type: "0"
 *     session-ttl: 7200
 *     transaction-ttl: 600
 *     ticket-ttl: 60
 *     security:
 *       fingerprint-enabled: true
 *       fingerprint-mode: device
 *     apps:
 *       - app-id: tenant-admin
 *         domain: TENANT
 *         admin-origin: https://admin.example
 *         login-origin: https://login.example
 *         oauth-client-id: in-bff-tenant
 *         oauth-redirect-uri: https://bff.example/bff/auth/tenant/callback
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 *
 * @see BffAppRegistration
 * @see BffCookiePolicy
 */
@Data
@Component
@ConfigurationProperties(prefix = "ingot.bff")
public class BffProperties {
    /**
     * 管理端用户类型字面量，预授权时传给 Auth。默认 {@code 0}（管理用户）。
     */
    private String userType = "0";
    /**
     * 正式会话 Redis / Cookie 有效期（秒）。默认 7 天；Nacos 常用 7200。
     */
    private long sessionTtl = 60 * 60 * 24 * 7;
    /**
     * 登录事务 Redis 有效期（秒）。默认 600。
     */
    private long transactionTtl = 600;
    /**
     * 一次性 ticket 有效期（秒）。默认 60，且不得超过事务剩余时间。
     */
    private long ticketTtl = 60;
    /**
     * 是否按生产 Cookie 与 origin 规则运行。默认 {@code true}。
     * <p>{@code true}：{@code apps} 不可空，origin 必须 HTTPS 且同一主域；Cookie 为
     * {@link BffCookiePolicy#SESSION_COOKIE_HOST} 并带 Secure。
     * {@code false}：仅本机 DEV HTTP；Cookie 为 {@link BffCookiePolicy#SESSION_COOKIE_PLAIN} 且不加 Secure。
     * 不要按请求 scheme 判断。</p>
     */
    private boolean requireHttps = true;
    /**
     * 前端应用注册表，按 Gateway 注入的 appId 查找回跳 origin 与 OAuth 客户端。
     */
    private List<BffAppRegistration> apps = new ArrayList<>();
    /**
     * 指纹校验。
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * <p>会话指纹开关与模式。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @Data
    public static class SecurityConfig {
        /**
         * 是否在创建/读取会话时校验指纹。默认开启。
         */
        private boolean fingerprintEnabled = true;
        /**
         * 指纹模式：{@link FingerprintMode#DEVICE} 的 {@code device}（读
         * {@link HeaderConstants#BFF_DEVICE_FINGERPRINT_HEADER}）或
         * {@link FingerprintMode#IPUA} 的 {@code ip_ua}。默认 {@code device}。
         */
        private String fingerprintMode = FingerprintMode.DEVICE.getValue();
    }
}
