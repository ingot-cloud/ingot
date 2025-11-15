package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.mapper.TenantRoleUserPrivateMapper;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
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
public class TenantRoleUserPrivateServiceImpl extends BaseServiceImpl<TenantRoleUserPrivateMapper, TenantRoleUserPrivate> implements TenantRoleUserPrivateService {

    @Override
    public void clearByRoleId(long id) {
        remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getRoleId, id));
    }
}
