package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysRoleUserDept;
import com.ingot.cloud.pms.mapper.SysRoleUserDeptMapper;
import com.ingot.cloud.pms.service.domain.SysRoleUserDeptService;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-03-08
 */
@Service
public class SysRoleUserDeptServiceImpl extends BaseServiceImpl<SysRoleUserDeptMapper, SysRoleUserDept> implements SysRoleUserDeptService {

    @Override
    public List<Long> getRoleUserDeptIdsByDeptAndRole(long deptId, long roleId) {
        return baseMapper.getRoleUserDeptIdsByDeptAndRole(deptId, roleId);
    }
}
