package com.ingot.cloud.iam.application.tenant;

import java.util.List;

import com.ingot.cloud.iam.api.model.dto.tenant.TenantDetailsDTO;
import com.ingot.cloud.iam.infrastructure.persistence.AccountCredentialRepository;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantEntity;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>内部租户查询读取 {@code iam_tenant} 与账号成员关系，不回退旧租户表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class DefaultTenantDetailsQuery implements TenantDetailsQuery {
    private final AccountCredentialRepository accounts;
    private final TenantRepository tenants;

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantDetailsResponse getUserTenantDetails(String username) {
        TenantDetailsResponse response = new TenantDetailsResponse();
        accounts.findByLogin(username).ifPresentOrElse(
                account -> response.setAllows(accounts.tenantAllows(Long.toString(account.id()))),
                () -> response.setAllows(List.of()));
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantDetailsResponse getTenantByIds(List<Long> ids) {
        TenantDetailsResponse response = new TenantDetailsResponse();
        if (ids == null || ids.isEmpty()) {
            response.setAllows(List.of());
            return response;
        }
        response.setAllows(tenants.listByIds(ids).stream().map(this::summary).toList());
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantDetailsDTO getTenantById(Long id) {
        if (id == null) {
            return null;
        }
        IamTenantEntity tenant = tenants.findActive(id);
        return tenant == null ? null : map(tenant);
    }

    private TenantMainDTO summary(IamTenantEntity tenant) {
        TenantMainDTO dto = new TenantMainDTO();
        dto.setId(tenant.getId().toString());
        dto.setName(tenant.getName());
        dto.setAvatar(tenant.getAvatar());
        dto.setMain(false);
        return dto;
    }

    private TenantDetailsDTO map(IamTenantEntity tenant) {
        TenantDetailsDTO dto = new TenantDetailsDTO();
        dto.setId(tenant.getId().longValueExact());
        dto.setName(tenant.getName());
        dto.setAvatar(tenant.getAvatar());
        dto.setStatus(Boolean.TRUE.equals(tenant.getEnabled()) ? CommonStatusEnum.ENABLE : CommonStatusEnum.LOCK);
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());
        return dto;
    }
}
