package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.assignment.TenantLiveAssignmentService;
import com.ingot.cloud.iam.assignment.TenantScopeCandidates;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.TenantAssignmentBatchInput;
import com.ingot.framework.commons.model.iam.AssignmentPreviewResult;
import com.ingot.framework.commons.model.iam.TenantAssignmentRecord;
import com.ingot.framework.commons.model.iam.TenantAssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
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
import java.util.List;

/**
 * <p>提供租户原子角色分配，提交时重验委派来源与范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户授权")
@RequestMapping("/v1/tenant/assignments")
@RequiredArgsConstructor
public class TenantAssignmentAPI implements RShortcuts {
    private final TenantLiveAssignmentService assignments;

    /**
     * 按固定角色版本及参数查询真实范围对象，未知资源明确返回不支持。
     * @param roleId 当前角色 ID
     * @param parameterKey 已声明参数键
     * @param keyword 搜索文字
     * @param ids 已选 ID 回显
     * @param page 页码
     * @param pageSize 页大小
     * @param tree 是否按部门树分支分页
     * @param parentId 父部门 ID
     * @return 当前租户分页候选
     */
    @Operation(summary = "租户分配范围对象候选")
    @com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest
    @GetMapping("/scope-candidates")
    public R<AuthorizationCandidatePage> scopeCandidates(@RequestParam String roleId,
            @RequestParam String parameterKey, @RequestParam(required=false) String delegationGrantId, @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) List<String> ids,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(defaultValue = "false") boolean tree,
            @RequestParam(required = false) String parentId) {
        return ok(assignments.scopeCandidates(roleId, parameterKey, delegationGrantId, keyword, ids, page, pageSize, tree, parentId));
    }

    /**
     * 分页列出租户授权。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @return 授权页
     */
    @Operation(summary = "授权列表")
    @com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest
    @GetMapping
    public R<PageResponse<ResourceDetail<TenantAssignmentRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize, @RequestParam(required=false) com.ingot.framework.commons.model.iam.SubjectType subjectType, @RequestParam(required=false) String keyword) {
        return ok(assignments.list(page, pageSize, subjectType, keyword));
    }

    /**
     * 原子批量分配。
     *
     * @param input 批次
     * @return 首条授权 ID
     */
    @Operation(summary = "原子批量分配")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody TenantAssignmentBatchInput input) {
        return ok(assignments.create(input));
    }

    /**
     * 预览批量分配，无写入。
     *
     * @param input 批次
     * @return 逐条效果
     */
    @Operation(summary = "预览原子分配批次")
    @PostMapping("/preview")
    public R<Preview<AssignmentPreviewResult>> preview(@Valid @RequestBody TenantAssignmentBatchInput input) {
        return ok(assignments.preview(input));
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
    public R<ResourceDetail<TenantAssignmentRecord>> replace(@PathVariable String id,
                                                       @Valid @RequestBody TenantAssignmentUpdateInput input) {
        return ok(assignments.replace(id, input));
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
        return ok(assignments.delete(id));
    }
    /** 查询当前角色或受限主体候选。 */
    @com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest
    @GetMapping("/options")
    public R<com.ingot.framework.commons.model.iam.PageResponse<com.ingot.framework.commons.model.iam.TenantAssignmentOption>> options(
        @RequestParam com.ingot.framework.commons.model.iam.TenantAssignmentOptionKind kind,
        @RequestParam(required=false) String delegationGrantId,@RequestParam(required=false) String keyword,
        @RequestParam(required=false) List<String> ids,@RequestParam(required=false) String departmentId,@RequestParam(defaultValue=""+IamPages.DEFAULT_PAGE) int page,@RequestParam(defaultValue=""+IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(assignments.options(kind,delegationGrantId,keyword,ids,page,pageSize,departmentId));
    }
    /** 受限分配流程读取当前角色，服务器重验白名单。 */
    @com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest
    @GetMapping("/roles/{id}")
    public R<com.ingot.framework.commons.model.iam.CurrentRoleGrantList> role(@PathVariable String id,@RequestParam(required=false) String delegationGrantId) {
        return ok(assignments.grants(id,delegationGrantId));
    }

}
