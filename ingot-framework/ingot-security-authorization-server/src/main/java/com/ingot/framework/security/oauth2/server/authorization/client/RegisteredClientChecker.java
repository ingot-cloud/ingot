package com.ingot.framework.security.oauth2.server.authorization.client;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * <p>Description  : RegisteredClientChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 6:35 PM.</p>
 */
public interface RegisteredClientChecker {

    /**
     * Examines the Client
     *
     * @param client {@link RegisteredClient}
     */
    void check(RegisteredClient client);
}
