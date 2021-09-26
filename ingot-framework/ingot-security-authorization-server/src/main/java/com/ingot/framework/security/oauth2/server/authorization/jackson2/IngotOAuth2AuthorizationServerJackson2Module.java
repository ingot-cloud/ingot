package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.ingot.framework.security.core.userdetails.IngotUser;
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
    }
}
