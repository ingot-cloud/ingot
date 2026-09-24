package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.catalog.CatalogService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.CatalogRecordView;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PlanDraft;
import com.ingot.framework.commons.model.iam.PlanRecord;
import com.ingot.framework.commons.model.iam.PlanUpdateInput;
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
 * <p>提供套餐目录，修改套餐不自动改变既有租户开通。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台套餐")
@RequestMapping("/v1/platform/plans")
@RequiredArgsConstructor
public class PlatformPlanAPI implements RShortcuts {
    private final CatalogService catalog;

    /**
     * 分页列出套餐。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 套餐名称包含匹配，可空
     * @param status 启停状态，可空；仅接受 ENABLED/DISABLED
     * @param view CATALOG 返回完整记录，SUMMARY 仅返回 id 与 name
     * @return 套餐页
     */
    @Operation(summary = "套餐目录")
    @GetMapping
    public R<?> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = CatalogRecordView.VALUE_CATALOG) String view) {
        CatalogRecordView recordView;
        try {
            recordView = CatalogRecordView.getEnum(view);
        } catch (IllegalArgumentException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (recordView == CatalogRecordView.SUMMARY) {
            return ok(catalog.listPlanSummaries(page, pageSize, name, status));
        }
        return ok(catalog.listPlans(page, pageSize, name, status));
    }

    /**
     * 创建套餐。
     *
     * @param input 套餐草稿
     * @return 新套餐 ID
     */
    @Operation(summary = "创建套餐")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody PlanDraft input) {
        return ok(catalog.createPlan(input));
    }

    /**
     * 读取套餐详情。
     *
     * @param id 套餐 ID
     * @return 套餐详情
     */
    @Operation(summary = "套餐详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<PlanRecord>> get(@PathVariable String id) {
        return ok(catalog.getPlan(id));
    }

    /**
     * 替换套餐应用清单。
     *
     * @param id 套餐 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    @Operation(summary = "更新套餐")
    @PutMapping("/{id}")
    public R<ResourceDetail<PlanRecord>> update(@PathVariable String id, @Valid @RequestBody PlanUpdateInput input) {
        return ok(catalog.updatePlan(id, input));
    }
}
