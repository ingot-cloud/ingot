package com.ingot.cloud.member.web.v1.platform;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.cloud.member.service.biz.BizPermissionService;
import com.ingot.framework.commons.model.enums.PermissionTypeEnum;
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
    private final MemberGlobalAccess access;
    private final BizPermissionService bizPermissionService;

    /** 权限树，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "权限树", description = "获取权限树结构")
    @GetMapping("/tree")
    public R<List<MemberPermissionTreeNodeVO>> tree(MemberPermission condition) {
        access.require(MemberAdminResources.PERMISSION, MemberAdminResources.PERMISSION_QUERY);
        return ok(bizPermissionService.treeList(condition));
    }

    /** 创建权限，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "创建权限", description = "创建新的权限")
    @PostMapping
    public R<Void> create(@Validated(Group.Create.class) @RequestBody MemberPermission params) {
        access.require(MemberAdminResources.PERMISSION, MemberAdminResources.PERMISSION_CREATE);
        params.setType(PermissionTypeEnum.API);
        bizPermissionService.createNonMenuPermission(params);
        return ok();
    }

    /** 更新权限，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "更新权限", description = "更新权限信息")
    @PutMapping
    public R<Void> update(@Validated(Group.Update.class) @RequestBody MemberPermission params) {
        if (params.getCreatedAt() != null || params.getUpdatedAt() != null || params.getDeletedAt() != null) {
            throw new com.ingot.framework.commons.error.BizException(
                    com.ingot.framework.commons.model.iam.IamReasonCode.INVALID_ARGUMENT);
        }
        if (params.getStatus() != null) {
            access.require(MemberAdminResources.PERMISSION, MemberAdminResources.PERMISSION_CHANGE_STATUS);
        }
        if (params.getStatus() == null || params.getPid() != null || params.getName() != null || params.getCode() != null || params.getRemark() != null) {
            access.require(MemberAdminResources.PERMISSION, MemberAdminResources.PERMISSION_UPDATE);
        }
        params.setType(null);
        bizPermissionService.updateNonMenuPermission(params);
        return ok();
    }

    /** 删除权限，要求当前平台身份具备相应全局操作权限。 */
    @Operation(summary = "删除权限", description = "根据ID删除权限")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        access.require(MemberAdminResources.PERMISSION, MemberAdminResources.PERMISSION_DELETE);
        bizPermissionService.deleteNonMenuPermission(id);
        return ok();
    }
}

