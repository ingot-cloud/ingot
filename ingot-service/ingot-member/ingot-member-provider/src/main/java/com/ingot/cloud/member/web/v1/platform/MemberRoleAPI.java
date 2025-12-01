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
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
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
    private final BizRoleService bizRoleService;

    @Operation(summary = "角色选项", description = "获取角色选项列表")
    @AdminOrHasAnyAuthority({"platform:member:role:query"})
    @GetMapping("/options")
    public R<List<Option<Long>>> options() {
        return ok(bizRoleService.options(null));
    }

    @Operation(summary = "角色树", description = "获取角色树结构")
    @AdminOrHasAnyAuthority({"platform:member:role:query"})
    @GetMapping("/tree")
    public R<List<MemberRoleTreeNodeVO>> tree(MemberRole condition) {
        return ok(bizRoleService.conditionTree(condition));
    }

    @Operation(summary = "创建角色", description = "创建新的角色")
    @AdminOrHasAnyAuthority({"platform:member:role:create"})
    @PostMapping
    public R<Void> create(@Validated(Group.Create.class) @RequestBody MemberRole params) {
        bizRoleService.create(params);
        return ok();
    }

    @Operation(summary = "更新角色", description = "更新角色信息")
    @AdminOrHasAnyAuthority({"platform:member:role:update"})
    @PutMapping
    public R<Void> update(@Validated(Group.Update.class) @RequestBody MemberRole params) {
        bizRoleService.update(params);
        return ok();
    }

    @Operation(summary = "删除角色", description = "根据ID删除角色")
    @AdminOrHasAnyAuthority({"platform:member:role:delete"})
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        bizRoleService.delete(id);
        return ok();
    }

    @Operation(summary = "角色分配用户", description = "为角色分配用户")
    @AdminOrHasAnyAuthority({"platform:member:role:user:assign"})
    @PutMapping("/{id}/users")
    public R<Void> assignUsers(@PathVariable Long id,
                               @RequestBody AssignDTO<Long, Long> params) {
        params.setId(id);
        bizRoleService.assignUsers(params);
        return ok();
    }

    @Operation(summary = "设置角色权限", description = "设置角色的权限")
    @AdminOrHasAnyAuthority("platform:member:role:permissions:set")
    @PutMapping("/{id}/permissions")
    public R<Void> setPermissions(@PathVariable Long id,
                                  @RequestBody SetDTO<Long, Long> params) {
        params.setId(id);
        bizRoleService.setPermissions(params);
        return ok();
    }

    @Operation(summary = "获取角色权限树", description = "获取角色的权限树")
    @AdminOrHasAnyAuthority("platform:member:role:permissions:query")
    @GetMapping("/{id}/permissions")
    public R<List<MemberPermissionTreeNodeVO>> getRolePermissionsTree(@PathVariable Long id) {
        return ok(bizRoleService.getRolePermissionsTree(id));
    }

}

