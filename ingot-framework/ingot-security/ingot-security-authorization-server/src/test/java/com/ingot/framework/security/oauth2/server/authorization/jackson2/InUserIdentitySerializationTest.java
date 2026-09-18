package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.jackson2.InSecurityJackson2Modules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>使用 Redis SecurityContext 同一套模块验证刷新链路不会丢失或拼接成员身份。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class InUserIdentitySerializationTest {
    @Test
    void authenticationJsonRestoresTenantMemberIdentity() throws Exception {
        var context = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
        var user = InUser.stateless(1L, 10L, "web", "standard", "0", "account", List.of(), List.of(11L), Map.of())
                .toBuilder().authorizationContext(context).build();
        var mapper = mapper();
        var restored = mapper.readValue(mapper.writeValueAsBytes(user), InUser.class);
        assertEquals(context, restored.getAuthorizationContext());
        assertEquals(List.of(11L), restored.getDeptIds());
    }

    @Test
    void authenticationJsonRestoresPlatformWithoutInventingTenant() throws Exception {
        var context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
        var user = InUser.stateless(1L, null, "web", "standard", "0", "account", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(context).build();
        var mapper = mapper();
        var restored = mapper.readValue(mapper.writeValueAsBytes(user), InUser.class);
        assertEquals(context, restored.getAuthorizationContext());
        assertNull(restored.getTenantId());
    }

    @Test
    void authenticationJsonRestoresMutableDeptCollections() throws Exception {
        var user = InUser.stateless(1L, 10L, "web", "standard", "0", "account", List.of(),
                List.of(11L, 12L), Map.of(10L, List.of(11L)));
        var mapper = mapper();
        var restored = mapper.readValue(mapper.writeValueAsBytes(user), InUser.class);
        assertEquals(List.of(11L, 12L), restored.getDeptIds());
        assertEquals(List.of(11L), restored.getTenantDeptIds().get(10L));
    }

    private ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        InSecurityJackson2Modules.registerModules(mapper, InUserIdentitySerializationTest.class.getClassLoader());
        return mapper;
    }
}
