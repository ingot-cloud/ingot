package com.ingot.framework.authorization.field;

import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.ingot.framework.authorization.SdkAuthorizationException;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>在反序列化之前收集实际 JSON 键，保留缺键与显式 null 的区别。</p>
 * @author jy
 * @since 1.0.0
 */
public final class FieldInputCollector {
    private FieldInputCollector() {
    }

    /**
     * 校验所有输入键并按资源和逻辑键归组；公开元数据不成为可写字段；嵌套单对象递归验证，批量命令逐对象调用。
     * @param input 原始对象 JSON
     * @param plan 已注册 WRITE 或 FILTER 计划
     * @return 实际提交的逻辑字段，null 节点表示显式清空
     */
    public static Map<ResourceKey, Map<String, JsonNode>> collect(JsonNode input, FieldBindingRegistry.Plan plan) {
        if (input == null || !input.isObject() || plan.use() == FieldUse.READ)
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        Map<ResourceKey, Map<String, JsonNode>> result = new LinkedHashMap<>();
        input.fields().forEachRemaining(entry -> {
            var property = plan.properties().get(entry.getKey());
            if (property == null)
                throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
            if (property.nestedType() != null) {
                var child = plan.children().get(entry.getKey());
                if (child == null || !entry.getValue().isObject())
                    throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
                collect(entry.getValue(), child).forEach((resource, values) -> {
                    var collected = result.computeIfAbsent(resource, ignored -> new LinkedHashMap<>());
                    values.forEach((field, value) -> {
                        if (collected.putIfAbsent(field, value) != null)
                            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
                    });
                });
            }
            if (property.fieldKey() == null)
                return;
            if (!property.uses().contains(plan.use()))
                throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
            result.computeIfAbsent(property.resource(), ignored -> new LinkedHashMap<>())
                    .compute(property.fieldKey(), (key, existing) -> {
                        if (existing != null) throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
                        return entry.getValue().deepCopy();
                    });
        });
        Map<ResourceKey, Map<String, JsonNode>> copy = new LinkedHashMap<>();
        result.forEach((key, value) -> copy.put(key, Collections.unmodifiableMap(value)));
        return Collections.unmodifiableMap(copy);
    }
}
