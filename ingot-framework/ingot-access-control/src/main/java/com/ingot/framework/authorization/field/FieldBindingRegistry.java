package com.ingot.framework.authorization.field;

import java.lang.annotation.Annotation;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.extension.FieldBindingManifest;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>启动期编译 JSON 属性和逻辑字段映射；请求期只读取不可变索引。</p>
 * @author jy
 * @since 1.0.0
 */
public final class FieldBindingRegistry {
    private final ObjectMapper mapper;
    private final Set<PlanKey> registering = new HashSet<>();
    private final Map<PlanKey, Plan> plans = new ConcurrentHashMap<>();
    private final Map<ResourceKey, Set<FieldBindingManifest.Binding>> manifests = new ConcurrentHashMap<>();

    /** 使用业务 Jackson 配置，包括属性别名和 mixin。 */
    public FieldBindingRegistry(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    /**
     * 注册精确操作的 DTO；未分类、冲突和不完整资源声明立即失败。
     * @param type DTO 类型
     * @param resource 接口可信资源
     * @param actionCode 精确操作
     * @param use 输入或输出用途
     * @return 编译结果
     */
    public synchronized Plan register(Class<?> type, ResourceKey resource, String actionCode, FieldUse use) {
        Objects.requireNonNull(resource);
        if (actionCode == null || actionCode.isBlank())
            throw new IllegalArgumentException("字段接口必须声明精确操作");
        var key = new PlanKey(type, resource, use);
        if (!registering.add(key)) throw new IllegalArgumentException("受控 DTO 嵌套循环: " + type.getName());
        try {
        var plan = plans.computeIfAbsent(key, this::compile);
        Map<String, Plan> children = new LinkedHashMap<>();
        for (var property : plan.properties().values()) {
            if (property.nestedType() != null)
                children.put(property.jsonName(), register(property.nestedType(), property.resource(), actionCode, use));
            if (property.fieldKey() != null && property.uses().contains(use)) {
                if (use == FieldUse.READ && manifests.getOrDefault(property.resource(), Set.of()).stream().anyMatch(
                        binding -> binding.use() == FieldUse.READ && binding.fieldKey().equals(property.fieldKey()) && binding.textual() != property.textual()))
                    throw new IllegalArgumentException("同一逻辑字段的文本类型声明冲突: " + property.fieldKey());
                manifests.computeIfAbsent(property.resource(), ignored -> new LinkedHashSet<>()).add(
                        new FieldBindingManifest.Binding(actionCode, use, type.getName(), property.jsonName(),
                                property.fieldKey(), property.textual()));
            }
        }
        plan = new Plan(plan.type(), plan.resource(), plan.use(), plan.properties(), plan.recordLayout(), children);
        plans.put(key, plan);
        return plan;
        } finally { registering.remove(key); }
    }

    /** 获取已注册执行计划；请求期不扫描或隐式登记。 */
    public Plan require(Class<?> type, ResourceKey resource, FieldUse use) {
        var result = plans.get(new PlanKey(type, resource, use));
        if (result == null)
            throw new IllegalStateException("DTO 尚未注册字段接入: " + type.getName());
        return result;
    }

    /** 获取资源清单，版本取决于排序后的内容，与注册先后顺序无关。 */
    public synchronized FieldBindingManifest manifest(ResourceKey resource) {
        var bindings = manifests.getOrDefault(resource, Set.of()).stream()
                .sorted(Comparator.comparing(FieldBindingManifest.Binding::actionCode)
                        .thenComparing(b -> b.use().getValue()).thenComparing(FieldBindingManifest.Binding::valueType)
                        .thenComparing(FieldBindingManifest.Binding::property)).toList();
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return new FieldBindingManifest(resource, HexFormat.of().formatHex(
                    digest.digest(mapper.writeValueAsBytes(bindings))), bindings);
        }
        catch (java.io.IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法生成字段清单版本", exception);
        }
    }

    /** 返回本服务已登记的资源集合。 */
    public Set<ResourceKey> resources() {
        return Set.copyOf(manifests.keySet());
    }

    private Plan compile(PlanKey key) {
        var javaType = mapper.constructType(key.type());
        var bean = key.use() == FieldUse.READ ? mapper.getSerializationConfig().introspect(javaType)
                : mapper.getDeserializationConfig().introspect(javaType);
        Map<String, Property> properties = new LinkedHashMap<>();
        Set<String> inputKeys = new HashSet<>();
        for (var property : bean.findProperties()) {
            if (key.use() == FieldUse.READ && !property.couldSerialize()
                    || key.use() != FieldUse.READ && !property.couldDeserialize())
                continue;
            if (bean.getIgnoredPropertyNames() != null && bean.getIgnoredPropertyNames().contains(property.getName())) continue;
            if (key.type().isRecord()) {
                try {
                    var internal = key.type().getDeclaredField(property.getInternalName()).getAnnotation(com.fasterxml.jackson.annotation.JsonIgnore.class);
                    if (internal != null && internal.value()) continue;
                } catch (NoSuchFieldException ignoredField) { /* 合成属性继续按实际 Jackson 属性分类。 */ }
            }
            var ignored = annotation(property, com.fasterxml.jackson.annotation.JsonIgnore.class);
            if (ignored != null && ignored.value()) continue;
            var binding = annotation(property, FieldBinding.class);
            var publicField = annotation(property, PublicField.class);
            if ((binding == null) == (publicField == null))
                throw new IllegalArgumentException("属性必须唯一声明 FieldBinding 或 PublicField: "
                        + key.type().getName() + "." + property.getName());
            ResourceKey resource = key.resource();
            if (binding != null) {
                if (binding.key().isBlank() || binding.uses().length == 0
                        || binding.applicationCode().isBlank() != binding.resourceCode().isBlank())
                    throw new IllegalArgumentException("字段绑定声明不完整: " + property.getName());
                if (!binding.applicationCode().isBlank())
                    resource = new ResourceKey(binding.domain(), binding.applicationCode(), binding.resourceCode());
                if (key.use() != FieldUse.READ && Arrays.asList(binding.uses()).contains(key.use())
                        && !inputKeys.add(resource + ":" + binding.key()))
                    throw new IllegalArgumentException("同一输入不能重复绑定逻辑字段: " + binding.key());
            }
            if (key.use() == FieldUse.READ && property.getAccessor() != null)
                property.getAccessor().fixAccess(mapper.isEnabled(com.fasterxml.jackson.databind.MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            var propertyType = property.getPrimaryMember().getType();
            if (binding != null && key.use() == FieldUse.READ && propertyType.isPrimitive())
                throw new IllegalArgumentException("可隐藏字段必须使用可空类型: " + property.getName());
            var child = propertyType.isContainerType() ? propertyType.getContentType() : propertyType;
            Class<?> nested = child != null && controlled(child.getRawClass()) ? child.getRawClass() : null;
            properties.put(property.getName(), new Property(property.getName(), property.getInternalName(),
                    binding == null ? null : binding.key(), resource,
                    binding == null ? Set.of() : Set.copyOf(Arrays.asList(binding.uses())),
                    property.getPrimaryMember().getRawType() == String.class, nested, property.getAccessor()));
        }
        if (properties.isEmpty())
            throw new IllegalArgumentException("字段 DTO 不能没有可分类属性: " + key.type().getName());
        return new Plan(key.type(), key.resource(), key.use(), properties, recordLayout(key.type()));
    }

    private static boolean controlled(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.isAnnotationPresent(FieldBinding.class))
                || Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(FieldBinding.class));
    }

    /** 获取精确操作中已登记的逻辑字段，不能用其他接口能力补足。 */
    public Set<String> keys(ResourceKey resource, String action, FieldUse use) {
        return manifest(resource).bindings().stream().filter(binding -> binding.actionCode().equals(action)
                && binding.use() == use).map(FieldBindingManifest.Binding::fieldKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static RecordLayout recordLayout(Class<?> type) {
        if (!type.isRecord())
            return null;
        try {
            var components = type.getRecordComponents();
            var lookup = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
            List<MethodHandle> readers = new ArrayList<>();
            for (var component : components)
                readers.add(lookup.unreflect(component.getAccessor()));
            var constructor = type.getDeclaredConstructor(Arrays.stream(components).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new));
            return new RecordLayout(Arrays.stream(components).map(java.lang.reflect.RecordComponent::getName).toList(),
                    readers, lookup.unreflectConstructor(constructor));
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("无法编译 record 字段投影: " + type.getName(), exception);
        }
    }

    private static <A extends Annotation> A annotation(BeanPropertyDefinition property, Class<A> type) {
        A result = null;
        for (AnnotatedMember member : Arrays.asList(property.getPrimaryMember(), property.getField(),
                property.getGetter(), property.getSetter(), property.getConstructorParameter())) {
            if (member == null)
                continue;
            var value = member.getAnnotation(type);
            if (value != null && result != null && !value.equals(result))
                throw new IllegalArgumentException("属性绑定注解冲突: " + property.getName());
            if (value != null)
                result = value;
        }
        return result;
    }

    /** 类型、完整资源与用途共同组成启动注册身份。 */
    private record PlanKey(Class<?> type, ResourceKey resource, FieldUse use) {
    }

    /**
     * <p>已编译属性，JSON 名称来自实际 Jackson 配置。</p>
     * @param jsonName 实际 JSON 名称
     * @param javaName 实际 Java 属性
     * @param fieldKey 逻辑字段，公开属性为空
     * @param resource 属性资源
     * @param uses 声明用途
     * @param textual 是否文本属性
     * @param nestedType 嵌套受控类型，固定公开值为空
     * @param accessor 启动期解析的属性读取器
     * @author jy
     * @since 1.0.0
     */
    public record Property(String jsonName, String javaName, String fieldKey, ResourceKey resource, Set<FieldUse> uses, boolean textual, Class<?> nestedType, AnnotatedMember accessor) {
        /** 防御复制。 */
        public Property { uses = Set.copyOf(uses); }
    }

    /**
     * <p>DTO 不可变执行计划，不持有业务值。</p>
     * @param type DTO 类型
     * @param resource 接口资源
     * @param use 接口用途
     * @param properties 实际属性索引
     * @param recordLayout record 构造计划，普通 bean 为空
     * @param children 嵌套属性的不可变计划
     * @author jy
     * @since 1.0.0
     */
    public record Plan(Class<?> type, ResourceKey resource, FieldUse use, Map<String, Property> properties, RecordLayout recordLayout, Map<String, Plan> children) {
        /** 编译根属性后由注册流程补齐嵌套计划。 */
        public Plan(Class<?> type, ResourceKey resource, FieldUse use, Map<String, Property> properties, RecordLayout layout) {
            this(type, resource, use, properties, layout, Map.of());
        }
        /** 防御复制。 */
        public Plan { properties = Map.copyOf(properties); children = Map.copyOf(children); }
    }

    /**
     * <p>启动期编译的 record 读取及新实例构造句柄。</p>
     * @param names 组件顺序
     * @param readers 只读句柄
     * @param constructor 新实例构造，不修改原对象
     * @author jy
     * @since 1.0.0
     */
    public record RecordLayout(List<String> names, List<MethodHandle> readers, MethodHandle constructor) {
        /** 冻结布局。 */
        public RecordLayout { names = List.copyOf(names); readers = List.copyOf(readers); }
    }
}
