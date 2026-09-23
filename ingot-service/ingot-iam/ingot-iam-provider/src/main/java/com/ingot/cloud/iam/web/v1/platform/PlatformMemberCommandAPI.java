package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.organization.MemberCommandService;
import com.ingot.cloud.iam.organization.MemberQueryService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.GroupRecord;
import com.ingot.framework.commons.model.iam.MemberCreateInput;
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

    /**
     * 分页列出平台成员。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 显示名包含匹配，可空
     * @param status 成员资格，可空；仅接受 ACTIVE、SUSPENDED、REMOVED
     * @return 成员页
     */
    @Operation(summary = "成员列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<MemberRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return ok(queries.listPlatform(page, pageSize, name, status));
    }

    /**
     * 把已有账号关联为平台成员。
     *
     * @param input 账号与空任职
     * @return 新成员 ID
     */
    @Operation(summary = "创建成员资格")
    @PostMapping
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
    public R<PageResponse<ResourceDetail<GroupRecord>>> groups(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(queries.listPlatformGroups(id, page, pageSize));
    }

    /**
     * 更新平台成员显示资料。
     *
     * @param id 平台成员 ID
     * @param input 资料
     * @return 更新后投影
     */
    @Operation(summary = "更新成员资料")
    @PatchMapping("/{id}")
    public R<ResourceDetail<MemberRecord>> patch(@PathVariable String id, @Valid @RequestBody MemberProfileInput input) {
        return ok(queries.patch(AuthorizationDomain.PLATFORM, id, input));
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
    public R<CreatedResource> remove(@PathVariable String id, @Valid @RequestBody VersionInput input) {
        return ok(members.remove(AuthorizationDomain.PLATFORM, id, input));
    }
}
