package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.spi.SecurityEventStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>按 storeId 选择主 Store；多 Store 未配置 primary-store 时 fail-fast。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class StoreSelector {

    private StoreSelector() {
    }

    public static SecurityEventStore selectPrimary(
            List<SecurityEventStore> stores,
            String primaryStoreId) {
        if (stores == null || stores.isEmpty()) {
            throw new IllegalStateException(
                    "No SecurityEventStore bean available for target=local; "
                            + "add ingot-security-event-store-mysql (or -log) dependency and ensure "
                            + "SecurityEventStoreMapper is scanned");
        }
        Map<String, SecurityEventStore> indexed = index(stores);
        if (stores.size() == 1) {
            return stores.get(0);
        }
        if (primaryStoreId == null || primaryStoreId.isBlank()) {
            throw new IllegalStateException(
                    "Multiple SecurityEventStore beans found; configure ingot.security.event.primary-store");
        }
        SecurityEventStore selected = indexed.get(primaryStoreId.trim());
        if (selected == null) {
            throw new IllegalStateException(
                    "Configured primary-store '" + primaryStoreId + "' not found; available="
                            + indexed.keySet());
        }
        return selected;
    }

    public static Map<String, SecurityEventStore> index(List<SecurityEventStore> stores) {
        Map<String, SecurityEventStore> indexed = new LinkedHashMap<>();
        for (SecurityEventStore store : stores) {
            indexed.put(store.storeId(), store);
        }
        return indexed;
    }
}
