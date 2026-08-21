package com.ingot.cloud.gateway.filter.auth.internal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link BearerJwtPayloadReader} JWT payload 解析单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class BearerJwtPayloadReaderTest {

    @Test
    void readUserIdAndSid_fromBearerHeader() {
        String header = bearer("{\"i\":123,\"sid\":\"session-1\",\"org\":1}");

        assertEquals("123", BearerJwtPayloadReader.readUserId(header));
        assertEquals("session-1", BearerJwtPayloadReader.readSid(header));
    }

    @Test
    void read_missingClaims_returnsNull() {
        String header = bearer("{\"sub\":\"x\"}");

        assertNull(BearerJwtPayloadReader.readUserId(header));
        assertNull(BearerJwtPayloadReader.readSid(header));
    }

    private static String bearer(String payloadJson) {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return "Bearer hdr." + payload + ".sig";
    }
}
