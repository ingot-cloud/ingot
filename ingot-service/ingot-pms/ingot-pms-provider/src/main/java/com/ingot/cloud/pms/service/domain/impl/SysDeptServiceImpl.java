package com.ingot.cloud.pms.service.domain.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRoleDept;
import com.ingot.cloud.pms.api.model.transform.DeptTrans;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.cloud.pms.mapper.SysDeptMapper;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.cloud.pms.service.domain.SysRoleDeptService;
import com.ingot.framework.common.utils.DateUtils;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
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
public class SysDeptServiceImpl extends BaseServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {
    private final SysRoleDeptService sysRoleDeptService;

    private final DeptTrans deptTrans;
    private final AssertionChecker assertI18nService;

    @Override
    public List<DeptTreeNodeVO> treeList() {
        List<SysDept> all = list();
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptTrans::to).collect(Collectors.toList());

        return TreeUtils.build(allNode, IDConstants.ROOT_TREE_ID);
    }

    @Override
    public List<DeptTreeNodeVO> treeList(SysDept condition) {
        List<DeptTreeNodeVO> nodeList = list().stream()
                .filter(BizFilter.deptFilter(condition))
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptTrans::to).collect(Collectors.toList());

        List<DeptTreeNodeVO> tree = TreeUtils.build(nodeList);
        TreeUtils.compensate(tree, nodeList);
        return tree;
    }

    @Override
    public void createDept(SysDept params) {
        params.setCreatedAt(DateUtils.now());
        if (params.getStatus() == null) {
            params.setStatus(CommonStatusEnum.ENABLE);
        }
        assertI18nService.checkOperation(save(params),
                "SysDeptServiceImpl.CreateFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDeptById(long id) {
        boolean success = count(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getPid, id)) == 0;
        assertI18nService.checkOperation(success, "SysDeptServiceImpl.ExistLeaf");

        // 取消关联角色
        long count = sysRoleDeptService.count(Wrappers.<SysRoleDept>lambdaQuery()
                .eq(SysRoleDept::getDeptId, id));
        if (count > 0) {
            success = sysRoleDeptService.remove(Wrappers.<SysRoleDept>lambdaQuery()
                    .eq(SysRoleDept::getDeptId, id));
            assertI18nService.checkOperation(success, "SysDeptServiceImpl.RemoveFailed");
        }

        success = removeById(id);
        assertI18nService.checkOperation(success, "SysDeptServiceImpl.RemoveFailed");
    }

    @Override
    public void updateDept(SysDept params) {
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysDeptServiceImpl.UpdateFailed");
    }
}
