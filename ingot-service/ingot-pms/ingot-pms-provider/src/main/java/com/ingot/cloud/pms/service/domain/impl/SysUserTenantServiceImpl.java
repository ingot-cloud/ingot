package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.mapper.SysUserTenantMapper;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        List<SysUserTenant> joinedOrgList = CollUtil.emptyIfNull(list(Wrappers.<SysUserTenant>lambdaQuery()
                .eq(SysUserTenant::getUserId, userId)));

        // 如果已经加入过，那么不处理
        if (joinedOrgList.stream().anyMatch(item -> Objects.equals(item.getTenantId(), TenantContextHolder.get()))) {
            return;
        }

        SysUserTenant userTenant = new SysUserTenant();
        userTenant.setUserId(userId);
        userTenant.setTenantId(TenantContextHolder.get());
        userTenant.setMain(CollUtil.isEmpty(joinedOrgList));
        userTenant.setCreatedAt(DateUtils.now());
        save(userTenant);
    }

    @Override
    public void leaveTenant(long userId) {
        remove(Wrappers.<SysUserTenant>lambdaQuery()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, TenantContextHolder.get()));
    }
}
