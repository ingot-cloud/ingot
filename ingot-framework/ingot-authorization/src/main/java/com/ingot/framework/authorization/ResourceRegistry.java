package com.ingot.framework.authorization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>完整命名空间的可信适配器注册表，重复注册启动失败。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ResourceRegistry {

    private final Map<ResourceKey, ResourceObjectProvider> providers;

    /**
     * 构造不可变注册表。
     * @param values 已装配的可信provider
     */
    public ResourceRegistry(Collection<ResourceObjectProvider> values) {
        Map<ResourceKey, ResourceObjectProvider> index = new LinkedHashMap<>();
        for (var value : values) {
            if (index.putIfAbsent(value.descriptor().key(), value) != null) {
                throw new IllegalArgumentException("重复资源注册：" + value.descriptor().key());
            }
        }
        providers = Map.copyOf(index);
    }

    /**
     * 读取可信注册描述，供目录候选和执行能力比对。
     * @return 不可变描述
     */
    public List<ResourceDescriptor> descriptors() {
        return providers.values().stream().map(ResourceObjectProvider::descriptor).toList();
    }

    /**
     * 查找完整资源。
     * @param key 完整键
     * @return provider或null
     */
    public ResourceObjectProvider find(ResourceKey key) {
        return providers.get(key);
    }

    /**
     * 强制获取已接入资源。
     * @param key 完整键
     * @return provider
     */
    public ResourceObjectProvider require(ResourceKey key) {
        var value = find(key);
        if (value == null)
            throw new SdkAuthorizationException(IamReasonCode.INVALID_ARGUMENT);
        return value;
    }

}
