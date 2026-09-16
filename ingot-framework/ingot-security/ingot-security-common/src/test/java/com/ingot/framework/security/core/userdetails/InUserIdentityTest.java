package com.ingot.framework.security.core.userdetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证成员身份在用户复制和会话序列化中不丢失，拒绝跨域上下文拼接。</p>
 * @author jy
 * @since 1.0.0
 */
class InUserIdentityTest {
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final AuthorizationContext PLATFORM = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");

    @Test
    void copyingProtocolAttributesPreservesIdentity() {
        InUser user = user(1L, 10L, List.of(), Map.of()).toBuilder().authorizationContext(TENANT).build();
        assertEquals(TENANT, user.toBuilder().clientId("other-client").build().getAuthorizationContext());
        assertThrows(IllegalArgumentException.class, () -> user.toBuilder().tenantId(20L).build());
    }

    @Test
    void accountCannotBorrowAnotherMembersContext() {
        assertThrows(IllegalArgumentException.class,
                () -> user(2L, 10L, List.of(), Map.of()).toBuilder().authorizationContext(TENANT).build());
    }

    @Test
    void platformRequiresNullTenantAndNoDepartments() {
        assertEquals(PLATFORM, user(1L, null, List.of(), Map.of()).toBuilder().authorizationContext(PLATFORM).build().getAuthorizationContext());
        assertThrows(IllegalArgumentException.class,
                () -> user(1L, 10L, List.of(), Map.of()).toBuilder().authorizationContext(PLATFORM).build());
        assertThrows(IllegalArgumentException.class,
                () -> user(1L, null, List.of(3L), Map.of()).toBuilder().authorizationContext(PLATFORM).build());
    }

    @Test
    void mixedTenantDepartmentMapsCannotEnterIamIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> user(1L, 10L, List.of(), Map.of(20L, List.of(5L))).toBuilder().authorizationContext(TENANT).build());
    }

    @Test
    void callerCannotMutateBoundDepartmentList() {
        var departments = new ArrayList<>(List.of(3L));
        InUser user = user(1L, 10L, departments, Map.of()).toBuilder().authorizationContext(TENANT).build();
        departments.add(4L);
        assertEquals(List.of(3L), user.getDeptIds());
        assertThrows(UnsupportedOperationException.class, () -> user.getDeptIds().add(4L));
    }

    @Test
    void onlineSessionJsonPreservesDomainMemberAndAccount() throws Exception {
        var mapper = new ObjectMapper();
        OnlineToken session = OnlineToken.builder().userId(1L).tenantId(10L).authorizationContext(TENANT).build();
        OnlineToken restored = mapper.readValue(mapper.writeValueAsBytes(session), OnlineToken.class);
        assertEquals(TENANT, restored.getAuthorizationContext());
    }

    @Test
    void existingNonIamUsersNeverInferMemberIdentity() {
        assertNull(user(1L, 10L, List.of(), Map.of()).getAuthorizationContext());
    }

    private InUser user(Long accountId, Long tenantId, List<Long> departments, Map<Long, List<Long>> tenantDepartments) {
        return InUser.stateless(accountId, tenantId, "web", "standard", "admin", "user", List.of(), departments, tenantDepartments);
    }
}
