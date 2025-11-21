package com.ingot.cloud.pms.service.biz.impl;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.MetaAuthority;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.dto.common.BizBindDTO;
import com.ingot.cloud.pms.api.model.bo.role.BizRoleAssignUsersBO;
import com.ingot.cloud.pms.api.model.dto.role.BizRoleAssignUsersDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.authority.BizAuthorityVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.cloud.pms.common.BizFilter;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.core.BizAuthorityUtils;
import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.common.AssignDTO;
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
    private final MetaAuthorityService authorityService;
    private final MetaRoleAuthorityService roleAuthorityService;

    private final TenantRolePrivateService tenantRolePrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;
    private final TenantRoleAuthorityPrivateService tenantRoleAuthorityPrivateService;

    private final BizAppService bizAppService;

    private final AuthorityConvert authorityConvert;
    private final RoleConvert roleConvert;
    private final AssertionChecker assertionChecker;

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
                                .eq(TenantRolePrivate::getStatus, CommonStatusEnum.ENABLE)
                )
                .stream()
                .filter(BizFilter.roleFilter(condition))
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
                .map(role -> BizUtils.convert(role, roleConvert))
                .toList());
        return TreeUtil.build(list);
    }

    @Override
    public List<BizAuthorityVO> getRoleAuthorities(long roleId) {
        // private
        List<Long> tenantIds = tenantRoleAuthorityPrivateService.getRoleBindAuthorityIds(roleId);
        Set<Long> ids = new HashSet<>(tenantIds);
        // meta
        List<Long> metaIds = roleAuthorityService.getRoleBindAuthorityIds(roleId);
        ids.addAll(metaIds);

        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return authorityService.list(Wrappers.<MetaAuthority>lambdaQuery()
                        .in(MetaAuthority::getId, ids))
                .stream()
                .map(item -> {
                    BizAuthorityVO vo = authorityConvert.to(item);
                    vo.setMetaRoleBind(metaIds.stream()
                            .anyMatch(id -> id.equals(item.getId())));
                    return vo;
                })
                .toList();
    }

    @Override
    public List<BizAuthorityTreeNodeVO> getRoleAuthoritiesTree(long roleId, MetaAuthority condition) {
        List<BizAuthorityVO> authorities = getRoleAuthorities(roleId);
        List<BizAuthorityVO> finallyAuthorities = BizAuthorityUtils.filterOrgLockAuthority(
                authorities, bizAppService);
        return BizAuthorityUtils.bizMapTree(finallyAuthorities, authorityConvert, condition);
    }

    @Override
    public List<AuthorityType> getRolesAuthorities(List<RoleType> roles) {
        List<MetaAuthority> all = authorityService.list();
        return roles.stream()
                .flatMap(role -> {
                    OrgTypeEnum orgType = role.getOrgType();
                    if (orgType == OrgTypeEnum.Platform) {
                        return roleAuthorityService.getRoleBindAuthorityIds(role.getId()).stream()
                                .map(id -> all.stream()
                                        .filter(item -> item.getId().equals(id))
                                        .findFirst()
                                        .orElse(null))
                                .filter(Objects::nonNull);
                    }
                    return tenantRoleAuthorityPrivateService.getRoleBindAuthorityIds(role.getId()).stream()
                            .map(id -> all.stream()
                                    .filter(item -> item.getId().equals(id))
                                    .findFirst()
                                    .orElse(null))
                            .filter(Objects::nonNull);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthorityType> getRolesAuthoritiesAndChildren(List<RoleType> roles) {
        List<MetaAuthority> all = authorityService.list();
        CopyOnWriteArrayList<AuthorityType> authorities = new CopyOnWriteArrayList<>(getRolesAuthorities(roles));
        authorities.forEach(item -> BizAuthorityUtils.fillChildren(authorities, all, item));
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
        tenantRoleAuthorityPrivateService.clearByRoleId(id);
        // 清除关联用户
        tenantRoleUserPrivateService.clearByRoleId(id);
        // 删除角色
        tenantRolePrivateService.delete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAuthorities(AssignDTO<Long, Long> params) {
        MetaRole metaRole = metaRoleService.getById(params.getId());
        if (metaRole != null) {
            assertionChecker.checkOperation(!StrUtil.equals(metaRole.getCode(), RoleConstants.ROLE_ORG_ADMIN_CODE),
                    "BizRoleServiceImpl.OrgAdminCanNotBindAuth");
        }

        List<Long> bindList = params.getAssignIds();
        if (CollUtil.isNotEmpty(bindList)) {
            List<Long> authorities = CollUtil.emptyIfNull(BizAuthorityUtils.getTenantAuthorities(
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
        tenantRoleAuthorityPrivateService.roleSetAuthorities(bindParams);
    }

    @Override
    public void assignUsers(BizRoleAssignUsersDTO params) {
        Long deptId = params.getDeptId();
        MetaRole metaRole = metaRoleService.getById(params.getId());
        if (metaRole != null) {
            assertionChecker.checkOperation(BooleanUtil.isFalse(metaRole.getFilterDept()) || deptId != null,
                    "BizRoleServiceImpl.OrgAdminCanNotBindAuth");
        }

        BizRoleAssignUsersBO bindParams = new BizRoleAssignUsersBO();
        bindParams.setId(params.getId());
        bindParams.setMetaFlag(metaRole != null);
        bindParams.setDeptId(deptId);
        bindParams.setAssignIds(params.getAssignIds());
        bindParams.setUnassignIds(params.getUnassignIds());
        tenantRoleUserPrivateService.roleBindUsers(bindParams);
    }
}
