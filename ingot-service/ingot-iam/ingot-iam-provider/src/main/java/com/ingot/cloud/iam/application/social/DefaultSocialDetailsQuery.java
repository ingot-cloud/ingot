package com.ingot.cloud.iam.application.social;

import java.util.List;
import com.ingot.cloud.iam.api.model.dto.social.SocialDetailsDTO;
import com.ingot.cloud.iam.infrastructure.social.SocialDetailsAssembler;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.infrastructure.persistence.entity.SocialConfigEntity;
import com.ingot.cloud.iam.infrastructure.persistence.store.SocialConfigStore;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : DefaultSocialDetailsQuery.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 16:05.</p>
 */
@Service
@RequiredArgsConstructor
public class DefaultSocialDetailsQuery implements SocialDetailsQuery {
    private final SocialConfigStore socialConfigs;

    @Override
    public List<SocialDetailsDTO> getSocialDetailsByType(SocialTypeEnum type) {
        return CollUtil.emptyIfNull(socialConfigs.list(Wrappers.<SocialConfigEntity>lambdaQuery()
                .in(SocialConfigEntity::getType, type))).stream().map(SocialDetailsAssembler::toDTO).toList();
    }

    @Override
    public SocialDetailsDTO getSocialDetailsByAppId(String appId) {
        return SocialDetailsAssembler.toDTO(socialConfigs.getOne(Wrappers.<SocialConfigEntity>lambdaQuery()
                .eq(SocialConfigEntity::getAppId, appId)));
    }
}
