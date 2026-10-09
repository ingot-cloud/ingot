package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证旧菜单 JSON 仍可反序列化，结构化参数执行字段校验。</p>
 * @author jy
 * @since 1.0.0
 */
class MenuContractTest {
    @Test
    void omittedAdvancedFieldsRemainNullForUpdateCompatibility() throws Exception {
        var draft = new ObjectMapper().readValue("""
                {"name":"旧菜单","kind":"PAGE","path":"/orders","accessMode":"OPEN",
                 "matchMode":"ANY","actionIds":[],"sortOrder":0}
                """, MenuDraft.class);
        assertNull(draft.hidden()); assertNull(draft.isCache()); assertNull(draft.props());
        assertNull(draft.routeParams());
    }

    @Test
    void parameterNameIsValidatedAndRemarksRetained() throws Exception {
        var mapper = new ObjectMapper();
        var param = mapper.readValue("{\"name\":\"a\",\"remark\":\"订单编号\"}", MenuRouteParam.class);
        assertEquals("订单编号", param.remark());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(param).isEmpty());
            assertFalse(factory.getValidator().validate(new MenuRouteParam("9a", null)).isEmpty());
        }
    }
}
