package com.ingot.cloud.pms.service.biz.impl;

import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.common.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>Description  : BizRoleServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 8:58 AM.</p>
 */
@Service
@RequiredArgsConstructor
public class BizRoleServiceImpl implements BizRoleService {
    private final SysRoleUserService sysRoleUserService;
    private final SysRoleService sysRoleService;

    private final AssertionChecker assertionChecker;

    @Override
    public void orgRoleBindUsers(RelationDTO<Long, Long> params) {
        SysRole managerRole = sysRoleService.getRoleByCode(RoleConstants.ROLE_ORG_SUB_ADMIN_CODE);
        long roleId = params.getId();
        assertionChecker.checkOperation(roleId != managerRole.getId(),
                "BizRoleServiceImpl.CantBindAndRemoveManager");

        sysRoleUserService.roleBindUsers(params);
    }

    @Override
    public void setOrgUserRoles(long userId, List<Long> roles) {
        SysRole managerRole = sysRoleService.getRoleByCode(RoleConstants.ROLE_ORG_SUB_ADMIN_CODE);

        // todo
    }
}
