package com.ingot.cloud.bff.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>BFF 登录前账号锁定短路配置，绑定 {@code ingot.security.account.bff}。</p>
 *
 * <p>仅 BFF 执行面使用，配在 {@code in-service-bff.yml}，不要放进共享策略文件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ingot.security.account.bff")
public class AccountLockBffProperties {

    /**
     * 是否在登录解密后检查 Redis name key，默认开启。
     */
    private boolean enabled = true;

    /**
     * BFF 拦截时是否仍调 Auth 以产生 LOGIN_FAILURE；默认关闭。
     */
    private boolean emitLoginFailureOnBffBlock = false;
}
