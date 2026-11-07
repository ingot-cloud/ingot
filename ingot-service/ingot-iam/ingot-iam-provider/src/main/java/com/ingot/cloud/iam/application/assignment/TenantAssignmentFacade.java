package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.infrastructure.persistence.MemberPickerBranch;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.assignment.model.AssignmentPreviewResult;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentOption;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentRecord;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentUpdateInput;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.CurrentRoleGrantList;
import com.ingot.cloud.iam.application.role.model.RoleSubjectPage;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.assignment.TenantAssignmentOptionKind;
import java.time.*;
import java.util.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>聚合assignment用例的内部调用入口，规则和事务分别由职责明确的应用服务执行。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantAssignmentFacade {
    private final TenantAssignmentCommandService tenantAssignmentCommandService;
    private final TenantAssignmentQueryService tenantAssignmentQueryService;

    /** 分页读取允许披露的当前分配。 */
    public PageResponse<ResourceDetail<TenantAssignmentRecord>> list(int page,int size,SubjectType type,String keyword) {
        return tenantAssignmentQueryService.list(page, size, type, keyword);
    }

    /**
     * 查询角色当前有效成员或用户组，组继承在 SQL 中展开并去重。
     *
     * @param roleId 可见角色 ID
     * @param type 成员或用户组
     * @param keyword 主体名称
     * @param page 页码
     * @param size 页大小
     * @return 可见主体；组读取受限时标记继承来源受限
     */
    public RoleSubjectPage subjects(String roleId, SubjectType type, String keyword, int page, int size) {
        return tenantAssignmentQueryService.subjects(roleId, type, keyword, page, size);
    }

    /**
     * 查询一名成员持有该角色的直接分配和组继承来源。
     *
     * @param roleId 可见角色 ID
     * @param memberId 成员 ID
     * @param page 页码
     * @param size 页大小
     * @return 仍须按分配 ID 修改或撤销的来源页
     */
    public PageResponse<ResourceDetail<TenantAssignmentRecord>> sources(String roleId, String memberId, int page, int size) {
        return tenantAssignmentQueryService.sources(roleId, memberId, page, size);
    }

    /** 原子提交批次，受限管理员只能使用同一条本人有效委派。 */
    public CreatedResource create(TenantAssignmentBatchInput batch) {
        return tenantAssignmentCommandService.create(batch);
    }

    /** 预览使用与提交相同的当前角色、范围和来源校验。 */
    public Preview<AssignmentPreviewResult> preview(TenantAssignmentBatchInput batch) {
        return tenantAssignmentQueryService.preview(batch);
    }

    /** 调整完整当前分配，来源不能由受限管理员替换。 */
    public ResourceDetail<TenantAssignmentRecord> replace(String id,TenantAssignmentUpdateInput input) {
        return tenantAssignmentCommandService.replace(id, input);
    }

    /** 撤销分配并保留历史。 */
    public CreatedResource delete(String id) {
        return tenantAssignmentCommandService.delete(id);
    }

    /** 查询当前可选角色、成员、组和资源，已选ID使用同一来源过滤。 */
    public PageResponse<TenantAssignmentOption> options(TenantAssignmentOptionKind kind,String source,String keyword,List<String> ids,int page,int size) {
        return tenantAssignmentQueryService.options(kind, source, keyword, ids, page, size);
    }

    /** 组织成员浏览以部门条件与原直接/委派候选求交，不补充任何候选资格。 */
    public PageResponse<TenantAssignmentOption> options(TenantAssignmentOptionKind kind,String source,String keyword,List<String> ids,int page,int size,String departmentId) {
        return tenantAssignmentQueryService.options(kind, source, keyword, ids, page, size, departmentId);
    }

    /** 组织人员浏览沿用直接/本人委派来源的原有候选资格。 */
    public PageResponse<TenantAssignmentOption> pickerOptions(String source, String keyword, int page, int size, com.ingot.cloud.iam.infrastructure.persistence.MemberPickerBranch branch) {
        return tenantAssignmentQueryService.pickerOptions(source, keyword, page, size, branch);
    }

    /** 受限分配专用角色详情，不补通用角色读取资格。 */
    public CurrentRoleGrantList grants(String id,String source) {
        return tenantAssignmentQueryService.grants(id, source);
    }

    /** 当前角色参数候选；未知业务资源返回明确不支持。 */
    public AuthorizationCandidatePage scopeCandidates(String roleId,String key,String source,String keyword,List<String> selected,int page,int size,boolean tree,String parent) {
        return tenantAssignmentQueryService.scopeCandidates(roleId, key, source, keyword, selected, page, size, tree, parent);
    }
}
