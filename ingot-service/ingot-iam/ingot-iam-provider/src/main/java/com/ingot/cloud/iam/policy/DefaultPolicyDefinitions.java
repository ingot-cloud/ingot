package com.ingot.cloud.iam.policy;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.MemberFieldKey;

/**
 * <p>解释固定默认策略版本的 JSON 定义体，空对象回落到全组织可见与手机邮箱脱敏。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DefaultPolicyDefinitions {
    private static final TypeReference<DirectoryBody> DIRECTORY = new TypeReference<>() {
    };
    private static final TypeReference<FieldBody> FIELD = new TypeReference<>() {
    };

    private DefaultPolicyDefinitions() {
    }

    /**
     * 解析通讯录默认范围；缺省或空对象表示全组织。
     *
     * @param definition 版本定义 JSON；可空
     * @return 默认范围
     */
    public static DirectoryDefaultScope directoryScope(String definition) {
        DirectoryBody body = IamJson.read(definition, DIRECTORY);
        return body == null || body.scope() == null ? DirectoryDefaultScope.ALL : body.scope();
    }

    /**
     * 解析字段默认访问；缺省键回落到手机邮箱脱敏、其余完整可编辑。
     *
     * @param definition 版本定义 JSON；可空
     * @return 按字段键索引的访问
     */
    public static Map<String, FieldAccess> fieldAccess(String definition) {
        return fieldMap(definition, false);
    }

    /**
     * 解析平台字段上限；缺省表示不额外收紧，租户规则最高可到完整可见。
     *
     * @param definition 最新平台版本 JSON；可空
     * @return 按字段键索引的上限
     */
    public static Map<String, FieldAccess> fieldCeiling(String definition) {
        return fieldMap(definition, true);
    }

    /**
     * 读取单个字段的文档化基线，供定义体缺键时使用。
     *
     * @param fieldKey 字段键
     * @return 手机邮箱脱敏且不可编辑，其余完整可编辑
     */
    public static FieldAccess documented(String fieldKey) {
        if (MemberFieldKey.VALUE_PHONE.equals(fieldKey) || MemberFieldKey.VALUE_EMAIL.equals(fieldKey)) {
            return new FieldAccess(FieldVisibility.MASKED, false);
        }
        return new FieldAccess(FieldVisibility.FULL, true);
    }

    /**
     * 平台未声明上限时的最宽可见性，允许租户规则授予完整字段。
     *
     * @param fieldKey 字段键
     * @return 完整可见且可编辑
     */
    public static FieldAccess openCeiling(String fieldKey) {
        return new FieldAccess(FieldVisibility.FULL, true);
    }

    /**
     * 取更严格的可见性，可编辑性取逻辑与；非完整可见时不可编辑。
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
            return documented(MemberFieldKey.VALUE_DISPLAY_NAME);
        }
        if (access.visibility() != FieldVisibility.FULL) {
            return new FieldAccess(access.visibility(), false);
        }
        return access;
    }

    private static Map<String, FieldAccess> fieldMap(String definition, boolean ceiling) {
        FieldBody body = IamJson.read(definition, FIELD);
        Map<String, FieldAccess> configured = ceiling
                ? body == null || body.ceiling() == null ? Map.of() : body.ceiling()
                : body == null || body.fields() == null ? Map.of() : body.fields();
        Map<String, FieldAccess> access = new LinkedHashMap<>();
        for (MemberFieldKey field : MemberFieldKey.values()) {
            FieldAccess item = configured.get(field.getValue());
            FieldAccess fallback = ceiling ? openCeiling(field.getValue()) : documented(field.getValue());
            access.put(field.getValue(), item == null ? fallback : normalize(item));
        }
        return Map.copyOf(access);
    }

    private record DirectoryBody(DirectoryDefaultScope scope) {
    }

    private record FieldBody(Map<String, FieldAccess> fields, Map<String, FieldAccess> ceiling) {
    }
}
