package com.ingot.cloud.pms.social.provider;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.social.common.provider.SocialDetailsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * <p>Description  : 本地社交详情提供者（直接调用本地Service）.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 18:10.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LocalSocialDetailsProvider implements SocialDetailsProvider {

    private final SysSocialDetailsService sysSocialDetailsService;

    @Override
    public List<SysSocialDetails> getDetailsByType(SocialTypeEnum socialType) {
        try {
            log.debug("LocalSocialDetailsProvider - 本地查询社交详情: type={}", socialType);
            
            List<SysSocialDetails> details = sysSocialDetailsService.list(
                Wrappers.<SysSocialDetails>lambdaQuery()
                    .eq(SysSocialDetails::getType, socialType)
            );
            
            if (CollUtil.isNotEmpty(details)) {
                log.info("LocalSocialDetailsProvider - 成功获取{}个配置", details.size());
                return details;
            }
            
            log.warn("LocalSocialDetailsProvider - 未获取到任何配置");
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("LocalSocialDetailsProvider - 获取社交详情失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public SysSocialDetails getDetailsByAppId(String appId) {
        try {
            log.debug("LocalSocialDetailsProvider - 本地查询社交详情: appId={}", appId);
            
            SysSocialDetails details = sysSocialDetailsService.getOne(
                Wrappers.<SysSocialDetails>lambdaQuery()
                    .eq(SysSocialDetails::getAppId, appId)
            );
            
            if (details != null) {
                log.debug("LocalSocialDetailsProvider - 成功获取配置: appId={}", appId);
            }
            
            return details;
            
        } catch (Exception e) {
            log.error("LocalSocialDetailsProvider - 获取社交详情失败: appId={}", appId, e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return true; // 本地服务始终可用
    }
}

