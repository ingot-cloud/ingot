package com.ingot.cloud.member.common.wechat;

import java.util.ArrayList;
import java.util.List;

import cn.binarywang.wx.miniapp.api.WxMaQrcodeService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberSocialDetails;
import com.ingot.cloud.member.service.domain.MemberSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : WechatConfiguration.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/18.</p>
 * <p>Time         : 15:39.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WechatProperties.class)
@RequiredArgsConstructor
public class WechatConfiguration implements InitializingBean {
    private final static List<WxMaConfig> wxConfigs = new ArrayList<>();

    private final MemberSocialDetailsService sysSocialDetailsService;

    @Bean
    public WxMaService wxMaService() {
        WxMaService service = new WxMaServiceImpl();
        for (WxMaConfig config : wxConfigs) {
            service.addConfig(config.getAppid(), config);
        }
        return service;
    }

    @Bean
    public WxMaQrcodeService wxMaQrcodeService(WxMaService service) {
        return service.getQrcodeService();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 获取所有小程序配置
        List<MemberSocialDetails> list = sysSocialDetailsService.list(Wrappers.<MemberSocialDetails>lambdaQuery()
                .in(MemberSocialDetails::getType, SocialTypeEnum.WECHAT_MINI_PROGRAM));
        if (CollUtil.isEmpty(list)) {
            return;
        }

        list.stream()
                .map(social -> {
                    WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
                    config.setAppid(social.getAppId());
                    config.setSecret(social.getAppSecret());
                    return config;
                }).forEach(config -> {
                    if (wxConfigs.stream().anyMatch(item -> StrUtil.equals(item.getAppid(), config.getAppid()))) {
                        return;
                    }
                    wxConfigs.add(config);
                });
    }
}
