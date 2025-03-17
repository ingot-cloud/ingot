package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.security.core.authority.AllowTenantGrantedAuthority;
import com.ingot.framework.security.core.authority.ClientGrantedAuthority;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.jackson2.ClientGrantedAuthorityMixin;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

/**
 * <p>Description  : IngotOAuth2AuthorizationServerJackson2Module.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 4:18 下午.</p>
 */
public class InOAuth2AuthorizationServerJackson2Module extends OAuth2AuthorizationServerJackson2Module {

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.setMixInAnnotations(InUser.class, InUserMixin.class);
        context.setMixInAnnotations(Long.class, LongMixin.class);
        context.setMixInAnnotations(OAuth2UserDetailsAuthenticationToken.class,
                OAuth2UserDetailsAuthenticationTokenMixin.class);
        context.setMixInAnnotations(OAuth2PreAuthorizationCodeRequestAuthenticationToken.class,
                OAuth2PreAuthorizationCodeRequestAuthenticationTokenMixin.class);
        context.setMixInAnnotations(AllowTenantDTO.class, AllowTenantDTOMixin.class);
        context.setMixInAnnotations(ClientGrantedAuthority.class, ClientGrantedAuthorityMixin.class);
        context.setMixInAnnotations(AllowTenantGrantedAuthority.class, AllowTenantGrantedAuthorityMixin.class);
    }
}
