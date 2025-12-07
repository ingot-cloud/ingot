package com.ingot.framework.social.common.provider;

import java.util.Collections;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.api.rpc.RemotePmsSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : 远程社交详情提供者（通过RPC调用PMS服务）.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 18:05.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteSocialDetailsProvider implements SocialDetailsProvider {

    private final RemotePmsSocialDetailsService remotePmsSocialDetailsService;

    /**
     * 提供者是否可用（用于健康检查）
     */
    private volatile boolean available = false;

    @Override
    public List<SysSocialDetails> getDetailsByType(SocialTypeEnum socialType) {
        try {
            log.debug("RemoteSocialDetailsProvider - 通过RPC获取社交详情: type={}", socialType);

            R<List<SysSocialDetails>> result = remotePmsSocialDetailsService
                    .getSocialDetailsByType(socialType.getValue());

            if (result.isSuccess()) {
                List<SysSocialDetails> details = result.getData();
                if (CollUtil.isNotEmpty(details)) {
                    available = true;
                    log.info("RemoteSocialDetailsProvider - 成功获取{}个配置", details.size());
                    return details;
                } else {
                    log.warn("RemoteSocialDetailsProvider - RPC调用成功但未获取到任何配置");
                    available = true; // RPC成功，但数据为空
                    return Collections.emptyList();
                }
            } else {
                available = false;
                log.warn("RemoteSocialDetailsProvider - RPC调用失败: code={}, message={}",
                        result.getCode(), result.getMessage());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            available = false;
            log.warn("RemoteSocialDetailsProvider - 获取社交详情异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public SysSocialDetails getDetailsByAppId(String appId) {
        try {
            log.debug("RemoteSocialDetailsProvider - 通过RPC获取社交详情: appId={}", appId);

            R<SysSocialDetails> result = remotePmsSocialDetailsService.getDetailsByAppId(appId);

            if (result.isSuccess()) {
                SysSocialDetails details = result.getData();
                if (details != null) {
                    available = true;
                    log.debug("RemoteSocialDetailsProvider - 成功获取配置: appId={}", appId);
                } else {
                    log.debug("RemoteSocialDetailsProvider - RPC调用成功但配置不存在: appId={}", appId);
                    available = true; // RPC成功，但数据为空
                }
                return details;
            } else {
                available = false;
                log.warn("RemoteSocialDetailsProvider - RPC调用失败: appId={}, code={}, message={}",
                        appId, result.getCode(), result.getMessage());
                return null;
            }

        } catch (Exception e) {
            available = false;
            log.warn("RemoteSocialDetailsProvider - 获取社交详情异常: appId={}, error={}",
                    appId, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
