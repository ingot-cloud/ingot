package com.ingot.framework.security.core.bus.listener;

import com.ingot.framework.security.core.bus.event.RefreshJwtKeyApplicationEvent;
import com.ingot.framework.security.service.JwtKeyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.util.StringUtils;

/**
 * <p>Description  : RefreshJwtKeyEventListener.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-25.</p>
 * <p>Time         : 11:23.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RefreshJwtKeyEventListener implements ApplicationListener<RefreshJwtKeyApplicationEvent> {
    private final JwtAccessTokenConverter jwtTokenEnhancer;
    private final JwtKeyService jwtKeyService;

    @Override public void onApplicationEvent(RefreshJwtKeyApplicationEvent event) {
        log.info(">>> RefreshJwtKeyEventListener - 开始更新 jwt key，事件来源={}", event.getOriginService());
        String keyValue = jwtKeyService.fetchFromCache();
        if (StringUtils.hasText(keyValue) && !keyValue.startsWith("-----BEGIN")) {
            jwtTokenEnhancer.setSigningKey(keyValue);
        }
        if (keyValue != null) {
            jwtTokenEnhancer.setVerifierKey(keyValue);
        }
        log.info(">>> RefreshJwtKeyEventListener - 更新成功。");
    }

}
