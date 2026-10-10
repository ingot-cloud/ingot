package com.ingot.cloud.iam.extension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator.AuthorizationView;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.framework.commons.model.iam.MaskSpec;
import com.ingot.framework.commons.model.iam.ResourceFieldDefinition;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldMergeMode;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 发布资源字段快照并按每条分配的精确操作范围编译字段授权。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class RoleFieldPermissionService {

    private static final FieldAccess HIDDEN = new FieldAccess(FieldVisibility.HIDDEN, false);

    private static final TypeReference<Map<String, ResourceFieldDefinition>> SNAPSHOT = new TypeReference<>() {
    };

    private final ResourceFieldMetadata metadata;

    private final RoleRepository roles;

    private final ScopeTransportCompiler compiler;

    private final AuthorizationEvaluator evaluator;
    private final FieldManifestService manifests;
    private final org.springframework.beans.factory.ObjectProvider<com.ingot.framework.cache.spi.LayeredCache<String, FrozenVersions>> cache;

    /**
     * 对精确操作求值；写操作使用最新授权事实。
     * @param key 注册资源
     * @param actor 可信身份
     * @param action 实际读写操作
     * @return 本操作字段授权
     */
    public FieldPolicyDecision evaluate(ResourceKey key, AuthorizationContext actor, IamAction action) {
        return evaluateAll(key, actor, List.of(action)).get(action);
    }

    /**
     * 同一次授权视图批量计算精确读写操作，列表逐行只匹配已加载条款。
     * @param key 注册资源
     * @param actor 可信身份
     * @param actions 精确操作集合
     * @return 操作字段授权
     */
    public Map<IamAction, FieldPolicyDecision> evaluateAll(ResourceKey key, AuthorizationContext actor,
            List<IamAction> actions) {
        var view = evaluator.evaluateForExecution(actor,
                actions.stream().anyMatch(action -> action.getOperation().isMutating()));
        var values = evaluate(key, actor, view, actions.stream().map(IamAction::getCode).toList(),
                actions.stream().anyMatch(action -> action.getOperation().isMutating()));
        Map<IamAction, FieldPolicyDecision> result = new java.util.EnumMap<>(IamAction.class);
        actions.forEach(action -> result.put(action, values.get(action.getCode())));
        return Map.copyOf(result);
    }

    /** 批量计算界面预览，操作包含写类别也不触发执行路径的 fresh 读取。 */
    public Map<IamAction, FieldPolicyDecision> previewAll(ResourceKey key, AuthorizationContext actor, List<IamAction> actions) {
        var view = evaluator.evaluate(actor);
        var values = evaluate(key, actor, view, actions.stream().map(IamAction::getCode).toList(), false);
        Map<IamAction, FieldPolicyDecision> result = new java.util.EnumMap<>(IamAction.class);
        actions.forEach(action -> result.put(action, values.get(action.getCode())));
        return Map.copyOf(result);
    }

    /**
     * 固化本次角色定义的全部可配置字段，拒绝未知资源及能力越界。
     * @param kind 角色种类
     * @param grants 完整操作授权
     * @param input 可空草稿
     * @return 固定版本快照；其他管理域为空对象
     */
    public Map<String, ResourceFieldDefinition> freeze(RoleKind kind, List<ActionGrant> grants,
            Map<String, ResourceFieldDefinition> input) {
        if (kind != RoleKind.PLATFORM_CUSTOM) {
            if (input != null && !input.isEmpty())
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            return Map.of();
        }
        var ids = metadata.resourceIds(grants);
        var descriptors = metadata.load(ids);
        if (input != null && !ids.stream()
            .map(BigInteger::toString)
            .collect(java.util.stream.Collectors.toSet())
            .containsAll(input.keySet()))
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        Map<String, ResourceFieldDefinition> result = new LinkedHashMap<>();
        for (var id : ids) {
            String key = id.toString();
            var descriptor = descriptors.get(key);
            var selected = input == null ? ResourceFieldDefinition.EMPTY : input.getOrDefault(key, ResourceFieldDefinition.EMPTY);
            if (descriptor == null) {
                if ((!selected.visibility().isEmpty() || !selected.operations().isEmpty()))
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                continue;
            }
            if (!descriptor.defaults().keySet().containsAll(selected.visibility().keySet())
                    || !descriptor.defaults().keySet().containsAll(selected.operations().keySet()))
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            Map<String, FieldVisibility> visibility = new LinkedHashMap<>();
            Map<String, FieldOperations> operations = new LinkedHashMap<>();
            for (var field : descriptor.fields()) {
                var value = selected.visibility().getOrDefault(field.key(), descriptor.defaults().get(field.key()).visibility());
                var ability = selected.operations().getOrDefault(field.key(),
                        new FieldOperations(descriptor.defaults().get(field.key()).editable(), field.filterable()));
                if (value == null || !field.visibilities().contains(value)
                        || ability.editable() && (!field.editable() || value == FieldVisibility.HIDDEN)
                        || ability.filterable() && (!field.filterable() || value != FieldVisibility.FULL))
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                visibility.put(field.key(), value);
                operations.put(field.key(), ability);
            }
            if (!visibility.isEmpty())
                result.put(key, new ResourceFieldDefinition(visibility, operations));
        }
        return Map.copyOf(result);
    }

    /**
     * 读取固定版本字段快照；缺失快照拒绝求值。
     * @param json 数据库 JSON
     * @return 字段快照
     */
    public static Map<String, ResourceFieldDefinition> snapshot(String json) {
        if (json == null || json.isBlank() || "null".equals(json.trim()))
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        return IamJson.read(json, SNAPSHOT);
    }

    /**
     * 在一次求值内批量读取版本，逐操作保留各来源范围而不拼接权限。
     * @param key 完整资源键
     * @param actor 可信身份
     * @param view 同次授权视图
     * @param actionCodes 精确操作
     * @return 按操作区分的字段结论
     */
    public Map<String, FieldPolicyDecision> evaluate(ResourceKey key, AuthorizationContext actor,
            AuthorizationView view, Collection<String> actionCodes) {
        return evaluate(key, actor, view, actionCodes, false);
    }

    /** 执行端显式选择服务器确定的 fresh 写模式，不能由浏览器指定。 */
    public Map<String, FieldPolicyDecision> evaluate(ResourceKey key, AuthorizationContext actor,
            AuthorizationView view, Collection<String> actionCodes, boolean fresh) {
        var entry = fresh ? metadata.requireFresh(key, view.platformAdministrator()) : metadata.require(key, view.platformAdministrator());
        var descriptor = entry.descriptor();
        var sources = view.fieldSources().stream().filter(source -> actionCodes.contains(source.actionCode())).toList();
        String versionKey = IamJson.array(sources.stream().map(source -> BigInteger.valueOf(source.revisionId())).distinct().sorted().toList());
        var configured = cache.getIfAvailable();
        var versions = (configured == null ? loadVersions(versionKey) : configured.get(versionKey)).values();
        var manifest = manifests.require(key);
        Map<String, FieldAccess> ceilings = new LinkedHashMap<>();
        Map<String, FieldAccess> hidden = new LinkedHashMap<>();
        for (var field : descriptor.fields()) {
            var visibility = field.visibilities()
                .stream()
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(FieldVisibility.HIDDEN);
            ceilings.put(field.key(),
                    new FieldAccess(visibility, visibility != FieldVisibility.HIDDEN && field.editable()));
            hidden.put(field.key(), HIDDEN);
        }
        Map<String, MaskSpec> masks = new LinkedHashMap<>();
        descriptor.fields().stream().filter(field -> field.mask() != null).forEach(field -> masks.put(field.key(), field.mask()));
        Map<String, FieldPolicyDecision> result = new LinkedHashMap<>();
        for (var action : actionCodes) {
            var writable = manifest.bindings().stream().filter(binding -> action.equals(binding.actionCode())
                    && binding.use() == com.ingot.framework.commons.annotation.field.FieldUse.WRITE)
                    .map(com.ingot.framework.commons.model.iam.extension.FieldBindingManifest.Binding::fieldKey).collect(java.util.stream.Collectors.toSet());
            var filterable = manifest.bindings().stream().filter(binding -> action.equals(binding.actionCode())
                    && binding.use() == com.ingot.framework.commons.annotation.field.FieldUse.FILTER)
                    .map(com.ingot.framework.commons.model.iam.extension.FieldBindingManifest.Binding::fieldKey).collect(java.util.stream.Collectors.toSet());
            Map<String, FieldAccess> actionCeilings = new LinkedHashMap<>();
            ceilings.forEach((field, upper) -> actionCeilings.put(field, new FieldAccess(upper.visibility(), upper.editable() && writable.contains(field))));
            List<ResolvedFieldRule> rules = new ArrayList<>();
            Map<String, FieldOperations> operations = new LinkedHashMap<>();
            if (view.platformAdministrator() && view.actionCodes().contains(action)) {
                var scope = compiler.compile(actor, List.of(com.ingot.cloud.iam.evaluation.ScopeClause.universe()));
                descriptor.fields().forEach(field -> {
                    rules.add(new ResolvedFieldRule(field.key(), scope, new FieldAccess(FieldVisibility.FULL, field.editable())));
                    operations.put(field.key(), new FieldOperations(field.editable() && writable.contains(field.key()), field.filterable() && filterable.contains(field.key())));
                });
            }
            for (var source : sources) {
                if (!source.actionCode().equals(action) || source.clauses().isEmpty())
                    continue;
                if (!versions.containsKey(source.revisionId()))
                    throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
                var version = versions.get(source.revisionId());
                var fields = version.getOrDefault(entry.id(), ResourceFieldDefinition.EMPTY);
                var scope = compiler.compile(actor, source.clauses());
                for (var field : descriptor.fields()) {
                    var visibility = fields.visibility().getOrDefault(field.key(), FieldVisibility.HIDDEN);
                    var ability = fields.operations().getOrDefault(field.key(), FieldOperations.NONE);
                    rules.add(new ResolvedFieldRule(field.key(), scope,
                            new FieldAccess(visibility, ability.editable() && field.editable() && visibility != FieldVisibility.HIDDEN)));
                    var previous = operations.getOrDefault(field.key(), FieldOperations.NONE);
                    operations.put(field.key(), new FieldOperations(previous.editable() || ability.editable() && field.editable() && writable.contains(field.key()),
                            previous.filterable() || ability.filterable() && field.filterable() && filterable.contains(field.key())));
                }
            }
            var policy = new FieldPolicyDecision(hidden, actionCeilings, rules, operations, masks, FieldMergeMode.GRANTS, entry.expiresAt());
            var queryScope = compiler.compile(actor, view.scope(action).clauses());
            Map<String, FieldOperations> proven = new LinkedHashMap<>();
            operations.forEach((fieldKey, ability) -> {
                boolean filter = ability.filterable();
                if (filter) {
                    try { com.ingot.framework.authorization.FieldPolicyProcessor.requireOriginalLookup(policy, fieldKey, queryScope); }
                    catch (com.ingot.framework.authorization.SdkAuthorizationException denied) { filter = false; }
                }
                proven.put(fieldKey, new FieldOperations(ability.editable(), filter));
            });
            result.put(action, new FieldPolicyDecision(hidden, actionCeilings, rules, proven, masks, FieldMergeMode.GRANTS, entry.expiresAt()));
        }
        return Map.copyOf(result);
    }
    /** 固定角色版本只批量加载一次，解析产物不包含用户、原值或编译句柄。 */
    public FrozenVersions loadVersions(String key) {
        List<BigInteger> ids = IamJson.read(key, new TypeReference<List<BigInteger>>() { });
        if (ids == null) throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        Map<Long, Map<String, ResourceFieldDefinition>> values = new LinkedHashMap<>();
        roles.findRevisions(ids).forEach(row -> values.put(row.getId().longValueExact(), snapshot(row.getResourceFieldPermissions())));
        if (values.size() != ids.size()) throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        return new FrozenVersions(values);
    }

    /**
     * <p>不可变固定版本集合，可进入 L2；目标范围编译结果只留在请求内。</p>
     * @param values 版本 ID 到资源字段定义
     * @author jy
     * @since 1.0.0
     */
    public record FrozenVersions(Map<Long, Map<String, ResourceFieldDefinition>> values) {
        /** 防御复制两层索引。 */
        public FrozenVersions { values = values.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue()))); }
    }

}
