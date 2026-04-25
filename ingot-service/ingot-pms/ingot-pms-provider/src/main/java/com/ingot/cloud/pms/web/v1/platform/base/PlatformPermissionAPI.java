package com.ingot.cloud.pms.web.v1.platform.base;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.service.biz.BizPlatformPermissionService;
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
 * <p>Description  : PlatformAuthorityAPI.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/15.</p>
 * <p>Time         : 15:50.</p>
 */
@RestController
@Tag(description = "PlatformPermission", name = "平台权限管理模块")
@RequestMapping(value = "/v1/platform/base/permission")
@RequiredArgsConstructor
public class PlatformPermissionAPI implements RShortcuts {
    private final BizPlatformPermissionService platformAuthorityService;

    @AdminOrHasAnyAuthority({"platform:base:permission:query"})
    @GetMapping("/tree")
    @Operation(summary = "权限树", description = "权限树列表")
    public R<List<PermissionTreeNodeVO>> tree(PlatformPermission params) {
        return ok(platformAuthorityService.treeList(params));
    }

    @AdminOrHasAnyAuthority({"platform:base:permission:create"})
    @PostMapping
    @Operation(summary = "创建权限", description = "创建权限")
    public R<?> create(@Validated(Group.Create.class) @RequestBody PlatformPermission params) {
        params.setType(PermissionTypeEnum.API);
        platformAuthorityService.createNonMenuPermission(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:base:permission:update"})
    @PutMapping
    @Operation(summary = "更新权限", description = "更新权限")
    public R<?> update(@Validated(Group.Update.class) @RequestBody PlatformPermission params) {
        params.setType(null);
        platformAuthorityService.updateNonMenuPermission(params);
        return ok();
    }

    @AdminOrHasAnyAuthority({"platform:base:permission:delete"})
    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限", description = "删除权限")
    public R<?> removeById(@PathVariable Long id) {
        platformAuthorityService.deleteNonMenuPermission(id);
        return ok();
    }
}
