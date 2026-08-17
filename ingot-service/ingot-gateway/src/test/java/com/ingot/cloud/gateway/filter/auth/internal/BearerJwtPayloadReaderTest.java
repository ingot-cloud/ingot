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
    void readUserIdAndUserType_fromLegacyBearerHeader() {
        String header = bearer("{\"i\":123,\"ut\":\"0\",\"jti\":\"abc\"}");

        assertEquals("123", BearerJwtPayloadReader.readUserId(header));
        assertEquals("0", BearerJwtPayloadReader.readUserType(header));
        assertEquals("abc", BearerJwtPayloadReader.readJti(header));
    }

    @Test
    void readSlimJwt_hasIdAndJti_butNoUserType() {
        String header = bearer("{\"i\":123,\"jti\":\"slim-jti\",\"org\":1}");

        assertEquals("123", BearerJwtPayloadReader.readUserId(header));
        assertEquals("slim-jti", BearerJwtPayloadReader.readJti(header));
        assertNull(BearerJwtPayloadReader.readUserType(header));
    }

    @Test
    void read_missingClaims_returnsNull() {
        String header = bearer("{\"sub\":\"x\"}");

        assertNull(BearerJwtPayloadReader.readUserId(header));
        assertNull(BearerJwtPayloadReader.readUserType(header));
        assertNull(BearerJwtPayloadReader.readJti(header));
    }

    private static String bearer(String payloadJson) {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return "Bearer hdr." + payload + ".sig";
    }
}
