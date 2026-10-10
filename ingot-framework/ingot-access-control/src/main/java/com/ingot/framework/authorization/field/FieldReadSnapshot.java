package com.ingot.framework.authorization.field;

import java.util.Map;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>显式传递给响应、导出和异步任务的字段快照；投影期间不查询外部依赖。</p>
 * @param access 已按实际对象计算的访问权限
 * @param masks 同一次配置版本的脱敏规则
 * @author jy
 * @since 1.0.0
 */
public record FieldReadSnapshot(Map<ResourceKey, Map<String, FieldAccess>> access,
        Map<ResourceKey, Map<String, MaskSpec>> masks) {
    /** 复制两层索引。 */
    public FieldReadSnapshot {
        access = access.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
        masks = masks.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    }
}
