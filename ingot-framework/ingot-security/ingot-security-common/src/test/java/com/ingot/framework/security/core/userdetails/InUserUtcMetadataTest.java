package com.ingot.framework.security.core.userdetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>API ISO、内部 UTC LocalDateTime 和旧 epoch 毫秒元数据均表示同一时间点。</p>
 * @author jy
 * @since 1.0.0
 */
class InUserUtcMetadataTest {
    private static final String DEADLINE = "deadline";

    @Test
    void metadataDoesNotShiftWhenJvmTimeZoneChanges() {
        TimeZone original = TimeZone.getDefault();
        Instant instant = Instant.parse("2026-10-08T01:00:00Z");
        LocalDateTime utc = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                for (Object value : List.of("2026-10-08T09:00:00+08:00", "2026-10-08T01:00:00Z",
                        "2026-10-08T01:00:00", instant.toEpochMilli(), utc)) {
                    InUser user = InUser.stateless(1L, 10L, "web", "standard", "admin", "user", List.of(), List.of(), Map.of())
                            .toBuilder().meta(Map.of(DEADLINE, value)).build();
                    assertEquals(utc, user.getMetaValue(DEADLINE, LocalDateTime.class));
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
