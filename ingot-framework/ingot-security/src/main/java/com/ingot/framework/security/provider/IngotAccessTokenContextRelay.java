package com.ingot.framework.security.provider;

import org.springframework.cloud.security.oauth2.client.AccessTokenContextRelay;
import org.springframework.security.oauth2.client.OAuth2ClientContext;

/**
 * <p>Description  : IngotAccessTokenContextRelay.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/15.</p>
 * <p>Time         : 5:36 PM.</p>
 */
public class IngotAccessTokenContextRelay extends AccessTokenContextRelay {

    private OAuth2ClientContext context;

    public IngotAccessTokenContextRelay(OAuth2ClientContext context) {
        super(context);
        this.context = context;
    }

    /**
     * Force to copy an access token from the security context into the oauth2 context.
     */
    public void forceCopyToken(){
        context.setAccessToken(null);
        copyToken();
    }
}
