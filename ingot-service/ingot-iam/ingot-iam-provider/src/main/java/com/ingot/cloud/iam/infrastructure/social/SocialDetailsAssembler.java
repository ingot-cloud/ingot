package com.ingot.cloud.iam.infrastructure.social;

import com.ingot.cloud.iam.api.model.dto.social.SocialDetailsDTO;
import com.ingot.cloud.iam.infrastructure.persistence.entity.SocialConfigEntity;

/**
 * <p>将渠道配置转换为受控内部传输对象，不向调用服务暴露持久化行为。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SocialDetailsAssembler {
    private SocialDetailsAssembler() {
    }

    /**
     * 保留内部RPC的配置字段；配置不存在时仍返回空。
     * @param source 渠道配置，可空
     * @return 内部配置响应
     */
    public static SocialDetailsDTO toDTO(SocialConfigEntity source) {
        if (source == null) {
            return null;
        }
        SocialDetailsDTO target = new SocialDetailsDTO();
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setAppId(source.getAppId());
        target.setAppSecret(source.getAppSecret());
        target.setRedirectUrl(source.getRedirectUrl());
        target.setName(source.getName());
        target.setType(source.getType());
        target.setStatus(source.getStatus());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setDeletedAt(source.getDeletedAt());
        return target;
    }
}
