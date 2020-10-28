package com.ingot.framework.security.core.bus.event;

import lombok.Getter;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

/**
 * <p>Description  : RefreshJwtKeyApplicationEvent.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-25.</p>
 * <p>Time         : 11:15.</p>
 */
public class RefreshJwtKeyApplicationEvent extends RemoteApplicationEvent {

    @Getter
    private String message;

    public RefreshJwtKeyApplicationEvent() {
    }

    public RefreshJwtKeyApplicationEvent(Object source, String originService, String message) {
        super(source, originService, null);
        this.message = message;
    }
}
