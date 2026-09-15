package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.assignment.JdbcAssignmentService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AssignmentBatchInput;
import com.ingot.framework.commons.model.iam.AssignmentPreviewResult;
import com.ingot.framework.commons.model.iam.AssignmentRecord;
import com.ingot.framework.commons.model.iam.AssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供平台原子角色分配，禁止使用部门范围参数。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台授权")
@RequestMapping("/v1/platform/assignments")
@RequiredArgsConstructor
public class PlatformAssignmentAPI implements RShortcuts {
    private final JdbcAssignmentService assignments;

    /**
     * 分页列出平台授权。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 授权页
     */
    @Operation(summary = "授权列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<AssignmentRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(assignments.list(AuthorizationDomain.PLATFORM, page, pageSize));
    }

    /**
     * 原子批量分配。
     *
     * @param input 批次
     * @return 首条授权 ID
     */
    @Operation(summary = "原子批量分配")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody AssignmentBatchInput input) {
        return ok(assignments.create(AuthorizationDomain.PLATFORM, input));
    }

    /**
     * 预览批量分配，无写入。
     *
     * @param input 批次
     * @return 逐条效果
     */
    @Operation(summary = "预览原子分配批次")
    @PostMapping("/preview")
    public R<Preview<AssignmentPreviewResult>> preview(@Valid @RequestBody AssignmentBatchInput input) {
        return ok(assignments.preview(AuthorizationDomain.PLATFORM, input));
    }

    /**
     * 调整既有授权。
     *
     * @param id 授权 ID
     * @param input 待保存定义
     * @return 更新后详情
     */
    @Operation(summary = "调整授权")
    @PutMapping("/{id}")
    public R<ResourceDetail<AssignmentRecord>> replace(@PathVariable String id,
                                                       @Valid @RequestBody AssignmentUpdateInput input) {
        return ok(assignments.replace(AuthorizationDomain.PLATFORM, id, input));
    }

    /**
     * 撤销授权并保留审计。
     *
     * @param id 授权 ID
     * @return 撤销前版本
     */
    @Operation(summary = "撤销授权并保留审计")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(assignments.delete(AuthorizationDomain.PLATFORM, id));
    }
}
