package com.ingot.framework.security.recording.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Store 实例的能力集合，用于运行时判定查询与 retention 是否可用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public record StoreCapabilities(Set<StoreCapability> capabilities) {

    public StoreCapabilities {
        capabilities = capabilities == null || capabilities.isEmpty()
                ? Set.of()
                : Set.copyOf(capabilities);
    }

    public static StoreCapabilities of(StoreCapability first, StoreCapability... rest) {
        return new StoreCapabilities(EnumSet.of(first, rest));
    }

    public static StoreCapabilities none() {
        return new StoreCapabilities(Set.of());
    }

    public boolean supports(StoreCapability capability) {
        return capabilities.contains(capability);
    }
}
