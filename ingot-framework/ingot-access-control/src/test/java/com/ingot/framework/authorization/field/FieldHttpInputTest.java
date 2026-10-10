package com.ingot.framework.authorization.field;

import java.io.*;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>HTTP 原始属性在反序列化前拒绝，null 与请求字节完整保留。</p>
 * @author jy
 * @since 1.0.0
 */
class FieldHttpInputTest {
    private static final String APP = "test", RESOURCE = "member", ACTION = "test:member:update", PHONE = "phone";
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Patch(@FieldBinding(key = PHONE, uses = FieldUse.WRITE) String phone, @PublicField String expectedVersion) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private static final class API {
        @FieldControl(domain = AuthorizationDomain.PLATFORM, applicationCode = APP, resourceCode = RESOURCE,
                action = ACTION, use = FieldUse.WRITE, valueType = Patch.class)
        public void patch(Patch input) { }
    }

    @Test
    void rejectsUnknownBeforeBindingAndPreservesExplicitNull() throws Exception {
        var registry = new FieldBindingRegistry(new ObjectMapper());
        registry.register(Patch.class, new ResourceKey(AuthorizationDomain.PLATFORM, APP, RESOURCE), ACTION, FieldUse.WRITE);
        var advice = new FieldInputAdvice(new ObjectMapper(), registry);
        var parameter = new MethodParameter(API.class.getMethod("patch", Patch.class), 0);
        assertTrue(advice.supports(parameter, Patch.class, MappingJackson2HttpMessageConverter.class));
        assertThrows(SdkAuthorizationException.class, () -> advice.beforeBodyRead(message("{\"joinedAt\":null}"), parameter,
                Patch.class, MappingJackson2HttpMessageConverter.class));
        String payload = "{\"phone\":null,\"expectedVersion\":\"1\"}";
        assertEquals(payload, new String(advice.beforeBodyRead(message(payload), parameter, Patch.class,
                MappingJackson2HttpMessageConverter.class).getBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static HttpInputMessage message(String body) {
        return new HttpInputMessage() {
            @Override public InputStream getBody() { return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)); }
            @Override public HttpHeaders getHeaders() { return new HttpHeaders(); }
        };
    }
}
