package com.ingot.cloud.member.service.biz.impl;

import com.ingot.cloud.member.service.biz.BizTenantService;
import com.ingot.cloud.member.service.domain.MemberRolePermissionService;
import com.ingot.cloud.member.service.domain.MemberRoleUserService;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.framework.commons.model.common.TenantBaseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizTenantServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 08:47.</p>
 */
@Service
@RequiredArgsConstructor
public class BizTenantServiceImpl implements BizTenantService {
    private final MemberUserTenantService memberUserTenantService;
    private final MemberRoleUserService memberRoleUserService;
    private final MemberRolePermissionService memberRolePermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(long id) {
        memberUserTenantService.clearByTenantId(id);
        memberRoleUserService.clearByTenantId(id);
        memberRolePermissionService.clearByTenantId(id);
    }

    @Override
    public void updateBase(TenantBaseDTO params) {
        memberUserTenantService.updateBase(params);
    }
}
