package com.ingot.cloud.iam.extension;

import com.ingot.cloud.iam.policy.FieldPolicyCacheConfiguration;
import com.ingot.cloud.iam.policy.FieldDefaultReference;
import com.ingot.framework.authorization.field.FieldBindingRegistry;
import com.ingot.framework.cache.spi.LayeredCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * <p>仅预热本节点注册资源与公共默认引用；失败保持请求拒绝并允许后续读取重试。</p>
 * @author jy
 * @since 1.0.0
 */
@Component @RequiredArgsConstructor @Slf4j
public class FieldCachePrewarmer {
    private final FieldBindingRegistry bindings;
    private final ResourceFieldMetadata metadata;
    private final LayeredCache<String, FieldDefaultReference> defaults;

    /** 应用已完成接口清单注册后预热，不全量遍历租户或用户。 */
    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        for (var resource : bindings.resources()) {
            try { metadata.require(resource, false); }
            catch (RuntimeException failure) { log.warn("字段目录预热失败，受控请求保持拒绝: {}", resource, failure); }
        }
        try { defaults.get(FieldPolicyCacheConfiguration.LATEST); }
        catch (RuntimeException failure) { log.warn("公共字段默认策略预热失败，后续读取可重试", failure); }
    }
}
