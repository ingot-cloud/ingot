package com.ingot.cloud.iam.policy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ObjectScopeClause;
import com.ingot.cloud.iam.evaluation.ScopeBinder;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldPolicyDraft;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.FieldRule;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.framework.commons.model.iam.FieldOperationRule;
import com.ingot.framework.commons.model.iam.IamActionOperation;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按资源、查看者与目标合并字段策略，投影响应并按独立操作规则拒绝不可编辑写入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class FieldAccessEvaluator {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private final PolicyWriteRepository policies;
    private final IamMemberDepartmentMapper memberships;
    private final DepartmentClosure closures;
    private final org.springframework.beans.factory.ObjectProvider<com.ingot.framework.cache.spi.LayeredCache<String, FieldPolicySnapshot>> cache;
    private final com.ingot.framework.authorization.field.FieldProjectionEngine projection;
    private final com.ingot.cloud.iam.extension.ResourceFieldMetadata metadata;
    private final org.springframework.beans.factory.ObjectProvider<com.ingot.framework.cache.spi.LayeredCache<String, FieldDefaultReference>> defaults;

    /**
     * 加载当前租户在指定资源下的默认版本、平台上限与规则，供一次请求复用。
     *
     * @param tenantId 当前租户
     * @param resource 已接入资源
     * @return 字段策略快照
     */
    public FieldPolicySnapshot snapshot(long tenantId, ResourceKey resource) {
        // 租户策略每次请求读取当前事实，不接受变更提交前的热缓存。
        return snapshotFresh(tenantId, resource);
    }

    /** 加载当前配置事实；写事务和缓存 loader 使用，绝不回读热缓存。 */
    public FieldPolicySnapshot snapshotFresh(long tenantId, ResourceKey resource) {
        try { return loadSnapshot(tenantId, resource, true); }
        catch(org.springframework.dao.DataAccessException failure) { throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE); }
    }

    /** 缓存 loader 使用当前默认引用缓存；业务写入调用 snapshotFresh。 */
    public FieldPolicySnapshot snapshotCachedConfiguration(long tenantId, ResourceKey resource) {
        return loadSnapshot(tenantId, resource, false);
    }

    private FieldPolicySnapshot loadSnapshot(long tenantId, ResourceKey resource, boolean fresh) {
        var entry = metadata.requireFresh(resource, false);
        long resourceId = Long.parseLong(entry.id());
        var descriptor = entry.descriptor();
        var configured = fresh ? null : defaults.getIfAvailable();
        IamFieldPolicyEntity policy = policies.findField(tenantId, resourceId);
        FieldDefaultReference latest = configured == null
                ? referenceOf(policies.latestRevision(DefaultPolicyKind.FIELD, resourceId), descriptor)
                : reference(configured, FieldPolicyCacheConfiguration.latest(resourceId), descriptor);
        FieldDefaultReference baseline = policy == null ? latest
                : configured == null ? referenceOf(policies.findRevision(policy.getDefaultRevisionId().longValueExact(), DefaultPolicyKind.FIELD), descriptor)
                : reference(configured, policy.getDefaultRevisionId().toString(), descriptor);
        if (baseline == null || latest == null)
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        List<FieldRuleRow> rows = new ArrayList<>();
        List<IamFieldRuleEntity> items = policy == null ? List.of() : policies.listFieldRules(tenantId, resourceId);
        LinkedHashSet<Long> selectorIds = new LinkedHashSet<>();
        for (IamFieldRuleEntity item : items) {
            selectorIds.add(item.getViewerSelectorId().longValueExact());
        }
        Map<Long, Selection> selectors = policies.selectors(tenantId, selectorIds);
        for (IamFieldRuleEntity item : items) {
            rows.add(new FieldRuleRow(
                    selectors.getOrDefault(item.getViewerSelectorId().longValueExact(),
                            new Selection(List.of(), List.of())),
                    IamJson.read(item.getTargetScope(), SCOPES),
                    IamJson.read(item.getScopeBindings(), BINDINGS),
                    item.getFieldKey(), item.getVisibility()));
        }
        return governedSnapshot(tenantId, resource, descriptor, entry.expiresAt(),
                DefaultPolicyDefinitions.fieldAccess(baseline.definition(), descriptor),
                DefaultPolicyDefinitions.fieldCeiling(latest.definition(), descriptor),
                List.copyOf(rows), policy == null || policy.getOperationRules() == null ? List.of()
                        : IamJson.read(policy.getOperationRules(), new TypeReference<List<FieldOperationRule>>() { }),
                DefaultPolicyDefinitions.fieldOperations(baseline.definition(), false, descriptor),
                DefaultPolicyDefinitions.fieldOperations(latest.definition(), true, descriptor),
                com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime.earliest(latest.expiresAt(), baseline.expiresAt()));
    }

    private static FieldDefaultReference referenceOf(IamDefaultPolicyRevisionEntity revision, ResourceDescriptor descriptor) {
        return revision == null ? new FieldDefaultReference(DefaultPolicyDefinitions.definition(descriptor),
                com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime.deadline()) : FieldDefaultReference.from(revision);
    }

    private static FieldDefaultReference reference(com.ingot.framework.cache.spi.LayeredCache<String, FieldDefaultReference> cache, String key,
            ResourceDescriptor descriptor) {
        var value = cache.get(key);
        if (value == null || !java.time.Instant.now().isBefore(value.expiresAt())) {
            cache.evict(key);
            value = cache.get(key);
        }
        return value == null || !java.time.Instant.now().isBefore(value.expiresAt())
                ? new FieldDefaultReference(DefaultPolicyDefinitions.definition(descriptor),
                        com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime.deadline()) : value;
    }

    private FieldPolicySnapshot governedSnapshot(long tenantId, ResourceKey resource, ResourceDescriptor descriptor,
            java.time.Instant entryExpiresAt, Map<String, FieldAccess> baseline, Map<String, FieldAccess> ceiling,
            List<FieldRuleRow> rules, List<FieldOperationRule> operationRules, Map<String, FieldOperations> defaults,
            Map<String, FieldOperations> operationCeiling, java.time.Instant expiresAt) {
        Map<String, com.ingot.framework.commons.model.iam.MaskSpec> masks = new LinkedHashMap<>();
        Map<String, FieldAccess> values = new LinkedHashMap<>(), upper = new LinkedHashMap<>();
        Map<String, FieldOperations> abilities = new LinkedHashMap<>(), limits = new LinkedHashMap<>();
        for (var field : descriptor.fields()) {
            if (field.mask() != null) masks.put(field.key(), field.mask());
            var cap = field.visibilities().stream().max(java.util.Comparator.comparingInt(Enum::ordinal)).orElse(FieldVisibility.HIDDEN);
            var configured = ceiling.getOrDefault(field.key(), new FieldAccess(FieldVisibility.FULL, true));
            var visibility = cap.ordinal() < configured.visibility().ordinal() ? cap : configured.visibility();
            values.put(field.key(), baseline.getOrDefault(field.key(), new FieldAccess(FieldVisibility.HIDDEN, false)));
            upper.put(field.key(), new FieldAccess(visibility, field.editable()));
            abilities.put(field.key(), defaults.getOrDefault(field.key(), FieldOperations.NONE));
            var operation = operationCeiling.getOrDefault(field.key(), new FieldOperations(field.editable(), field.filterable()));
            limits.put(field.key(), new FieldOperations(field.editable() && operation.editable(), field.filterable() && operation.filterable()));
        }
        return new FieldPolicySnapshot(tenantId, resource, values, upper, rules, operationRules, abilities, limits, masks,
                com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime.earliest(expiresAt, entryExpiresAt));
    }

    /** 编译给其他服务执行的无原值策略，序列化之后不再读取数据库。 */
    public com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision decision(
            com.ingot.framework.commons.model.iam.AuthorizationContext actor, ResourceKey resource, String action,
            ObjectScope scope, boolean fresh) {
        if (policies.currentOwner(IamIds.require(actor.tenantId()), IamIds.require(actor.memberId())))
            return ownerDecision(resource, action);
        long tenantId = IamIds.require(actor.tenantId()), viewer = IamIds.require(actor.memberId());
        var snapshot = fresh ? snapshotFresh(tenantId, resource) : snapshot(tenantId, resource);
        var batch = prepare(snapshot, viewer, List.of());
        var abilities = operations(snapshot, viewer, resource, action, scope);
        Map<String, FieldAccess> defaults = new LinkedHashMap<>(), ceilings = new LinkedHashMap<>();
        snapshot.baseline().forEach((field, access) -> {
            defaults.put(field, new FieldAccess(access.visibility(), access.visibility() != FieldVisibility.HIDDEN && abilities.getOrDefault(field, FieldOperations.NONE).editable()));
            ceilings.put(field, new FieldAccess(snapshot.ceilingOf(field).visibility(), snapshot.ceilingOf(field).visibility() != FieldVisibility.HIDDEN && abilities.getOrDefault(field, FieldOperations.NONE).editable()));
        });
        var rules = batch.rules().stream().map(rule -> new com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule(
                rule.fieldKey(), rule.scopes(), new FieldAccess(rule.visibility(), rule.visibility() != FieldVisibility.HIDDEN && abilities.getOrDefault(rule.fieldKey(), FieldOperations.NONE).editable()))).toList();
        return new com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision(defaults, ceilings, rules, abilities,
                snapshot.masks(), com.ingot.framework.commons.model.iam.FieldMergeMode.RESTRICTIONS, snapshot.expiresAt());
    }

    /**
     * 当前成员是否为租户拥有者。拥有者字段覆盖不与角色快照求交。
     * @param actor 可信身份
     * @return 是否为当前拥有者
     */
    public boolean currentOwner(com.ingot.framework.commons.model.iam.AuthorizationContext actor) {
        return policies.currentOwner(IamIds.require(actor.tenantId()), IamIds.require(actor.memberId()));
    }

    /**
     * 按未保存草稿组装字段策略快照，与提交后求值使用同一合并规则。
     *
     * @param tenantId 当前租户
     * @param resource 已接入资源
     * @param draft 字段草稿
     * @return 字段策略快照
     */
    public FieldPolicySnapshot snapshot(long tenantId, ResourceKey resource, FieldPolicyDraft draft) {
        var entry = metadata.requireFresh(resource, false);
        long resourceId = Long.parseLong(entry.id());
        IamDefaultPolicyRevisionEntity latest = policies.latestRevision(DefaultPolicyKind.FIELD, resourceId);
        IamDefaultPolicyRevisionEntity baseline = policies.findRevision(
                IamIds.require(draft.defaultRevisionId()), DefaultPolicyKind.FIELD);
        if (baseline == null || latest == null || baseline.getResourceId() == null
                || baseline.getResourceId().longValueExact() != resourceId) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
        var descriptor = entry.descriptor();
        List<FieldRuleRow> rows = new ArrayList<>();
        List<FieldRule> rules = draft.rules() == null ? List.of() : draft.rules();
        for (FieldRule rule : rules) {
            rows.add(new FieldRuleRow(rule.viewerSelection(), rule.targetScope(), rule.scopeBindings(),
                    rule.fieldKey(), rule.visibility()));
        }
        return governedSnapshot(tenantId, resource, descriptor, entry.expiresAt(),
                DefaultPolicyDefinitions.fieldAccess(baseline.getDefinition(), descriptor),
                DefaultPolicyDefinitions.fieldCeiling(latest.getDefinition(), descriptor),
                List.copyOf(rows), draft.operationRules(),
                DefaultPolicyDefinitions.fieldOperations(baseline.getDefinition(), false, descriptor),
                DefaultPolicyDefinitions.fieldOperations(latest.getDefinition(), true, descriptor),
                com.ingot.framework.commons.model.iam.extension.FieldPolicyLifetime.deadline());
    }

    /**
     * 计算单一字段的可见与编辑上限。
     *
     * @param tenantId 当前租户
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @param resource 已接入资源
     * @param fieldKey 字段键
     * @return 合并后的访问结果
     */
    public FieldAccess access(long tenantId, long viewerId, long targetMemberId, ResourceKey resource,
                              String fieldKey) {
        return access(snapshot(tenantId, resource), viewerId, targetMemberId, fieldKey);
    }

    /**
     * 在已加载快照上计算单一字段访问。
     *
     * @param snapshot 字段策略快照
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @param fieldKey 字段键
     * @return 合并并受平台上限约束的访问
     */
    public FieldAccess access(FieldPolicySnapshot snapshot, long viewerId, long targetMemberId, String fieldKey) {
        FieldAccess matched = null;
        for (FieldRuleRow rule : snapshot.rules()) {
            if (!fieldKey.equals(rule.fieldKey())
                    || !matchesViewer(snapshot.tenantId(), viewerId, rule.viewer())
                    || !matchesTarget(snapshot.tenantId(), viewerId, targetMemberId, rule.scopes(), rule.bindings())) {
                continue;
            }
            matched = DefaultPolicyDefinitions.stricter(matched,
                    new FieldAccess(rule.visibility(), rule.visibility() != FieldVisibility.HIDDEN));
        }
        FieldAccess resolved = matched == null ? snapshot.baselineOf(fieldKey) : matched;
        var visibility = resolved.visibility().ordinal() <= snapshot.ceilingOf(fieldKey).visibility().ordinal()
                ? resolved.visibility() : snapshot.ceilingOf(fieldKey).visibility();
        boolean editable = operation(snapshot, viewerId, snapshot.resource(), updateAction(snapshot.resource()), fieldKey).editable();
        return new FieldAccess(visibility, visibility != FieldVisibility.HIDDEN && editable);
    }

    /**
     * 计算成员资料全部字段访问说明。
     *
     * @param tenantId 当前租户
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @param resource 已接入资源
     * @return 按字段键索引的访问说明
     */
    public Map<String, FieldAccess> memberAccess(long tenantId, long viewerId, long targetMemberId,
                                                 ResourceKey resource) {
        return memberAccess(snapshot(tenantId, resource), viewerId, targetMemberId);
    }

    /**
     * 在已加载快照上计算成员资料全部字段访问。
     *
     * @param snapshot 字段策略快照
     * @param viewerId 查看者成员
     * @param targetMemberId 目标成员
     * @return 按字段键索引的访问说明
     */
    public Map<String, FieldAccess> memberAccess(FieldPolicySnapshot snapshot, long viewerId, long targetMemberId) {
        return memberAccess(prepare(snapshot, viewerId, List.of(targetMemberId)), targetMemberId);
    }

    /**
     * 批量预载真实任职和查看者规则；输出阶段仅做内存范围匹配。
     * @param snapshot 同次配置
     * @param viewerId 可信查看者
     * @param targetIds 当前完整批次对象标识
     * @return 不含敏感原值的执行批次
     */
    public FieldBatch prepare(FieldPolicySnapshot sourceSnapshot, long viewerId, java.util.Collection<Long> targetIds) {
        final var snapshot = ownerSnapshot(sourceSnapshot, viewerId);
        Set<BigInteger> ids = targetIds.stream().map(BigInteger::valueOf).collect(Collectors.toSet());
        ids.add(BigInteger.valueOf(viewerId));
        Map<Long, List<String>> departments = new LinkedHashMap<>();
        memberships.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                .select(IamMemberDepartmentEntity::getMemberId, IamMemberDepartmentEntity::getDepartmentId)
                .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(snapshot.tenantId()))
                .in(IamMemberDepartmentEntity::getMemberId, ids)).forEach(row -> departments
                    .computeIfAbsent(row.getMemberId().longValueExact(), ignored -> new ArrayList<>()).add(row.getDepartmentId().toString()));
        Map<Selection, Boolean> viewers = new java.util.HashMap<>();
        List<CompiledRule> rules = new ArrayList<>();
        for (var rule : snapshot.rules()) {
            if (!viewers.computeIfAbsent(rule.viewer(), selection -> matchesViewer(snapshot.tenantId(), viewerId, selection)))
                continue;
            var clauses = ScopeBinder.bind(new ActionGrant(MemberFieldKey.VALUE_DISPLAY_NAME, rule.scopes()), rule.bindings());
            List<com.ingot.framework.commons.model.iam.extension.ScopeCondition> scopes = new ArrayList<>();
            if (rule.scopes().isEmpty())
                scopes.add(new com.ingot.framework.commons.model.iam.extension.ScopeCondition(true, List.of(), null, List.of()));
            for (var clause : clauses) {
                if (clause.empty()) continue;
                List<List<String>> sets = new ArrayList<>();
                if (clause.memberDepartments())
                    sets.add(closures.expand(BigInteger.valueOf(snapshot.tenantId()),
                            departments.getOrDefault(viewerId, List.of()).stream().map(BigInteger::new).toList(),
                            clause.memberDepartmentDescendants()).stream().map(BigInteger::toString).toList());
                if (!clause.departmentIds().isEmpty())
                    sets.add(closures.expand(BigInteger.valueOf(snapshot.tenantId()),
                            clause.departmentIds().stream().map(BigInteger::new).toList(), clause.departmentDescendants())
                            .stream().map(BigInteger::toString).toList());
                scopes.add(new com.ingot.framework.commons.model.iam.extension.ScopeCondition(clause.all(),
                        List.copyOf(clause.objectIds()), clause.self() ? IamIds.text(viewerId) : null, sets));
            }
            rules.add(new CompiledRule(rule.fieldKey(), rule.visibility(), scopes));
        }
        Map<String, FieldOperations> operations = new LinkedHashMap<>();
        // 选择器按请求内值缓存，字段数量不产生重复关系查询。
        for (var field : snapshot.baseline().keySet()) {
            FieldOperations matched = null;
            String update = updateAction(snapshot.resource());
            for (var rule : snapshot.operationRules()) {
                if (!update.equals(rule.actionCode()) || !field.equals(rule.fieldKey())
                        || !viewers.computeIfAbsent(rule.viewerSelection(), selection -> matchesViewer(snapshot.tenantId(), viewerId, selection)))
                    continue;
                matched = matched == null ? rule.operations() : new FieldOperations(matched.editable() && rule.operations().editable(),
                        matched.filterable() && rule.operations().filterable());
            }
            var baseline = matched == null ? snapshot.operationDefaults().getOrDefault(field, FieldOperations.NONE) : matched;
            var upper = snapshot.operationCeiling().getOrDefault(field, FieldOperations.NONE);
            operations.put(field, new FieldOperations(baseline.editable() && upper.editable(), false));
        }
        Map<Long, List<String>> immutable = new LinkedHashMap<>();
        departments.forEach((id, values) -> immutable.put(id, List.copyOf(values)));
        return new FieldBatch(snapshot, List.copyOf(rules), Map.copyOf(immutable), Map.copyOf(operations));
    }

    /** 从已预载批次计算行级字段，调用期间 SQL/RPC 为零。 */
    public Map<String, FieldAccess> memberAccess(FieldBatch batch, long targetId) {
        Map<String, FieldAccess> result = new LinkedHashMap<>();
        var snapshot = batch.snapshot();
        var target = new com.ingot.framework.commons.model.iam.extension.ScopeTarget(IamIds.text(targetId), IamIds.text(targetId),
                IamIds.text(snapshot.tenantId()), batch.departments().getOrDefault(targetId, List.of()));
        snapshot.baseline().keySet().forEach(field -> {
            FieldVisibility matched = null;
            for (var rule : batch.rules()) {
                if (field.equals(rule.fieldKey()) && com.ingot.framework.authorization.ScopeRules.matches(rule.scopes(), target))
                    matched = matched == null || rule.visibility().ordinal() < matched.ordinal() ? rule.visibility() : matched;
            }
            var baseline = matched == null ? snapshot.baselineOf(field).visibility() : matched;
            var upper = snapshot.ceilingOf(field).visibility();
            var visibility = baseline.ordinal() < upper.ordinal() ? baseline : upper;
            result.put(field, new FieldAccess(visibility, visibility != FieldVisibility.HIDDEN
                    && batch.operations().getOrDefault(field, FieldOperations.NONE).editable()));
        });
        return Map.copyOf(result);
    }

    /**
     * <p>请求内编译的目标可见性规则，编译结果不进入 L2。</p>
     * @param fieldKey 逻辑字段
     * @param visibility 可见性
     * @param scopes 编译的真实归属范围
     * @author jy
     * @since 1.0.0
     */
    public record CompiledRule(String fieldKey, FieldVisibility visibility,
            List<com.ingot.framework.commons.model.iam.extension.ScopeCondition> scopes) {
        /** 冻结编译范围。 */
        public CompiledRule { scopes = List.copyOf(scopes); }
    }

    /**
     * <p>一次请求的只读执行索引，行数增加不会增加授权读取次数。</p>
     * @param snapshot 策略配置
     * @param rules 已匹配查看者并编译的规则
     * @param departments 批量真实任职
     * @param operations 全局编辑能力
     * @author jy
     * @since 1.0.0
     */
    public record FieldBatch(FieldPolicySnapshot snapshot, List<CompiledRule> rules, Map<Long, List<String>> departments,
            Map<String, FieldOperations> operations) {
        /** 冻结批次事实，供异步和序列化阶段使用。 */
        public FieldBatch {
            rules = List.copyOf(rules);
            Map<Long, List<String>> copy = new LinkedHashMap<>();
            departments.forEach((key, value) -> copy.put(key, List.copyOf(value)));
            departments = Map.copyOf(copy); operations = Map.copyOf(operations);
        }
    }

    /** 按绑定注解投影全部受控属性，时间、别名及新增 DTO 不需要成员枚举分支。 */
    public MemberRecord project(MemberRecord raw, Map<String, FieldAccess> access, ResourceKey resource,
            Map<String, com.ingot.framework.commons.model.iam.MaskSpec> masks) {
        return projection.projectRecord(raw, resource,
                new com.ingot.framework.authorization.field.FieldReadSnapshot(Map.of(resource, access), Map.of(resource, masks)));
    }

    /**
     * 拒绝提交隐藏或不可编辑字段；不根据文本内容猜测脱敏状态。
     *
     * @param tenantId 当前租户
     * @param viewerId 操作者
     * @param targetMemberId 目标成员
     * @param fieldKey 字段键
     * @param submitted 实际提交值；显式空引用由业务 nullable 规则处理
     */
    public void requireWritable(long tenantId, long viewerId, long targetMemberId, String fieldKey, String submitted) {
        FieldAccess access = access(tenantId, viewerId, targetMemberId,
                com.ingot.cloud.iam.extension.BuiltinResourceProviders.TENANT_MEMBER_RESOURCE, fieldKey);
        if (!access.editable() || access.visibility() == FieldVisibility.HIDDEN) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
    }

    /**
     * 未对查询范围内全部可能匹配目标完整可见时，禁止按原值筛选。
     *
     * @param tenantId 当前租户
     * @param viewerId 操作者
     * @param fieldKey 字段键
     * @param submitted 筛选原值
     */
    public void requireOriginalLookup(long tenantId, long viewerId, String fieldKey, String submitted) {
        requireOriginalLookup(snapshot(tenantId, com.ingot.cloud.iam.extension.BuiltinResourceProviders.TENANT_MEMBER_RESOURCE), viewerId, fieldKey, submitted,
                ObjectScope.all());
    }

    /**
     * 按对象范围内可能匹配的目标执行字段披露边界，不能从隐藏值筛选、排序或计数推断。
     *
     * @param snapshot 字段策略快照
     * @param viewerId 操作者
     * @param fieldKey 字段键
     * @param submitted 筛选原值
     * @param scope 查询对象范围
     */
    public void requireOriginalLookup(FieldPolicySnapshot snapshot, long viewerId, String fieldKey, String submitted,
                                      ObjectScope scope) {
        if (submitted == null || submitted.isBlank() || scope == null || scope.coversNone()) {
            return;
        }
        var resource = snapshot.resource();
        var action = metadata.requireFresh(resource, false).descriptor().readAction();
        if (!operation(snapshot, viewerId, resource, action, fieldKey).filterable()
                || !universallyFull(snapshot, viewerId, fieldKey, scope)) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
    }

    /** 当前查看者可能看到的列；行值仍由真实对象独立判断，不用列表样本推断能力。 */
    public Map<String, FieldVisibility> contextVisibility(FieldPolicySnapshot sourceSnapshot, long viewerId) {
        final var snapshot = ownerSnapshot(sourceSnapshot, viewerId);
        var batch = prepare(snapshot, viewerId, List.of());
        Map<String, FieldVisibility> result = new LinkedHashMap<>();
        snapshot.baseline().forEach((key, baseline) -> {
            var candidates = batch.rules().stream().filter(rule -> key.equals(rule.fieldKey())).toList();
            var global = candidates.stream().filter(rule -> rule.scopes().stream().anyMatch(com.ingot.framework.commons.model.iam.extension.ScopeCondition::all)).toList();
            var visibility = global.isEmpty() ? baseline.visibility()
                    : global.stream().map(CompiledRule::visibility).min(java.util.Comparator.comparingInt(Enum::ordinal)).orElse(FieldVisibility.HIDDEN);
            if (global.isEmpty()) {
                for (var rule : candidates) if (rule.visibility().ordinal() > visibility.ordinal()) visibility = rule.visibility();
            }
            var upper = snapshot.ceilingOf(key).visibility();
            result.put(key, visibility.ordinal() < upper.ordinal() ? visibility : upper);
        });
        return Map.copyOf(result);
    }

    /** 按查看者和精确操作计算全局能力，匹配规则覆盖默认，多匹配取交集。 */
    public FieldOperations operation(FieldPolicySnapshot sourceSnapshot, long viewerId, ResourceKey resource,
            String action, String fieldKey) {
        final var snapshot = ownerSnapshot(sourceSnapshot, viewerId);
        FieldOperations matched = null;
        for (var rule : snapshot.operationRules()) {
            if (!action.equals(rule.actionCode()) || !fieldKey.equals(rule.fieldKey())
                    || !matchesViewer(snapshot.tenantId(), viewerId, rule.viewerSelection()))
                continue;
            matched = matched == null ? rule.operations() : new FieldOperations(
                    matched.editable() && rule.operations().editable(), matched.filterable() && rule.operations().filterable());
        }
        var baseline = matched == null ? snapshot.operationDefaults().getOrDefault(fieldKey, FieldOperations.NONE) : matched;
        var upper = snapshot.operationCeiling().getOrDefault(fieldKey, FieldOperations.NONE);
        boolean write = writes(action);
        return new FieldOperations(write && baseline.editable() && upper.editable(), !write && baseline.filterable() && upper.filterable());
    }

    /** 整份查询范围的可用筛选能力，不能使用当前页样本判断。 */
    public Map<String, FieldOperations> operations(FieldPolicySnapshot sourceSnapshot, long viewerId, ResourceKey resource,
            String action, ObjectScope scope) {
        final var snapshot = ownerSnapshot(sourceSnapshot, viewerId);
        Set<Long> ids = new LinkedHashSet<>();
        scope.clauses().forEach(clause -> clause.requiredIds().forEach(id -> ids.add(id.longValueExact())));
        var batch = prepare(snapshot, viewerId, ids);
        Map<Selection, Boolean> viewers = new java.util.HashMap<>();
        Map<String, FieldOperations> result = new LinkedHashMap<>();
        for (var field : snapshot.baseline().keySet()) {
            FieldOperations matched = null;
            for (var rule : snapshot.operationRules()) {
                if (!action.equals(rule.actionCode()) || !field.equals(rule.fieldKey())
                        || !viewers.computeIfAbsent(rule.viewerSelection(), selection -> matchesViewer(snapshot.tenantId(), viewerId, selection))) continue;
                matched = matched == null ? rule.operations() : new FieldOperations(matched.editable() && rule.operations().editable(), matched.filterable() && rule.operations().filterable());
            }
            var value = matched == null ? snapshot.operationDefaults().getOrDefault(field, FieldOperations.NONE) : matched;
            var upper = snapshot.operationCeiling().getOrDefault(field, FieldOperations.NONE);
            boolean write = writes(action);
            result.put(field, new FieldOperations(write && value.editable() && upper.editable(),
                    !write && value.filterable() && upper.filterable() && universallyFull(batch, field, scope)));
        }
        return Map.copyOf(result);
    }

    /** 一次验证实际筛选键，SQL/count 之前执行。 */
    public void requireFilters(FieldPolicySnapshot snapshot, long viewerId, ResourceKey resource, String action,
            ObjectScope scope, Map<String, ?> submitted) {
        if (!submitted.isEmpty())
            com.ingot.framework.authorization.field.FieldFilterExecutor.require(submitted,
                    operations(snapshot, viewerId, resource, action, scope));
    }

    private boolean universallyFull(FieldPolicySnapshot snapshot, long viewerId, String fieldKey, ObjectScope scope) {
        Set<Long> ids = new LinkedHashSet<>();
        scope.clauses().forEach(clause -> clause.requiredIds().forEach(id -> ids.add(id.longValueExact())));
        return universallyFull(prepare(snapshot, viewerId, ids), fieldKey, scope);
    }

    private boolean universallyFull(FieldBatch batch, String fieldKey, ObjectScope scope) {
        if (scope.coversNone()) return false;
        if (batch.snapshot().ceilingOf(fieldKey).visibility() != FieldVisibility.FULL) return false;
        if (scope.coversAll() || hasDepartmentSets(scope) || scope.clauses().stream().anyMatch(clause -> clause.requiredIds().isEmpty())) {
            boolean fullAll = batch.snapshot().baselineOf(fieldKey).visibility() == FieldVisibility.FULL;
            for (var rule : batch.rules()) {
                if (!fieldKey.equals(rule.fieldKey())) continue;
                if (rule.visibility() != FieldVisibility.FULL) return false;
                fullAll |= rule.scopes().stream().anyMatch(com.ingot.framework.commons.model.iam.extension.ScopeCondition::all);
            }
            return fullAll;
        }
        for (var clause : scope.clauses())
            for (var id : clause.requiredIds())
                if (memberAccess(batch, id.longValueExact()).getOrDefault(fieldKey, new FieldAccess(FieldVisibility.HIDDEN, false)).visibility() != FieldVisibility.FULL) return false;
        return !scope.clauses().isEmpty();
    }

    private FieldAccess worstCase(FieldPolicySnapshot snapshot, long viewerId, String fieldKey) {
        FieldAccess matched = null;
        boolean subset = false;
        boolean any = false;
        for (FieldRuleRow rule : snapshot.rules()) {
            if (!fieldKey.equals(rule.fieldKey()) || !matchesViewer(snapshot.tenantId(), viewerId, rule.viewer())) {
                continue;
            }
            any = true;
            if (!allTargets(rule.scopes())) {
                subset = true;
            }
            matched = DefaultPolicyDefinitions.stricter(matched,
                    new FieldAccess(rule.visibility(), rule.visibility() != FieldVisibility.HIDDEN));
        }
        FieldAccess resolved = !any ? snapshot.baselineOf(fieldKey)
                : subset ? DefaultPolicyDefinitions.stricter(snapshot.baselineOf(fieldKey), matched) : matched;
        return DefaultPolicyDefinitions.stricter(resolved, snapshot.ceilingOf(fieldKey));
    }

    private static boolean hasDepartmentSets(ObjectScope scope) {
        for (ObjectScopeClause clause : scope.clauses()) {
            if (!clause.departmentSets().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean allTargets(List<ScopeExpression> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return true;
        }
        for (ScopeExpression scope : scopes) {
            if (scope == null || scope.kind() != ScopeKind.ALL) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesViewer(long tenantId, long viewerId, Selection selection) {
        return expand(tenantId, selection).contains(viewerId);
    }

    private boolean matchesTarget(long tenantId, long viewerId, long targetId, List<ScopeExpression> scopes,
                                  Map<String, ScopeBinding> bindings) {
        if (scopes == null || scopes.isEmpty()) {
            return true;
        }
        List<ScopeClause> clauses = ScopeBinder.bind(new ActionGrant(MemberFieldKey.VALUE_DISPLAY_NAME, scopes),
                bindings == null ? Map.of() : bindings);
        if (clauses.isEmpty()) {
            return false;
        }
        for (ScopeClause clause : clauses) {
            if (matchesClause(tenantId, viewerId, targetId, clause)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesClause(long tenantId, long viewerId, long targetId, ScopeClause clause) {
        if (clause == null || clause.empty()) {
            return false;
        }
        if (clause.all()) {
            return true;
        }
        boolean matched = false;
        if (clause.self()) {
            if (viewerId != targetId) {
                return false;
            }
            matched = true;
        }
        if (!clause.objectIds().isEmpty()) {
            if (!clause.objectIds().contains(IamIds.text(targetId))) {
                return false;
            }
            matched = true;
        }
        if (clause.memberDepartments()) {
            Set<BigInteger> departments = closures.expand(BigInteger.valueOf(tenantId),
                    memberDepartments(tenantId, viewerId), clause.memberDepartmentDescendants());
            if (!memberInDepartments(tenantId, targetId, departments)) {
                return false;
            }
            matched = true;
        }
        if (!clause.departmentIds().isEmpty()) {
            List<BigInteger> ids = new ArrayList<>();
            for (String id : clause.departmentIds()) {
                ids.add(BigInteger.valueOf(IamIds.require(id)));
            }
            Set<BigInteger> departments = closures.expand(BigInteger.valueOf(tenantId), ids,
                    clause.departmentDescendants());
            if (!memberInDepartments(tenantId, targetId, departments)) {
                return false;
            }
            matched = true;
        }
        return matched;
    }

    private Set<Long> expand(long tenantId, Selection selection) {
        java.util.HashSet<Long> ids = new java.util.HashSet<>();
        if (selection == null) {
            return ids;
        }
        for (String memberId : selection.members()) {
            ids.add(IamIds.require(memberId));
        }
        for (DepartmentSelection department : selection.departments()) {
            Set<BigInteger> departments = closures.expand(BigInteger.valueOf(tenantId),
                    List.of(BigInteger.valueOf(IamIds.require(department.id()))), department.includeDescendants());
            if (departments.isEmpty()) {
                continue;
            }
            ids.addAll(memberships.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                            .select(IamMemberDepartmentEntity::getMemberId)
                            .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                            .in(IamMemberDepartmentEntity::getDepartmentId, departments)).stream()
                    .map(row -> row.getMemberId().longValueExact()).collect(Collectors.toSet()));
        }
        return ids;
    }

    private Set<BigInteger> memberDepartments(long tenantId, long memberId) {
        return memberships.selectList(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .select(IamMemberDepartmentEntity::getDepartmentId)
                        .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                        .eq(IamMemberDepartmentEntity::getMemberId, BigInteger.valueOf(memberId))).stream()
                .map(IamMemberDepartmentEntity::getDepartmentId)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private boolean memberInDepartments(long tenantId, long memberId, Set<BigInteger> departments) {
        if (departments == null || departments.isEmpty()) {
            return false;
        }
        return memberships.selectCount(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                .eq(IamMemberDepartmentEntity::getTenantId, BigInteger.valueOf(tenantId))
                .eq(IamMemberDepartmentEntity::getMemberId, BigInteger.valueOf(memberId))
                .in(IamMemberDepartmentEntity::getDepartmentId, departments)) > 0;
    }

    /**
     * <p>快照内一条已展开查看者选择器的字段规则。</p>
     *
     * @param viewer 查看者选择
     * @param scopes 目标范围
     * @param bindings 范围绑定
     * @param fieldKey 字段键
     * @param visibility 可见程度
     */
    public record FieldRuleRow(Selection viewer, List<ScopeExpression> scopes, Map<String, ScopeBinding> bindings,
                               String fieldKey, FieldVisibility visibility) {
        /** 冻结可供异步执行的规则源值。 */
        public FieldRuleRow { scopes = List.copyOf(scopes); bindings = Map.copyOf(bindings); }
    }
    private FieldPolicySnapshot ownerSnapshot(FieldPolicySnapshot snapshot, long viewerId) {
        if (!policies.currentOwner(snapshot.tenantId(), viewerId)) return snapshot;
        return new FieldPolicySnapshot(snapshot.tenantId(), snapshot.resource(), snapshot.ceiling(), snapshot.ceiling(),
                List.of(), List.of(), snapshot.operationCeiling(), snapshot.operationCeiling(), snapshot.masks(), snapshot.expiresAt());
    }

    /** 为真实拥有者输出注册字段全权，仍保留平台目录能力上限及写入绑定能力。 */
    public com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision ownerDecision(ResourceKey resource, String action) {
        var entry = metadata.requireFresh(resource, false);
        var descriptor = entry.descriptor();
        boolean write = descriptor.actions().stream().anyMatch(a -> a.code().equals(action)
            && a.mode() == com.ingot.framework.commons.model.iam.extension.ExecutionMode.MUTATING);
        Map<String, FieldAccess> fields = new LinkedHashMap<>();
        Map<String, FieldOperations> operations = new LinkedHashMap<>();
        Map<String, com.ingot.framework.commons.model.iam.MaskSpec> masks = new LinkedHashMap<>();
        Map<String,FieldAccess> platformCeiling=Map.of();
        Map<String,FieldOperations> platformOperations=Map.of();
        try {
            var latest=policies.latestRevision(DefaultPolicyKind.FIELD, Long.parseLong(entry.id()));
            if (latest != null) {
                platformCeiling=DefaultPolicyDefinitions.fieldCeiling(latest.getDefinition(), descriptor);
                platformOperations=DefaultPolicyDefinitions.fieldOperations(latest.getDefinition(), true, descriptor);
            }
        } catch(org.springframework.dao.DataAccessException failure){throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);}
        var ceilings=platformCeiling;var operationCeilings=platformOperations;
        descriptor.fields().forEach(field -> {
            var visibility = field.visibilities().stream().max(java.util.Comparator.comparingInt(Enum::ordinal)).orElse(FieldVisibility.HIDDEN);
            var ceiling=ceilings.get(field.key());
            if(ceiling!=null && ceiling.visibility().ordinal()<visibility.ordinal())visibility=ceiling.visibility();
            var upper=operationCeilings.getOrDefault(field.key(),new FieldOperations(true,true));
            boolean editable=write && field.editable() && upper.editable();
            fields.put(field.key(), new FieldAccess(visibility, visibility != FieldVisibility.HIDDEN && editable));
            operations.put(field.key(), new FieldOperations(editable, !write && field.filterable() && upper.filterable()));
            if (field.mask() != null) masks.put(field.key(), field.mask());
        });
        return new com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision(fields, fields, List.of(), operations,
            masks, com.ingot.framework.commons.model.iam.FieldMergeMode.RESTRICTIONS, entry.expiresAt());
    }

    private String updateAction(ResourceKey resource) {
        return metadata.requireFresh(resource, false).descriptor().actions().stream().map(action -> action.code())
                .filter(code -> IamActionOperation.ofActionCode(code) == IamActionOperation.UPDATE).findFirst().orElse("");
    }

    private static boolean writes(String action) {
        if (action == null || action.isBlank()) {
            return false;
        }
        var operation = IamActionOperation.ofActionCode(action);
        return operation == IamActionOperation.CREATE || operation == IamActionOperation.UPDATE;
    }

}
