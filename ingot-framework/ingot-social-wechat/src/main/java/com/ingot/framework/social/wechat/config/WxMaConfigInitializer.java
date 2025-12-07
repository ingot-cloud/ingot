package com.ingot.framework.social.wechat.config;

import com.ingot.framework.social.wechat.core.WxMaConfigManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : 微信配置初始化器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 10:30.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxMaConfigInitializer implements ApplicationListener<ApplicationReadyEvent> {
    private final WxMaConfigManager wxMaConfigManager;

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        log.info("WxMaConfigInitializer - 应用启动完成，开始初始化微信配置...");
        wxMaConfigManager.initConfigs();
        log.info("WxMaConfigInitializer - 微信配置初始化完成");
    }
}

