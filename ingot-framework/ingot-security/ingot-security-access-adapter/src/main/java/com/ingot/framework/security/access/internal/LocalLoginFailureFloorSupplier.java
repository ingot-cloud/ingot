package com.ingot.framework.security.access.internal;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import com.ingot.framework.security.access.config.AccessProtectionProperties;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Nacos 本地地板供给器。
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class LocalLoginFailureFloorSupplier {

    private final AccessProtectionProperties properties;

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
