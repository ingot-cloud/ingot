package com.ingot.framework.authorization.field;

import com.ingot.framework.commons.annotation.field.FieldControl;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * <p>在 HTTP 路由初始化完成后编译所有显式字段接入点，冲突阻止应用启动。</p>
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class FieldBindingRegistrar implements SmartInitializingSingleton {
    private final RequestMappingHandlerMapping handlers;
    private final FieldBindingRegistry registry;

    @Override
    public void afterSingletonsInstantiated() {
        handlers.getHandlerMethods().values().forEach(handler -> {
            var controls = AnnotatedElementUtils.getMergedRepeatableAnnotations(handler.getMethod(), FieldControl.class);
            if (controls.isEmpty())
                controls = AnnotatedElementUtils.getMergedRepeatableAnnotations(handler.getBeanType(), FieldControl.class);
            for (var control : controls)
                registry.register(control.valueType(), new ResourceKey(control.domain(), control.applicationCode(),
                        control.resourceCode()), control.action(), control.use());
        });
    }
}
