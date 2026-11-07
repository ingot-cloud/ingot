package com.ingot.cloud.iam.api.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.cloud.iam.api.model.dto.social.SocialDetailsDTO;
import com.ingot.cloud.iam.api.model.dto.tenant.TenantDetailsDTO;
import com.ingot.framework.commons.jackson.InApiTimeModule;
import com.ingot.framework.commons.oss.OssService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证内部响应退出实体继承后仍保留字段、空值和UTC时间契约。</p>
 * @author jy
 * @since 1.0.0
 */
class InternalDetailsContractTest {
    private final OssService oss = mock(OssService.class);
    private final ObjectMapper mapper = mapper();

    private ObjectMapper mapper() {
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("ossService", oss);
        ObjectMapper configured = new ObjectMapper().registerModule(new JavaTimeModule())
                .registerModule(new InApiTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        configured.setHandlerInstantiator(new SpringHandlerInstantiator(beans));
        return configured;
    }

    @Test
    void tenantResponsePreservesLegacyOptionalFieldsAndUtcInstant() throws Exception {
        TenantDetailsDTO details = new TenantDetailsDTO();
        details.setId(42L);
        details.setName("组织");
        details.setAvatar("test-bucket/avatar.png");
        when(oss.getObjectURL("test-bucket/avatar.png")).thenReturn("https://test.invalid/avatar?signature=test");
        details.setCreatedAt(LocalDateTime.of(2026, 11, 7, 1, 2, 3));
        var json = mapper.readTree(mapper.writeValueAsString(details));
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id", "name", "code", "orgType", "avatar", "planId", "endAt", "status",
                "createdAt", "updatedAt", "deletedAt"), fields);
        assertEquals("2026-11-07T01:02:03Z", json.get("createdAt").asText());
        assertTrue(json.get("planId").isNull());
        assertEquals("https://test.invalid/avatar?signature=test", json.get("avatar").asText());
        assertEquals(Object.class, TenantDetailsDTO.class.getSuperclass());
    }

    @Test
    void socialResponseRetainsInternalConfigurationWithoutPersistenceMethods() throws Exception {
        SocialDetailsDTO details = new SocialDetailsDTO();
        details.setAppId("test-channel");
        details.setAppSecret("test-only-secret");
        var json = mapper.readTree(mapper.writeValueAsString(details));
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id", "tenantId", "appId", "appSecret", "redirectUrl", "name", "type", "status",
                "createdAt", "updatedAt", "deletedAt"), fields);
        assertEquals("test-only-secret", mapper.treeToValue(json, SocialDetailsDTO.class).getAppSecret());
        assertEquals(Object.class, SocialDetailsDTO.class.getSuperclass());
    }
}
