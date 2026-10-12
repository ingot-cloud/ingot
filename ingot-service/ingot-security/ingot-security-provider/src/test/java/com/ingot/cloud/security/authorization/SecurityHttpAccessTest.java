package com.ingot.cloud.security.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.security.service.account.AccountLockoutPolicyAdminService;
import com.ingot.cloud.security.web.platform.security.AccountLockoutPolicyAPI;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>通过真实 HTTP 入口验证只读角色及授权故障不会调用写服务。</p>
 * @author jy
 * @since 1.0.0
 */
class SecurityHttpAccessTest {
    @Test void readOnlyAndUnavailableAreEnforcedAtHttpBoundary() throws Exception {
        var client=mock(AuthorizationClient.class);
        when(client.evaluate(any())).thenAnswer(invocation -> {
            AuthorizationRequest request=invocation.getArgument(0);
            var action=request.actionCodes().getFirst();
            boolean read=action.equals(IamAction.VALUE_PLATFORM_LOCKOUT_POLICY_READ);
            var fields=new FieldPolicyDecision(Map.of(),Map.of(),List.of(),Map.of(),Map.of(),FieldMergeMode.GRANTS);
            return new AuthorizationDecision(request.resource(),new AuthorizationContext(AuthorizationDomain.PLATFORM,null,"1","2"),
                    Map.of(action,new ActionDecision(read,false,List.of(new ScopeCondition(true,List.of(),null,List.of())),fields)),"1",Instant.now().plusSeconds(30));
        });
        var service=mock(AccountLockoutPolicyAdminService.class);
        when(service.list()).thenReturn(List.of());
        var mvc=MockMvcBuilders.standaloneSetup(new AccountLockoutPolicyAPI(new SecurityGlobalAccess(new AuthorizationAccess(client)),service))
                .setControllerAdvice(new SdkAuthorizationErrorHandler()).build();
        mvc.perform(get("/platform/security/account/lockout-policies")).andExpect(status().isOk());
        mvc.perform(put("/platform/security/account/lockout-policies").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        verify(service,never()).upsert(any());
        doThrow(new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE)).when(client).evaluate(any());
        mvc.perform(get("/platform/security/account/lockout-policies")).andExpect(status().isServiceUnavailable());
        verify(service,times(1)).list();
    }
}
