package com.ingot.cloud.member.web.v1.platform;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.cloud.member.service.biz.BizPermissionService;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
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
 * <p>Description  : MemberPermissionAPI.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@Slf4j
@Tag(name = "会员权限模块", description = "MemberPermission")
@RestController
@RequestMapping(value = "/v1/platform/member/permission")
@RequiredArgsConstructor
public class MemberPermissionAPI implements RShortcuts {
    private final BizPermissionService bizPermissionService;

    @Operation(summary = "权限树", description = "获取权限树结构")
    @AdminOrHasAnyAuthority({"platform:member:permission:query"})
    @GetMapping("/tree")
    public R<List<MemberPermissionTreeNodeVO>> tree(MemberPermission condition) {
        return ok(bizPermissionService.treeList(condition));
    }

    @Operation(summary = "创建权限", description = "创建新的权限")
    @AdminOrHasAnyAuthority({"platform:member:permission:create"})
    @PostMapping
    public R<Void> create(@Validated(Group.Create.class) @RequestBody MemberPermission params) {
        params.setType(PermissionTypeEnum.API);
        bizPermissionService.createNonMenuPermission(params);
        return ok();
    }

    @Operation(summary = "更新权限", description = "更新权限信息")
    @AdminOrHasAnyAuthority({"platform:member:permission:update"})
    @PutMapping
    public R<Void> update(@Validated(Group.Update.class) @RequestBody MemberPermission params) {
        params.setType(null);
        bizPermissionService.updateNonMenuPermission(params);
        return ok();
    }

    @Operation(summary = "删除权限", description = "根据ID删除权限")
    @AdminOrHasAnyAuthority({"platform:member:permission:delete"})
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        bizPermissionService.deleteNonMenuPermission(id);
        return ok();
    }
}

