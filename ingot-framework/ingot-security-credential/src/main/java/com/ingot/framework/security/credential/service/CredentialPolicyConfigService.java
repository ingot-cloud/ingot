package com.ingot.framework.security.credential.service;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;

/**
 * 凭证策略配置访问 SPI。
 * <p>
 * 由装饰器链统一暴露，对外仅看到一个 {@code @Primary} bean。链路自外向内：
 * <pre>
 * Caffeine (L1) -> Redis (L2) -> Delegate (Local Mapper / Remote Feign)
 * </pre>
 * Delegate 在 ingot-security-provider 内是直查 MySQL 的 Local 实现，在其它消费微服务里
 * 是 Feign 调用 {@code RemoteCredentialService} 的 Remote 实现。
 *
 * @author jy
 * @since 2026/5/16
 */
public interface CredentialPolicyConfigService {

    /**
     * 获取全部策略配置。
     */
    List<CredentialPolicyConfigVO> getAll();

    /**
     * 清除当前装饰器层及其下层 delegate 的缓存。
     * <p>装饰器实现需要把 evict 沿链路向下传播，确保 L1+L2 同时失效。</p>
     */
    default void evictAll() {
        // default no-op
    }
}
