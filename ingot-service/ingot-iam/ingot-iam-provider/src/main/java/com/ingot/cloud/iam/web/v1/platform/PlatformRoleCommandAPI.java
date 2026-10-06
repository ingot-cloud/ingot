package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatusInput;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.EffectiveRole;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleCreateInput;
import com.ingot.framework.commons.model.iam.RoleDefinitionDraft;
import com.ingot.framework.commons.model.iam.RoleGrantList;
import com.ingot.framework.commons.model.iam.RolePublishInput;
import com.ingot.framework.commons.model.iam.RoleRevision;
import com.ingot.framework.commons.model.iam.RoleSummary;
import com.ingot.framework.commons.model.iam.RoleUpdateInput;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.model.support.RShortcuts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>提供平台自定义与系统角色目录，发布不自动升级既有授权。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestController
@Tag(name = "IAM 平台角色")
@RequestMapping("/v1/platform/roles")
@RequiredArgsConstructor
public class PlatformRoleCommandAPI implements RShortcuts {
    private final RoleService roles;
    private final com.ingot.cloud.iam.role.PlatformRoleWorkspace workspace;

    /**
     * 分页列出平台角色。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param name 角色名称包含匹配，可空
     * @param status 启停状态，可空；仅接受 ENABLED/DISABLED
     * @return 角色页
     */
    @Operation(summary = "角色目录")
    @GetMapping
    public R<PageResponse<ResourceDetail<RoleSummary>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        return ok(roles.list(AuthorizationDomain.PLATFORM, false, page, pageSize, name, status));
    }

    /**
     * 查询该角色所有或指定固定版本的有效成员，包含可见组继承。
     * @param id 可见角色 ID
     * @param revisionId 可选固定版本
     * @param keyword 成员名称
     * @param page 页码
     * @param pageSize 页大小
     * @return 去重有效成员页
     */
    @GetMapping("/{id}/members")
    @Operation(summary="角色有效成员")
    public R<com.ingot.framework.commons.model.iam.RoleSubjectPage> members(@PathVariable String id,
            @RequestParam(required=false) String revisionId, @RequestParam(required=false) String keyword,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        return ok(workspace.subjects(id, revisionId, com.ingot.framework.commons.model.iam.SubjectType.MEMBER, keyword, page, pageSize));
    }
    /**
     * 查询该角色当前有效分配的可见用户组。
     * @param id 可见角色 ID
     * @param revisionId 可选固定版本
     * @param keyword 用户组名称
     * @param page 页码
     * @param pageSize 页大小
     * @return 有效用户组页
     */
    @GetMapping("/{id}/groups")
    @Operation(summary="角色有效用户组")
    public R<com.ingot.framework.commons.model.iam.RoleSubjectPage> groups(@PathVariable String id,
            @RequestParam(required=false) String revisionId, @RequestParam(required=false) String keyword,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        return ok(workspace.subjects(id, revisionId, com.ingot.framework.commons.model.iam.SubjectType.GROUP, keyword, page, pageSize));
    }
    /**
     * 分页查看成员的直接或组继承来源，保持分配对象能力。
     * @param id 可见角色 ID
     * @param memberId 成员 ID
     * @param revisionId 可选固定版本
     * @param page 页码
     * @param pageSize 页大小
     * @return 当前有效来源页
     */
    @GetMapping("/{id}/members/{memberId}/assignments")
    @Operation(summary="角色成员有效分配来源")
    public R<PageResponse<ResourceDetail<com.ingot.framework.commons.model.iam.AssignmentRecord>>> sources(
            @PathVariable String id, @PathVariable String memberId, @RequestParam(required=false) String revisionId,
            @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize) {
        return ok(workspace.sources(id, memberId, revisionId, page, pageSize));
    }

    /**
     * 创建平台自定义角色并发布首个版本。
     *
     * @param input 创建命令
     * @return 新角色 ID
     */
    @Operation(summary = "创建角色并发布首个版本")
    @PostMapping
    public R<CreatedResource> create(@Valid @RequestBody RoleCreateInput input) {
        return ok(roles.create(AuthorizationDomain.PLATFORM, false, input));
    }

    /**
     * 只读预览新平台角色，不创建角色或版本。
     * @param input 创建草稿
     * @return 定义校验及字段默认值
     */
    @Operation(summary = "预览平台角色创建")
    @PostMapping("/preview")
    public R<Preview<RoleDefinitionDraft>> previewCreate(@Valid @RequestBody RoleCreateInput input) {
        return ok(roles.previewCreate(input));
    }

    /**
     * 读取角色元数据。
     *
     * @param id 角色 ID
     * @return 角色详情
     */
    @Operation(summary = "角色元数据")
    @GetMapping("/{id}")
    public R<ResourceDetail<RoleSummary>> get(@PathVariable String id) {
        return ok(roles.get(AuthorizationDomain.PLATFORM, false, id));
    }

    /**
     * 读取平台角色最新已发布版本的当前绑定权限。
     *
     * @param id 角色 ID
     * @return 当前绑定权限
     */
    @Operation(summary = "角色当前绑定权限")
    @GetMapping("/{id}/grants")
    public R<RoleGrantList> grants(@PathVariable String id) {
        return ok(new RoleGrantList(roles.listCurrentGrants(AuthorizationDomain.PLATFORM, false, id)));
    }

    /**
     * 更新平台角色名称、说明、分组与启停；未传名称时只改启停。
     *
     * @param id 角色 ID
     * @param input 基本信息或仅启停
     * @return 提交后版本
     */
    @Operation(summary = "更新角色基本信息或启停")
    @PatchMapping("/{id}")
    public R<CreatedResource> patch(@PathVariable String id, @Valid @RequestBody RoleUpdateInput input) {
        if (input.name() == null || input.name().isBlank()) {
            return ok(roles.changeStatus(
                    AuthorizationDomain.PLATFORM,
                    false,
                    id,
                    new ConfigurationStatusInput(input.status(), input.expectedVersion())));
        }
        return ok(roles.updateProfile(AuthorizationDomain.PLATFORM, false, id, input));
    }

    /**
     * 删除未被授权引用的非系统角色。
     *
     * @param id 角色 ID
     * @return 删除前版本
     */
    @Operation(summary = "删除未引用角色")
    @DeleteMapping("/{id}")
    public R<CreatedResource> delete(@PathVariable String id) {
        return ok(roles.delete(AuthorizationDomain.PLATFORM, false, id));
    }

    /**
     * 列出角色不可变版本。
     *
     * @param id 角色 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 版本页
     */
    @Operation(summary = "角色版本")
    @GetMapping("/{id}/revisions")
    public R<PageResponse<ResourceDetail<RoleRevision>>> revisions(
            @PathVariable String id,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(roles.listRevisions(AuthorizationDomain.PLATFORM, false, id, page, pageSize));
    }

    /**
     * 发布新版本，不改写既有授权引用。
     *
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 新版本 ID
     */
    @Operation(summary = "发布新版本")
    @PostMapping("/{id}/revisions")
    public R<CreatedResource> publish(@PathVariable String id, @Valid @RequestBody RolePublishInput input) {
        return ok(roles.publish(AuthorizationDomain.PLATFORM, false, id, input));
    }

    /**
     * 预览待发布定义，无写入。
     *
     * @param id 角色 ID
     * @param input 待发布定义
     * @return 合成预览
     */
    @Operation(summary = "预览待发布定义")
    @PostMapping("/{id}/preview")
    public R<Preview<EffectiveRole>> preview(@PathVariable String id, @Valid @RequestBody RoleDefinitionDraft input) {
        return ok(roles.preview(AuthorizationDomain.PLATFORM, false, id, input));
    }
}
