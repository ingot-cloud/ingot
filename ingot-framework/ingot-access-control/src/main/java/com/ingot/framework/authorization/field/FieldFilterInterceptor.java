package com.ingot.framework.authorization.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * <p>GET 筛选按真实 JSON/参数名称检查分类，权限由业务在实际查询范围确定后统一检查。</p>
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class FieldFilterInterceptor implements HandlerInterceptor {
    private final ObjectMapper mapper;
    private final FieldBindingRegistry fields;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        var declarations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method.getMethod(), FieldControl.class);
        if (declarations.isEmpty()) declarations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method.getBeanType(), FieldControl.class);
        for (var declaration : declarations) {
            if (declaration.use() != FieldUse.FILTER) continue;
            var actual = mapper.createObjectNode();
            request.getParameterMap().forEach((name, values) -> {
                if (values.length == 1) actual.put(name, values[0]);
                else actual.set(name, mapper.valueToTree(values));
            });
            var resource = new ResourceKey(declaration.domain(), declaration.applicationCode(), declaration.resourceCode());
            FieldInputCollector.collect(actual, fields.require(declaration.valueType(), resource, FieldUse.FILTER));
        }
        return true;
    }
}
