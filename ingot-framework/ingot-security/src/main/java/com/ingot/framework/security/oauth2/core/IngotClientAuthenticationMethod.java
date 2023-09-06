package com.ingot.framework.security.oauth2.core;

import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * <p>Description  : IngotClientAuthenticationMethod.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/5.</p>
 * <p>Time         : 10:16 PM.</p>
 */
public interface IngotClientAuthenticationMethod {
    ClientAuthenticationMethod PRE_AUTH = new ClientAuthenticationMethod("pre_auth");
}
