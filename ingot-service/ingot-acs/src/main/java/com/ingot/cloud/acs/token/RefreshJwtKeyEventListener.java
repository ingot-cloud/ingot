package com.ingot.cloud.acs.token;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.bus.event.RefreshJwtKeyApplicationEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : RefreshJwtKeyEventListener.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 22:10.</p>
 */
@Slf4j
@AllArgsConstructor
@Component
public class RefreshJwtKeyEventListener implements ApplicationListener<RefreshJwtKeyApplicationEvent> {
    private final BusProperties properties;
    private final JwtAccessTokenConverter jwtAccessTokenConverter;
    private final JwtKeyGenerator jwtKeyGenerator;

    @Override public void onApplicationEvent(RefreshJwtKeyApplicationEvent event) {
        String currentService = properties.getId();
        String originService = event.getOriginService();
        if (StrUtil.equals(currentService, originService)){
            log.info(">>> RefreshJwtKeyEventListener - 当前服务不刷新。");
            return;
        }

        try {
            jwtAccessTokenConverter.setKeyPair(jwtKeyGenerator.getKeyPair());
            log.info(">>> RefreshJwtKeyEventListener - 服务={} 刷新成功。", currentService);
        } catch (Exception e) {
            log.error(">>> RefreshJwtKeyEventListener - 服务={} 刷新失败e={}", currentService, e);
        }
    }
}
