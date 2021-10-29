package com.ingot.framework.security.bus.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * <p>Description  : JWTSetUpdateSender.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/28.</p>
 * <p>Time         : 5:11 下午.</p>
 */
@RequiredArgsConstructor
public class JWKSetUpdateSender {
    private final String appId;
    private final ApplicationEventPublisher publisher;

    public void exec() {
        publisher.publishEvent(new JWKSetUpdateEvent(this, appId));
    }
}
