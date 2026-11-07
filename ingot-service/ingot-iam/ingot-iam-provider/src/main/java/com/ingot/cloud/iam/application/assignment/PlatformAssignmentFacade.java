package com.ingot.cloud.iam.application.assignment;

import java.util.List;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.application.assignment.model.AssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentPreviewResult;
import com.ingot.cloud.iam.application.assignment.model.AssignmentRecord;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.application.organization.model.MemberRoleView;
import com.ingot.cloud.iam.application.organization.model.MemberRoleChanges;
import com.ingot.cloud.iam.application.organization.model.MemberBoundRole;
import com.ingot.cloud.iam.domain.organization.MemberRoleAssignmentDraft;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeResult;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>聚合assignment用例的内部调用入口，规则和事务分别由职责明确的应用服务执行。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformAssignmentFacade {
    private final PlatformAssignmentCommandService platformAssignmentCommandService;
    private final PlatformAssignmentPreviewService platformAssignmentPreviewService;
    private final PlatformAssignmentQueryService platformAssignmentQueryService;
    private final PlatformAssignmentVersionService platformAssignmentVersionService;

    /**
     * 分页列出当前域授权。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 授权页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        return platformAssignmentQueryService.list(domain, page, pageSize);
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
        return platformAssignmentQueryService.list(domain, page, pageSize, subjectType, keyword);
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
        return platformAssignmentQueryService.list(domain, page, pageSize, subjectType, keyword, effectiveStatus);
    }

    /**
     * 列出一个可见平台成员的角色分配，沿用分配治理与来源边界。
     * @param memberId 平台成员 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 关联分配页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> listForMember(String memberId, int page, int pageSize) {
        return platformAssignmentQueryService.listForMember(memberId, page, pageSize);
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
        return platformAssignmentQueryService.listForMember(memberId, page, pageSize, effectiveStatus, directOnly);
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
        return platformAssignmentQueryService.boundRoles(memberId, page, pageSize);
    }

    /**
     * 批量装配已通过角色关联 SQL 边界的分配行，复用历史账本的标签与逐条能力。
     * @param actor 当前可信身份
     * @param rows 仅限服务端完成可见性筛选的关联行
     * @return 可披露分配详情
     */
    public List<ResourceDetail<AssignmentRecord>> presentPlatformRows(ActiveIdentity actor, List<IamRoleAssignmentEntity> rows) {
        return platformAssignmentQueryService.presentPlatformRows(actor, rows);
    }

    /**
     * 原子批量创建授权，任一条失败整批回滚。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 首条授权 ID
     */
    public CreatedResource create(AuthorizationDomain domain, AssignmentBatchInput input) {
        return platformAssignmentCommandService.create(domain, input);
    }

    /**
     * 预览批量分配，无写入。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 逐条效果
     */
    public Preview<AssignmentPreviewResult> preview(AuthorizationDomain domain, AssignmentBatchInput input) {
        return platformAssignmentPreviewService.preview(domain, input);
    }

    /**
     * 预览一条既有分配，读、编辑与撤销遵循相同来源边界。
     * @param domain 管理域
     * @param id 分配 ID
     * @param input 草稿与版本
     * @return 无写入的校验效果
     */
    public Preview<AssignmentPreviewResult> previewUpdate(AuthorizationDomain domain, String id, AssignmentUpdateInput input) {
        return platformAssignmentPreviewService.previewUpdate(domain, id, input);
    }

    /**
     * 调整既有授权，禁止改写主体或伪造委派来源。
     *
     * @param domain 接口管理域
     * @param id 授权 ID
     * @param input 待保存定义
     * @return 更新后详情
     */
    public ResourceDetail<AssignmentRecord> replace(AuthorizationDomain domain, String id, AssignmentUpdateInput input) {
        return platformAssignmentCommandService.replace(domain, id, input);
    }

    /**
     * 预览同角色分配的版本升级，主体、期限与委派来源全部保留。
     * @param input 目标固定版本与记录
     * @return 逐条差异及参数问题
     */
    public Preview<AssignmentUpgradeResult> previewUpgrade(AssignmentUpgradeInput input) {
        return platformAssignmentVersionService.previewUpgrade(input);
    }

    /**
     * 原子升级同角色分配；事务内重验权限、版本和全部范围，不撤销重建记录。
     * @param input 目标与逐条草稿
     * @return 保存结果
     */
    public AssignmentUpgradeResult upgrade(AssignmentUpgradeInput input) {
        return platformAssignmentVersionService.upgrade(input);
    }

    /**
     * 以已恢复的可信身份预检双向应用；必须具备独立治理资格。
     * @param admission 实时身份和治理准入
     * @param input 目标固定版本和最多一百条记录
     * @return 完整差异及范围问题
     */
    public AssignmentUpgradeResult previewApplication(AuthorizationAdmission admission, AssignmentUpgradeInput input) {
        return platformAssignmentVersionService.previewApplication(admission, input);
    }

    /**
     * 在调用方事务内应用一个批次；调用方先锁授权、角色并恢复身份。
     * @param admission 当批次的实时身份
     * @param input 固定版本及预期分配版本
     * @return 当批次的完整差异
     */
    public AssignmentUpgradeResult applyApplication(AuthorizationAdmission admission, AssignmentUpgradeInput input) {
        return platformAssignmentVersionService.applyApplication(admission, input);
    }

    /**
     * 读取已通过治理对象边界的分配快照，供持久计划保存原主体、期限与来源。
     * @param admission 实时治理准入
     * @param row 当前分配行
     * @return 完整分配
     */
    public AssignmentRecord applicationSnapshot(AuthorizationAdmission admission, IamRoleAssignmentEntity row) {
        return platformAssignmentQueryService.applicationSnapshot(admission, row);
    }

    /**
     * 撤销授权并保留审计行。
     *
     * @param domain 接口管理域
     * @param id 授权 ID
     * @return 撤销前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, String id) {
        return platformAssignmentCommandService.delete(domain, id);
    }

    /**
     * 读取平台成员的简单直接角色，不含用户组继承或带范围、有效期、委派来源的授权。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @return 按名称排序的直接角色
     */
    public List<MemberRoleView> listDirectRoles(AuthorizationDomain domain, String memberId) {
        return platformAssignmentQueryService.listDirectRoles(domain, memberId);
    }

    /**
     * 为新建平台成员追加简单直接角色，调用方须已处于同一事务。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @param roleIds 角色定义 ID
     */
    public void grantDirectRoles(AuthorizationDomain domain, String memberId, List<String> roleIds) {
        platformAssignmentCommandService.grantDirectRoles(domain, memberId, roleIds);
    }

    /**
     * 在成员事务内写入明确选定的可分配固定版本及其全部范围参数。
     *
     * @param memberId 刚创建的平台成员
     * @param drafts 已配置的直接分配
     */
    public void grantMemberRoleAssignments(String memberId, List<MemberRoleAssignmentDraft> drafts) {
        platformAssignmentCommandService.grantMemberRoleAssignments(memberId, drafts);
    }

    /**
     * 无写入地重验成员差量；任何一条越界、版本冲突或缺少完整治理资格均拒绝。
     * @param memberId 成员
     * @param changes 差量
     * @return 共用分配校验错误
     */
    public List<ValidationIssue> previewMemberRoleChanges(String memberId,
            MemberRoleChanges changes) {
        return platformAssignmentPreviewService.previewMemberRoleChanges(memberId, changes);
    }

    /**
     * 在成员外层事务及授权首锁内差量保存，新增先于撤销以检查最终超管存续。
     * @param memberId 成员
     * @param changes 差量
     */
    public void applyMemberRoleChanges(String memberId, MemberRoleChanges changes) {
        platformAssignmentCommandService.applyMemberRoleChanges(memberId, changes);
    }

    /**
     * 替换平台成员的简单直接角色；带范围、有效期或委派来源的授权保持不变。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @param roleIds 目标角色定义 ID
     * @return 替换后的直接角色
     */
    public List<MemberRoleView> replaceDirectRoles(AuthorizationDomain domain, String memberId, List<String> roleIds) {
        return platformAssignmentCommandService.replaceDirectRoles(domain, memberId, roleIds);
    }

    /**
     * 返回平台分配配置资格；任何直接资格都必须来自对应精确操作的非委派来源。
     * @return 当前配置资格
     */
    public com.ingot.cloud.iam.application.assignment.model.AssignmentContext context() {
        return platformAssignmentQueryService.context();
    }

    /**
     * 成员创建事务在插入成员前取得共用授权首锁。
     * @param domain 管理域
     */
    public void lockAuthorization(AuthorizationDomain domain) {
        platformAssignmentCommandService.lockAuthorization(domain);
    }

    /**
     * 读取分配详情，沿用列表的可信来源边界。
     * @param domain 管理域
     * @param id 分配 ID
     * @return 可披露详情
     */
    public ResourceDetail<AssignmentRecord> detail(AuthorizationDomain domain, String id) {
        return platformAssignmentQueryService.detail(domain, id);
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
        return platformAssignmentQueryService.selectedCandidates(id, kind, parameterKey, page, pageSize);
    }
}
