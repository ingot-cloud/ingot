package com.ingot.cloud.pms.common.wechat;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

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

    private final SysSocialDetailsService sysSocialDetailsService;

    @Bean
    public WxMaService wxMaService() {
        WxMaService service = new WxMaServiceImpl();
        for (WxMaConfig config : wxConfigs) {
            service.addConfig(config.getAppid(), config);
        }
        return service;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 获取所有小程序配置
        List<SysSocialDetails> list = sysSocialDetailsService.list(Wrappers.<SysSocialDetails>lambdaQuery()
                .in(SysSocialDetails::getType, SocialTypeEnums.ADMIN_MINI_PROGRAM, SocialTypeEnums.APP_MINI_PROGRAM));
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
