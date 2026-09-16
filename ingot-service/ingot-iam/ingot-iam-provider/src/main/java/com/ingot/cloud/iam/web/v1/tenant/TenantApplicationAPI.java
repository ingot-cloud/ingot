package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.catalog.EntitlementService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.ActionRecord;
import com.ingot.framework.commons.model.iam.AudienceDraft;
import com.ingot.framework.commons.model.iam.AudienceUpdateInput;
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
 * <p>提供当前租户已开通应用、操作候选与可用人群，开通不授予业务操作。</p>
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
     * @return 开通页
     */
    @Operation(summary = "已开通应用")
    @GetMapping
    public R<PageResponse<ResourceDetail<EntitlementRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(entitlements.listForTenant(page, pageSize));
    }

    /**
     * 读取应用可用人群。
     *
     * @param id 应用 ID
     * @return 人群配置
     */
    @Operation(summary = "应用可用人群")
    @GetMapping("/{id}/audience")
    public R<ResourceDetail<AudienceDraft>> getAudience(@PathVariable String id) {
        return ok(entitlements.getAudience(id));
    }

    /**
     * 更新应用可用人群。
     *
     * @param id 应用 ID
     * @param input 完整人群
     * @return 更新后配置
     */
    @Operation(summary = "更新可用人群")
    @PutMapping("/{id}/audience")
    public R<ResourceDetail<AudienceDraft>> putAudience(@PathVariable String id,
                                                        @Valid @RequestBody AudienceUpdateInput input) {
        return ok(entitlements.putAudience(id, input));
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
