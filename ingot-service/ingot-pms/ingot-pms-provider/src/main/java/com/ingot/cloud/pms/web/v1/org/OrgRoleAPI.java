package com.ingot.cloud.pms.web.v1.org;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.dto.common.IdsDTO;
import com.ingot.cloud.pms.api.model.dto.role.BizRoleAssignUsersDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.vo.permission.BizPermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizRoleService;
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
 * <p>Description  : OrgRoleAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/22.</p>
 * <p>Time         : 2:40 PM.</p>
 */
@Slf4j
@Tag(description = "OrgRole", name = "组织角色模块")
@RestController
@RequestMapping(value = "/v1/org/role")
@RequiredArgsConstructor
public class OrgRoleAPI implements RShortcuts {
    private final BizRoleService bizRoleService;

    @Operation(summary = "角色选项", description = "角色选项")
    @AdminOrHasAnyAuthority({"contacts:role:query"})
    @GetMapping("/options")
    public R<List<Option<Long>>> options() {
        return ok(bizRoleService.options(null));
    }

    @Operation(summary = "角色树", description = "角色树")
    @AdminOrHasAnyAuthority({"contacts:role:query"})
    @GetMapping("/tree")
    public R<List<RoleTreeNodeVO>> tree(TenantRolePrivate condition) {
        return ok(bizRoleService.conditionTree(condition));
    }

    @Operation(summary = "创建角色", description = "创建角色")
    @AdminOrHasAnyAuthority({"contacts:role:create"})
    @PostMapping
    public R<Void> create(@Validated(Group.Create.class) @RequestBody TenantRolePrivate params) {
        // 自定义组织角色
        params.setOrgType(OrgTypeEnum.Tenant);
        bizRoleService.create(params);
        return ok();
    }

    @Operation(summary = "更新角色", description = "更新角色")
    @AdminOrHasAnyAuthority({"contacts:role:update"})
    @PutMapping
    public R<Void> update(@Validated(Group.Update.class) @RequestBody TenantRolePrivate params) {
        bizRoleService.update(params);
        return ok();
    }

    @Operation(summary = "删除角色", description = "删除角色")
    @AdminOrHasAnyAuthority({"contacts:role:delete"})
    @DeleteMapping("/{id}")
    public R<Void> removeById(@PathVariable Long id) {
        bizRoleService.delete(id);
        return ok();
    }

    @Operation(summary = "角色排序", description = "角色排序")
    @AdminOrHasAnyAuthority({"contacts:role:sort"})
    @PutMapping("/sort")
    public R<Void> sort(@RequestBody IdsDTO params) {
        bizRoleService.sort(params.getIds());
        return ok();
    }

    @Operation(summary = "角色分配用户", description = "角色分配用户")
    @AdminOrHasAnyAuthority({"contacts:role:user:assign"})
    @PutMapping("/{id}/users")
    public R<Void> assignUsers(@PathVariable Long id,
                               @RequestBody BizRoleAssignUsersDTO params) {
        params.setId(id);
        bizRoleService.assignUsers(params);
        return ok();
    }

    @Operation(summary = "设置角色权限", description = "设置角色权限")
    @AdminOrHasAnyAuthority("contacts:role:permissions:set")
    @PutMapping("/{id}/permissions")
    public R<Void> setPermissions(@PathVariable Long id,
                                  @RequestBody SetDTO<Long, Long> params) {
        params.setId(id);
        bizRoleService.setPermissions(params);
        return ok();
    }

    @Operation(summary = "获取角色权限树", description = "获取角色权限树")
    @AdminOrHasAnyAuthority("contacts:role:permissions:query")
    @GetMapping("/{id}/permissions")
    public R<List<BizPermissionTreeNodeVO>> getRolePermissionsTree(@PathVariable Long id,
                                                                   MetaPermission condition) {
        return ok(bizRoleService.getRolePermissionsTree(id, condition));
    }
}
