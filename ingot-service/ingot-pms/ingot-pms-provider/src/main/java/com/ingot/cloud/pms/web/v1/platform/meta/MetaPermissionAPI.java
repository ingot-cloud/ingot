package com.ingot.cloud.pms.web.v1.platform.meta;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizMetaPermissionService;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.core.utils.validation.Group;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>Description  : MetaAuthorityAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 15:50.</p>
 */
@RestController
@Tag(description = "MetaPermission", name = "元数据菜单管理模块")
@RequestMapping(value = "/v1/platform/meta/permission")
@RequiredArgsConstructor
public class MetaPermissionAPI implements RShortcuts {
    private final BizMetaPermissionService metaAuthorityService;

    @AdminOrHasAnyAuthority({"platform:meta:permission:query"})
    @GetMapping("/tree")
    @Operation(summary = "权限树", description = "权限树列表")
    public R<List<PermissionTreeNodeVO>> tree(MetaPermission params) {
        return ok(metaAuthorityService.treeList(params));
    }

    @AdminOrHasAnyAuthority({"platform:meta:permission:create"})
    @PostMapping
    @Operation(summary = "创建权限", description = "创建权限")
    public R<?> create(@Validated(Group.Create.class) @RequestBody MetaPermission params) {
        params.setType(PermissionTypeEnum.API);
        metaAuthorityService.createNonMenuPermission(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:meta:permission:update"})
    @PutMapping
    @Operation(summary = "更新权限", description = "更新权限")
    public R<?> update(@Validated(Group.Update.class) @RequestBody MetaPermission params) {
        params.setType(null);
        metaAuthorityService.updateNonMenuPermission(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:meta:permission:delete"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限", description = "删除权限")
    public R<?> removeById(@PathVariable Long id) {
        metaAuthorityService.deleteNonMenuPermission(id);
        return ok();
    }
}
