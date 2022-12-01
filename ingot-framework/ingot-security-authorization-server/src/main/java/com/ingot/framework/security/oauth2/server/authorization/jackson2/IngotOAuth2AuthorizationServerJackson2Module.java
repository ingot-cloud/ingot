package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.ingot.framework.security.core.authority.ClientGrantedAuthority;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.jackson2.ClientGrantedAuthorityMixin;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

/**
 * <p>Description  : IngotOAuth2AuthorizationServerJackson2Module.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 4:18 下午.</p>
 */
public class IngotOAuth2AuthorizationServerJackson2Module extends OAuth2AuthorizationServerJackson2Module {

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.setMixInAnnotations(IngotUser.class, IngotUserMixin.class);
        context.setMixInAnnotations(Long.class, LongMixin.class);
        context.setMixInAnnotations(OAuth2UserDetailsAuthenticationToken.class,
                OAuth2UserDetailsAuthenticationTokenMixin.class);
        context.setMixInAnnotations(ClientGrantedAuthority.class, ClientGrantedAuthorityMixin.class);
    }
}
