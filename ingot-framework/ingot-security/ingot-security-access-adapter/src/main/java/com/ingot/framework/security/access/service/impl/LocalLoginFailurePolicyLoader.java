package com.ingot.framework.security.access.service.impl;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.framework.security.access.config.AccessProtectionProperties;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地登录失败策略加载器。
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class LocalLoginFailurePolicyLoader implements LoginFailurePolicyLoader {

    private final AccessProtectionProperties properties;

    @Override
    public List<LoginFailurePolicy> loadAll() {
        AccessProtectionProperties.LoginFailureConfig config = properties.getLoginFailure();
        List<LoginFailurePolicy> list = new ArrayList<>(4);
        list.add(fromConfig(LoginFailureDimension.IP, config.getIp()));
        list.add(fromConfig(LoginFailureDimension.DEVICE, config.getDevice()));
        list.add(fromConfig(LoginFailureDimension.CLIENT, config.getClient()));
        list.add(fromConfig(LoginFailureDimension.ACCOUNT_IP, config.getAccountIp()));
        return list;
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
