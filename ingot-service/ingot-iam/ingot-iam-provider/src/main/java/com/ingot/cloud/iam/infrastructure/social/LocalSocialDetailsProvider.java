package com.ingot.cloud.iam.infrastructure.social;

import java.util.Collections;
import java.util.List;
import com.ingot.cloud.iam.api.model.dto.social.SocialDetailsDTO;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.infrastructure.persistence.entity.SocialConfigEntity;
import com.ingot.cloud.iam.infrastructure.persistence.store.SocialConfigStore;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.social.common.provider.SocialDetailsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : 本地社交详情提供者（直接调用本地Service）.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 18:10.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LocalSocialDetailsProvider implements SocialDetailsProvider {

    private final SocialConfigStore socialConfigs;

    @Override
    public List<SocialDetailsDTO> getDetailsByType(SocialTypeEnum socialType) {
        try {
            log.debug("LocalSocialDetailsProvider - 本地查询社交详情: type={}", socialType);

            List<SocialConfigEntity> details = socialConfigs.list(
                Wrappers.<SocialConfigEntity>lambdaQuery()
                    .eq(SocialConfigEntity::getType, socialType)
            );

            if (CollUtil.isNotEmpty(details)) {
                log.info("LocalSocialDetailsProvider - 成功获取{}个配置", details.size());
                return details.stream().map(SocialDetailsAssembler::toDTO).toList();
            }

            log.warn("LocalSocialDetailsProvider - 未获取到任何配置");
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("LocalSocialDetailsProvider - 获取社交详情失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public SocialDetailsDTO getDetailsByAppId(String appId) {
        try {
            log.debug("LocalSocialDetailsProvider - 本地查询社交详情: appId={}", appId);

            SocialConfigEntity details = socialConfigs.getOne(
                Wrappers.<SocialConfigEntity>lambdaQuery()
                    .eq(SocialConfigEntity::getAppId, appId)
            );

            if (details != null) {
                log.debug("LocalSocialDetailsProvider - 成功获取配置: appId={}", appId);
            }

            return SocialDetailsAssembler.toDTO(details);

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

