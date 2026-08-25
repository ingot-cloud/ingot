package com.ingot.framework.security.access.internal;

import java.util.ArrayList;
import java.util.List;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.framework.cache.spi.CacheFloorSupplier;
import com.ingot.framework.security.access.config.AccessProtectionProperties;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import lombok.RequiredArgsConstructor;

/**
 * <p>Nacos 本地地板：把 {@link AccessProtectionProperties#getLoginFailure()} 映射为四维策略列表。</p>
 *
 * <p>仅在远端不可用且无 LKG 时被调用。四维始终各产出一条，保证地板非空，避免 fail-open。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 每次调用都重新读取 Properties，从而反映 Nacos 动态刷新后的最新值。
 */
@RequiredArgsConstructor
public class LocalLoginFailureFloorSupplier implements CacheFloorSupplier<String, List<LoginFailurePolicy>> {

    private final AccessProtectionProperties properties;

    @Override
    public List<LoginFailurePolicy> get(String key) {
        return get();
    }

    /**
     * 组装当前地板策略列表。
     *
     * @return 非空的四维策略
     */
    public List<LoginFailurePolicy> get() {
        AccessProtectionProperties.LoginFailureConfig config = properties.getLoginFailure();
        List<LoginFailurePolicy> list = new ArrayList<>(4);
        list.add(fromConfig(LoginFailureDimension.IP, config.getIp()));
        list.add(fromConfig(LoginFailureDimension.DEVICE, config.getDevice()));
        list.add(fromConfig(LoginFailureDimension.CLIENT, config.getClient()));
        list.add(fromConfig(LoginFailureDimension.ACCOUNT_IP, config.getAccountIp()));
        return List.copyOf(list);
    }

    private LoginFailurePolicy fromConfig(LoginFailureDimension dimension,
                                          AccessProtectionProperties.DimensionPolicy source) {
        if (source == null) {
            source = new AccessProtectionProperties.DimensionPolicy();
        }
        return new LoginFailurePolicy(
                dimension,
                source.isEnabled(),
                source.getMaxAttempts(),
                source.getWindowMinutes(),
                source.getBlockTtlSec(),
                source.getBlockKeyType()
        );
    }
}
