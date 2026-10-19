package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.MemberResources;
import com.ingot.framework.commons.model.iam.TenantMemberFilter;

import com.ingot.cloud.iam.authorization.read.IamReadOnlyRequest;
import com.ingot.cloud.iam.organization.MemberCommandService;
import com.ingot.cloud.iam.organization.MemberExportService;
import com.ingot.cloud.iam.organization.MemberQueryService;
import com.ingot.cloud.iam.organization.TenantMemberOnboarding;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.ExportTask;
import com.ingot.framework.commons.model.iam.GroupRecord;
import com.ingot.framework.commons.model.iam.TenantMemberCreateInput;
import com.ingot.framework.commons.model.iam.TenantMemberCreated;
import com.ingot.framework.commons.model.iam.MemberDepartmentInput;
import com.ingot.framework.commons.model.iam.MemberProfileInput;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatusInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.VersionInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import com.ingot.framework.security.crypto.annotation.InEncrypt;

/**
 * <p>提供租户成员列表、创建、资料、暂停、移出与任职替换，跨租户引用由服务端拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 租户成员命令")
@RequestMapping("/v1/tenant/members")
@RequiredArgsConstructor
public class TenantMemberCommandAPI implements RShortcuts {
    private final MemberCommandService members;
    private final MemberQueryService queries;
    private final MemberExportService exports;
    private final TenantMemberOnboarding onboarding;

    /** 返回当前身份与完整查询范围的字段操作能力。 */
    @IamReadOnlyRequest
    @GetMapping("/context")
    @Operation(summary = "字段能力上下文")
    public R<com.ingot.framework.commons.model.iam.ResourceFieldContext> context() { return ok(queries.tenantContext()); }

    /**
     * 分页列出租户成员。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @param departmentId 直属任职部门，可空
     * @param status 成员资格，可空；仅接受 ACTIVE、SUSPENDED、REMOVED
     * @return 成员页
     */
    @Operation(summary = "成员列表")
    @IamReadOnlyRequest
    @GetMapping
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.MEMBER, action = IamAction.VALUE_TENANT_MEMBER_READ, valueType = MemberRecord.class)
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.MEMBER, action = IamAction.VALUE_TENANT_MEMBER_READ, use = FieldUse.FILTER, valueType = TenantMemberFilter.class)
    public R<PageResponse<ResourceDetail<MemberRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String status) {
        return ok(queries.list(AuthorizationDomain.TENANT, page, pageSize, phone, email, departmentId, status));
    }

    /**
     * 按手机号加入当前组织；没有账号时先创建全局登录凭证。
     *
     * @param input 姓名、手机号、邮箱与任职
     * @return 新成员及可选一次性密码
     */
    @Operation(summary = "添加成员")
    @PostMapping
    @InCryptoHybridContext
    @InEncrypt
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.MEMBER, action = IamAction.VALUE_TENANT_MEMBER_CREATE, use = FieldUse.WRITE, valueType = TenantMemberCreateInput.class)
    public R<TenantMemberCreated> create(@Valid @RequestBody TenantMemberCreateInput input) {
        return ok(onboarding.create(input));
    }

    /**
     * 读取租户成员详情。
     *
     * @param id 租户成员 ID
     * @return 安全投影
     */
    @Operation(summary = "成员详情")
    @IamReadOnlyRequest
    @GetMapping("/{id}")
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.MEMBER, action = IamAction.VALUE_TENANT_MEMBER_READ, valueType = MemberRecord.class)
    public R<ResourceDetail<MemberRecord>> get(@PathVariable String id) {
        return ok(queries.get(AuthorizationDomain.TENANT, id));
    }

    /**
     * 分页列出租户成员所在用户组，含直接加入和部门来源。
     *
     * @param id 租户成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 组页
     */
    @Operation(summary = "成员所在用户组")
    @IamReadOnlyRequest
    @GetMapping("/{id}/groups")
    public R<PageResponse<ResourceDetail<GroupRecord>>> groups(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(queries.listTenantGroups(id, page, pageSize));
    }

    /**
     * 更新租户成员显示资料。
     *
     * @param id 租户成员 ID
     * @param input 资料
     * @return 更新后投影
     */
    @Operation(summary = "更新成员资料")
    @PatchMapping("/{id}")
    @FieldControl(domain = AuthorizationDomain.TENANT, applicationCode = MemberResources.TENANT_APPLICATION, resourceCode = MemberResources.MEMBER, action = IamAction.VALUE_TENANT_MEMBER_UPDATE, use = FieldUse.WRITE, valueType = MemberProfileInput.class)
    public R<ResourceDetail<MemberRecord>> patch(@PathVariable String id, @Valid @RequestBody MemberProfileInput input) {
        return ok(queries.patch(AuthorizationDomain.TENANT, id, input));
    }

    /**
     * 暂停或恢复租户成员资格。
     *
     * @param id 租户成员 ID
     * @param input 状态与预期版本
     * @return 提交后的版本
     */
    @Operation(summary = "暂停或恢复成员")
    @PatchMapping("/{id}/status")
    public R<CreatedResource> changeStatus(@PathVariable String id, @Valid @RequestBody MemberStatusInput input) {
        return ok(members.changeStatus(AuthorizationDomain.TENANT, id, input));
    }

    /**
     * 移出租户成员。
     *
     * @param id 租户成员 ID
     * @param input 预期版本
     * @return 提交后的版本
     */
    @Operation(summary = "移出当前域")
    @PostMapping("/{id}/remove")
    public R<CreatedResource> remove(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(members.remove(AuthorizationDomain.TENANT, id, input));
    }

    /**
     * 整体替换任职关系。
     *
     * @param id 租户成员 ID
     * @param input 目标部门
     * @return 不含凭证原值的成员投影
     */
    @Operation(summary = "调整成员任职")
    @PutMapping("/{id}/departments")
    public R<ResourceDetail<MemberRecord>> replaceDepartments(@PathVariable String id,
                                                              @Valid @RequestBody MemberDepartmentInput input) {
        return ok(members.replaceDepartments(id, input));
    }

    /**
     * 登记成员导出请求并重验组织版本。
     *
     * @param input 组织版本
     * @return 导出任务 ID
     */
    @Operation(summary = "导出成员")
    @PostMapping("/export")
    public R<CreatedResource> export(@Valid @RequestBody VersionInput input) {
        return ok(exports.create(input));
    }

    /**
     * 读取导出任务状态，不返回成员快照。
     *
     * @param id 导出任务 ID
     * @return 任务状态
     */
    @Operation(summary = "查询成员导出任务")
    @IamReadOnlyRequest
    @GetMapping("/export/{id}/status")
    public R<ExportTask> exportStatus(@PathVariable String id) {
        return ok(exports.status(id));
    }

    /**
     * 再次校验导出操作、范围与字段策略后返回投影结果。
     *
     * @param id 导出任务 ID
     * @return 投影后的成员页
     */
    @Operation(summary = "下载成员导出")
    @IamReadOnlyRequest
    @GetMapping("/export/{id}")
    public R<PageResponse<ResourceDetail<MemberRecord>>> downloadExport(@PathVariable String id) {
        return ok(exports.download(id));
    }
}
