package com.ingot.framework.commons.jackson;

import java.time.*;
import java.util.Date;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.commons.model.iam.EntitlementDraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证公共 API 严格时间点契约和独立 Redis 编码边界。</p>
 * @author jy
 * @since 1.0.0
 */
class InApiTimeModuleTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .registerModule(new InApiTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void offsetsAndAllTimePointTypesRepresentTheSameInstantAcrossJvmZones() throws Exception {
        TimeZone original = TimeZone.getDefault();
        Instant instant = Instant.parse("2026-10-08T01:02:03.123456789Z");
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                ObjectMapper api = mapper.copy().setTimeZone(TimeZone.getDefault());
                for (String value : new String[]{"2026-10-08T01:02:03.123456789Z",
                        "2026-10-08T09:02:03.123456789+08:00", "2026-10-07T21:02:03.123456789-04:00"}) {
                    assertEquals(instant, api.readValue('"' + value + '"', Instant.class));
                    assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), api.readValue('"' + value + '"', LocalDateTime.class));
                }
                for (Object value : new Object[]{instant, LocalDateTime.ofInstant(instant, ZoneOffset.UTC),
                        instant.atOffset(ZoneOffset.ofHours(8)), instant.atZone(ZoneId.of(zone))}) {
                    assertEquals('"' + instant.toString() + '"', api.writeValueAsString(value));
                }
                assertEquals("\"2026-10-08T01:02:03.123Z\"", api.writeValueAsString(Date.from(instant)));
                assertEquals(Date.from(instant), api.readValue("\"2026-10-08T09:02:03.123+08:00\"", Date.class));
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void rejectsOldFormatNoOffsetInvalidDateBlankAndNumericTimePoints() {
        for (String input : new String[]{"\"2026-10-08 09:00:00\"", "\"2026-10-08T09:00:00\"",
                "\"2026-02-30T09:00:00Z\"", "\"\"", "12345"}) {
            for (Class<?> type : new Class<?>[]{Instant.class, LocalDateTime.class, Date.class, OffsetDateTime.class, ZonedDateTime.class}) {
                assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> mapper.readValue(input, type));
            }
        }
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> mapper.readValue(
                "{\"applicationId\":\"4\",\"status\":\"ENABLED\",\"validFrom\":\"2026-10-08 09:00:00\"}", EntitlementDraft.class));
    }

    @Test
    void calendarValuesAndInternalStorageKeepTheirOwnEncoding() throws Exception {
        assertEquals("\"2026-10-08\"", mapper.writeValueAsString(LocalDate.of(2026, 10, 8)));
        assertEquals("\"09:00:00\"", mapper.writeValueAsString(LocalTime.of(9, 0)));
        assertEquals(LocalDate.of(2026, 10, 8), mapper.readValue("\"2026-10-08\"", LocalDate.class));
        assertEquals(LocalTime.of(9, 0), mapper.readValue("\"09:00:00\"", LocalTime.class));
        assertEquals(Duration.ofHours(24), mapper.readValue("\"PT24H\"", Duration.class));
        ObjectMapper storage = new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(new InJavaTimeModule());
        LocalDateTime utc = LocalDateTime.of(2026, 10, 8, 1, 0);
        assertEquals("\"2026-10-08 01:00:00\"", storage.writeValueAsString(utc));
        assertEquals(utc, storage.readValue(storage.writeValueAsString(utc), LocalDateTime.class));
        assertEquals("\"2026-10-08T01:00:00Z\"", mapper.writeValueAsString(utc));
    }
}
