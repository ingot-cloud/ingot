package com.ingot.cloud.pms.service.biz.impl;

import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.DeptConvert;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithManagerDTO;
import com.ingot.cloud.pms.api.model.dto.role.BizRoleAssignUsersDTO;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptWithManagerVO;
import com.ingot.cloud.pms.api.model.vo.user.SimpleUserVO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.TenantDeptService;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
import com.ingot.cloud.pms.service.domain.TenantUserDeptPrivateService;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizDeptServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/18.</p>
 * <p>Time         : 4:27 PM.</p>
 */
@Service
@RequiredArgsConstructor
public class BizDeptServiceImpl implements BizDeptService {
    private final SysUserService sysUserService;

    private final TenantDeptService tenantDeptService;
    private final TenantUserDeptPrivateService tenantUserDeptPrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;

    private final BizRoleService bizRoleService;

    private final AssertionChecker assertionChecker;
    private final DeptConvert deptConvert;

    @Override
    public List<TenantDept> getDescendantList(Long deptId, boolean includeSelf) {
        // 查询全部部门
        List<TenantDept> allDeptList = tenantDeptService.list();

        // 递归查询所有子节点
        List<TenantDept> resDeptList = new ArrayList<>();
        recursiveDept(allDeptList, deptId, resDeptList);

        // 添加当前节点
        if (includeSelf) {
            resDeptList.addAll(allDeptList.stream()
                    .filter(sysDept -> deptId.equals(sysDept.getId()))
                    .toList());
        }
        return resDeptList;
    }

    /**
     * 递归查询所有子节点。
     *
     * @param allDeptList 所有部门列表
     * @param parentId    父部门ID
     * @param resDeptList 结果集合
     */
    private void recursiveDept(List<TenantDept> allDeptList, Long parentId, List<TenantDept> resDeptList) {
        // 使用 Stream API 进行筛选和遍历
        allDeptList.stream()
                .filter(sysDept -> sysDept.getPid().equals(parentId))
                .forEach(sysDept -> {
                    resDeptList.add(sysDept);
                    recursiveDept(allDeptList, sysDept.getId(), resDeptList);
                });
    }

    @Override
    public List<DeptWithManagerVO> listWithManager() {
        // 获取主管角色
        RoleType role = bizRoleService.getByCode(RoleConstants.ROLE_ORG_MANAGER);
        // 获取当前组织所有主管
        List<SysUser> managerUsers = new ArrayList<>();
        List<TenantRoleUserPrivate> roleUsers = CollUtil.emptyIfNull(tenantRoleUserPrivateService.listRoleUsers(role.getId()));
        if (CollUtil.isNotEmpty(roleUsers)) {
            managerUsers.addAll(sysUserService.list(Wrappers.<SysUser>lambdaQuery()
                    .in(SysUser::getId, roleUsers.stream()
                            .map(TenantRoleUserPrivate::getUserId).toList())));
        }
        return tenantDeptService.listWithMemberCount().stream()
                .map(dept -> {
                    DeptWithManagerVO item = new DeptWithManagerVO();
                    BeanUtil.copyProperties(dept, item);

                    // 如果当前组织没有设置过主管，那么直接返回
                    if (CollUtil.isEmpty(roleUsers)) {
                        return item;
                    }

                    // 获取当前部门的主管ID
                    item.setManagerUsers(roleUsers.stream()
                            .filter(roleUser -> Objects.equals(roleUser.getDeptId(), dept.getId()))
                            .map(roleUser -> {
                                SimpleUserVO simpleUser = new SimpleUserVO();
                                managerUsers.stream()
                                        .filter(managerUser -> Objects.equals(managerUser.getId(), roleUser.getUserId()))
                                        .findFirst()
                                        .ifPresent(user -> BeanUtil.copyProperties(user, simpleUser));
                                return simpleUser;
                            })
                            .toList());

                    return item;
                })
                .toList();
    }

    @Override
    public List<DeptTreeNodeVO> orgList() {
        List<DeptWithManagerVO> all = listWithManager();
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(DeptWithManagerVO::getSort))
                .map(deptConvert::to).toList();

        List<DeptTreeNodeVO> childNode = allNode.stream().filter(item -> !item.getMainFlag()).toList();
        DeptTreeNodeVO mainNode = allNode.stream().filter(DeptTreeNodeVO::getMainFlag).findFirst().orElse(null);
        if (mainNode == null) {
            return ListUtil.empty();
        }
        List<DeptTreeNodeVO> childTree = TreeUtil.build(childNode, mainNode.getId());
        List<DeptTreeNodeVO> result = new ArrayList<>(childTree.size() + 1);
        result.add(mainNode);
        result.addAll(childTree);
        return result;
    }

    @Override
    public List<DeptTreeNodeVO> orgTree() {
        List<DeptWithManagerVO> all = listWithManager();
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(DeptWithManagerVO::getSort))
                .map(deptConvert::to).toList();

        return TreeUtil.build(allNode);
    }

    @Override
    public void setDeptManager(long deptId, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }

        // 获取主管角色
        RoleType role = bizRoleService.getByCode(RoleConstants.ROLE_ORG_MANAGER);

        // 清空当前部门主管
        tenantRoleUserPrivateService.clearByRoleAndDept(role.getId(), deptId);

        // 绑定主管
        BizRoleAssignUsersDTO params = new BizRoleAssignUsersDTO();
        params.setId(role.getId());
        params.setAssignIds(userIds);
        params.setDeptId(deptId);
        bizRoleService.assignUsers(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgCreateDept(DeptWithManagerDTO params) {
        assertionChecker.checkOperation(params.getPid() != null,
                "BizDeptServiceImpl.createError");
        tenantDeptService.create(params);

        if (CollUtil.isEmpty(params.getManagerUserIds())) {
            return;
        }

        setDeptManager(params.getId(), params.getManagerUserIds());
    }

    @Override
    public void orgUpdateDept(DeptWithManagerDTO params) {
        TenantDept main = tenantDeptService.getMainDept();
        // 不能更新主部门
        assertionChecker.checkOperation(!Objects.equals(params.getId(), main.getId()),
                "BizDeptServiceImpl.updateError");
        params.setMainFlag(null);
        params.setPid(null);

        tenantDeptService.update(params);

        if (CollUtil.isEmpty(params.getManagerUserIds())) {
            return;
        }

        setDeptManager(params.getId(), params.getManagerUserIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgDeleteDept(long id) {
        TenantDept main = tenantDeptService.getMainDept();
        // 不能删除主部门
        assertionChecker.checkOperation(id != main.getId(),
                "BizDeptServiceImpl.deleteError");
        tenantDeptService.delete(id);

        // 删除改部门关联数据
        tenantRoleUserPrivateService.clearByDeptId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserDeptsEnsureMainDept(long userId, List<Long> deptIds) {
        TenantDept main = tenantDeptService.getMainDept();
        if (main != null) {
            if (CollUtil.isEmpty(deptIds)) {
                deptIds = ListUtil.list(false, main.getId());
            } else {
                deptIds.add(main.getId());
            }
        }

        Set<Long> temp = new HashSet<>(deptIds);
        tenantUserDeptPrivateService.setDepartments(userId, temp);
    }
}
