package com.ingot.framework.social.wechat.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.api.rpc.RemotePmsSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : 微信小程序配置管理器，支持动态配置更新.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 17:00.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class WxMaConfigManager {
    /**
     * 存储所有配置的Map，key为appId
     */
    private final Map<String, WxMaConfig> configMap = new ConcurrentHashMap<>();

    private final WxMaService wxMaService;
    private final RemotePmsSocialDetailsService remotePmsSocialDetailsService;

    /**
     * 初始化加载所有配置
     */
    public void initConfigs() {
        log.info("WxMaConfigManager - 开始初始化微信小程序配置");
        loadConfigsFromRemote();
        log.info("WxMaConfigManager - 初始化完成，共加载{}个配置", configMap.size());
    }

    /**
     * 从远程服务加载配置
     */
    private void loadConfigsFromRemote() {
        List<SysSocialDetails> list = remotePmsSocialDetailsService
                .getSocialDetailsByType(SocialTypeEnum.WECHAT_MINI_PROGRAM.getValue())
                .ifError(response -> {
                    log.error("WxMaConfigManager - 获取远程社交信息失败 - {}", response.getMessage());
                    throw new RuntimeException("获取微信小程序配置失败: " + response.getMessage());
                }).getData();

        if (CollUtil.isEmpty(list)) {
            log.warn("WxMaConfigManager - 未获取到任何微信小程序配置");
            return;
        }

        list.forEach(this::addOrUpdateConfig);
    }

    /**
     * 刷新所有配置（从远程重新加载）
     */
    public synchronized void refreshAllConfigs() {
        log.info("WxMaConfigManager - 开始刷新所有配置");
        
        // 获取最新配置
        List<SysSocialDetails> remoteConfigs = remotePmsSocialDetailsService
                .getSocialDetailsByType(SocialTypeEnum.WECHAT_MINI_PROGRAM.getValue())
                .ifError(response -> {
                    log.error("WxMaConfigManager - 刷新配置失败 - {}", response.getMessage());
                    throw new RuntimeException("刷新微信小程序配置失败: " + response.getMessage());
                }).getData();

        if (CollUtil.isEmpty(remoteConfigs)) {
            log.warn("WxMaConfigManager - 远程配置为空，清除所有本地配置");
            clearAllConfigs();
            return;
        }

        // 获取远程配置的appId集合
        Map<String, SysSocialDetails> remoteConfigMap = remoteConfigs.stream()
                .collect(Collectors.toMap(SysSocialDetails::getAppId, detail -> detail));

        // 移除本地存在但远程不存在的配置
        configMap.keySet().removeIf(appId -> {
            if (!remoteConfigMap.containsKey(appId)) {
                removeConfig(appId);
                return true;
            }
            return false;
        });

        // 添加或更新配置
        remoteConfigs.forEach(this::addOrUpdateConfig);

        log.info("WxMaConfigManager - 配置刷新完成，当前共{}个配置", configMap.size());
    }

    /**
     * 添加或更新配置
     *
     * @param socialDetails 社交配置详情
     */
    public synchronized void addOrUpdateConfig(SysSocialDetails socialDetails) {
        if (socialDetails == null || StrUtil.isBlank(socialDetails.getAppId())) {
            log.warn("WxMaConfigManager - 配置信息无效，忽略");
            return;
        }

        String appId = socialDetails.getAppId();
        String appSecret = socialDetails.getAppSecret();

        if (StrUtil.isBlank(appSecret)) {
            log.warn("WxMaConfigManager - AppId[{}]的AppSecret为空，忽略", appId);
            return;
        }

        try {
            WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
            config.setAppid(appId);
            config.setSecret(appSecret);

            // 添加或更新到WxMaService
            wxMaService.addConfig(appId, config);
            configMap.put(appId, config);

            log.info("WxMaConfigManager - 成功添加/更新配置: AppId={}", appId);
        } catch (Exception e) {
            log.error("WxMaConfigManager - 添加/更新配置失败: AppId={}", appId, e);
        }
    }

    /**
     * 移除配置
     *
     * @param appId 应用ID
     */
    public synchronized void removeConfig(String appId) {
        if (StrUtil.isBlank(appId)) {
            return;
        }

        try {
            wxMaService.removeConfig(appId);
            configMap.remove(appId);
            log.info("WxMaConfigManager - 成功移除配置: AppId={}", appId);
        } catch (Exception e) {
            log.error("WxMaConfigManager - 移除配置失败: AppId={}", appId, e);
        }
    }

    /**
     * 清除所有配置
     */
    public synchronized void clearAllConfigs() {
        configMap.keySet().forEach(this::removeConfig);
        log.info("WxMaConfigManager - 已清除所有配置");
    }

    /**
     * 获取当前所有配置的AppId列表
     *
     * @return AppId列表
     */
    public List<String> getAllAppIds() {
        return CollUtil.newArrayList(configMap.keySet());
    }

    /**
     * 检查是否存在指定AppId的配置
     *
     * @param appId 应用ID
     * @return 是否存在
     */
    public boolean hasConfig(String appId) {
        return configMap.containsKey(appId);
    }

    /**
     * 获取配置数量
     *
     * @return 配置数量
     */
    public int getConfigCount() {
        return configMap.size();
    }
}


