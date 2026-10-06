package com.ingot.cloud.iam.web.v1.platform;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.model.iam.AssignmentBatchInput;
import com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus;
import com.ingot.framework.commons.model.iam.AssignmentPreviewResult;
import com.ingot.framework.commons.model.iam.AssignmentRecord;
import com.ingot.framework.commons.model.iam.AssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.SubjectType;
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
    private final AssignmentService assignments;
    private final com.ingot.cloud.iam.assignment.PlatformAuthorizationEditor editor;

    /**
     * 升级专用角色树。
     * @param assignmentId 分配
     * @param roleId 展开角色
     * @param keyword 搜索
     * @param ids 回显
     * @param page 页码
     * @param pageSize 大小
     * @return 候选
     */
    @GetMapping("/upgrade/role-candidates")
    public R<com.ingot.framework.commons.model.iam.AuthorizationRoleCandidatePage> upgradeRoleCandidates(
            @RequestParam String assignmentId, @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) java.util.List<String> ids,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(editor.upgradeRoleCandidates(assignmentId, roleId, keyword, ids == null ? java.util.List.of() : ids,
                page, pageSize));
    }

    /**
     * 升级专用版本详情及对象。
     * @param assignmentId 分配
     * @param kind 类型
     * @param revisionId 目标版本
     * @param parameterKey 参数
     * @param keyword 搜索
     * @param ids 回显
     * @param page 页码
     * @param pageSize 大小
     * @param tree 树
     * @param parentId 父
     * @return 候选
     */
    @GetMapping("/upgrade/candidates")
    public R<com.ingot.framework.commons.model.iam.AuthorizationCandidatePage> upgradeCandidates(
            @RequestParam String assignmentId,
            @RequestParam com.ingot.framework.commons.model.iam.AuthorizationCandidateKind kind,
            @RequestParam(required = false) String revisionId, @RequestParam(required = false) String parameterKey,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) java.util.List<String> ids,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(defaultValue = "false") boolean tree, @RequestParam(required = false) String parentId) {
        return ok(editor.upgradeCandidates(assignmentId, kind, revisionId, parameterKey, keyword,
                ids == null ? java.util.List.of() : ids, page, pageSize, tree, parentId));
    }

    /**
     * 分页列出平台授权。
     *
     * @param page 页码
     * @param pageSize 页大小
     * @param subjectType 可选接收主体类型
     * @param keyword 可选接收成员或组名称
     * @param effectiveStatus 可选计算生效状态
     * @return 授权页
     */
    @Operation(summary = "授权列表")
    @GetMapping
    public R<PageResponse<ResourceDetail<AssignmentRecord>>> list(
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize,
            @RequestParam(required = false) SubjectType subjectType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AssignmentEffectiveStatus effectiveStatus) {
        return ok(assignments.list(AuthorizationDomain.PLATFORM, page, pageSize, subjectType, keyword, effectiveStatus));
    }

    /**
     * 当前身份的分配配置资格。
     * @return 资格与有效委派数量
     */
    @GetMapping("/context")
    @Operation(summary = "角色分配上下文")
    public R<com.ingot.framework.commons.model.iam.AssignmentContext> context() {
        return ok(assignments.context());
    }

    /**
     * 读取一条可见分配。
     * @param id 分配 ID
     * @return 分配详情与逐条能力
     */
    @GetMapping("/{id}")
    @Operation(summary = "角色分配详情")
    public R<ResourceDetail<AssignmentRecord>> detail(@PathVariable String id) {
        return ok(assignments.detail(AuthorizationDomain.PLATFORM, id));
    }

    /**
     * 分页读取可见分配的固定版本资料与已绑定对象。
     * @param id 分配标识
     * @param kind ROLE_REVISION 或 OBJECT
     * @param parameterKey OBJECT 的固定版本参数键
     * @param page 页码
     * @param pageSize 每页数量
     * @return 真实关联且符合当前边界的候选页
     */
    @GetMapping("/{id}/selected-candidates")
    @Operation(summary = "角色分配已选候选")
    public R<com.ingot.framework.commons.model.iam.AuthorizationCandidatePage> selectedCandidates(
            @PathVariable String id,
            @RequestParam com.ingot.framework.commons.model.iam.AuthorizationCandidateKind kind,
            @RequestParam(required = false) String parameterKey,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = "" + IamPages.DEFAULT_SIZE) int pageSize) {
        return ok(assignments.selectedCandidates(id, kind, parameterKey, page, pageSize));
    }

    /**
     * 预览已有分配的固定版本升级。
     * @param input 目标与草稿
     * @return 差异
     */
    @PostMapping("/upgrade/preview")
    public R<Preview<com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeResult>> previewUpgrade(
            @Valid @RequestBody com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput input) {
        return ok(assignments.previewUpgrade(input));
    }

    /**
     * 原子升级既有记录，保留主体、来源与期限。
     * @param input 目标与版本
     * @return 升级结果
     */
    @PostMapping("/upgrade")
    public R<com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeResult> upgrade(
            @Valid @RequestBody com.ingot.framework.commons.model.iam.extension.AssignmentUpgradeInput input) {
        return ok(assignments.upgrade(input));
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
     * 预览既有分配草稿。
     * @param id 分配 ID
     * @param input 草稿与版本
     * @return 预览
     */
    @PostMapping("/{id}/preview")
    @Operation(summary = "预览分配调整")
    public R<Preview<AssignmentPreviewResult>> previewUpdate(@PathVariable String id,
            @Valid @RequestBody AssignmentUpdateInput input) {
        return ok(assignments.previewUpdate(AuthorizationDomain.PLATFORM, id, input));
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
