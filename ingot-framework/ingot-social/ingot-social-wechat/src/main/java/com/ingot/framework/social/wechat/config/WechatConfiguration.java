package com.ingot.framework.social.wechat.config;

import cn.binarywang.wx.miniapp.api.WxMaQrcodeService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import com.ingot.cloud.pms.api.rpc.RemotePmsSocialDetailsService;
import com.ingot.framework.social.common.config.SocialCommonConfiguration;
import com.ingot.framework.social.common.provider.RemoteSocialDetailsProvider;
import com.ingot.framework.social.common.provider.SocialDetailsProvider;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import com.ingot.framework.social.wechat.api.WxMaConfigRefreshAPI;
import com.ingot.framework.social.wechat.core.WxMaConfigManager;
import com.ingot.framework.social.wechat.core.WxMaServiceHelper;
import com.ingot.framework.social.wechat.event.WechatConfigChangedListener;
import com.ingot.framework.social.wechat.properties.SocialWechatProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * <p>Description  : WechatConfiguration - 微信配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 16:30.</p>
 */
@Slf4j
@EnableAsync
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SocialWechatProperties.class)
@Import({WxMaConfigInitializer.class, SocialCommonConfiguration.class})
public class WechatConfiguration {

    // ==================== 社交详情提供者 ====================

    /**
     * 远程社交详情提供者（用于非PMS服务）
     * 条件：不存在其他 SocialDetailsProvider Bean
     * 注意：PMS服务会在自己的配置中注册 LocalSocialDetailsProvider
     */
    @Bean
    @ConditionalOnMissingBean(SocialDetailsProvider.class)
    public SocialDetailsProvider remoteSocialDetailsProvider(RemotePmsSocialDetailsService remotePmsSocialDetailsService) {
        log.info("WechatConfiguration - 使用远程社交详情提供者（通过RPC调用PMS）");
        return new RemoteSocialDetailsProvider(remotePmsSocialDetailsService);
    }

    // ==================== 核心服务 ====================

    /**
     * 微信小程序服务
     */
    @Bean
    public WxMaService wxMaService() {
        return new WxMaServiceImpl();
    }

    /**
     * 微信小程序二维码服务
     */
    @Bean
    public WxMaQrcodeService wxMaQrcodeService(WxMaService wxMaService) {
        return wxMaService.getQrcodeService();
    }

    /**
     * 微信配置管理器
     */
    @Bean
    public WxMaConfigManager wxMaConfigManager(
            WxMaService wxMaService,
            SocialDetailsProvider socialDetailsProvider) {
        return new WxMaConfigManager(wxMaService, socialDetailsProvider);
    }

    /**
     * 微信服务助手
     */
    @Bean
    public WxMaServiceHelper wxMaServiceHelper(WxMaService wxMaService, SocialWechatProperties properties) {
        return new WxMaServiceHelper(wxMaService, properties);
    }

    // ==================== 事件监听器 ====================

    /**
     * 微信配置变更事件监听器
     */
    @Bean
    public WechatConfigChangedListener wechatConfigChangedListener(WxMaConfigManager wxMaConfigManager) {
        return new WechatConfigChangedListener(wxMaConfigManager);
    }

    // ==================== API接口 ====================

    /**
     * 配置刷新管理API
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public WxMaConfigRefreshAPI wxMaConfigRefreshAPI(
            WxMaConfigManager wxMaConfigManager,
            SocialConfigMessagePublisher configMessagePublisher) {
        return new WxMaConfigRefreshAPI(wxMaConfigManager, configMessagePublisher);
    }
}
