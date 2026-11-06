package com.ingot.cloud.member.web.v1.platform;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.cloud.member.api.model.vo.role.MemberRoleTreeNodeVO;
import com.ingot.cloud.member.service.biz.BizRoleService;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.cloud.member.authorization.MemberGlobalAccess;
import com.ingot.framework.commons.model.iam.MemberAdminResources;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : MemberRoleAPI.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@Slf4j
@Tag(name = "会员角色模块", description = "MemberRole")
@RestController
@RequestMapping(value = "/v1/platform/member/role")
@RequiredArgsConstructor
public class MemberRoleAPI implements RShortcuts {
    private final MemberGlobalAccess access;
    private final BizRoleService bizRoleService;

    /** 角色选项，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "角色选项", description = "获取角色选项列表")
    @GetMapping("/options")
    public R<List<Option<Long>>> options() {
        access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_QUERY);
        return ok(bizRoleService.options(null));
    }

    /** 角色树，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "角色树", description = "获取角色树结构")
    @GetMapping("/tree")
    public R<List<MemberRoleTreeNodeVO>> tree(MemberRole condition) {
        access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_QUERY);
        return ok(bizRoleService.conditionTree(condition));
    }

    /** 创建角色，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "创建角色", description = "创建新的角色")
    @PostMapping
    public R<Void> create(@Validated(Group.Create.class) @RequestBody MemberRole params) {
        access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_CREATE);
        bizRoleService.create(params);
        return ok();
    }

    /** 更新角色，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "更新角色", description = "更新角色信息")
    @PutMapping
    public R<Void> update(@Validated(Group.Update.class) @RequestBody MemberRole params) {
        if (params.getCreatedAt() != null || params.getUpdatedAt() != null || params.getDeletedAt() != null) {
            throw new com.ingot.framework.commons.error.BizException(
                    com.ingot.framework.commons.model.iam.IamReasonCode.INVALID_ARGUMENT);
        }
        if (params.getStatus() != null) {
            access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_CHANGE_STATUS);
        }
        if (params.getStatus() == null || params.getPid() != null || params.getName() != null || params.getCode() != null || params.getBuiltIn() != null) {
            access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_UPDATE);
        }
        bizRoleService.update(params);
        return ok();
    }

    /** 删除角色，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "删除角色", description = "根据ID删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_DELETE);
        bizRoleService.delete(id);
        return ok();
    }

    /** 角色分配用户，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "角色分配用户", description = "为角色分配用户")
    @PutMapping("/{id}/users")
    public R<Void> assignUsers(@PathVariable Long id,
                               @RequestBody AssignDTO<Long, Long> params) {
        access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_USER_ASSIGN);
        params.setId(id);
        bizRoleService.assignUsers(params);
        return ok();
    }

    /** 设置角色权限，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "设置角色权限", description = "设置角色的权限")
    @PutMapping("/{id}/permissions")
    public R<Void> setPermissions(@PathVariable Long id,
                                  @RequestBody SetDTO<Long, Long> params) {
        access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_PERMISSIONS_SET);
        params.setId(id);
        bizRoleService.setPermissions(params);
        return ok();
    }

    /** 获取角色权限树，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "获取角色权限树", description = "获取角色的权限树")
    @GetMapping("/{id}/permissions")
    public R<List<MemberPermissionTreeNodeVO>> getRolePermissionsTree(@PathVariable Long id) {
        access.require(MemberAdminResources.ROLE, MemberAdminResources.ROLE_PERMISSIONS_QUERY);
        return ok(bizRoleService.getRolePermissionsTree(id));
    }

}

