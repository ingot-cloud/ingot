package com.ingot.framework.authorization.field;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.annotation.field.FieldControl;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * <p>普通 DTO 在反序列化前收集实际 JSON 键，拒绝未知/只读键；最终权限仍在写事务内检查。</p>
 * @author jy
 * @since 1.0.0
 */
@ControllerAdvice @RequiredArgsConstructor
public final class FieldInputAdvice extends RequestBodyAdviceAdapter {
    private final ObjectMapper mapper;
    private final FieldBindingRegistry fields;

    @Override
    public boolean supports(MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return declaration(parameter) != null;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage message, MethodParameter parameter, Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        var declaration = declaration(parameter);
        var bytes = message.getBody().readAllBytes();
        var resource = new ResourceKey(declaration.domain(), declaration.applicationCode(), declaration.resourceCode());
        FieldInputCollector.collect(mapper.readTree(bytes), fields.require(declaration.valueType(), resource, FieldUse.WRITE));
        return new HttpInputMessage() {
            @Override public java.io.InputStream getBody() { return new ByteArrayInputStream(bytes); }
            @Override public HttpHeaders getHeaders() { return message.getHeaders(); }
        };
    }

    private static FieldControl declaration(MethodParameter parameter) {
        if (parameter.getMethod() == null) return null;
        var declarations = AnnotatedElementUtils.getMergedRepeatableAnnotations(parameter.getMethod(), FieldControl.class);
        if (declarations.isEmpty()) declarations = AnnotatedElementUtils.getMergedRepeatableAnnotations(parameter.getContainingClass(), FieldControl.class);
        return declarations.stream()
                .filter(value -> value.use() == FieldUse.WRITE && value.valueType() == parameter.getParameterType())
                .findFirst().orElse(null);
    }
}
