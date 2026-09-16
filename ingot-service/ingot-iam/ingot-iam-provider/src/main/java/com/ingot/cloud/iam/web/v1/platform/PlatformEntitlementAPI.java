package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.catalog.EntitlementService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.EntitlementPreviewResult;
import com.ingot.framework.commons.model.iam.EntitlementRecord;
import com.ingot.framework.commons.model.iam.EntitlementReplaceInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供组织显式开通查询、预览与替换，开通不等于业务操作权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台组织开通")
@RequestMapping("/v1/platform/tenants/{tenantId}/entitlements")
@RequiredArgsConstructor
public class PlatformEntitlementAPI implements RShortcuts {
    private final EntitlementService entitlements;

    /**
     * 读取组织开通清单。
     *
     * @param tenantId 组织 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 开通页
     */
    @Operation(summary = "组织开通")
    @GetMapping
    public R<PageResponse<ResourceDetail<EntitlementRecord>>> list(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(entitlements.listForPlatform(tenantId, page, pageSize));
    }

    /**
     * 整体替换组织开通。
     *
     * @param tenantId 组织 ID
     * @param input 完整清单
     * @return 替换后清单
     */
    @Operation(summary = "替换组织开通")
    @PutMapping
    public R<PageResponse<ResourceDetail<EntitlementRecord>>> replace(
            @PathVariable String tenantId, @Valid @RequestBody EntitlementReplaceInput input) {
        return ok(entitlements.replace(tenantId, input));
    }

    /**
     * 预览开通影响，无写入。
     *
     * @param tenantId 组织 ID
     * @param input 完整清单
     * @return 只读预览
     */
    @Operation(summary = "预览开通影响")
    @PostMapping("/preview")
    public R<Preview<EntitlementPreviewResult>> preview(
            @PathVariable String tenantId, @Valid @RequestBody EntitlementReplaceInput input) {
        return ok(entitlements.preview(tenantId, input));
    }
}
