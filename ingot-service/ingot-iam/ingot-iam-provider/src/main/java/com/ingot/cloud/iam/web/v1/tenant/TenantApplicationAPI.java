package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.catalog.EntitlementService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.ActionCatalogView;
import com.ingot.framework.commons.model.iam.ActionRecord;
import com.ingot.framework.commons.model.iam.AudienceUpdateInput;
import com.ingot.framework.commons.model.iam.AudienceView;
import com.ingot.framework.commons.model.iam.EntitlementRecord;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供当前租户已开通应用、权限树、操作候选与可用人群，开通不授予业务操作。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户应用开通")
@RequestMapping("/v1/tenant/applications")
@RequiredArgsConstructor
public class TenantApplicationAPI implements RShortcuts {
    private final EntitlementService entitlements;

    /**
     * 列出当前租户已开通应用。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 应用目录名称包含匹配，空白表示不限制
     * @return 开通页
     */
    @Operation(summary = "已开通应用")
    @GetMapping
    public R<PageResponse<ResourceDetail<EntitlementRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name) {
        return ok(entitlements.listForTenant(page, pageSize, name));
    }

    /**
     * 读取应用可用人群的展示快照。
     *
     * @param id 应用 ID
     * @return 含名称的人群快照
     */
    @Operation(summary = "应用可用人群")
    @GetMapping("/{id}/audience")
    public R<ResourceDetail<AudienceView>> getAudience(@PathVariable String id) {
        return ok(entitlements.getAudience(id));
    }

    /**
     * 替换应用可用人群，响应为更新后的展示快照。
     *
     * @param id 应用 ID
     * @param input 完整人群，只提交 ID
     * @return 更新后的展示快照
     */
    @Operation(summary = "更新可用人群")
    @PutMapping("/{id}/audience")
    public R<ResourceDetail<AudienceView>> putAudience(@PathVariable String id,
                                                       @Valid @RequestBody AudienceUpdateInput input) {
        return ok(entitlements.putAudience(id, input));
    }

    /**
     * 一次返回已开通应用的启用资源及启用操作树。
     *
     * @param id 应用 ID
     * @return 权限树
     */
    @Operation(summary = "已开通应用权限树")
    @GetMapping("/{id}/action-catalog")
    public R<ActionCatalogView> actionCatalog(@PathVariable String id) {
        return ok(entitlements.tenantActionCatalog(id));
    }

    /**
     * 列出已开通应用的操作候选。
     *
     * @param id 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 操作页
     */
    @Operation(summary = "已开通应用操作候选")
    @GetMapping("/{id}/actions")
    public R<PageResponse<ResourceDetail<ActionRecord>>> listActions(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(entitlements.listTenantApplicationActions(id, page, pageSize));
    }
}
