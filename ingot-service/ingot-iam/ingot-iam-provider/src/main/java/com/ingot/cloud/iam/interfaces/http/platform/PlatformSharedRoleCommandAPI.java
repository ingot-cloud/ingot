package com.ingot.cloud.iam.interfaces.http.platform;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.RoleKind;

import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.CurrentRoleGrantList;
import com.ingot.cloud.iam.application.role.model.CurrentRolePermissionInput;
import com.ingot.cloud.iam.application.role.model.CurrentRolePermissionResult;
import com.ingot.cloud.iam.application.role.model.CurrentRolePreviewResult;
import com.ingot.cloud.iam.application.role.model.CurrentRoleProfileInput;
import com.ingot.cloud.iam.application.role.model.CurrentRoleSummary;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.domain.role.CurrentRoleCreateInput;

import com.ingot.cloud.iam.application.identity.CurrentIdentityService;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionService;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>提供平台维护共享角色的当前权限，不向租户暴露业务版本。</p>
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 当前角色")
@RequestMapping("/v1/platform/shared-roles")
@RequiredArgsConstructor
public class PlatformSharedRoleCommandAPI implements RShortcuts {
    private final TenantRoleDefinitionService roles;
    private final CurrentIdentityService identities;

    /** 按归属及筛选分页列出当前角色。 */
    @Operation(summary = "当前角色目录")
    @com.ingot.cloud.iam.application.authorization.read.IamReadOnlyRequest
    @GetMapping
    public R<PageResponse<ResourceDetail<CurrentRoleSummary>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) RoleKind kind) {
        return ok(roles.list(true, page, pageSize, name, status, kind));
    }

    /** 预览创建完整角色草稿，不分配标识。 */
    @Operation(summary = "预览创建角色")
    @PostMapping("/preview")
    public R<Preview<CurrentRolePreviewResult>> previewCreate(@Valid @RequestBody CurrentRoleCreateInput input) {
        return ok(roles.previewCreate(true, input));
    }

    /** 原子创建角色及首个完整当前权限。 */
    @Operation(summary = "创建当前角色")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody CurrentRoleCreateInput input) {
        return ok(roles.create(true, input));
    }

    /** 读取可见角色身份。 */
    @Operation(summary = "当前角色资料")
    @com.ingot.cloud.iam.application.authorization.read.IamReadOnlyRequest
    @GetMapping("/{id}")
    public R<ResourceDetail<CurrentRoleSummary>> get(@PathVariable String id) { return ok(roles.get(true, id)); }

    /** 读取完整当前权限和目录回显。 */
    @Operation(summary = "角色当前权限")
    @com.ingot.cloud.iam.application.authorization.read.IamReadOnlyRequest
    @GetMapping("/{id}/grants")
    public R<CurrentRoleGrantList> grants(@PathVariable String id, @RequestParam(required=false) String revisionId) { if(revisionId!=null)throw new BizException(IamReasonCode.INVALID_ARGUMENT);return ok(roles.grants(true, id)); }

    /** 编辑当前资料与启停，角色归属由服务端重验。 */
    @Operation(summary = "编辑角色资料")
    @PatchMapping("/{id}")
    public R<ResourceDetail<CurrentRoleSummary>> patch(@PathVariable String id, @Valid @RequestBody CurrentRoleProfileInput input) {
        return ok(roles.profile(true, id, input));
    }

    /** 预览保存后的当前权限与范围变化。 */
    @Operation(summary = "预览当前权限")
    @PostMapping("/{id}/preview")
    public R<Preview<CurrentRolePreviewResult>> preview(@PathVariable String id, @Valid @RequestBody CurrentRolePermissionInput input, @RequestParam(required=false) String sourceRevisionId) {
        if (sourceRevisionId != null) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return ok(roles.preview(true, id, input));
    }

    /** 保存完整当前权限，关联人员在后续请求自动跟随。 */
    @Operation(summary = "保存角色当前权限")
    @PutMapping("/{id}/grants")
    public R<CurrentRolePermissionResult> save(@PathVariable String id, @Valid @RequestBody CurrentRolePermissionInput input) {
        return ok(roles.save(true, id, input));
    }

    /** 删除无实际引用角色，保留审计。 */
    @Operation(summary = "删除未引用角色")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) { return ok(roles.delete(true, id)); }

    /** 已认证旧入口明确退出，不解释或转换旧版本标识。 */
    @Operation(summary = "已退出的角色版本入口")
    @RequestMapping({"/{id}/revisions", "/{id}/revisions/{revisionId}", "/{id}/revisions/{revisionId}/metadata", "/{id}/revisions/{revisionId}/grants"})
    public R<Void> retired() {
        identities.requireDomain(AuthorizationDomain.PLATFORM);
        throw new BizException(IamReasonCode.ENDPOINT_RETIRED);
    }
}
