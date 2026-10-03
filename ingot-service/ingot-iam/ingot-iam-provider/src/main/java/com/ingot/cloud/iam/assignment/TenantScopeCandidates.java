package com.ingot.cloud.iam.assignment;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.TenantScopeCandidateSql;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按固定角色版本和范围参数提供租户内实际业务对象候选，并在提交时重验。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantScopeCandidates {
    private static final String CORE_APPLICATION = "iam-tenant";
    private static final String UNSUPPORTED_MESSAGE = "该资源暂未接入范围对象查询，不能配置指定对象";
    private final IamAccess access;
    private final AssignmentRepository assignments;
    private final RoleRepository roleStore;
    private final RoleService roles;
    private final TenantScopeCandidateMapper candidates;

    /**
     * 获取带服务端分页和已选回显的范围候选。
     * @param revisionId 固定版本
     * @param parameterKey 范围参数
     * @param keyword 搜索文字
     * @param ids 已选对象标识
     * @param page 页码
     * @param pageSize 页大小
     * @return 租户内候选或明确不支持状态
     */
    public AuthorizationCandidatePage list(String revisionId, String parameterKey, String keyword,
            List<String> ids, int page, int pageSize) {
        return list(revisionId, parameterKey, keyword, ids, page, pageSize, false, null);
    }

    /**
     * 按真实部门层级分页查询范围对象，其他资源保持普通列表。
     * @param revisionId 固定版本
     * @param parameterKey 范围参数
     * @param keyword 搜索文字
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 页大小
     * @param tree 是否读取当前树分支
     * @param parentId 父部门 ID
     * @return 候选页
     */
    public AuthorizationCandidatePage list(String revisionId, String parameterKey, String keyword,
            List<String> ids, int page, int pageSize, boolean tree, String parentId) {
        IamPages.require(page, pageSize);
        boolean create = access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_ASSIGNMENT_CREATE, false);
        boolean update = access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_ASSIGNMENT_UPDATE, false);
        if (!create && !update) { throw new BizException(IamReasonCode.ACTION_DENIED); }
        ActiveIdentity actor = access.requireCurrent();
        long tenantId = IamIds.require(actor.context().tenantId());
        ScopeTarget target = resolve(actor, revisionId, parameterKey);
        if (target == null) {
            return new AuthorizationCandidatePage(List.of(), 0, page, pageSize, false, UNSUPPORTED_MESSAGE);
        }
        List<BigInteger> selected = ids == null ? List.of() : ids.stream()
                .map(value -> BigInteger.valueOf(IamIds.require(value))).distinct().toList();
        if (selected.size() > IamPages.MAX_SIZE) { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
        String search = keyword == null ? "" : keyword.trim();
        search = "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        boolean hierarchical = target.resource() == TenantScopeObjectResource.DEPARTMENT;
        var query = new TenantScopeCandidateSql.Query(target.resource(), tenantId, search, selected,
                Math.multiplyExact(page - 1, pageSize), pageSize, hierarchical && tree,
                parentId == null || parentId.isBlank() ? null : BigInteger.valueOf(IamIds.require(parentId)));
        var rows = candidates.page(query);
        var paths = hierarchical && !rows.isEmpty()
                ? candidates.departmentPaths(tenantId, rows.stream().map(
                        TenantScopeCandidateMapper.Candidate::id).toList()).stream()
                    .collect(java.util.stream.Collectors.toMap(TenantScopeCandidateMapper.TreePath::leafId,
                            TenantScopeCandidateMapper.TreePath::ancestorPath))
                : java.util.Map.<BigInteger, String>of();
        var items = rows.stream().map(row ->
                new AuthorizationOption(row.id().toString(), row.name(), null, null, null, null, null, null,
                        row.parentId() == null ? null : row.parentId().toString(),
                        row.hasChildren(), paths.get(row.id()))).toList();
        return new AuthorizationCandidatePage(items, candidates.count(query), page, pageSize, true, null,
                target.label(), hierarchical);
    }

    /**
     * 验证提交的参数名、类型及每个实际对象标识。
     * @param actor 当前可信租户成员
     * @param input 单条分配
     * @return 验证问题
     */
    public List<ValidationIssue> validate(ActiveIdentity actor, AssignmentInput input) {
        if (input.scopeBindings() == null) {
            return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "缺少范围参数"));
        }
        long revisionId = IamIds.require(input.roleRevisionRef().id());
        var definitions = roleStore.listParameters(revisionId);
        if (input.scopeBindings().size() != definitions.size() || definitions.stream()
                .anyMatch(parameter -> !input.scopeBindings().containsKey(parameter.getParameterKey()))) {
            return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT,
                    "固定角色版本的范围参数必须全部配置"));
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        for (var entry : input.scopeBindings().entrySet()) {
            ScopeTarget target = resolve(actor, input.roleRevisionRef().id(), entry.getKey());
            ScopeBinding binding = entry.getValue();
            if (binding == null || target == null || binding.ids() == null || binding.ids().isEmpty()) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, UNSUPPORTED_MESSAGE));
            }
            ScopeBindingKind expected = target.resource() == TenantScopeObjectResource.DEPARTMENT
                    && parameterKind(input.roleRevisionRef().id(), entry.getKey()) == ScopeBindingKind.DEPARTMENTS
                    ? ScopeBindingKind.DEPARTMENTS : ScopeBindingKind.OBJECTS;
            if (binding.kind() != expected) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "范围参数类型不匹配"));
            }
            var ids = binding.ids().stream().map(value -> BigInteger.valueOf(IamIds.require(value))).distinct().toList();
            for (int offset = 0; offset < ids.size(); offset += IamPages.MAX_SIZE) {
                var batch = ids.subList(offset, Math.min(offset + IamPages.MAX_SIZE, ids.size()));
                var query = new TenantScopeCandidateSql.Query(target.resource(), tenantId, "%", batch, 0, IamPages.MAX_SIZE);
                if (candidates.count(query) != batch.size()) {
                    return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT,
                            "包含不属于当前租户或资源的对象"));
                }
            }
        }
        return List.of();
    }

    private ScopeBindingKind parameterKind(String revisionId, String key) {
        return roleStore.listParameters(IamIds.require(revisionId)).stream()
                .filter(value -> value.getParameterKey().equals(key))
                .map(value -> value.getBindingKind()).findFirst().orElse(null);
    }

    private ScopeTarget resolve(ActiveIdentity actor, String revisionId, String key) {
        long id = IamIds.require(revisionId);
        var revision = assignments.findRevision(id);
        long tenantId = IamIds.require(actor.context().tenantId());
        if (revision == null || !Boolean.TRUE.equals(revision.getEnabled())
                || !(revision.getKind() == RoleKind.SHARED && revision.getDomain() == AuthorizationDomain.TENANT
                    || revision.getKind() == RoleKind.SYSTEM && revision.getDomain() == AuthorizationDomain.TENANT
                    || revision.getKind() == RoleKind.TENANT_CUSTOM && revision.getTenantId() != null
                        && revision.getTenantId().longValue() == tenantId)) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        ScopeBindingKind kind = parameterKind(revisionId, key);
        if (kind == null) { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
        if (kind == ScopeBindingKind.DEPARTMENTS) {
            return new ScopeTarget(TenantScopeObjectResource.DEPARTMENT, "管理部门");
        }
        List<String> actionIds = roles.synthesizedGrants(id).stream()
                .filter(grant -> grant.scopes().stream().anyMatch(scope ->
                        scope.kind() == ScopeKind.OBJECT_SET && Objects.equals(scope.parameterKey(), key)))
                .map(ActionGrant::actionId).distinct().toList();
        if (actionIds.isEmpty()) { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
        var rows = candidates.actions(actionIds.stream().map(value -> BigInteger.valueOf(IamIds.require(value))).toList());
        if (rows.size() != actionIds.size() || rows.stream()
                .anyMatch(row -> !CORE_APPLICATION.equals(row.applicationCode()))) {
            return null;
        }
        List<TenantScopeObjectResource> resources = rows.stream()
                .map(row -> TenantScopeObjectResource.find(row.resourceCode()))
                .map(resource -> resource == TenantScopeObjectResource.DIRECTORY
                        ? TenantScopeObjectResource.MEMBER : resource).toList();
        if (resources.contains(null) || resources.stream().distinct().count() != 1) {
            return null;
        }
        String label = rows.stream().map(TenantScopeCandidateMapper.ActionResource::resourceName)
                .distinct().reduce((left, right) -> left + " / " + right).orElse("指定对象");
        return new ScopeTarget(resources.getFirst(), label);
    }

    private record ScopeTarget(TenantScopeObjectResource resource, String label) { }
}
