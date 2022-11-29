package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.cloud.pms.api.model.transform.DeptTrans;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.api.utils.TreeUtils;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleDeptMapper;
import com.ingot.cloud.pms.service.domain.SysRoleDeptService;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@RequiredArgsConstructor
public class SysRoleDeptServiceImpl extends CommonRoleRelationService<SysRoleDeptMapper, SysRoleDept> implements SysRoleDeptService {
    private final DeptTrans deptTrans;

    private final Do remove = (roleId, targetId) -> remove(Wrappers.<SysRoleDept>lambdaQuery()
            .eq(SysRoleDept::getRoleId, roleId)
            .eq(SysRoleDept::getDeptId, targetId));
    private final Do bind = (roleId, targetId) -> {
        getBaseMapper().insertIgnore(roleId, targetId);
        return true;
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deptBindRoles(RelationDTO<Long, Long> params) {
        bindRoles(params, remove, bind,
                "SysRoleDeptServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindDepts(RelationDTO<Long, Long> params) {
        bindTargets(params, remove, bind,
                "SysRoleDeptServiceImpl.RemoveFailed");
    }

    @Override
    public List<DeptTreeNodeVO> getRoleDepts(long roleId,
                                             boolean isBind,
                                             SysDept condition) {
        List<SysDept> all = getBaseMapper().getRoleDepts(roleId, isBind, condition);
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptTrans::to).collect(Collectors.toList());

        List<DeptTreeNodeVO> tree = TreeUtils.build(allNode, 0);

        if (isBind) {
            allNode.forEach(item -> {
                if (!TreeUtils.contains(tree, item)) {
                    tree.add(item);
                }
            });
        }

        return tree;
    }
}
