package com.ingot.cloud.pms.service.biz.impl;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.bo.permission.PermissionIdBO;
import com.ingot.cloud.pms.api.model.bo.role.BizRoleAssignUsersBO;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.domain.TenantRolePermissionPrivate;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.dto.common.BizBindDTO;
import com.ingot.cloud.pms.api.model.dto.role.BizRoleAssignUsersDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.permission.BizPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.permission.BizPermissionVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.core.BizPermissionUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.utils.RoleUtil;
import com.ingot.framework.commons.utils.tree.TreeNode;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizRoleServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 09:34.</p>
 */
@Service
@RequiredArgsConstructor
public class BizRoleServiceImpl implements BizRoleService {
    private final MetaRoleService metaRoleService;
    private final MetaPermissionService authorityService;
    private final MetaRolePermissionService roleAuthorityService;

    private final TenantRolePrivateService tenantRolePrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;
    private final TenantRolePermissionPrivateService tenantRolePermissionPrivateService;

    private final BizAppService bizAppService;

    private final AuthorityConvert authorityConvert;
    private final RoleConvert roleConvert;
    private final AssertionChecker assertionChecker;

    @Override
    public MetaRole getMetaRole(long id) {
        return metaRoleService.getById(id);
    }

    @Override
    public RoleType getRole(long id) {
        MetaRole metaRole = metaRoleService.getById(id);
        if (metaRole != null) {
            return metaRole;
        }
        return tenantRolePrivateService.getById(id);
    }

    @Override
    public List<RoleType> getRoles(List<Long> ids) {
        List<RoleType> result = new ArrayList<>();

        result.addAll(metaRoleService.list().stream()
                .filter(item -> ids.contains(item.getId()))
                .toList());

        result.addAll(tenantRolePrivateService.list().stream()
                .filter(item -> ids.contains(item.getId()))
                .toList());

        return result;
    }

    @Override
    public List<RoleType> getRolesByCodes(List<String> codes) {
        List<RoleType> result = new ArrayList<>();

        if (codes.stream().anyMatch(RoleUtil::isMetaRoleCode)) {
            result.addAll(metaRoleService.list().stream()
                    .filter(item -> codes.contains(item.getCode()))
                    .toList());
        }

        if (codes.stream().anyMatch(RoleUtil::isOrgRoleCode)) {
            result.addAll(tenantRolePrivateService.list().stream()
                    .filter(item -> codes.contains(item.getCode()))
                    .toList());
        }

        return result;
    }

    @Override
    public RoleType getByCode(String code) {
        if (RoleUtil.isMetaRoleCode(code)) {
            return metaRoleService.getByCode(code);
        }
        return tenantRolePrivateService.getByCode(code);
    }

    @Override
    public List<Option<Long>> options(TenantRolePrivate condition) {
        // meta
        List<Option<Long>> options = new ArrayList<>(metaRoleService.list(
                        Wrappers.<MetaRole>lambdaQuery()
                                .eq(MetaRole::getType, RoleTypeEnum.ROLE)
                                .eq(MetaRole::getOrgType, OrgTypeEnum.Tenant)
                                .eq(MetaRole::getStatus, CommonStatusEnum.ENABLE))
                .stream()
                .filter(item -> item.getOrgType() == OrgTypeEnum.Tenant)
                .filter(BizFilter.roleFilter(condition))
                .map(role -> Option.of(role.getId(), role.getName()))
                .toList());
        // tenant
        List<Option<Long>> tenantOptions = tenantRolePrivateService.list(
                        Wrappers.<TenantRolePrivate>lambdaQuery()
                                .eq(TenantRolePrivate::getType, RoleTypeEnum.ROLE)
                                .eq(TenantRolePrivate::getStatus, CommonStatusEnum.ENABLE)
                )
                .stream()
                .filter(BizFilter.roleFilter(condition))
                .sorted(Comparator.comparing(TenantRolePrivate::getSort))
                .map(role -> Option.of(role.getId(), role.getName()))
                .toList();
        options.addAll(tenantOptions);
        return options;
    }

    @Override
    public List<RoleTreeNodeVO> conditionTree(TenantRolePrivate condition) {
        List<RoleTreeNodeVO> list = new ArrayList<>(metaRoleService.list(
                        Wrappers.<MetaRole>lambdaQuery()
                                .eq(MetaRole::getOrgType, OrgTypeEnum.Tenant)).
                stream()
                .filter(BizFilter.roleFilter(condition))
                .map(role -> BizUtils.convert(role, roleConvert))
                .toList());
        list.addAll(tenantRolePrivateService.list()
                .stream().filter(BizFilter.roleFilter(condition))
                .sorted(Comparator.comparing(TenantRolePrivate::getSort))
                .map(role -> {
                    RoleTreeNodeVO item = BizUtils.convert(role, roleConvert);
                    item.setCustom(true);
                    return item;
                })
                .toList());
        return TreeUtil.build(list);
    }

    @Override
    public List<PermissionIdBO> getRolePermissionIds(long roleId) {
        // private
        List<TenantRolePermissionPrivate> tenant = tenantRolePermissionPrivateService.getRoleBindPermissionIds(roleId);
        Set<Long> ids = new HashSet<>(tenant.stream().map(TenantRolePermissionPrivate::getPermissionId).toList());
        // meta
        List<Long> metaIds = roleAuthorityService.getRoleBindPermissionIds(roleId);
        ids.addAll(metaIds);

        List<PermissionIdBO> permissions = new ArrayList<>(tenant.stream()
                .map(item ->
                        PermissionIdBO.of(item.getPermissionId(), item.getMetaRole()))
                .toList());
        permissions.addAll(metaIds.stream().map(id -> PermissionIdBO.of(id, true)).toList());

        return ids.stream()
                .map(id -> permissions.stream()
                        .filter(item -> item.getId().equals(id)).findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<BizPermissionVO> getRolePermissions(long roleId) {
        List<PermissionIdBO> ids = getRolePermissionIds(roleId);
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        if (CollUtil.size(ids) == 1) {
            MetaPermission permission = authorityService.getById(ids.get(0).getId());
            BizPermissionVO result = authorityConvert.to(permission);
            result.setMetaRoleBind(ids.get(0).getMetaRoleBind());
            return List.of(result);
        }

        return authorityService.list(Wrappers.<MetaPermission>lambdaQuery()
                        .in(MetaPermission::getId, ids.stream().map(PermissionIdBO::getId).toList()))
                .stream()
                .map(item -> {
                    BizPermissionVO vo = authorityConvert.to(item);
                    vo.setMetaRoleBind(ids.stream()
                            .anyMatch(id ->
                                    id.getId().equals(item.getId()) && id.getMetaRoleBind()));
                    return vo;
                })
                .toList();
    }

    @Override
    public List<BizPermissionTreeNodeVO> getRolePermissionsTree(long roleId, MetaPermission condition) {
        List<BizPermissionVO> authorities = getRolePermissions(roleId);
        List<BizPermissionVO> finallyAuthorities = BizPermissionUtils.filterOrgLockAuthority(
                authorities, bizAppService);
        return BizPermissionUtils.bizMapTree(finallyAuthorities, authorityConvert, condition);
    }

    @Override
    public List<PermissionType> getRolesPermissions(List<RoleType> roles) {
        List<MetaPermission> enabledAuthorities = authorityService.list()
                .stream()
                .filter(auth -> auth.getStatus() == CommonStatusEnum.ENABLE)
                .toList();
        return roles.stream()
                .flatMap(role -> {
                    OrgTypeEnum orgType = role.getOrgType();
                    if (orgType == OrgTypeEnum.Platform) {
                        return roleAuthorityService.getRoleBindPermissionIds(role.getId()).stream()
                                .map(id -> enabledAuthorities.stream()
                                        .filter(item -> item.getId().equals(id))
                                        .findFirst()
                                        .orElse(null))
                                .filter(Objects::nonNull);
                    }
                    return getRolePermissionIds(role.getId())
                            .stream()
                            .map(PermissionIdBO::getId)
                            .map(id -> enabledAuthorities.stream()
                                    .filter(item -> item.getId().equals(id))
                                    .findFirst()
                                    .orElse(null))
                            .filter(Objects::nonNull);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionType> getRolesPermissionsAndChildren(List<RoleType> roles) {
        List<MetaPermission> all = authorityService.list();
        CopyOnWriteArrayList<PermissionType> authorities = new CopyOnWriteArrayList<>(getRolesPermissions(roles));
        authorities.forEach(item -> BizPermissionUtils.fillChildren(authorities, all, item));
        return authorities;
    }

    @Override
    public void create(TenantRolePrivate params) {
        tenantRolePrivateService.create(params);
    }

    @Override
    public void update(TenantRolePrivate params) {
        tenantRolePrivateService.update(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(long id) {
        // 清除关联权限
        tenantRolePermissionPrivateService.clearByRoleId(id);
        // 清除关联用户
        tenantRoleUserPrivateService.clearByRoleId(id);
        // 删除角色
        tenantRolePrivateService.delete(id);
    }

    @Override
    public void sort(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }

        AtomicInteger index = new AtomicInteger(0);
        List<TenantRolePrivate> list = ids.stream().map(id -> {
            TenantRolePrivate role = new TenantRolePrivate();
            role.setId(id);
            role.setSort(index.getAndIncrement());
            return role;
        }).toList();

        tenantRolePrivateService.updateBatchById(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPermissions(SetDTO<Long, Long> params) {
        MetaRole metaRole = metaRoleService.getById(params.getId());
        if (metaRole != null) {
            assertionChecker.checkOperation(!StrUtil.equals(metaRole.getCode(), RoleConstants.ROLE_ORG_ADMIN_CODE),
                    "BizRoleServiceImpl.OrgAdminCanNotBindAuth");
        }

        List<Long> bindList = params.getSetIds();
        if (CollUtil.isNotEmpty(bindList)) {
            List<Long> authorities = CollUtil.emptyIfNull(BizPermissionUtils.getTenantAuthorities(
                            TenantContextHolder.get(), bizAppService, authorityService, authorityConvert))
                    .stream().map(TreeNode::getId).toList();
            boolean canBind = new HashSet<>(authorities).containsAll(bindList);
            assertionChecker.checkOperation(canBind, "BizRoleServiceImpl.CantBindAndUnBindAuth");
        }

        // 元数据角色，和自定义角色，都在私有绑定关系中进行绑定
        BizBindDTO bindParams = new BizBindDTO();
        bindParams.setId(params.getId());
        bindParams.setMetaFlag(metaRole != null);
        bindParams.setAssignIds(bindList);
        tenantRolePermissionPrivateService.roleSetPermissions(bindParams);
    }

    @Override
    public void assignUsers(BizRoleAssignUsersDTO params) {
        Long deptId = params.getDeptId();
        RoleType role = getRole(params.getId());
        assertionChecker.checkOperation(role != null, "BizRoleServiceImpl.RoleNonNul");
        assert role != null;
        // 如果需要分配用户，那么需要判断是否传递了部门ID
        assertionChecker.checkOperation(CollUtil.isNotEmpty(params.getAssignIds()),
                BooleanUtil.isFalse(role.getFilterDept()) || deptId != null,
                "BizRoleServiceImpl.BindDeptRoleDeptNonNull");
        assertionChecker.checkOperation(role.getType() != RoleTypeEnum.GROUP,
                "BizRoleServiceImpl.CantBindRoleGroup");

        BizRoleAssignUsersBO bindParams = new BizRoleAssignUsersBO();
        bindParams.setId(params.getId());
        bindParams.setMetaFlag(role.getMetaRole());
        bindParams.setDeptId(deptId);
        bindParams.setAssignIds(params.getAssignIds());
        bindParams.setUnassignIds(params.getUnassignIds());
        tenantRoleUserPrivateService.roleBindUsers(bindParams);
    }

    @Override
    public void orgManagerAssignPermissions(List<Long> ids, boolean assign) {
        RoleType managerRole = getByCode(RoleConstants.ROLE_ORG_ADMIN_CODE);

        AssignDTO<Long, Long> bindParams = new AssignDTO<>();
        bindParams.setId(managerRole.getId());
        if (assign) {
            bindParams.setAssignIds(ids);
        } else {
            bindParams.setUnassignIds(ids);
        }
        roleAuthorityService.roleAssignPermissions(bindParams);
    }
}
