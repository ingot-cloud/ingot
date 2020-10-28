package com.ingot.framework.security.core.bus;

import com.ingot.framework.security.core.bus.event.RefreshJwtKeyApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * <p>Description  : RefreshJwtKeySender.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-25.</p>
 * <p>Time         : 16:08.</p>
 */
public class RefreshJwtKeySender {
    private final String appId;
    private final ApplicationEventPublisher publisher;

    public RefreshJwtKeySender(String appId, ApplicationEventPublisher publisher){
        this.appId = appId;
        this.publisher = publisher;
    }

    public void send(){
        send("");
    }

    public void send(String message){
        publisher.publishEvent(new RefreshJwtKeyApplicationEvent(this, appId, message));
    }
}
