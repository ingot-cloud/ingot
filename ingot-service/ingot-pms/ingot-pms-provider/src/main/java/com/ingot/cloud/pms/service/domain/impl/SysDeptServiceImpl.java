package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysUserDept;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithMemberCountDTO;
import com.ingot.cloud.pms.api.model.convert.DeptConvert;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.mapper.SysDeptMapper;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.cloud.pms.service.domain.SysUserDeptService;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.DateUtils;
import com.ingot.framework.commons.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    private final SysUserDeptService sysUserDeptService;

    private final DeptConvert deptConvert;
    private final AssertionChecker assertI18nService;

    @Override
    public List<DeptTreeNodeVO> treeList() {
        List<SysDept> all = list();
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptConvert::to).collect(Collectors.toList());

        return TreeUtils.build(allNode, IDConstants.ROOT_TREE_ID);
    }

    @Override
    public List<DeptTreeNodeVO> treeList(SysDept condition) {
        List<DeptTreeNodeVO> nodeList = list().stream()
                .filter(BizFilter.deptFilter(condition))
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptConvert::to).collect(Collectors.toList());

        List<DeptTreeNodeVO> tree = TreeUtils.build(nodeList);
        TreeUtils.compensate(tree, nodeList);
        return tree;
    }

    @Override
    public List<DeptWithMemberCountDTO> listWithMemberCount() {
        return baseMapper.listWithMemberCount();
    }

    @Override
    public List<Long> getUserDeptIds(long userId) {
        return CollUtil.emptyIfNull(sysUserDeptService.list(Wrappers.<SysUserDept>lambdaQuery()
                        .eq(SysUserDept::getUserId, userId)))
                .stream().map(SysUserDept::getDeptId).toList();
    }

    @Override
    public List<SysDept> getUserDescendant(long userId, boolean includeSelf) {
        List<Long> deptIds = getUserDeptIds(userId);
        if (CollUtil.isEmpty(deptIds)) {
            return ListUtil.empty();
        }

        return deptIds.stream()
                .flatMap(deptId -> getDescendantList(deptId, includeSelf).stream())
                .toList();
    }

    @Override
    public List<SysDept> getDescendantList(Long deptId, boolean includeSelf) {
        // 查询全部部门
        List<SysDept> allDeptList = list(Wrappers.emptyWrapper());

        // 递归查询所有子节点
        List<SysDept> resDeptList = new ArrayList<>();
        recursiveDept(allDeptList, deptId, resDeptList);

        // 添加当前节点
        if (includeSelf) {
            resDeptList.addAll(allDeptList.stream()
                    .filter(sysDept -> deptId.equals(sysDept.getId()))
                    .toList());
        }
        return resDeptList;
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
        SysDept dept = getById(id);
        assertI18nService.checkOperation(dept != null, "SysDeptServiceImpl.NonExist");

        boolean success = count(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getPid, id)) == 0;
        assertI18nService.checkOperation(success, "SysDeptServiceImpl.ExistLeaf");

        success = removeById(id);
        assertI18nService.checkOperation(success, "SysDeptServiceImpl.RemoveFailed");
    }

    @Override
    public void updateDept(SysDept params) {
        params.setUpdatedAt(DateUtils.now());
        assertI18nService.checkOperation(updateById(params),
                "SysDeptServiceImpl.UpdateFailed");
    }

    @Override
    public SysDept getMainDept() {
        return getOne(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getMainFlag, Boolean.TRUE));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void setDepts(long userId, List<Long> deptIds) {
        // 先清空部门
        long userCount = sysUserDeptService.count(Wrappers.<SysUserDept>lambdaQuery().eq(SysUserDept::getUserId, userId));
        if (userCount != 0) {
            sysUserDeptService.remove(Wrappers.<SysUserDept>lambdaQuery()
                    .eq(SysUserDept::getUserId, userId));
        }

        if (CollUtil.isEmpty(deptIds)) {
            return;
        }

        if (CollUtil.size(deptIds) == 1) {
            SysUserDept item = new SysUserDept();
            item.setUserId(userId);
            item.setDeptId(deptIds.get(0));
            sysUserDeptService.save(item);
            return;
        }

        List<SysUserDept> list = deptIds.stream().map(deptId -> {
            SysUserDept item = new SysUserDept();
            item.setUserId(userId);
            item.setDeptId(deptId);
            return item;
        }).toList();

        sysUserDeptService.saveBatch(list);
    }

    @Override
    public List<SysUserDept> getUserDepts(long userId) {
        return sysUserDeptService.list(Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getUserId, userId));
    }

    /**
     * 递归查询所有子节点。
     *
     * @param allDeptList 所有部门列表
     * @param parentId    父部门ID
     * @param resDeptList 结果集合
     */
    private void recursiveDept(List<SysDept> allDeptList, Long parentId, List<SysDept> resDeptList) {
        // 使用 Stream API 进行筛选和遍历
        allDeptList.stream()
                .filter(sysDept -> sysDept.getPid().equals(parentId))
                .forEach(sysDept -> {
                    resDeptList.add(sysDept);
                    recursiveDept(allDeptList, sysDept.getId(), resDeptList);
                });
    }
}
