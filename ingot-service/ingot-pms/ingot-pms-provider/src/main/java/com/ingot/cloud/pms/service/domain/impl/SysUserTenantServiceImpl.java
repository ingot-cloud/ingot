package com.ingot.cloud.pms.service.domain.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.mapper.SysUserTenantMapper;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : SysUserTenantServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/2.</p>
 * <p>Time         : 12:07 PM.</p>
 */
@Service
public class SysUserTenantServiceImpl extends BaseServiceImpl<SysUserTenantMapper, SysUserTenant> implements SysUserTenantService {

    @Override
    public void joinTenant(long userId) {
        // 如果是第一次加入，那么为主要租户
        boolean isMain = count(Wrappers.<SysUserTenant>lambdaQuery()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, TenantContextHolder.get())) == 0;

        SysUserTenant userTenant = new SysUserTenant();
        userTenant.setUserId(userId);
        userTenant.setTenantId(TenantContextHolder.get());
        userTenant.setMain(isMain);
        userTenant.setCreatedAt(DateUtils.now());
        save(userTenant);
    }
}
