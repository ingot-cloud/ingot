package com.ingot.framework.security.bus.jwt;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;

/**
 * <p>Description  : JWKSetUpdateEvent.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/28.</p>
 * <p>Time         : 4:58 下午.</p>
 */
public class JWKSetUpdateEvent extends RemoteApplicationEvent {

    public JWKSetUpdateEvent() {
    }

    public JWKSetUpdateEvent(Object source, String originService) {
        super(source, originService, DEFAULT_DESTINATION_FACTORY.getDestination(null));
    }
}
