package com.ingot.framework.cache.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>进程内所有分层缓存实例的注册表，由 {@code LayeredCacheBuilder} 在构建时自动登记。</p>
 *
 * <p>存在的目的是让新增缓存无需再写一个专属 Actuator 端点：注册后即可在统一汇总端点中看到其层次开关
 * 与当前降级态。各模块原有的专属端点仍然保留，两者互不冲突。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheDescriptor
 */
public class LayeredCacheRegistry {

    private final Map<String, LayeredCacheDescriptor> descriptors = new ConcurrentHashMap<>();

    /**
     * 登记一个缓存实例；同名重复登记以最后一次为准。
     *
     * @param descriptor 装配画像
     */
    public void register(LayeredCacheDescriptor descriptor) {
        if (descriptor != null && descriptor.name() != null) {
            descriptors.put(descriptor.name(), descriptor);
        }
    }

    /**
     * 已登记的全部缓存实例。
     *
     * @return 只读视图
     */
    public Collection<LayeredCacheDescriptor> all() {
        return Collections.unmodifiableCollection(descriptors.values());
    }

    /**
     * 按名查找。
     *
     * @param name 缓存实例名
     * @return 画像；未登记时为 {@code null}
     */
    public LayeredCacheDescriptor find(String name) {
        return descriptors.get(name);
    }
}
