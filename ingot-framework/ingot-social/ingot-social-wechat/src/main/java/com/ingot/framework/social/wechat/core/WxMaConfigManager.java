package com.ingot.framework.social.wechat.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.social.common.provider.SocialDetailsProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : 微信小程序配置管理器，支持动态配置更新.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 17:00.</p>
 */
@Slf4j
public class WxMaConfigManager {
    /**
     * 存储所有配置的Map，key为appId
     */
    private final Map<String, WxMaConfig> configMap = new ConcurrentHashMap<>();

    private final WxMaService wxMaService;
    private final SocialDetailsProvider socialDetailsProvider;

    /**
     * 配置是否已初始化
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 重试次数计数器
     */
    private final AtomicInteger retryCount = new AtomicInteger(0);

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_TIMES = 10;

    /**
     * 重试间隔（秒）
     */
    private static final int RETRY_INTERVAL_SECONDS = 30;

    /**
     * 定时任务执行器
     */
    private ScheduledExecutorService scheduler;

    public WxMaConfigManager(WxMaService wxMaService, SocialDetailsProvider socialDetailsProvider) {
        this.wxMaService = wxMaService;
        this.socialDetailsProvider = socialDetailsProvider;
    }

    /**
     * 初始化加载所有配置（异步，非阻塞）
     */
    public void initConfigs() {
        log.info("WxMaConfigManager - 开始异步初始化微信小程序配置");
        
        // 异步加载配置，不阻塞服务启动
        tryLoadConfigs();
    }

    /**
     * 尝试加载配置（带重试机制）
     */
    private void tryLoadConfigs() {
        // 如果已经初始化成功，不再重试
        if (initialized.get()) {
            log.debug("WxMaConfigManager - 配置已初始化，跳过重试");
            stopRetryScheduler();
            return;
        }
        
        try {
            List<SysSocialDetails> details = socialDetailsProvider.getDetailsByType(SocialTypeEnum.WECHAT_MINI_PROGRAM);
            
            if (CollUtil.isNotEmpty(details)) {
                details.forEach(this::addOrUpdateConfig);
                initialized.set(true);
                retryCount.set(0);
                log.info("WxMaConfigManager - 初始化成功，共加载{}个配置", configMap.size());
                
                // 停止重试任务
                stopRetryScheduler();
            } else {
                log.warn("WxMaConfigManager - 未获取到任何配置");
                handleLoadFailure();
            }
            
        } catch (Exception e) {
            log.warn("WxMaConfigManager - 初始化配置失败: {}", e.getMessage());
            handleLoadFailure();
        }
    }

    /**
     * 处理加载失败，启动重试机制
     */
    private void handleLoadFailure() {
        int currentRetry = retryCount.incrementAndGet();
        
        if (currentRetry <= MAX_RETRY_TIMES) {
            log.info("WxMaConfigManager - 将在{}秒后进行第{}次重试（最多{}次）", 
                    RETRY_INTERVAL_SECONDS, currentRetry, MAX_RETRY_TIMES);
            scheduleRetry();
        } else {
            log.warn("WxMaConfigManager - 已达到最大重试次数{}，停止主动重试", MAX_RETRY_TIMES);
            log.info("WxMaConfigManager - 配置将在以下情况下自动同步：");
            log.info("  1. PMS服务启动时会广播同步消息");
            log.info("  2. 配置变更时会收到通知消息");
            log.info("  3. 手动调用刷新API: POST /social/wechat/config/refresh/local");
            stopRetryScheduler();
        }
    }

    /**
     * 调度重试任务
     */
    private synchronized void scheduleRetry() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = new ScheduledThreadPoolExecutor(1, 
                ThreadFactoryBuilder.create()
                    .setNamePrefix("wx-config-retry-")
                    .setDaemon(true)
                    .build()
            );
        }
        
        scheduler.schedule(this::tryLoadConfigs, RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 停止重试调度器
     */
    private synchronized void stopRetryScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.debug("WxMaConfigManager - 重试调度器已停止");
        }
    }

    /**
     * 刷新所有配置（从提供者重新加载）
     */
    public synchronized void refreshAllConfigs() {
        log.info("WxMaConfigManager - 开始刷新所有配置");
        
        try {
            // 获取最新配置
            List<SysSocialDetails> remoteConfigs = socialDetailsProvider.getDetailsByType(SocialTypeEnum.WECHAT_MINI_PROGRAM);

            if (CollUtil.isEmpty(remoteConfigs)) {
                log.warn("WxMaConfigManager - 配置为空，清除所有本地配置");
                clearAllConfigs();
                return;
            }

            // 获取配置的appId集合
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

            initialized.set(true);
            retryCount.set(0);
            
            // 停止重试任务（如果正在重试中）
            stopRetryScheduler();
            
            log.info("WxMaConfigManager - 配置刷新完成，当前共{}个配置", configMap.size());
            
        } catch (Exception e) {
            log.error("WxMaConfigManager - 刷新配置失败", e);
        }
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

    /**
     * 检查配置是否已初始化
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * 检查提供者是否可用
     *
     * @return 是否可用
     */
    public boolean isProviderAvailable() {
        return socialDetailsProvider.isAvailable();
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        stopRetryScheduler();
        clearAllConfigs();
        log.info("WxMaConfigManager - 资源已释放");
    }
}

