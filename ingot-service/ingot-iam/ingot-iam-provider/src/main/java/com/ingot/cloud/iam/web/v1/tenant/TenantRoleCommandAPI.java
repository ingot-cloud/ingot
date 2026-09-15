package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.role.JdbcRoleService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatusInput;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.EffectiveRole;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleCreateInput;
import com.ingot.framework.commons.model.iam.RoleDefinitionDraft;
import com.ingot.framework.commons.model.iam.RolePublishInput;
import com.ingot.framework.commons.model.iam.RoleRevision;
import com.ingot.framework.commons.model.iam.RoleSummary;
import com.ingot.framework.commons.model.iam.UpgradeInput;
import com.ingot.framework.commons.model.iam.UpgradePreview;
import com.ingot.framework.commons.model.iam.UpgradePreviewInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供租户可见角色、自定义差异及共享基础升级，提交不覆盖未指定授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户角色")
@RequestMapping("/v1/tenant/roles")
@RequiredArgsConstructor
public class TenantRoleCommandAPI implements RShortcuts {
    private final JdbcRoleService roles;

    /**
     * 分页列出租户可见角色。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 角色页
     */
    @Operation(summary = "角色目录")
    @GetMapping
    public R<PageResponse<ResourceDetail<RoleSummary>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(roles.list(AuthorizationDomain.TENANT, false, page, pageSize));
    }

    /**
     * 创建租户自定义角色并发布首个差异版本。
     *
     * @param input 创建命令
     * @return 新角色 ID
     */
    @Operation(summary = "创建角色并发布首个版本")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody RoleCreateInput input) {
        return ok(roles.create(AuthorizationDomain.TENANT, false, input));
    }

    /**
     * 读取角色元数据。
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    @Operation(summary = "角色元数据")
    @GetMapping("/{id}")
    public R<ResourceDetail<RoleSummary>> get(@PathVariable String id) {
        return ok(roles.get(AuthorizationDomain.TENANT, false, id));
    }

    /**
     * 启停租户自定义角色。
     *
     * @param id 角色 ID
     * @param input 目标状态
     * @return 提交后版本
     */
    @Operation(summary = "启停角色")
    @PatchMapping("/{id}")
    public R<CreatedResource> status(@PathVariable String id, @Valid @RequestBody ConfigurationStatusInput input) {
        return ok(roles.changeStatus(AuthorizationDomain.TENANT, false, id, input));
    }

    /**
     * 删除未被授权引用的租户自定义角色。
     *
     * @param id 角色 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除未引用角色")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(roles.delete(AuthorizationDomain.TENANT, false, id));
    }

    /**
     * 列出角色不可变版本。
     *
     * @param id 角色 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 版本页
     */
    @Operation(summary = "角色版本")
    @GetMapping("/{id}/revisions")
    public R<PageResponse<ResourceDetail<RoleRevision>>> revisions(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(roles.listRevisions(AuthorizationDomain.TENANT, false, id, page, pageSize));
    }

    /**
     * 发布新差异版本，不改写既有授权引用。
     *
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 新版本 ID
     */
    @Operation(summary = "发布新版本")
    @PostMapping("/{id}/revisions")
    public R<CreatedResource> publish(@PathVariable String id, @Valid @RequestBody RolePublishInput input) {
        return ok(roles.publish(AuthorizationDomain.TENANT, false, id, input));
    }

    /**
     * 预览待发布定义，无写入。
     *
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 合成预览
     */
    @Operation(summary = "预览待发布定义")
    @PostMapping("/{id}/preview")
    public R<Preview<EffectiveRole>> preview(@PathVariable String id, @Valid @RequestBody RoleDefinitionDraft input) {
        return ok(roles.preview(AuthorizationDomain.TENANT, false, id, input));
    }

    /**
     * 预览共享基础升级，无写入。
     *
     * @param id 角色 ID
     * @param input 目标基础
     * @return 三方比较
     */
    @Operation(summary = "预览共享基础升级")
    @PostMapping("/{id}/upgrade-preview")
    public R<Preview<UpgradePreview>> previewUpgrade(@PathVariable String id,
                                                     @Valid @RequestBody UpgradePreviewInput input) {
        return ok(roles.previewUpgrade(id, input));
    }

    /**
     * 提交共享基础升级，仅改写请求中的授权引用。
     *
     * @param id 角色 ID
     * @param input 处置与要改写的授权
     * @return 新版本 ID
     */
    @Operation(summary = "提交共享基础升级")
    @PostMapping("/{id}/upgrade")
    public R<CreatedResource> upgrade(@PathVariable String id, @Valid @RequestBody UpgradeInput input) {
        return ok(roles.upgrade(id, input));
    }
}
