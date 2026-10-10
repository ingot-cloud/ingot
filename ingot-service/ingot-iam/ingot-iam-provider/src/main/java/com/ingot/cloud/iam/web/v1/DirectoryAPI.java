package com.ingot.cloud.iam.web.v1;

import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.MemberResources;
import com.ingot.framework.commons.model.iam.TenantMemberFilter;

import com.ingot.cloud.iam.policy.PolicyService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.cloud.iam.support.IamPurposes;
import com.ingot.framework.commons.model.iam.DepartmentRecord;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.SelectionPurpose;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供普通通讯录搜索与详情，按策略投影且不返回管理 ACTION。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 普通通讯录")
@RequestMapping("/v1/directory")
@RequiredArgsConstructor
public class DirectoryAPI implements RShortcuts {
    private final PolicyService policies;

    /** 返回当前身份与完整查询范围的字段操作能力。 */
    @GetMapping("/context")
    @Operation(summary = "字段能力上下文")
    public R<com.ingot.framework.commons.model.iam.ResourceFieldContext> context() { return ok(policies.directoryContext()); }

    /**
     * 分页列出可见通讯录成员。
     *
     * @param purpose 必须为 {@link SelectionPurpose#DIRECTORY}
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空；未完整可见时拒绝
     * @param email 邮箱精确筛选，可空；未完整可见时拒绝
     * @return 成员页
     */
    @Operation(summary = "普通通讯录成员")
    @GetMapping("/members")
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.DIRECTORY, action = IamAction.VALUE_TENANT_DIRECTORY_READ, valueType = MemberRecord.class)
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.DIRECTORY, action = IamAction.VALUE_TENANT_DIRECTORY_READ, use = FieldUse.FILTER, valueType = TenantMemberFilter.class)
    public R<PageResponse<ResourceDetail<MemberRecord>>> members(
            @RequestParam SelectionPurpose purpose,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        IamPurposes.require(purpose, SelectionPurpose.DIRECTORY);
        return ok(policies.listDirectoryMembers(page, pageSize, phone, email));
    }

    /**
     * 读取可见通讯录成员详情。
     *
     * @param id 成员 ID
     * @return 投影详情
     */
    @Operation(summary = "普通通讯录成员详情")
    @GetMapping("/members/{id}")
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.DIRECTORY, action = IamAction.VALUE_TENANT_DIRECTORY_READ, valueType = MemberRecord.class)
    public R<ResourceDetail<MemberRecord>> member(@PathVariable String id) {
        return ok(policies.getDirectoryMember(id));
    }

    /**
     * 列出可见部门树。
     *
     * @param purpose 必须为 {@link SelectionPurpose#DIRECTORY}
     * @param page 页码
     * @param pageSize 页大小
     * @return 部门页
     */
    @Operation(summary = "普通通讯录部门树")
    @GetMapping("/departments")
    public R<PageResponse<ResourceDetail<DepartmentRecord>>> departments(
            @RequestParam SelectionPurpose purpose,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        IamPurposes.require(purpose, SelectionPurpose.DIRECTORY);
        return ok(policies.listDirectoryDepartments(page, pageSize));
    }
}
