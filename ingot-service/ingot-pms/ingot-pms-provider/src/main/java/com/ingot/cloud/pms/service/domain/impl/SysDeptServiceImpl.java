package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysUserDept;
import com.ingot.cloud.pms.api.model.transform.DeptTrans;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptWithManagerVO;
import com.ingot.cloud.pms.api.model.vo.user.SimpleUserVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.mapper.SysDeptMapper;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.cloud.pms.service.domain.SysUserDeptService;
import com.ingot.framework.core.constants.IDConstants;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.service.BaseServiceImpl;
import com.ingot.framework.security.common.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SysRoleService sysRoleService;
    private final SysRoleUserService sysRoleUserService;

    private final DeptTrans deptTrans;
    private final AssertionChecker assertI18nService;

    @Override
    public List<DeptWithManagerVO> listWithManager() {
        // 获取主管角色
        SysRole role = sysRoleService.getRoleByCode(RoleConstants.ROLE_ORG_MANAGER);
        // 获取当前组织所有主管
        List<SimpleUserVO> managerUsers = CollUtil.emptyIfNull(sysRoleUserService.getRoleUsers(role.getId()))
                .stream().map(user -> {
                    SimpleUserVO item = new SimpleUserVO();
                    BeanUtil.copyProperties(user, item);
                    return item;
                }).toList();

        return list().stream()
                .map(dept -> {
                    DeptWithManagerVO item = new DeptWithManagerVO();
                    BeanUtil.copyProperties(dept, item);
                    // 获取当前部门的主管ID
                    List<Long> userIds = CollUtil.emptyIfNull(sysUserDeptService.list(Wrappers.<SysUserDept>lambdaQuery()
                                    .eq(SysUserDept::getDeptId, dept.getId())))
                            .stream()
                            .map(SysUserDept::getUserId)
                            .toList();
                    item.setManagerUsers(managerUsers.stream()
                            .filter(user -> userIds.contains(user.getId()))
                            .toList());

                    return item;
                })
                .toList();
    }

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
}
