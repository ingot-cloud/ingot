package com.ingot.framework.crypto.web;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.crypto.InCryptoProperties;
import com.ingot.framework.crypto.annotation.InDecrypt;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;

/**
 * <p>Description  : DecryptParamResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:56 AM.</p>
 */
@RequiredArgsConstructor
public class InDecryptParamResolver implements HandlerMethodArgumentResolver {
    private final InCryptoProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AnnotatedElementUtils.hasAnnotation(parameter.getParameter(), InDecrypt.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter methodParameter,
                                  ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        Parameter parameter = methodParameter.getParameter();
        InDecrypt decrypt = AnnotatedElementUtils.getMergedAnnotation(parameter, InDecrypt.class);
        String text = webRequest.getParameter(properties.getParamKey());
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] decryptData = CryptoUtils.decrypt(textBytes, new CryptoInfoRecord(decrypt.value(), decrypt.secretKey()));

        return objectMapper.readValue(decryptData, parameter.getType());
    }
}
