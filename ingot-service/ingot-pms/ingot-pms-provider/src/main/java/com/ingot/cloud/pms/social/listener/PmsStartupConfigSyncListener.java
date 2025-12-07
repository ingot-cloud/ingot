package com.ingot.cloud.pms.social.listener;

import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : PMS启动后配置同步监听器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 19:00.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PmsStartupConfigSyncListener {

    private final SysSocialDetailsService sysSocialDetailsService;
    private final SocialConfigMessagePublisher configMessagePublisher;

    /**
     * 监听应用启动完成事件
     * 在 PMS 启动完成后，广播通知所有服务刷新配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (configMessagePublisher == null) {
            log.warn("PmsStartupConfigSyncListener - 消息发布器未配置，跳过启动同步");
            return;
        }

        try {
            log.info("PmsStartupConfigSyncListener - PMS启动完成，开始广播配置同步消息");

            // 查询所有已配置的社交类型
            List<SysSocialDetails> allDetails = sysSocialDetailsService.list(
                    Wrappers.<SysSocialDetails>lambdaQuery()
                            .select(SysSocialDetails::getType)
                            .groupBy(SysSocialDetails::getType)
            );

            if (allDetails.isEmpty()) {
                log.info("PmsStartupConfigSyncListener - 当前没有任何社交配置，跳过同步");
                return;
            }

            // 获取所有不重复的社交类型
            List<SocialTypeEnum> socialTypes = allDetails.stream()
                    .map(SysSocialDetails::getType)
                    .distinct()
                    .toList();

            // 为每种社交类型发送刷新消息
            for (SocialTypeEnum socialType : socialTypes) {
                try {
                    configMessagePublisher.publishRefreshAll(socialType);
                    log.info("PmsStartupConfigSyncListener - 已发送配置同步消息: type={}", socialType);
                } catch (Exception e) {
                    log.error("PmsStartupConfigSyncListener - 发送配置同步消息失败: type={}", socialType, e);
                }
            }

            log.info("PmsStartupConfigSyncListener - 配置同步消息广播完成，共{}种社交类型", socialTypes.size());

        } catch (Exception e) {
            log.error("PmsStartupConfigSyncListener - 启动配置同步失败", e);
        }
    }
}

