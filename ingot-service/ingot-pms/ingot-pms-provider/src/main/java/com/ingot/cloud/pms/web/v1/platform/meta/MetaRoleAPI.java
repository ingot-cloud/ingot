package com.ingot.cloud.pms.web.v1.platform.meta;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleItemVO;
import com.ingot.cloud.pms.service.biz.BizMetaRoleService;
import com.ingot.framework.commons.model.common.RelationDTO;
import com.ingot.framework.commons.model.support.Option;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : MetaRoleAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 14:44.</p>
 */
@Slf4j
@RestController
@Tag(description = "MetaRole", name = "元数据角色管理模块")
@RequestMapping(value = "/v1/platform/meta/role")
@RequiredArgsConstructor
public class MetaRoleAPI implements RShortcuts {
    private final BizMetaRoleService bizMetaRoleService;

    @AdminOrHasAnyAuthority({"meta:role:query"})
    @GetMapping(value = "/options")
    @Operation(summary = "角色选项", description = "角色选项列表")
    public R<List<Option<Long>>> options(MetaRole condition) {
        return ok(bizMetaRoleService.options(condition));
    }

    @AdminOrHasAnyAuthority({"meta:role:query"})
    @GetMapping(value = "/list")
    @Operation(summary = "角色列表", description = "角色列表")
    public R<List<RoleItemVO>> conditionList(MetaRole condition) {
        return ok(bizMetaRoleService.conditionList(condition));
    }

    @AdminOrHasAnyAuthority({"meta:role:create"})
    @PostMapping
    @Operation(summary = "创建角色", description = "创建角色")
    public R<Void> create(@RequestBody MetaRole params) {
        bizMetaRoleService.create(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"meta:role:update"})
    @PutMapping
    @Operation(summary = "更新角色", description = "更新角色")
    public R<Void> update(@RequestBody MetaRole params) {
        bizMetaRoleService.update(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"meta:role:delete"})
    @DeleteMapping(value = "/{id}")
    @Operation(summary = "删除角色", description = "删除角色")
    public R<Void> delete(@PathVariable Long id) {
        bizMetaRoleService.delete(id);
        return ok();
    }

    @AdminOrHasAnyAuthority({"meta:role:authorities:assign"})
    @PutMapping(value = "/{id}/authorities")
    @Operation(summary = "绑定权限", description = "绑定权限")
    public R<Void> bindAuthorities(@PathVariable Long id,
                                   @RequestBody RelationDTO<Long, Long> params) {
        params.setId(id);
        bizMetaRoleService.bindAuthorities(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"meta:role:authorities:query"})
    @GetMapping(value = "/{id}/authorities")
    @Operation(summary = "获取角色权限", description = "获取角色权限")
    public R<List<AuthorityTreeNodeVO>> getAuthorities(@PathVariable Long id) {
        return ok(bizMetaRoleService.getRoleAuthorities(id));
    }

}
