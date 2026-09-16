package com.ingot.cloud.iam.web.v1.tenant;

import com.ingot.cloud.iam.organization.MemberCommandService;
import com.ingot.cloud.iam.organization.MemberExportService;
import com.ingot.cloud.iam.organization.MemberQueryService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.ExportTask;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
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

    /**
     * 分页列出租户成员。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param phone 手机号精确筛选，可空
     * @param email 邮箱精确筛选，可空
     * @return 成员页
     */
    @Operation(summary = "成员列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<MemberRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        return ok(queries.list(AuthorizationDomain.TENANT, page, pageSize, phone, email));
    }

    /**
     * 把已有账号关联为租户成员。
     *
     * @param input 账号与任职
     * @return 新成员 ID
     */
    @Operation(summary = "创建成员资格")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody MemberCreateInput input) {
        return ok(queries.create(AuthorizationDomain.TENANT, input));
    }

    /**
     * 读取租户成员详情。
     *
     * @param id 租户成员 ID
     * @return 安全投影
     */
    @Operation(summary = "成员详情")
    @GetMapping("/{id}")
    public R<ResourceDetail<MemberRecord>> get(@PathVariable String id) {
        return ok(queries.get(AuthorizationDomain.TENANT, id));
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
    @GetMapping("/export/{id}")
    public R<PageResponse<ResourceDetail<MemberRecord>>> downloadExport(@PathVariable String id) {
        return ok(exports.download(id));
    }
}
