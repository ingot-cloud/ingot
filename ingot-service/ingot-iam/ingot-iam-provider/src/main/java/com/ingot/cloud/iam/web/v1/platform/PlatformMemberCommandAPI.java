package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.commons.model.iam.IamAction;

import java.util.List;

import com.ingot.cloud.iam.organization.MemberCommandService;
import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.organization.MemberQueryService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AssignmentRecord;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.GroupRecord;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
import com.ingot.framework.commons.model.iam.PlatformMemberEditInput;
import com.ingot.framework.commons.model.iam.PlatformMemberEditPreview;
import com.ingot.framework.commons.model.iam.MemberBoundRole;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberRoleReplaceInput;
import com.ingot.framework.commons.model.iam.MemberRoleView;
import com.ingot.framework.commons.model.iam.MemberStatusInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.PlatformMemberContext;
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
 * <p>提供平台成员列表、创建、资料更新、暂停与移出，不修改全局账号或其他域身份。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台成员命令")
@RequestMapping("/v1/platform/members")
@RequiredArgsConstructor
public class PlatformMemberCommandAPI implements RShortcuts {
    private final MemberCommandService members;
    private final MemberQueryService queries;
    private final AssignmentService assignments;

    /**
     * 读取成员列可见性和创建字段上下文，不授予额外查看或写入权限。
     * @return 当前身份的展示上下文
     */
    @Operation(summary = "平台成员字段上下文")
    @GetMapping("/context")
    @AdminOrHasAnyAuthority({IamAction.VALUE_PLATFORM_MEMBER_READ, IamAction.VALUE_PLATFORM_MEMBER_CREATE})
    public R<PlatformMemberContext> context() {
        return ok(queries.platformContext());
    }

    /**
     * 分页列出平台成员。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 显示名包含匹配，可空
     * @param status 成员资格，可空；仅接受 ACTIVE、SUSPENDED、REMOVED
     * @param ids 逗号分隔成员 ID，可空；用于已选回显
     * @return 成员页
     */
    @Operation(summary = "成员列表")
    @GetMapping
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_READ)
    public R<PageResponse<ResourceDetail<MemberRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ids) {
        return ok(queries.listPlatform(page, pageSize, name, status, ids));
    }

    /**
     * 把已有账号关联为平台成员。
     *
     * @param input 账号与空任职
     * @return 新成员 ID
     */
    @Operation(summary = "创建成员资格")
    @PostMapping
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_CREATE)
    public R<CreatedResource> create(@Valid @RequestBody MemberCreateInput input) {
        return ok(queries.create(AuthorizationDomain.PLATFORM, input));
    }

    /**
     * 读取平台成员详情。
     *
     * @param id 平台成员 ID
     * @return 安全投影
     */
    @Operation(summary = "成员详情")
    @GetMapping("/{id}")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_READ)
    public R<ResourceDetail<MemberRecord>> get(@PathVariable String id) {
        return ok(queries.get(AuthorizationDomain.PLATFORM, id));
    }

    /**
     * 分页列出平台成员所在用户组。
     *
     * @param id 平台成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 组页
     */
    @Operation(summary = "成员所在用户组")
    @GetMapping("/{id}/groups")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_READ)
    public R<PageResponse<ResourceDetail<GroupRecord>>> groups(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(queries.listPlatformGroups(id, page, pageSize));
    }

    /**
     * 列出平台成员的简单直接角色。
     *
     * @param id 平台成员 ID
     * @return 角色 ID 与名称
     */
    @Operation(summary = "成员直接角色")
    @GetMapping("/{id}/roles")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_READ)
    public R<List<MemberRoleView>> roles(@PathVariable String id) {
        return ok(queries.listDirectRoles(id));
    }

    /**
     * 按成员关联分页读取实际角色分配及逐条操作能力。
     * @param id 平台成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @param effectiveStatus 计算后的生效状态，可空
     * @param directOnly 排除来源委派的直接分配
     * @return 当前身份可见的分配页
     */
    @Operation(summary = "成员角色分配")
    @GetMapping("/{id}/assignments")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_ASSIGNMENT_READ)
    public R<PageResponse<ResourceDetail<AssignmentRecord>>> assignments(@PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) AssignmentEffectiveStatus effectiveStatus,
            @RequestParam(defaultValue = "false") boolean directOnly) {
        return ok(assignments.listForMember(id, page, pageSize, effectiveStatus, directOnly));
    }

    /**
     * 分页读取成员当前有效角色摘要，不包含历史记录和组名称。
     * @param id 成员
     * @param page 页码
     * @param pageSize 页大小
     * @return 固定角色版本摘要
     */
    @Operation(summary = "成员有效绑定角色")
    @GetMapping("/{id}/bound-roles")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_READ)
    public R<PageResponse<MemberBoundRole>> boundRoles(@PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(assignments.boundRoles(id, page, pageSize));
    }

    /**
     * 预览成员资料和角色差量，不持久化。
     * @param id 成员
     * @param input 编辑草稿
     * @return 差量摘要和校验
     */
    @Operation(summary = "成员编辑预览")
    @PostMapping("/{id}/preview")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_UPDATE)
    public R<Preview<PlatformMemberEditPreview>> preview(@PathVariable String id,
            @Valid @RequestBody PlatformMemberEditInput input) {
        return ok(queries.previewPlatformEdit(id, input));
    }

    /**
     * 替换平台成员的简单直接角色。
     *
     * @param id 平台成员 ID
     * @param input 目标角色 ID
     * @return 替换后的直接角色
     */
    @Operation(summary = "替换成员直接角色")
    @PutMapping("/{id}/roles")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_UPDATE)
    public R<List<MemberRoleView>> replaceRoles(@PathVariable String id,
                                                @Valid @RequestBody MemberRoleReplaceInput input) {
        return ok(queries.replaceDirectRoles(id, input));
    }

    /**
     * 原子更新平台成员资料与角色差量。
     *
     * @param id 平台成员 ID
     * @param input 资料与可选角色差量
     * @return 更新后投影
     */
    @Operation(summary = "更新成员资料与角色分配")
    @PatchMapping("/{id}")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_UPDATE)
    public R<ResourceDetail<MemberRecord>> patch(@PathVariable String id, @Valid @RequestBody PlatformMemberEditInput input) {
        return ok(queries.patchPlatform(id, input));
    }

    /**
     * 暂停或恢复平台成员资格。
     *
     * @param id 平台成员 ID
     * @param input 状态与预期版本
     * @return 提交后的版本
     */
    @Operation(summary = "暂停或恢复成员")
    @PatchMapping("/{id}/status")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_STATUS)
    public R<CreatedResource> changeStatus(@PathVariable String id, @Valid @RequestBody MemberStatusInput input) {
        return ok(members.changeStatus(AuthorizationDomain.PLATFORM, id, input));
    }

    /**
     * 移出平台成员。
     *
     * @param id 平台成员 ID
     * @param input 预期版本
     * @return 提交后的版本
     */
    @Operation(summary = "移出当前域")
    @PostMapping("/{id}/remove")
    @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_REMOVE)
    public R<CreatedResource> remove(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(members.remove(AuthorizationDomain.PLATFORM, id, input));
    }
}
