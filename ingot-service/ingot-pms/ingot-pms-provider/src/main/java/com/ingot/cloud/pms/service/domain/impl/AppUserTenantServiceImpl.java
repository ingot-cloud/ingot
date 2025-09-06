package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.AppUserTenant;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.mapper.AppUserTenantMapper;
import com.ingot.cloud.pms.service.domain.AppUserTenantService;
import com.ingot.framework.commons.utils.DateUtils;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Service
public class AppUserTenantServiceImpl extends BaseServiceImpl<AppUserTenantMapper, AppUserTenant> implements AppUserTenantService {

    @Override
    public void joinTenant(long userId, SysTenant tenant) {
        List<AppUserTenant> joinedOrgList = CollUtil.emptyIfNull(list(Wrappers.<AppUserTenant>lambdaQuery()
                .eq(AppUserTenant::getUserId, userId)));

        long joinId = tenant.getId();

        // 如果已经加入过，那么不处理
        if (joinedOrgList.stream().anyMatch(item -> Objects.equals(item.getTenantId(), joinId))) {
            return;
        }

        AppUserTenant userTenant = new AppUserTenant();
        userTenant.setUserId(userId);
        userTenant.setTenantId(joinId);
        userTenant.setMain(CollUtil.isEmpty(joinedOrgList));
        userTenant.setName(tenant.getName());
        userTenant.setAvatar(tenant.getAvatar());
        userTenant.setCreatedAt(DateUtils.now());
        save(userTenant);
    }

    @Override
    public void leaveTenant(long userId) {
        remove(Wrappers.<AppUserTenant>lambdaQuery()
                .eq(AppUserTenant::getUserId, userId)
                .eq(AppUserTenant::getTenantId, TenantContextHolder.get()));
    }

    @Override
    public void updateBase(SysTenant params) {
        boolean needUpdate = false;
        AppUserTenant entity = new AppUserTenant();
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

        update(entity, Wrappers.<AppUserTenant>lambdaUpdate()
                .eq(AppUserTenant::getTenantId, params.getId()));
    }

    @Override
    public List<AppUserTenant> getUserOrgs(long userId) {
        return list(Wrappers.<AppUserTenant>lambdaQuery()
                .eq(AppUserTenant::getUserId, userId));
    }
}
