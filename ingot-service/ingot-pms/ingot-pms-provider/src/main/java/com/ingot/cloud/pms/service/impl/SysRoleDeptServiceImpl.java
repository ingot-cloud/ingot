package com.ingot.cloud.pms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleDeptMapper;
import com.ingot.cloud.pms.service.SysRoleDeptService;
import com.ingot.framework.core.model.dto.common.RelationDto;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
public class SysRoleDeptServiceImpl extends CommonRoleRelationService<SysRoleDeptMapper, SysRoleDept> implements SysRoleDeptService {

    @Override
    public void deptBindRoles(RelationDto<Long, Long> params) {
        bindRoles(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleDept>lambdaQuery()
                        .eq(SysRoleDept::getRoleId, roleId)
                        .eq(SysRoleDept::getDeptId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleDeptServiceImpl.RemoveFailed");
    }

    @Override
    public void roleBindDepts(RelationDto<Long, Long> params) {
        bindTargets(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleDept>lambdaQuery()
                        .eq(SysRoleDept::getRoleId, roleId)
                        .eq(SysRoleDept::getDeptId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleDeptServiceImpl.RemoveFailed");
    }
}
