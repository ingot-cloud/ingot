package com.ingot.cloud.iam.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.persistence.entity.IamAuthorizationAuditEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证真实审计 Writer 统一字符串契约，并兼容历史结构值且不披露未知字段。</p>
 * @author jy
 * @since 1.0.0
 */
class IamAuditValuesTest {
    @Test
    void writerNormalizesStructuredAndNullValuesWithoutChangingText() throws Exception {
        var mapper = mock(IamAuthorizationAuditMapper.class);
        var fields = new LinkedHashMap<AuditField, Object>();
        fields.put(AuditField.NAME, "名称");
        fields.put(AuditField.FIELD_CAPABILITIES, List.of(Map.of("key", "displayName")));
        fields.put(AuditField.SCOPE, Map.of("kind", "ALL"));
        fields.put(AuditField.STATUS, true);
        fields.put(AuditField.REMARK, null);
        new IamAuditWriter(mapper).write(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2"),
                9, "resource", "3", AuditChangeType.UPDATE, Map.of(), fields, Map.of("resource", "1"));
        var row = ArgumentCaptor.forClass(IamAuthorizationAuditEntity.class);
        verify(mapper).insert(row.capture());
        var stored = new ObjectMapper().readTree(row.getValue().getSafeAfter());
        assertTrue(stored.get("FIELD_CAPABILITIES").isTextual());
        assertTrue(stored.get("SCOPE").isTextual());
        assertEquals("true", stored.get("STATUS").asText());
        assertTrue(stored.get("REMARK").isNull());
        var values = IamAuditValues.readFields("9", IamAuditValues.AFTER_COLUMN, row.getValue().getSafeAfter());
        assertEquals("名称", values.get(AuditField.NAME));
        assertEquals("[{\"key\":\"displayName\"}]", values.get(AuditField.FIELD_CAPABILITIES));
        assertNull(values.get(AuditField.REMARK));
    }

    @Test
    void readsExistingArrayObjectScalarAndStringAndDropsUnknownKeys() {
        var values = IamAuditValues.readFields("1000604", IamAuditValues.AFTER_COLUMN,
                "{\"FIELD_CAPABILITIES\":[],\"SCOPE\":{\"kind\":\"ALL\"},\"STATUS\":1,\"NAME\":\"[]\",\"REMARK\":null,\"UNKNOWN\":{\"hidden\":true}}");
        assertEquals("[]", values.get(AuditField.FIELD_CAPABILITIES));
        assertEquals("{\"kind\":\"ALL\"}", values.get(AuditField.SCOPE));
        assertEquals("1", values.get(AuditField.STATUS));
        assertEquals("[]", values.get(AuditField.NAME));
        assertEquals(5, values.size());
    }

    @Test
    void corruptStorageAndInvalidRevisionsUseServerErrorAndSafeMessage() {
        for (String value : List.of("not-json", "[]", "null")) {
            BizException error = assertThrows(BizException.class,
                    () -> IamAuditValues.readFields("9", IamAuditValues.AFTER_COLUMN, value));
            assertEquals(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), error.getCode());
            assertFalse(error.getMessage().contains(value));
        }
        BizException error = assertThrows(BizException.class,
                () -> IamAuditValues.readRevisions("9", "{\"role\":[]} "));
        assertEquals(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), error.getCode());
        assertEquals(Map.of("role", "1"), IamAuditValues.readRevisions("9", "{\"role\":\"1\"}"));
        assertEquals(500, new com.ingot.cloud.iam.web.IamErrorHandler().bizException(error).getStatusCode().value());
    }
}
