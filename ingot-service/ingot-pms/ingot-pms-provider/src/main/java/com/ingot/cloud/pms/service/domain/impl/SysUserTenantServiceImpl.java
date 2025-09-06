package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.cloud.pms.mapper.SysUserTenantMapper;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
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
    public void joinTenant(long userId, SysTenant tenant) {
        List<SysUserTenant> joinedOrgList = CollUtil.emptyIfNull(list(Wrappers.<SysUserTenant>lambdaQuery()
                .eq(SysUserTenant::getUserId, userId)));

        long joinId = tenant.getId();

        // 如果已经加入过，那么不处理
        if (joinedOrgList.stream().anyMatch(item -> Objects.equals(item.getTenantId(), joinId))) {
            return;
        }

        SysUserTenant userTenant = new SysUserTenant();
        userTenant.setUserId(userId);
        userTenant.setTenantId(joinId);
        userTenant.setMain(CollUtil.isEmpty(joinedOrgList));
        userTenant.setName(tenant.getName());
        userTenant.setAvatar(tenant.getAvatar());
        userTenant.setCreatedAt(DateUtil.now());
        save(userTenant);
    }

    @Override
    public void leaveTenant(long userId) {
        remove(Wrappers.<SysUserTenant>lambdaQuery()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, TenantContextHolder.get()));
    }

    @Override
    public void updateBase(SysTenant params) {
        boolean needUpdate = false;
        SysUserTenant entity = new SysUserTenant();
        if (StrUtil.isNotEmpty(params.getName())) {
            entity.setName(params.getName());
            needUpdate = true;
        }
        if (StrUtil.isNotEmpty(params.getAvatar())) {
            entity.setAvatar(params.getAvatar());
            needUpdate = true;
        }

        if (!needUpdate) {
            return;
        }

        update(entity, Wrappers.<SysUserTenant>lambdaUpdate()
                .eq(SysUserTenant::getTenantId, params.getId()));
    }

    @Override
    public List<SysUserTenant> getUserOrgs(long userId) {
        return list(Wrappers.<SysUserTenant>lambdaQuery()
                .eq(SysUserTenant::getUserId, userId));
    }
}
