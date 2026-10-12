package com.ingot.framework.commons.model.security;

import com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证过期原因的 JSON 稳定字面量、严格解析及生产枚举处理器兼容性。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CredentialExpirationReasonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @EnumSource(CredentialExpirationReason.class)
    void literalsRoundTrip(CredentialExpirationReason reason) throws Exception {
        String json = mapper.writeValueAsString(reason);
        assertEquals('"' + reason.getValue() + '"', json);
        assertEquals(reason, mapper.readValue(json, CredentialExpirationReason.class));
        assertEquals(reason, CredentialExpirationReason.getEnum(reason.getValue()));
    }

    @Test
    void unknownLiteralIsRejectedButNullIsAllowed() {
        assertNull(CredentialExpirationReason.getEnum(null));
        assertThrows(IllegalArgumentException.class, () -> CredentialExpirationReason.getEnum("future-reason"));
        assertThrows(Exception.class, () -> mapper.readValue("\"future-reason\"", CredentialExpirationReason.class));
    }

    @Test
    void productionEnumHandlerCanBeConstructed() {
        assertDoesNotThrow(() -> new CompositeEnumTypeHandler<>(CredentialExpirationReason.class));
    }
}
