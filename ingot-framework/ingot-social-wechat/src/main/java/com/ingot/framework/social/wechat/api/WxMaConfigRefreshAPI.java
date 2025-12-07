package com.ingot.framework.social.wechat.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.social.wechat.core.WxMaConfigManager;
import com.ingot.framework.social.wechat.publisher.SocialConfigMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : 微信小程序配置刷新API.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 17:45.</p>
 */
@Slf4j
@RestController
@RequestMapping("/social/wechat/config")
@RequiredArgsConstructor
public class WxMaConfigRefreshAPI implements RShortcuts {
    private final WxMaConfigManager wxMaConfigManager;
    private final SocialConfigMessagePublisher configMessagePublisher;

    /**
     * 手动刷新本地配置（仅刷新当前服务实例）
     */
    @PostMapping("/refresh/local")
    public R<?> refreshLocal() {
        try {
            log.info("WxMaConfigRefreshAPI - 开始手动刷新本地微信小程序配置");
            wxMaConfigManager.refreshAllConfigs();
            return ok("本地配置刷新成功");
        } catch (Exception e) {
            log.error("WxMaConfigRefreshAPI - 刷新本地配置失败", e);
            return error(e.getMessage());
        }
    }

    /**
     * 手动刷新所有服务实例的配置（通过消息队列广播）
     */
    @PostMapping("/refresh/all")
    public R<?> refreshAll() {
        try {
            log.info("WxMaConfigRefreshAPI - 开始广播刷新所有服务实例的微信小程序配置");
            configMessagePublisher.publishRefreshAll();
            return ok("配置刷新广播已发送");
        } catch (Exception e) {
            log.error("WxMaConfigRefreshAPI - 广播刷新配置失败", e);
            return error(e.getMessage());
        }
    }

    /**
     * 获取当前配置状态
     */
    @GetMapping("/status")
    public R<Map<String, Object>> getConfigStatus() {
        try {
            List<String> appIds = wxMaConfigManager.getAllAppIds();
            int count = wxMaConfigManager.getConfigCount();

            Map<String, Object> status = new HashMap<>();
            status.put("count", count);
            status.put("appIds", appIds);
            status.put("timestamp", System.currentTimeMillis());

            log.debug("WxMaConfigRefreshAPI - 当前配置数量: {}", count);
            return ok(status);
        } catch (Exception e) {
            log.error("WxMaConfigRefreshAPI - 获取配置状态失败", e);
            return error(e.getMessage());
        }
    }
}


