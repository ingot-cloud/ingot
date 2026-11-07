package com.ingot.cloud.iam.interfaces.http.platform;

import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.PageResponse;

import com.ingot.cloud.iam.application.assignment.model.AssignmentRecord;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationChangeInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationConfirmInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationItem;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationPrepareInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationRefreshInput;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationTask;
import com.ingot.cloud.iam.application.roleapplication.model.RoleVersionApplicationPublishInput;
import com.ingot.cloud.iam.domain.role.RoleRevisionDistribution;

import com.ingot.cloud.iam.application.authorization.read.IamReadOnlyRequest;

import com.ingot.cloud.iam.application.roleapplication.RoleVersionApplicationService;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>平台自定义角色的固定版本批量应用；角色编辑权限不替代独立分配升级资格。</p>
 * @author jy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/platform/roles/{roleId}/version-applications")
@Tag(name = "IAM 角色版本应用")
@RequiredArgsConstructor
public class PlatformRoleVersionApplicationAPI implements RShortcuts {
    private final RoleVersionApplicationService applications;

    /** 准备已发布版本的集合快照和完整预检。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "准备版本应用")
    @PostMapping
    public R<RoleVersionApplicationTask> prepare(@PathVariable String roleId, @Valid @RequestBody RoleVersionApplicationPrepareInput input) {
        return ok(applications.prepare(roleId, input));
    }

    /** 发布新权限版本及应用计划在同一事务保存。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "发布并准备应用")
    @PostMapping("/publish")
    public R<RoleVersionApplicationTask> publish(@PathVariable String roleId, @Valid @RequestBody RoleVersionApplicationPublishInput input) {
        return ok(applications.publishAndPrepare(roleId, input));
    }

    /** 分页列出当前角色的应用记录。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "版本应用任务记录")
    @IamReadOnlyRequest
    @GetMapping
    public R<PageResponse<RoleVersionApplicationTask>> list(@PathVariable String roleId,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(applications.list(roleId, page, pageSize));
    }

    /** 角色关联分配的候选和计数共用治理对象范围。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "角色版本应用分配候选")
    @GetMapping("/candidates")
    public R<PageResponse<ResourceDetail<AssignmentRecord>>> candidates(@PathVariable String roleId,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(applications.candidates(roleId, page, pageSize));
    }

    /** 有效分配版本分布使用与候选一致的治理范围。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "角色分配版本分布")
    @IamReadOnlyRequest
    @GetMapping("/distribution")
    public R<java.util.List<RoleRevisionDistribution>> distribution(@PathVariable String roleId) {
        return ok(applications.distribution(roleId));
    }

    /** 查询持久执行进度。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "版本应用计划详情")
    @GetMapping("/{id}")
    public R<RoleVersionApplicationTask> get(@PathVariable String roleId, @PathVariable String id) {
        return ok(applications.get(roleId, id));
    }

    /** 分页预览分配的操作、范围和字段权限差异。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "版本应用分配明细")
    @GetMapping("/{id}/items")
    public R<PageResponse<RoleVersionApplicationItem>> items(@PathVariable String roleId, @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(applications.items(roleId, id, page, pageSize));
    }

    /** 明确调整逐条或选定集合的范围及排除状态。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "配置版本应用集合")
    @PatchMapping("/{id}/items")
    public R<RoleVersionApplicationTask> configure(@PathVariable String roleId, @PathVariable String id,
            @Valid @RequestBody RoleVersionApplicationChangeInput input) {
        return ok(applications.configure(roleId, id, input));
    }

    /** 未完成项按最新事实预检并等待再次确认。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "重新预检版本应用")
    @PostMapping("/{id}/preflight")
    public R<RoleVersionApplicationTask> refresh(@PathVariable String roleId, @PathVariable String id,
            @Valid @RequestBody RoleVersionApplicationRefreshInput input) {
        return ok(applications.refresh(roleId, id, input));
    }

    /** 全部预检通过才进入异步执行；可幂等重放。 */
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_UPGRADE)
    @Operation(summary = "确认执行版本应用")
    @PostMapping("/{id}/confirm")
    public R<RoleVersionApplicationTask> confirm(@PathVariable String roleId, @PathVariable String id,
            @Valid @RequestBody RoleVersionApplicationConfirmInput input) {
        return ok(applications.confirm(roleId, id, input));
    }
}
