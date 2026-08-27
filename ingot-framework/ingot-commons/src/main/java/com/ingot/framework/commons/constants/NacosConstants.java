package com.ingot.framework.commons.constants;

/**
 * <p>共享 Nacos dataId 常量，供 {@code spring.config.import} 与刷新过滤对照。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface NacosConstants {

    /**
     * 跨服务安全策略地板：凭证、防重放、{@code account.signal}、PMS/Member/Security 的 event.delivery。
     * <p>仅凭证/重放/锁信号的消费者 import；Auth 与 Gateway 不挂。</p>
     */
    String IN_SECURITY_POLICY = "in-security-policy.yml";

    /**
     * 网关限流 / 黑白名单 / 违规升级的 Nacos 地板（不含各域 {@code enabled} / {@code mode}）。
     * <p>仅 Gateway import。</p>
     */
    String IN_SECURITY_GATEWAY = "in-security-gateway.yml";

    /**
     * 传输加密（信封加密密钥）。
     * <p>仅 MVC 信封链路消费者 import；Auth 与 Gateway 不挂。</p>
     */
    String IN_SECURITY_CRYPTO = "in-security-crypto.yml";
}
