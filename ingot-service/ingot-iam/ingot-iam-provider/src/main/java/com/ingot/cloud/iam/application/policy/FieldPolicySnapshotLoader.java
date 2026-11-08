package com.ingot.cloud.iam.application.policy;

import com.ingot.cloud.iam.domain.policy.FieldPolicyCacheKeys;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamFieldPolicyEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamFieldRuleEntity;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.shared.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyDraft;
import com.ingot.cloud.iam.application.policy.model.FieldRule;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.cloud.iam.application.policy.model.FieldOperationRule;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import lombok.RequiredArgsConstructor;
import com.ingot.cloud.iam.infrastructure.persistence.FieldPolicyRepository;
import com.ingot.cloud.iam.application.policy.FieldAccessEvaluator.FieldRuleRow;
import org.springframework.stereotype.Service;


/**
 * <p>加载当前或草稿字段策略快照，fresh 路径直接读取当前事实，缓存 loader 不回读热快照。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class FieldPolicySnapshotLoader {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private final FieldPolicyRepository policies;
    private final com.ingot.cloud.iam.application.authorization.extension.ResourceFieldMetadata metadata;
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
                : reference(configured, FieldPolicyCacheKeys.latest(resourceId), descriptor);
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
                    IamJsonCodec.read(item.getTargetScope(), SCOPES),
                    IamJsonCodec.read(item.getScopeBindings(), BINDINGS),
                    item.getFieldKey(), item.getVisibility()));
        }
        return governedSnapshot(tenantId, resource, descriptor, entry.expiresAt(),
                DefaultPolicyDefinitions.fieldAccess(baseline.definition(), descriptor),
                DefaultPolicyDefinitions.fieldCeiling(latest.definition(), descriptor),
                List.copyOf(rows), policy == null || policy.getOperationRules() == null ? List.of()
                        : IamJsonCodec.read(policy.getOperationRules(), new TypeReference<List<FieldOperationRule>>() { }),
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
}
