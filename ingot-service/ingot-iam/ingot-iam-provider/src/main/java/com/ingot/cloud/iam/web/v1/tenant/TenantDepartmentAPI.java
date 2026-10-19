package com.ingot.cloud.iam.web.v1.tenant;

import java.util.List;

import com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest;
import com.ingot.cloud.iam.organization.DepartmentService;
import com.ingot.cloud.iam.organization.DepartmentLeaderService;
import com.ingot.framework.commons.model.iam.DepartmentLeaderPage;
import com.ingot.framework.commons.model.iam.DepartmentLeaderUpdateInput;
import org.springframework.web.bind.annotation.PatchMapping;
import com.ingot.cloud.iam.support.IamPurposes;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DepartmentDraft;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.DepartmentTreeNode;
import com.ingot.framework.commons.model.iam.DepartmentUpdateInput;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.SelectionPurpose;
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
 * <p>提供当前租户部门树维护，拒绝跨租户引用和非空删除。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户部门")
@RequestMapping("/v1/tenant/departments")
@RequiredArgsConstructor
public class TenantDepartmentAPI implements RShortcuts {
    private final DepartmentService departments;
    private final DepartmentLeaderService leaders;
    private final com.ingot.cloud.iam.organization.DepartmentEditService editor;
    private final com.ingot.cloud.iam.organization.DepartmentNavigationService navigation;

    /**
     * 返回管理部门树，子部门挂在各自父节点下。
     *
     * @param purpose 必须为 {@link SelectionPurpose#MANAGED_DEPARTMENT}
     * @return 部门树
     */
    @Operation(summary = "部门树")
    @IamReadOnlyRequest
    @GetMapping
    public R<List<DepartmentTreeNode>> list(@RequestParam SelectionPurpose purpose) {
        IamPurposes.require(purpose, SelectionPurpose.MANAGED_DEPARTMENT);
        return ok(departments.list());
    }

    /** 人员选择专用部门分支，部门只导航而不作为人员选择结果。 */
    @IamReadOnlyRequest
    @GetMapping("/member-picker-navigation")
    @Operation(summary = "组织成员部门导航")
    public R<com.ingot.framework.commons.model.iam.DepartmentNavigationPage> navigation(
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "" + com.ingot.cloud.iam.support.IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + com.ingot.cloud.iam.support.IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(navigation.page(parentId, page, pageSize));
    }

    /** 分页读取当前部门已绑定的可见负责人。 */
    @IamReadOnlyRequest
    @com.ingot.framework.commons.annotation.field.FieldControl(domain = com.ingot.framework.commons.model.iam.AuthorizationDomain.TENANT,
            applicationCode = com.ingot.framework.commons.model.iam.MemberResources.TENANT_APPLICATION,
            resourceCode = com.ingot.framework.commons.model.iam.MemberResources.MEMBER,
            action = com.ingot.framework.commons.model.iam.IamAction.VALUE_TENANT_MEMBER_READ,
            use = com.ingot.framework.commons.annotation.field.FieldUse.FILTER,
            valueType = com.ingot.framework.commons.model.iam.OrganizationMemberNameFilter.class)
    @GetMapping("/{id}/leaders")
    @Operation(summary = "部门负责人列表")
    public R<DepartmentLeaderPage> leaders(@PathVariable String id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "" + com.ingot.cloud.iam.support.IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + com.ingot.cloud.iam.support.IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(leaders.page(id, keyword, page, pageSize, false));
    }

    /** 仅返回本次操作者有权选择的有效成员，不开放全员列表。 */
    @IamReadOnlyRequest
    @com.ingot.framework.commons.annotation.field.FieldControl(domain = com.ingot.framework.commons.model.iam.AuthorizationDomain.TENANT,
            applicationCode = com.ingot.framework.commons.model.iam.MemberResources.TENANT_APPLICATION,
            resourceCode = com.ingot.framework.commons.model.iam.MemberResources.MEMBER,
            action = com.ingot.framework.commons.model.iam.IamAction.VALUE_TENANT_MEMBER_READ,
            use = com.ingot.framework.commons.annotation.field.FieldUse.FILTER,
            valueType = com.ingot.framework.commons.model.iam.DepartmentLeaderCandidateFilter.class)
    @GetMapping("/{id}/leader-candidates")
    @Operation(summary = "部门负责人候选")
    public R<DepartmentLeaderPage> leaderCandidates(@PathVariable String id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String browseDepartmentId,
            @RequestParam(defaultValue = "" + com.ingot.cloud.iam.support.IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + com.ingot.cloud.iam.support.IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(leaders.candidates(id, keyword, page, pageSize, browseDepartmentId));
    }

    /** 一次版本原子提交基本信息和负责人，权限按实际变化分别校验。 */
    @PatchMapping("/{id}")
    @Operation(summary = "统一编辑部门")
    public R<ResourceDetail<DepartmentRecord>> edit(@PathVariable String id,
            @Valid @RequestBody com.ingot.framework.commons.model.iam.DepartmentEditInput input) {
        return ok(editor.edit(id, input));
    }

    /** 保存负责人差量，不授予或同步任何角色。 */
    @PatchMapping("/{id}/leaders")
    @Operation(summary = "设置部门负责人")
    public R<CreatedResource> updateLeaders(@PathVariable String id, @Valid @RequestBody DepartmentLeaderUpdateInput input) {
        return ok(leaders.update(id, input));
    }

    /**
     * 创建部门.
     *
     * @param input 部门草稿
     * @return 新部门 ID
     */
    @Operation(summary = "创建部门")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody DepartmentDraft input) {
        return ok(departments.create(input));
    }

    /**
     * 读取部门详情。
     *
     * @param id 部门 ID
     * @return 部门详情
     */
    @Operation(summary = "部门详情")
    @IamReadOnlyRequest
    @GetMapping("/{id}")
    public R<ResourceDetail<DepartmentRecord>> get(@PathVariable String id) {
        return ok(departments.get(id));
    }

    /**
     * 更新部门资料或位置。
     *
     * @param id 部门 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    public R<ResourceDetail<DepartmentRecord>> update(@PathVariable String id,
                                                      @Valid @RequestBody DepartmentUpdateInput input) {
        return ok(departments.update(id, input));
    }

    /**
     * 删除空部门。
     *
     * @param id 部门 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除空部门")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(departments.delete(id));
    }
}
