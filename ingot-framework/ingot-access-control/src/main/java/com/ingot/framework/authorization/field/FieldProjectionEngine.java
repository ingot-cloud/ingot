package com.ingot.framework.authorization.field;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import lombok.RequiredArgsConstructor;

/**
 * <p>按已编译 JSON 绑定投影不可变 DTO，不修改实体、共享对象或 record。</p>
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class FieldProjectionEngine {
    private final ObjectMapper mapper;
    private final FieldBindingRegistry registry;
    private final MaskStrategy masks;

    /**
     * 从保留实际提交键的 DTO 收集输入，适用于具有自定义 OSS 或 JSON 反序列化器的接口。
     * @param value 已绑定输入
     * @param supplied 实际请求键，包含未知键和显式 null
     * @param resource 服务声明资源
     * @return 按资源和逻辑键索引的提交值
     */
    public Map<ResourceKey, Map<String, JsonNode>> submitted(Object value, Set<String> supplied, ResourceKey resource) {
        ObjectNode bound = mapper.valueToTree(value);
        ObjectNode actual = mapper.createObjectNode();
        supplied.forEach(name -> actual.set(name, bound.has(name) ? bound.get(name) : mapper.nullNode()));
        return FieldInputCollector.collect(actual, registry.require(value.getClass(), resource, FieldUse.WRITE));
    }

    /**
     * 创建安全 record 副本，保留 Instant 和 OSS 原始类型，最终 Jackson 写出仍使用业务 mixin。
     * @param <T> 不可变 record 类型
     * @param value 原始 DTO
     * @param resource 接口资源
     * @param snapshot 请求内预计算快照
     * @return 安全新实例
     */
    @SuppressWarnings("unchecked")
    public <T> T projectRecord(T value, ResourceKey resource, FieldReadSnapshot snapshot) {
        var plan = registry.require(value.getClass(), resource, FieldUse.READ);
        if (plan.recordLayout() == null)
            throw new IllegalArgumentException("类型不是 record，请使用 JSON 投影入口");
        Map<String, FieldBindingRegistry.Property> properties = new java.util.HashMap<>();
        plan.properties().values().forEach(property -> properties.put(property.javaName(), property));
        List<Object> args = new ArrayList<>();
        try {
            for (int index = 0; index < plan.recordLayout().names().size(); index++) {
                Object raw = plan.recordLayout().readers().get(index).invoke(value);
                var property = properties.get(plan.recordLayout().names().get(index));
                // 忽略的 record 组件不参与输出，仍保留对象的内部元数据。
                if (property == null || property.fieldKey() == null) {
                    args.add(property != null && property.nestedType() != null ? nested(raw, property.resource(), snapshot) : raw);
                    continue;
                }
                var access = snapshot.access().getOrDefault(property.resource(), Map.of()).get(property.fieldKey());
                if (!property.uses().contains(FieldUse.READ) || access == null || access.visibility() == FieldVisibility.HIDDEN)
                    args.add(null);
                else if (access.visibility() == FieldVisibility.MASKED && raw != null) {
                    if (!(raw instanceof String text))
                        throw new IllegalArgumentException("非文本字段不能脱敏: " + property.fieldKey());
                    args.add(masks.mask(text, snapshot.masks().getOrDefault(property.resource(), Map.of()).get(property.fieldKey())));
                }
                else args.add(raw);
            }
            return (T) plan.recordLayout().constructor().invokeWithArguments(args);
        }
        catch (RuntimeException exception) { throw exception; }
        catch (Throwable exception) { throw new IllegalStateException("无法构造安全 record 投影", exception); }
    }

    private Object nested(Object value, ResourceKey resource, FieldReadSnapshot snapshot) {
        if (value == null) return null;
        if (value instanceof java.util.Collection<?> items)
            return items.stream().map(item -> nested(item, resource, snapshot)).toList();
        if (value instanceof Map<?, ?> items) {
            Map<Object, Object> output = new java.util.LinkedHashMap<>();
            items.forEach((key, item) -> output.put(key, nested(item, resource, snapshot)));
            return java.util.Collections.unmodifiableMap(output);
        }
        return value.getClass().isRecord() ? projectRecord(value, resource, snapshot) : project(value, resource, snapshot);
    }

    private Object nestedJson(Object value, ResourceKey resource, FieldReadSnapshot snapshot) {
        if (value == null) return null;
        if (value instanceof java.util.Collection<?> items) return items.stream().map(item -> nestedJson(item, resource, snapshot)).toList();
        if (value instanceof Map<?, ?> items) {
            Map<Object, Object> output = new java.util.LinkedHashMap<>();
            items.forEach((key, item) -> output.put(key, nestedJson(item, resource, snapshot)));
            return output;
        }
        return project(value, resource, snapshot);
    }

    /**
     * 投影一批对象；快照提供者只读取请求内已加载索引，禁止在此阶段查询数据库或 RPC。
     * @param <T> DTO 类型
     * @param values 已加载批次
     * @param resource 接口资源
     * @param snapshots 显式行级快照
     * @return 同顺序的安全 JSON 对象
     */
    public <T> List<ObjectNode> projectBatch(List<T> values, ResourceKey resource, Function<T, FieldReadSnapshot> snapshots) {
        return values.stream().map(value -> project(value, resource, snapshots.apply(value))).toList();
    }

    /**
     * 在写出响应属性之前省略隐藏值；Java 类型保持不可变。
     * @param value 已加载 DTO
     * @param resource 可信接口资源
     * @param snapshot 请求内已经计算完成的权限和配置
     * @return 安全 JSON 对象
     */
    public ObjectNode project(Object value, ResourceKey resource, FieldReadSnapshot snapshot) {
        var plan = registry.require(value.getClass(), resource, FieldUse.READ);
        ObjectNode output = mapper.valueToTree(value);
        if (output == null)
            throw new IllegalArgumentException("字段 DTO 必须序列化为对象");
        output.fieldNames().forEachRemaining(name -> {
            if (!plan.properties().containsKey(name))
                throw new IllegalStateException("DTO 包含未分类的运行时属性: " + name);
        });
        plan.properties().forEach((name, property) -> {
            if (!output.has(name)) return;
            if (property.nestedType() != null) {
                output.set(name, mapper.valueToTree(nestedJson(property.accessor().getValue(value), property.resource(), snapshot)));
            }
            if (property.fieldKey() == null) return;
            var access = snapshot.access().getOrDefault(property.resource(), Map.of()).get(property.fieldKey());
            if (!property.uses().contains(FieldUse.READ) || access == null
                    || access.visibility() == FieldVisibility.HIDDEN) {
                output.remove(name);
                return;
            }
            if (access.visibility() == FieldVisibility.MASKED && !output.get(name).isNull()) {
                if (!output.get(name).isTextual())
                    throw new IllegalStateException("非文本字段不能配置脱敏: " + property.fieldKey());
                var spec = snapshot.masks().getOrDefault(property.resource(), Map.of()).get(property.fieldKey());
                output.put(name, masks.mask(output.get(name).textValue(), spec));
            }
        });
        return output;
    }
}
