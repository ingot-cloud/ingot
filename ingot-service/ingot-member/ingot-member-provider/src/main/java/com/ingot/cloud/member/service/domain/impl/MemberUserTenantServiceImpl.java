package com.ingot.cloud.member.service.domain.impl;

import java.util.List;
import java.util.Objects;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUserTenant;
import com.ingot.cloud.member.mapper.MemberUserTenantMapper;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.rpc.PmsTenantDetailsService;
import com.ingot.framework.commons.model.common.TenantBaseDTO;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Service
@RequiredArgsConstructor
public class MemberUserTenantServiceImpl extends BaseServiceImpl<MemberUserTenantMapper, MemberUserTenant> implements MemberUserTenantService {
    private final PmsTenantDetailsService pmsTenantDetailsService;
    private final AssertionChecker assertionChecker;

    @Override
    public void joinTenant(long userId, long tenantId) {
        List<MemberUserTenant> joinedOrgList = CollUtil.emptyIfNull(list(Wrappers.<MemberUserTenant>lambdaQuery()
                .eq(MemberUserTenant::getUserId, userId)));

        // 如果已经加入过，那么不处理
        if (joinedOrgList.stream().anyMatch(item -> Objects.equals(item.getTenantId(), tenantId))) {
            return;
        }

        SysTenant tenant = pmsTenantDetailsService.getTenantById(tenantId)
                .ifError(OAuth2ErrorUtils::checkResponse)
                .getData();
        assertionChecker.checkOperation(tenant != null, "MemberUserTenantServiceImpl.TenantNonNull");
        assert tenant != null;

        MemberUserTenant userTenant = new MemberUserTenant();
        userTenant.setUserId(userId);
        userTenant.setTenantId(tenantId);
        userTenant.setMain(CollUtil.isEmpty(joinedOrgList));
        userTenant.setName(tenant.getName());
        userTenant.setAvatar(tenant.getAvatar());
        userTenant.setCreatedAt(DateUtil.now());
        save(userTenant);
    }

    @Override
    public void leaveTenant(long userId) {
        remove(Wrappers.<MemberUserTenant>lambdaQuery()
                .eq(MemberUserTenant::getUserId, userId)
                .eq(MemberUserTenant::getTenantId, TenantContextHolder.get()));
    }

    @Override
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<MemberUserTenant>lambdaQuery()
                .eq(MemberUserTenant::getTenantId, tenantId));
    }

    @Override
    public void updateBase(long tenantId) {
        SysTenant tenant = pmsTenantDetailsService.getTenantById(tenantId)
                .ifError(OAuth2ErrorUtils::checkResponse)
                .getData();
        assertionChecker.checkOperation(tenant != null, "MemberUserTenantServiceImpl.TenantNonNull");
        assert tenant != null;

        MemberUserTenant entity = new MemberUserTenant();
        entity.setName(tenant.getName());
        entity.setAvatar(tenant.getAvatar());

        update(entity, Wrappers.<MemberUserTenant>lambdaUpdate()
                .eq(MemberUserTenant::getTenantId, tenantId));
    }

    @Override
    public void updateBase(TenantBaseDTO params) {
        MemberUserTenant entity = new MemberUserTenant();
        entity.setName(params.getName());
        entity.setAvatar(params.getAvatar());

        update(entity, Wrappers.<MemberUserTenant>lambdaUpdate()
                .eq(MemberUserTenant::getTenantId, params.getId()));
    }

    @Override
    public List<MemberUserTenant> getUserOrgs(long userId) {
        return list(Wrappers.<MemberUserTenant>lambdaQuery()
                .eq(MemberUserTenant::getUserId, userId));
    }
}
