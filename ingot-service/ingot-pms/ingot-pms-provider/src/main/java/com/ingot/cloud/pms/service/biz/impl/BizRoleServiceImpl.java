package com.ingot.cloud.pms.service.biz.impl;

import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.model.common.RelationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public void roleBindUsers(RelationDTO<Long, Long> params) {
        sysRoleUserService.roleBindUsers(params);
    }
}
