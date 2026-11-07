package com.ingot.cloud.iam.application.assignment;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.application.authorization.ActionCapabilitySnapshot;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.application.assignment.model.AssignmentRecord;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.organization.model.MemberRoleView;
import com.ingot.cloud.iam.application.organization.model.MemberBoundRole;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>执行对应平台分配用例，逐项保留当前身份、来源和范围校验。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformAssignmentQueryService {
    private final PlatformAssignmentOperations operationsContext;

    /**
     * 分页列出当前域授权。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 授权页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        return list(domain, page, pageSize, null, null);
    }

    /**
     * 在平台分配可见边界内按接收主体和名称分页；租户列表保持原契约。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param subjectType 平台接收主体类型，可空
     * @param keyword 平台成员显示名或组名称，可空
     * @return 筛选后的分配页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            SubjectType subjectType, String keyword) {
        return list(domain, page, pageSize, subjectType, keyword, null, null);
    }

    /**
     * 在既有平台分配可见边界内按计算状态组合筛选并分页。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param subjectType 接收主体类型，可空
     * @param keyword 成员或组名称，可空
     * @param effectiveStatus 计算状态，可空；租户域禁止传入
     * @return 筛选后的分配页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            SubjectType subjectType, String keyword,
            com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus effectiveStatus) {
        return list(domain, page, pageSize, subjectType, keyword, null, effectiveStatus);
    }

    /**
     * 列出一个可见平台成员的角色分配，沿用分配治理与来源边界。
     * @param memberId 平台成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 关联分配页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> listForMember(String memberId, int page, int pageSize) {
        long id = IamIds.require(memberId);
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_READ);
        operationsContext.resourceAccess().requireVisibleMember(actor.context(), IamAction.PLATFORM_MEMBER_READ, id);
        return list(AuthorizationDomain.PLATFORM, page, pageSize, SubjectType.MEMBER, null, id, null);
    }

    /**
     * 按成员关联筛选当前生效的非委派直接记录，供差量编辑使用。
     * @param memberId 成员
     * @param page 页码
     * @param pageSize 页大小
     * @param effectiveStatus 可选计算状态
     * @param directOnly 排除委派直接分配
     * @return 分配页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> listForMember(String memberId, int page, int pageSize,
            com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus effectiveStatus, boolean directOnly) {
        long id = IamIds.require(memberId);
        var actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_READ);
        operationsContext.resourceAccess().requireVisibleMember(actor.context(), IamAction.PLATFORM_MEMBER_READ, id);
        if (!directOnly) return list(AuthorizationDomain.PLATFORM, page, pageSize, SubjectType.MEMBER, null, id, effectiveStatus);
        var admission = operationsContext.access().admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ);
        IamPages.require(page, pageSize);
        if (!admission.governed()) return IamPages.details(List.of(), 0, page, pageSize);
        var rows = operationsContext.assignments().pagePlatform(page, pageSize, SubjectType.MEMBER, null, id, effectiveStatus, true);
        return IamPages.details(presentPlatformRows(actor, rows.getRecords()), rows.getTotal(), page, pageSize);
    }

    /**
     * 成员只读视图按有效关系聚合固定角色版本，不披露组名称或标识。
     * @param memberId 成员
     * @param page 页码
     * @param pageSize 页大小
     * @return 当前有效角色摘要
     */
    public PageResponse<MemberBoundRole> boundRoles(String memberId,
            int page, int pageSize) {
        var actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MEMBER_READ);
        long id = IamIds.require(memberId);
        operationsContext.resourceAccess().requireVisibleMember(actor.context(), IamAction.PLATFORM_MEMBER_READ, id);
        IamPages.require(page, pageSize);
        var rows = operationsContext.assignments().memberBoundRoles(id, page, pageSize);
        var items = rows.getRecords().stream().map(row -> {
            List<SubjectType> sources = new ArrayList<>();
            if (row.getDirectCount() > 0) sources.add(SubjectType.MEMBER);
            if (row.getGroupCount() > 0) sources.add(SubjectType.GROUP);
            return new MemberBoundRole(row.getRoleId().toString(), row.getName(),
                    new RoleRevisionRef(row.getKind(), row.getRevisionId().toString()), row.getRevisionNumber().toString(), sources, row.getVersionName());
        }).toList();
        return new PageResponse<>(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 批量装配已通过角色关联 SQL 边界的分配行，复用历史账本的标签与逐条能力。
     * @param actor 当前可信身份
     * @param rows 仅限服务端完成可见性筛选的关联行
     * @return 可披露分配详情
     */
    public List<ResourceDetail<AssignmentRecord>> presentPlatformRows(ActiveIdentity actor, List<IamRoleAssignmentEntity> rows) {
        var presentation = operationsContext.assignments().presentation(rows.stream().map(IamRoleAssignmentEntity::getId).toList());
        ActionCapabilitySnapshot permissions = rows.isEmpty() ? new ActionCapabilitySnapshot(Map.of()) : operationsContext.recordCapabilities(AuthorizationDomain.PLATFORM, actor);
        var sources = operationsContext.recordSources(AuthorizationDomain.PLATFORM, actor, rows, permissions);
        return rows.stream().map(row -> operationsContext.detail(AuthorizationDomain.PLATFORM, actor, row,
                presentation.get(row.getId()), permissions, sources)).toList();
    }

    /**
     * 读取已通过治理对象边界的分配快照，供持久计划保存原主体、期限与来源。
     * @param admission 实时治理准入
     * @param row 当前分配行
     * @return 完整分配
     */
    public AssignmentRecord applicationSnapshot(AuthorizationAdmission admission, IamRoleAssignmentEntity row) {
        if (!admission.governed()) throw new BizException(IamReasonCode.ACTION_DENIED);
        operationsContext.requireRecord(AuthorizationDomain.PLATFORM, admission, row);
        operationsContext.resourceAccess().requireVisibleObject(admission.actor().context(), IamAction.PLATFORM_ASSIGNMENT_UPGRADE,
                row.getId().longValueExact());
        var basic = operationsContext.record(row);
        var presentation = operationsContext.assignments().presentation(List.of(row.getId())).get(row.getId());
        return presentation == null ? basic : new AssignmentRecord(basic.id(), basic.assignment(), basic.status(),
                basic.source(), presentation.subjectName(), presentation.roleName(), PlatformAssignmentOperations.text(presentation.revisionNumber()),
                null, PlatformAssignmentOperations.instant(row.getCreatedAt()), null, null, presentation.versionName());
    }

    /**
     * 读取平台成员的简单直接角色，不含用户组继承或带范围、有效期、委派来源的授权。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @return 按名称排序的直接角色
     */
    public List<MemberRoleView> listDirectRoles(AuthorizationDomain domain, String memberId) {
        operationsContext.access().require(domain, IamAction.PLATFORM_MEMBER_READ);
        PlatformAssignmentOperations.requirePlatform(domain);
        long id = IamIds.require(memberId);
        if (!operationsContext.assignments().platformMemberExists(id)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return operationsContext.collectDirectRoles(id);
    }

    /**
     * 返回平台分配配置资格；任何直接资格都必须来自对应精确操作的非委派来源。
     * @return 当前配置资格
     */
    public com.ingot.cloud.iam.application.assignment.model.AssignmentContext context() {
        ActiveIdentity actor = operationsContext.access().requireCurrent();
        if (actor.context().domain() != AuthorizationDomain.PLATFORM) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        var actions = List.of(
                IamAction.PLATFORM_ASSIGNMENT_READ, IamAction.PLATFORM_ASSIGNMENT_CREATE,
                IamAction.PLATFORM_ASSIGNMENT_UPDATE, IamAction.PLATFORM_ASSIGNMENT_DELETE);
        ActionCapabilitySnapshot permissions = operationsContext.access().capabilities(actor, actions);
        if (actions.stream().noneMatch(action -> permissions.allows(action, false))) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        return new com.ingot.cloud.iam.application.assignment.model.AssignmentContext(
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_READ, true),
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_CREATE, true),
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_UPDATE, true),
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_DELETE, true),
                operationsContext.assignments().effectiveDelegations(IamIds.require(actor.context().memberId())).size());
    }

    /**
     * 读取分配详情，沿用列表的可信来源边界。
     * @param domain 管理域
     * @param id 分配 ID
     * @return 可披露详情
     */
    public ResourceDetail<AssignmentRecord> detail(AuthorizationDomain domain, String id) {
        AuthorizationAdmission admission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.READ));
        IamRoleAssignmentEntity row = operationsContext.assignments().find(domain, PlatformAssignmentOperations.tenantId(domain, admission.actor()), IamIds.require(id));
        operationsContext.requireRecord(domain, admission, row);
        return operationsContext.detail(domain, admission.actor(), row);
    }

    /**
     * 读取平台分配真实绑定的固定版本或范围对象，沿用详情的来源归属边界。
     * @param id 分配 ID
     * @param kind 固定版本或范围对象
     * @param parameterKey 范围对象对应参数
     * @param page 页码
     * @param pageSize 每页数量
     * @return 受限关联页
     */
    public com.ingot.framework.commons.model.iam.AuthorizationCandidatePage selectedCandidates(String id,
            com.ingot.cloud.iam.domain.shared.AuthorizationCandidateKind kind, String parameterKey,
            int page, int pageSize) {
        AuthorizationAdmission admission = operationsContext.access().admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ);
        IamRoleAssignmentEntity row = operationsContext.assignments().find(AuthorizationDomain.PLATFORM, null, IamIds.require(id));
        operationsContext.requireRecord(AuthorizationDomain.PLATFORM, admission, row);
        return operationsContext.editor().selectedAssignmentCandidates(admission.actor(), id, operationsContext.record(row).assignment(), kind,
                parameterKey, page, pageSize);
    }

    private PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            SubjectType subjectType, String keyword, Long memberId,
            com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus effectiveStatus) {
        AuthorizationAdmission admission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.READ));
        ActiveIdentity actor = admission.actor();
        IamPages.require(page, pageSize);
        String search = keyword == null ? null : keyword.strip();
        if (search != null && search.length() > PlatformAssignmentOperations.MAX_SUBJECT_KEYWORD_LENGTH) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (search != null && search.isEmpty()) {
            search = null;
        }
        if (domain != AuthorizationDomain.PLATFORM && (subjectType != null || search != null || effectiveStatus != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Page<IamRoleAssignmentEntity> rows;
        if (domain != AuthorizationDomain.PLATFORM) {
            rows = operationsContext.assignments().page(domain, PlatformAssignmentOperations.tenantId(domain, actor), page, pageSize);
        } else if (admission.governed()) {
            rows = effectiveStatus == null
                    ? operationsContext.assignments().pagePlatform(page, pageSize, subjectType, search, memberId)
                    : operationsContext.assignments().pagePlatform(page, pageSize, subjectType, search, memberId, effectiveStatus);
        } else {
            long administratorId = IamIds.require(actor.context().memberId());
            rows = effectiveStatus == null
                    ? operationsContext.assignments().pageOwned(administratorId, page, pageSize, subjectType, search, memberId)
                    : operationsContext.assignments().pageOwned(administratorId, page, pageSize, subjectType, search, memberId, effectiveStatus);
        }
        var presentation = domain == AuthorizationDomain.PLATFORM ? operationsContext.assignments().presentation(rows.getRecords().stream()
                .map(IamRoleAssignmentEntity::getId).toList()) : Map.<BigInteger,
                com.ingot.cloud.iam.infrastructure.persistence.projection.AssignmentPresentation>of();
        ActionCapabilitySnapshot permissions = rows.getRecords().isEmpty() ? new ActionCapabilitySnapshot(Map.of())
                : operationsContext.recordCapabilities(domain, actor);
        var sources = operationsContext.recordSources(domain, actor, rows.getRecords(), permissions);
        List<ResourceDetail<AssignmentRecord>> items = rows.getRecords().stream()
                .map(row -> operationsContext.detail(domain, actor, row, presentation.get(row.getId()), permissions, sources)).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }
}
