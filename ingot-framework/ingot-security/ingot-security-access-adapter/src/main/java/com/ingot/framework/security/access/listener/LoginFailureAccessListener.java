package com.ingot.framework.security.access.listener;

import com.ingot.framework.commons.model.common.AuthFailureDTO;
import com.ingot.framework.commons.model.common.AuthSuccessDTO;
import com.ingot.framework.commons.model.event.LoginFailureEvent;
import com.ingot.framework.commons.model.event.LoginSuccessEvent;
import com.ingot.framework.security.access.model.LoginFailureContext;
import com.ingot.framework.security.access.service.LoginFailureProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;

/**
 * 登录成功/失败访问防护监听器。
 *
 * <p>由 {@link com.ingot.framework.security.access.config.AccessAdapterAutoConfiguration} 注册，
 * 勿使用 {@code @Component}（框架包不在应用扫描路径内）。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class LoginFailureAccessListener {

    private final LoginFailureProtectionService protectionService;

    @Async
    @Order
    @EventListener(LoginFailureEvent.class)
    public void onLoginFailure(LoginFailureEvent event) {
        AuthFailureDTO payload = event.payload();
        if (payload == null) {
            return;
        }
        protectionService.recordFailure(toContext(payload));
    }

    @Async
    @Order
    @EventListener(LoginSuccessEvent.class)
    public void onLoginSuccess(LoginSuccessEvent event) {
        AuthSuccessDTO payload = event.payload();
        if (payload == null) {
            return;
        }
        LoginFailureContext ctx = new LoginFailureContext(
                payload.getIp(),
                null,
                null,
                payload.getUsername(),
                payload.getUserType()
        );
        protectionService.recordSuccess(ctx);
    }

    private LoginFailureContext toContext(AuthFailureDTO payload) {
        return new LoginFailureContext(
                payload.getIp(),
                payload.getDeviceId(),
                payload.getClientId(),
                payload.getUsername(),
                payload.getUserType()
        );
    }
}
