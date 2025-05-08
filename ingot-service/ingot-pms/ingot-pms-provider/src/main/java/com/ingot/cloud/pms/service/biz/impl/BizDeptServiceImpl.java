package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.dept.DeptWithManagerDTO;
import com.ingot.cloud.pms.api.model.transform.DeptTrans;
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.dept.DeptWithManagerVO;
import com.ingot.cloud.pms.api.model.vo.user.SimpleUserVO;
import com.ingot.cloud.pms.api.model.vo.user.UserWithDeptVO;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.utils.tree.TreeUtils;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.core.constants.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * <p>Description  : BizDeptServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/18.</p>
 * <p>Time         : 4:27 PM.</p>
 */
@Service
@RequiredArgsConstructor
public class BizDeptServiceImpl implements BizDeptService {
    private final SysDeptService sysDeptService;
    private final SysRoleService sysRoleService;
    private final SysRoleUserService sysRoleUserService;
    private final SysUserDeptService sysUserDeptService;
    private final SysRoleUserDeptService sysRoleUserDeptService;
    private final SysUserService sysUserService;

    private final AssertionChecker assertionChecker;
    private final DeptTrans deptTrans;

    @Override
    public List<DeptWithManagerVO> listWithManager() {
        // 获取主管角色
        SysRole role = sysRoleService.getRoleByCode(RoleConstants.ROLE_ORG_MANAGER);
        // 获取当前组织所有主管
        List<UserWithDeptVO> managerUsers = CollUtil.emptyIfNull(sysRoleUserService.getRoleUserWithDeptList(role.getId()));
        return sysDeptService.listWithMemberCount().stream()
                .map(dept -> {
                    DeptWithManagerVO item = new DeptWithManagerVO();
                    BeanUtil.copyProperties(dept, item);

                    // 如果当前组织没有设置过主管，那么直接返回
                    if (CollUtil.isEmpty(managerUsers)) {
                        return item;
                    }

                    // 获取当前部门的主管ID
                    item.setManagerUsers(managerUsers.stream()
                            .filter(user -> Objects.equals(user.getDeptId(), dept.getId()))
                            .map(user -> {
                                SimpleUserVO simpleUser = new SimpleUserVO();
                                BeanUtil.copyProperties(user, simpleUser);
                                return simpleUser;
                            })
                            .toList());

                    return item;
                })
                .toList();
    }

    @Override
    public List<SimpleUserVO> getDeptUsersWithRole(long deptId, List<String> roleCodeList) {
        if (CollUtil.isEmpty(roleCodeList)) {
            return ListUtil.empty();
        }
        List<SysRole> roleList = sysRoleService.getRoleListByCodes(roleCodeList);
        if (CollUtil.isEmpty(roleList)) {
            return ListUtil.empty();
        }
        // 需要过滤部门的角色
        List<SysRole> filterDeptRoles = roleList.stream().filter(SysRole::getFilterDept).toList();
        List<SimpleUserVO> filterDeptRolesUsers = getDeptUsersByFilterDeptRoles(deptId, filterDeptRoles);

        // 默认普通角色
        List<SysRole> defaultRoles = roleList.stream().filter(role -> !role.getFilterDept()).toList();
        List<SimpleUserVO> defaultRolesUsers = getDeptUsersByDefaultRoles(deptId, defaultRoles);

        // diff
        List<Long> tempUserIds = filterDeptRolesUsers.stream().map(SimpleUserVO::getId).toList();
        List<SimpleUserVO> diff = defaultRolesUsers.stream()
                .filter(item -> !tempUserIds.contains(item.getId()))
                .toList();

        filterDeptRolesUsers.addAll(diff);
        return filterDeptRolesUsers;
    }

    private List<SimpleUserVO> getDeptUsersByFilterDeptRoles(long deptId, List<SysRole> filterDeptRoles) {
        // 部门没有绑定的角色直接返回
        List<SysRoleUserDept> roleUserDepts = sysRoleUserDeptService.list(Wrappers.<SysRoleUserDept>lambdaQuery()
                .eq(SysRoleUserDept::getDeptId, deptId));
        if (CollUtil.isEmpty(roleUserDepts)) {
            return ListUtil.empty();
        }

        List<Long> roleIds = filterDeptRoles.stream().map(SysRole::getId).toList();
        List<Long> userIds = sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                        .in(SysRoleUser::getId, roleUserDepts.stream().map(SysRoleUserDept::getRoleUserId)))
                .stream()
                .filter(item -> roleIds.contains(item.getRoleId()))
                .map(SysRoleUser::getUserId)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(userIds)) {
            return ListUtil.empty();
        }

        return sysUserService.list(Wrappers.<SysUser>lambdaQuery().in(SysUser::getId, userIds))
                .stream().map(user -> {
                    SimpleUserVO item = new SimpleUserVO();
                    BeanUtil.copyProperties(user, item);
                    return item;
                }).toList();
    }

    private List<SimpleUserVO> getDeptUsersByDefaultRoles(long deptId, List<SysRole> defaultRoles) {
        if (CollUtil.isEmpty(defaultRoles)) {
            return ListUtil.empty();
        }

        List<SysUser> users = sysRoleUserService.getRoleListUsers(defaultRoles.stream().map(SysRole::getId).toList());
        if (CollUtil.isEmpty(users)) {
            return ListUtil.empty();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();

        // 部门用户
        List<Long> deptUserIds = CollUtil.emptyIfNull(sysUserDeptService.list(Wrappers.<SysUserDept>lambdaQuery()
                        .eq(SysUserDept::getDeptId, deptId)
                        .in(SysUserDept::getUserId, userIds)))
                .stream()
                .map(SysUserDept::getUserId)
                .toList();
        return users.stream()
                .filter(user -> deptUserIds.contains(user.getId()))
                .map(user -> {
                    SimpleUserVO item = new SimpleUserVO();
                    BeanUtil.copyProperties(user, item);
                    return item;
                })
                .toList();
    }

    @Override
    public List<DeptTreeNodeVO> orgList() {
        List<DeptWithManagerVO> all = listWithManager();
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptTrans::to).toList();

        List<DeptTreeNodeVO> childNode = allNode.stream().filter(item -> !item.getMainFlag()).toList();
        DeptTreeNodeVO mainNode = allNode.stream().filter(DeptTreeNodeVO::getMainFlag).findFirst().orElse(null);
        if (mainNode == null) {
            return ListUtil.empty();
        }
        List<DeptTreeNodeVO> childTree = TreeUtils.build(childNode, mainNode.getId());
        List<DeptTreeNodeVO> result = new ArrayList<>(childTree.size() + 1);
        result.add(mainNode);
        result.addAll(childTree);
        return result;
    }

    @Override
    public List<DeptTreeNodeVO> orgTree() {
        List<DeptWithManagerVO> all = listWithManager();
        List<DeptTreeNodeVO> allNode = all.stream()
                .sorted(Comparator.comparingInt(SysDept::getSort))
                .map(deptTrans::to).toList();

        return TreeUtils.build(allNode);
    }

    @Override
    public void setDeptManager(long deptId, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }

        // 获取主管角色
        SysRole role = sysRoleService.getRoleByCode(RoleConstants.ROLE_ORG_MANAGER);

        // 清空当前部门主管
        List<Long> rudIds = sysRoleUserDeptService.getRoleUserDeptIdsByDeptAndRole(deptId, role.getId());
        if (CollUtil.isNotEmpty(rudIds)) {
            sysRoleUserDeptService.remove(Wrappers.<SysRoleUserDept>lambdaQuery()
                    .in(SysRoleUserDept::getId, rudIds));
        }

        // 判断当前没有主管角色的用户
        List<Long> currentManagerList = CollUtil.emptyIfNull(sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getRoleId, role.getId())
                        .in(SysRoleUser::getUserId, userIds))).stream()
                .map(SysRoleUser::getUserId)
                .toList();
        List<Long> notBindManagerUserIds = userIds.stream()
                .filter(userId -> !currentManagerList.contains(userId))
                .toList();
        if (CollUtil.isNotEmpty(notBindManagerUserIds)) {
            RelationDTO<Long, Long> relation = new RelationDTO<>();
            relation.setId(role.getId());
            relation.setBindIds(notBindManagerUserIds);
            sysRoleUserService.roleBindUsers(relation);
        }

        // 设置主管
        List<Long> ruIds = sysRoleUserService.list(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getRoleId, role.getId())
                        .in(SysRoleUser::getUserId, userIds))
                .stream().map(SysRoleUser::getId).toList();
        List<SysRoleUserDept> list = ruIds.stream().map(id -> {
            SysRoleUserDept item = new SysRoleUserDept();
            item.setRoleUserId(id);
            item.setDeptId(deptId);
            return item;
        }).toList();
        if (CollUtil.size(list) == 1) {
            sysRoleUserDeptService.save(list.get(0));
        } else {
            sysRoleUserDeptService.saveBatch(list);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgCreateDept(DeptWithManagerDTO params) {
        assertionChecker.checkOperation(params.getPid() != null,
                "BizDeptServiceImpl.createError");
        sysDeptService.createDept(params);

        if (CollUtil.isEmpty(params.getManagerUserIds())) {
            return;
        }

        setDeptManager(params.getId(), params.getManagerUserIds());
    }

    @Override
    public void orgUpdateDept(DeptWithManagerDTO params) {
        SysDept main = sysDeptService.getMainDept();
        // 不能更新主部门
        assertionChecker.checkOperation(!Objects.equals(params.getId(), main.getId()),
                "BizDeptServiceImpl.updateError");
        params.setMainFlag(null);
        params.setPid(null);

        sysDeptService.updateDept(params);

        if (CollUtil.isEmpty(params.getManagerUserIds())) {
            return;
        }

        setDeptManager(params.getId(), params.getManagerUserIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void orgDeleteDept(long id) {
        SysDept main = sysDeptService.getMainDept();
        // 不能删除主部门
        assertionChecker.checkOperation(id != main.getId(),
                "BizDeptServiceImpl.deleteError");
        sysDeptService.removeDeptById(id);

        // 删除改部门关联数据
        sysRoleUserDeptService.remove(Wrappers.<SysRoleUserDept>lambdaQuery()
                .eq(SysRoleUserDept::getDeptId, id));

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserDeptsEnsureMainDept(long userId, List<Long> deptIds) {
        SysDept main = sysDeptService.getMainDept();
        if (main != null) {
            if (CollUtil.isEmpty(deptIds)) {
                deptIds = ListUtil.list(false, main.getId());
            } else {
                deptIds.add(main.getId());
            }
        }

        Set<Long> temp = new HashSet<>(deptIds);
        sysDeptService.setDepts(userId, temp.stream().toList());
    }
}
