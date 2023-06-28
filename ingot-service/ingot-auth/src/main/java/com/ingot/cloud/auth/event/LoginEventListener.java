package com.ingot.cloud.auth.event;

import com.ingot.framework.core.model.event.LoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : LoginEventListener.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/28.</p>
 * <p>Time         : 9:15 AM.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginEventListener {

    @Async
    @Order
    @EventListener(LoginEvent.class)
    public void saveSysLog(LoginEvent event) {
        log.info("[LoginEventListener] - payload={}", event.getPayload());
    }
}
