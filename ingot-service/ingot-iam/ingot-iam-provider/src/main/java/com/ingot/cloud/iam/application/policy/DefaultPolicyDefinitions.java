package com.ingot.cloud.iam.application.policy;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.FieldOperations;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;

/**
 * <p>解释某一资源的固定默认字段策略版本。缺键回落到该资源自己的字段默认值和能力上限。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DefaultPolicyDefinitions {
    private static final TypeReference<FieldBody> FIELD = new TypeReference<>() {
    };

    private DefaultPolicyDefinitions() {
    }

    /**
     * 用资源字段默认值生成第 1 版定义，不手写成员字段。
     *
     * @param resource 已注册资源
     * @return 字段与操作默认 JSON
     */
    public static String definition(ResourceDescriptor resource) {
        Map<String, FieldVisibility> fields = new LinkedHashMap<>();
        Map<String, FieldOperations> operations = new LinkedHashMap<>();
        for (FieldCapability field : resource.fields()) {
            FieldAccess access = resource.defaults().getOrDefault(field.key(), new FieldAccess(FieldVisibility.HIDDEN, false));
            fields.put(field.key(), access.visibility());
            operations.put(field.key(), new FieldOperations(access.editable(), field.filterable()));
        }
        return IamJsonCodec.object(Map.of("fields", fields, "operations", operations));
    }

    /**
     * 解析字段默认访问；缺键使用资源默认值。
     *
     * @param definition 版本定义 JSON；可空
     * @param resource 字段所属资源
     * @return 按字段键索引的访问
     */
    public static Map<String, FieldAccess> fieldAccess(String definition, ResourceDescriptor resource) {
        return fieldMap(definition, false, resource);
    }

    /**
     * 解析平台字段上限；缺省时取该字段能力中的最高可见性。
     *
     * @param definition 最新平台版本 JSON；可空
     * @param resource 字段所属资源
     * @return 按字段键索引的上限
     */
    public static Map<String, FieldAccess> fieldCeiling(String definition, ResourceDescriptor resource) {
        return fieldMap(definition, true, resource);
    }

    /**
     * 取更严格的可见性，可编辑性取逻辑与；隐藏时不可编辑。
     *
     * @param left 左侧；空则返回右侧
     * @param right 右侧；空则返回左侧
     * @return 合并结果
     */
    public static FieldAccess stricter(FieldAccess left, FieldAccess right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        FieldVisibility visibility = left.visibility().ordinal() <= right.visibility().ordinal()
                ? left.visibility() : right.visibility();
        return normalize(new FieldAccess(visibility, left.editable() && right.editable()));
    }

    static FieldAccess normalize(FieldAccess access) {
        if (access == null) {
            return new FieldAccess(FieldVisibility.HIDDEN, false);
        }
        if (access.visibility() == FieldVisibility.HIDDEN) {
            return new FieldAccess(access.visibility(), false);
        }
        return access;
    }

    private static Map<String, FieldAccess> fieldMap(String definition, boolean ceiling, ResourceDescriptor resource) {
        FieldBody body = IamJsonCodec.read(definition, FIELD);
        Map<String, FieldVisibility> configured = ceiling
                ? body == null || body.ceiling() == null ? Map.of() : body.ceiling()
                : body == null || body.fields() == null ? Map.of() : body.fields();
        Map<String, FieldAccess> access = new LinkedHashMap<>();
        for (FieldCapability field : resource.fields()) {
            FieldVisibility item = configured.get(field.key());
            FieldAccess fallback = ceiling ? openCeiling(field) : resource.defaults().getOrDefault(field.key(),
                    new FieldAccess(FieldVisibility.HIDDEN, false));
            access.put(field.key(), item == null ? fallback : new FieldAccess(item, item != FieldVisibility.HIDDEN));
        }
        return Map.copyOf(access);
    }

    /**
     * 读取固定操作默认或上限。缺省操作能力来自资源字段默认值和字段能力。
     *
     * @param definition 版本定义 JSON
     * @param ceiling 是否读取操作上限
     * @param resource 字段所属资源
     * @return 按字段键索引的操作能力
     */
    public static Map<String, FieldOperations> fieldOperations(String definition, boolean ceiling, ResourceDescriptor resource) {
        FieldBody body = IamJsonCodec.read(definition, FIELD);
        Map<String, FieldOperations> configured = body == null ? Map.of()
                : ceiling ? body.operationCeiling() : body.operations();
        configured = configured == null ? Map.of() : configured;
        Map<String, FieldOperations> result = new LinkedHashMap<>();
        for (FieldCapability field : resource.fields()) {
            FieldAccess fallbackAccess = resource.defaults().getOrDefault(field.key(), new FieldAccess(FieldVisibility.HIDDEN, false));
            var fallback = new FieldOperations(ceiling ? field.editable() : fallbackAccess.editable(),
                    ceiling ? field.filterable() : field.filterable() && fallbackAccess.visibility() == FieldVisibility.FULL);
            result.put(field.key(), configured.getOrDefault(field.key(), fallback));
        }
        return Map.copyOf(result);
    }

    private static FieldAccess openCeiling(FieldCapability field) {
        FieldVisibility visibility = field.visibilities().stream().max(java.util.Comparator.comparingInt(Enum::ordinal))
                .orElse(FieldVisibility.HIDDEN);
        return new FieldAccess(visibility, visibility != FieldVisibility.HIDDEN && field.editable());
    }

    private record FieldBody(Map<String, FieldVisibility> fields, Map<String, FieldVisibility> ceiling,
            Map<String, FieldOperations> operations, Map<String, FieldOperations> operationCeiling) {
    }
}
