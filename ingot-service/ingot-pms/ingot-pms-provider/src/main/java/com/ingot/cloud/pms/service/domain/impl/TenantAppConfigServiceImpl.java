package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantAppConfig;
import com.ingot.cloud.pms.mapper.TenantAppConfigMapper;
import com.ingot.cloud.pms.service.domain.TenantAppConfigService;
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
    public void clearByAppId(long id) {
        remove(Wrappers.<TenantAppConfig>lambdaQuery()
                .eq(TenantAppConfig::getMetaId, id));
    }
}
