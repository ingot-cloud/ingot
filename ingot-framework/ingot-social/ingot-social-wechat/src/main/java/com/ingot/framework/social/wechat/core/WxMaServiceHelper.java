package com.ingot.framework.social.wechat.core;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.social.wechat.properties.SocialWechatProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.MessageSourceAccessor;

/**
 * <p>Description  : WxMaServiceHelper.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 12:19.</p>
 */
@RequiredArgsConstructor
public class WxMaServiceHelper {
    private final WxMaService wxMaService;
    private final SocialWechatProperties properties;

    /**
     * 获取当前小程序服务
     *
     * @return {@link WxMaService}
     */
    public WxMaService getActiveService() {
        MessageSourceAccessor accessor = SocialWechatMessageSource.getAccessor();
        if (!StrUtil.isBlank(properties.getMiniProgramAppId())) {
            throw new RuntimeException(accessor.getMessage("SocialWechatProperties.miniAppIdEmpty"));
        }
        return wxMaService.switchoverTo(properties.getMiniProgramAppId());
    }
}
