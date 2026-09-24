package com.ingot.framework.commons.jackson;

import java.time.Instant;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.EntitlementDraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>开通期限按墙钟进出，空白视为空，ISO 瞬时仍可解析。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class WallClockInstantCodecTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setTimeZone(TimeZone.getTimeZone(ClientWallClock.ZONE_BEIJING));

    @Test
    void wallClockUsesShanghaiWhenJacksonFallsToGmt() throws Exception {
        EntitlementDraft draft = mapper.readValue("""
                {"applicationId":"4","status":"ENABLED","validFrom":"2029-09-21 00:00:00","validUntil":""}
                """, EntitlementDraft.class);
        assertEquals(Instant.parse("2029-09-20T16:00:00Z"), draft.validFrom());
        assertNull(draft.validUntil());
        assertEquals("2029-09-21 00:00:00", mapper.valueToTree(draft).get("validFrom").asText());
    }

    @Test
    void isoInstantStillAccepted() throws Exception {
        EntitlementDraft draft = mapper.readValue("""
                {"applicationId":"4","status":"ENABLED","validFrom":"2026-09-13T00:00:00Z"}
                """, EntitlementDraft.class);
        assertEquals(Instant.parse("2026-09-13T00:00:00Z"), draft.validFrom());
        assertEquals(ConfigurationStatus.ENABLED, draft.status());
    }
}
