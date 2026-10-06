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

    private static final TypeReference<Map<String, Map<String, FieldAccess>>> SNAPSHOT = new TypeReference<>() {
    };

    private final ResourceFieldMetadata metadata;

    private final RoleRepository roles;

    private final ScopeTransportCompiler compiler;

    private final AuthorizationEvaluator evaluator;

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
        var values = evaluate(key, actor, view, actions.stream().map(IamAction::getCode).toList());
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
    public Map<String, Map<String, FieldAccess>> freeze(RoleKind kind, List<ActionGrant> grants,
            Map<String, Map<String, FieldAccess>> input) {
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
        Map<String, Map<String, FieldAccess>> result = new LinkedHashMap<>();
        for (var id : ids) {
            String key = id.toString();
            var descriptor = descriptors.get(key);
            var selected = input == null ? Map.<String, FieldAccess>of() : input.getOrDefault(key, Map.of());
            if (descriptor == null) {
                if (!selected.isEmpty())
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                continue;
            }
            if (!descriptor.defaults().keySet().containsAll(selected.keySet()))
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            Map<String, FieldAccess> fields = new LinkedHashMap<>();
            for (var field : descriptor.fields()) {
                var value = selected.getOrDefault(field.key(), descriptor.defaults().get(field.key()));
                if (value == null || !field.visibilities().contains(value.visibility())
                        || value.editable() && (!field.editable() || value.visibility() != FieldVisibility.FULL))
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                fields.put(field.key(), value);
            }
            if (!fields.isEmpty())
                result.put(key, Map.copyOf(fields));
        }
        return Map.copyOf(result);
    }

    /**
     * 读取固定版本字段快照；缺失快照拒绝求值。
     * @param json 数据库 JSON
     * @return 字段快照
     */
    public static Map<String, Map<String, FieldAccess>> snapshot(String json) {
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
        var entry = metadata.require(key);
        var descriptor = entry.descriptor();
        Map<Long, Map<String, Map<String, FieldAccess>>> versions = new HashMap<>();
        var sources = view.fieldSources().stream().filter(s -> actionCodes.contains(s.actionCode())).toList();
        roles.findRevisions(sources.stream().map(s -> BigInteger.valueOf(s.revisionId())).distinct().toList())
            .forEach(row -> versions.put(row.getId().longValue(), snapshot(row.getResourceFieldPermissions())));
        Map<String, FieldAccess> ceilings = new LinkedHashMap<>();
        Map<String, FieldAccess> hidden = new LinkedHashMap<>();
        for (var field : descriptor.fields()) {
            var visibility = field.visibilities()
                .stream()
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(FieldVisibility.HIDDEN);
            ceilings.put(field.key(),
                    new FieldAccess(visibility, visibility == FieldVisibility.FULL && field.editable()));
            hidden.put(field.key(), HIDDEN);
        }
        var filterable = descriptor.fields()
            .stream()
            .filter(FieldCapability::filterable)
            .map(FieldCapability::key)
            .collect(java.util.stream.Collectors.toSet());
        var sortable = descriptor.fields()
            .stream()
            .filter(FieldCapability::sortable)
            .map(FieldCapability::key)
            .collect(java.util.stream.Collectors.toSet());
        Map<String, FieldPolicyDecision> result = new LinkedHashMap<>();
        for (var action : actionCodes) {
            List<ResolvedFieldRule> rules = new ArrayList<>();
            for (var source : sources) {
                if (!source.actionCode().equals(action) || source.clauses().isEmpty())
                    continue;
                if (!versions.containsKey(source.revisionId()))
                    throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
                var version = versions.get(source.revisionId());
                var fields = version.getOrDefault(entry.id(), Map.of());
                var scope = compiler.compile(actor, source.clauses());
                for (var field : descriptor.fields())
                    rules.add(new ResolvedFieldRule(field.key(), scope, fields.getOrDefault(field.key(), HIDDEN)));
            }
            result.put(action,
                    new FieldPolicyDecision(hidden, ceilings, rules, filterable, sortable, FieldMergeMode.GRANTS));
        }
        return Map.copyOf(result);
    }

}
