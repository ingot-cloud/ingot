package com.ingot.cloud.iam.web.v1;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.account.CurrentAccountService;
import com.ingot.cloud.iam.session.SessionService;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.InOAuth2ResourceProperties;
import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationToken;
import com.ingot.framework.security.web.InTokenAuthFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>实际当前账号 Controller 的改密标记只允许 GET/PUT，bootstrap/profile/capabilities 在门禁处拒绝。</p>
 * @author jy
 * @since 1.0.0
 */
class PasswordChangeBoundaryTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @SuppressWarnings("unchecked")
    @Test void passwordMethodsStayAuthenticatedAndBusinessDataIsNeverLoaded() throws Exception {
        var context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2");
        var sessions = mock(SessionService.class);
        var accounts = mock(CurrentAccountService.class);
        when(accounts.passwordState()).thenReturn(new PasswordChangeState(context, true));
        var api = new CurrentSessionAPI(sessions, accounts);
        var handler = mock(RequestMappingHandlerMapping.class);
        when(handler.getHandlerMethods()).thenReturn(Map.of(
                RequestMappingInfo.paths("/v1/me/password").methods(RequestMethod.GET).build(),
                new HandlerMethod(api, CurrentSessionAPI.class.getMethod("passwordState")),
                RequestMappingInfo.paths("/v1/me/password").methods(RequestMethod.PUT).build(),
                new HandlerMethod(api, CurrentSessionAPI.class.getMethod("updatePassword", CurrentPasswordInput.class))));
        var application = mock(WebApplicationContext.class);
        when(application.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class)).thenReturn(handler);
        var resolver = new PermitResolver(application, new InOAuth2ResourceProperties());
        resolver.afterPropertiesSet();
        var source = mock(TrustedAuthoritySource.class);
        when(source.requiresPasswordChange()).thenReturn(true);
        ObjectProvider<TrustedAuthoritySource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(source);
        var filter = new InTokenAuthFilter(resolver.publicRequestMatcher(), resolver.passwordChangeRequestMatcher(), provider);
        var mvc = MockMvcBuilders.standaloneSetup(api).addFilters(filter).build();
        var user = InUser.stateless(1L, null, "web", "standard", "0", "test", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(context).build();
        var jwt = Jwt.withTokenValue("test").header("alg", "none").subject("test").claim(JwtClaimNamesExtension.SID, "sid").build();
        SecurityContextHolder.getContext().setAuthentication(new InJwtAuthenticationToken(jwt, user, List.of()));
        for (String path : List.of("/v1/me/bootstrap", "/v1/me/profile", "/v1/me/capabilities")) {
            mvc.perform(get(path)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PasswordChangeRequired"));
        }
        mvc.perform(post("/v1/me/password")).andExpect(status().isForbidden());
        mvc.perform(get("/v1/me/password")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true)).andExpect(jsonPath("$.data.profile").doesNotExist());
        mvc.perform(put("/v1/me/password").contentType("application/json")
                .content("{\"newPassword\":\"new-password\",\"confirmPassword\":\"new-password\"}"))
                .andExpect(status().isOk());
        verifyNoInteractions(sessions);
        verify(accounts).updatePassword(new CurrentPasswordInput(null, "new-password", "new-password"));
    }
}
