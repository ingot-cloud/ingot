package com.ingot.framework.commons.model.iam;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * 复制角色固定版本的资源字段快照，未声明字段不授予权限。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ResourceFieldPermissions {

    private ResourceFieldPermissions() {
    }

    /**
     * 深复制字段快照；空输入规范化为未声明字段的空对象。
     * @param source 资源 ID 到字段权限
     * @return 不可变快照
     */
    public static Map<String, Map<String, FieldAccess>> copy(Map<String, Map<String, FieldAccess>> source) {
        if (source == null)
            return Map.of();
        Map<String, Map<String, FieldAccess>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, Map.copyOf(value)));
        return Map.copyOf(result);
    }

}
