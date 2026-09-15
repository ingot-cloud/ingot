package com.ingot.cloud.iam.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.TenantAppConfig;
import com.ingot.cloud.iam.mapper.TenantAppConfigMapper;
import com.ingot.cloud.iam.service.domain.TenantAppConfigService;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
@Service
public class TenantAppConfigServiceImpl extends BaseServiceImpl<TenantAppConfigMapper, TenantAppConfig> implements TenantAppConfigService {

    @Override
    public void create(TenantAppConfig params) {
        params.setCreatedAt(DateUtil.now());
        params.setUpdatedAt(params.getCreatedAt());
        save(params);
    }

    @Override
    public void update(TenantAppConfig params) {
        params.setUpdatedAt(DateUtil.now());
        updateById(params);
    }

    @Override
    public void clearByAppId(long id) {
        remove(Wrappers.<TenantAppConfig>lambdaQuery()
                .eq(TenantAppConfig::getAppId, id));
    }

    @Override
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<TenantAppConfig>lambdaQuery()
                .eq(TenantAppConfig::getTenantId, tenantId));
    }
}
