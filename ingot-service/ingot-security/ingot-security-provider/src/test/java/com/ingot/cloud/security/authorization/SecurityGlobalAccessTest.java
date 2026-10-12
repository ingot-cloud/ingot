package com.ingot.cloud.security.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证全局配置不能借空范围、跨域或过期决策放行。</p>
 * @author jy
 * @since 1.0.0
 */
class SecurityGlobalAccessTest {
    private static final IamAction READ = IamAction.PLATFORM_CREDENTIAL_POLICY_READ;

    private SecurityGlobalAccess access(boolean allowed, List<ScopeCondition> scope, AuthorizationDomain domain, Instant deadline) {
        var client = mock(AuthorizationClient.class);
        var fields = new FieldPolicyDecision(Map.of(), Map.of(), List.of(), Map.of(), Map.of(), FieldMergeMode.GRANTS, deadline);
        var key = SecurityResources.key(READ);
        when(client.evaluate(new AuthorizationRequest(key,List.of(READ.getCode())))).thenReturn(new AuthorizationDecision(key,
                new AuthorizationContext(domain,domain == AuthorizationDomain.TENANT ? "9" : null,"1","2"),
                Map.of(READ.getCode(),new ActionDecision(allowed,false,scope,fields)),"1",deadline));
        return new SecurityGlobalAccess(new AuthorizationAccess(client));
    }

    @Test void allowsOnlyEffectiveAll() {
        assertDoesNotThrow(() -> access(true,List.of(new ScopeCondition(true,List.of(),null,List.of())),AuthorizationDomain.PLATFORM,Instant.now().plusSeconds(30)).require(READ));
        assertThrows(SdkAuthorizationException.class,() -> access(true,List.of(),AuthorizationDomain.PLATFORM,Instant.now().plusSeconds(30)).require(READ));
        assertThrows(SdkAuthorizationException.class,() -> access(true,List.of(new ScopeCondition(false,List.of("1"),null,List.of())),AuthorizationDomain.PLATFORM,Instant.now().plusSeconds(30)).require(READ));
    }

    @Test void deniesOtherDomainsRevocationAndExpiry() {
        var all=List.of(new ScopeCondition(true,List.of(),null,List.of()));
        assertThrows(SdkAuthorizationException.class,() -> access(true,all,AuthorizationDomain.TENANT,Instant.now().plusSeconds(30)).require(READ));
        assertThrows(SdkAuthorizationException.class,() -> access(false,all,AuthorizationDomain.PLATFORM,Instant.now().plusSeconds(30)).require(READ));
        assertThrows(SdkAuthorizationException.class,() -> access(true,all,AuthorizationDomain.PLATFORM,Instant.now().minusSeconds(1)).require(READ));
    }

    @Test void dependencyFailureNeverExecutesBusiness() {
        var client=mock(AuthorizationClient.class);
        when(client.evaluate(any())).thenThrow(new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE));
        assertThrows(SdkAuthorizationException.class,() -> new SecurityGlobalAccess(new AuthorizationAccess(client)).require(READ));
    }

    @Test void registryHasSixAllResourcesAndSixteenExactActionsWithTrustedModes() {
        var descriptors=SecurityResources.descriptors();
        assertEquals(6,descriptors.size());
        assertEquals(16,descriptors.stream().mapToInt(d -> d.actions().size()).sum());
        for (var resource:descriptors) {
            assertEquals(SecurityResources.APPLICATION,resource.key().applicationCode());
            assertEquals(List.of(ScopeKind.ALL),resource.scopeCapabilities());
            assertEquals(ExecutionMode.READ_ONLY,resource.actions().stream().filter(a -> a.code().equals(resource.readAction())).findFirst().orElseThrow().mode());
            assertTrue(resource.actions().stream().filter(a -> !a.code().equals(resource.readAction())).allMatch(a -> a.mode()==ExecutionMode.MUTATING));
        }
    }
}
