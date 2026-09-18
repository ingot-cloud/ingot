package com.ingot.cloud.gateway.config;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.utils.BffCookiePolicy;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>Gateway 侧与 BFF 共用的应用注册表及 HTTPS 策略，绑定 {@code ingot.bff}。</p>
 *
 * <p>配置来自 Nacos {@code in-bff-apps.yml}，不要写入全服务 {@code in-common.yml}。
 * {@link #requireHttps} 必须与 BFF 一致，以便 JWT 中继读取同一 Cookie 名。</p>
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
public class GatewayBffProperties {
    /**
     * 是否按生产 Cookie / origin 规则运行。默认 {@code true}。
     * <p>与 BFF 的 {@code ingot.bff.require-https} 必须同值：true 时会话 Cookie 为
     * {@link BffCookiePolicy#SESSION_COOKIE_HOST}；false 时为
     * {@link BffCookiePolicy#SESSION_COOKIE_PLAIN}。生产环境空注册表会启动失败。</p>
     */
    private boolean requireHttps = true;
    /**
     * 前端应用注册表。Gateway 用请求 Host 匹配 {@code adminOrigin} / {@code loginOrigin} 后注入 appId。
     */
    private List<BffAppRegistration> apps = new ArrayList<>();

    /**
     * {@code requireHttps=true} 时拒绝空注册表，避免 Host 匹配被跳过。
     */
    @PostConstruct
    public void validateProductionRegistry() {
        if (!requireHttps) {
            return;
        }
        if (apps == null || apps.isEmpty()) {
            throw new IllegalStateException("BFF 应用注册表不能为空");
        }
    }
}
