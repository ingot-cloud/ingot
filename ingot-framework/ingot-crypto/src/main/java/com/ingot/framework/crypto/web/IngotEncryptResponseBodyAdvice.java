package com.ingot.framework.crypto.web;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.crypto.IngotCryptoProperties;
import com.ingot.framework.crypto.annotation.IngotEncrypt;
import com.ingot.framework.crypto.model.CryptoErrorCode;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : IngotEncryptResponseBodyAdvice.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 2:29 PM.</p>
 */
@Slf4j
@Order(1)
@ControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class IngotEncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    private final IngotCryptoProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(@NonNull MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return AnnotationUtil.hasAnnotation(returnType.getMethod(), IngotEncrypt.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {
        CryptoInfoRecord record = Utils.getDecryptInfo(returnType);
        if (record == null) {
            Utils.throwError(CryptoErrorCode.CRYPTO_CONFIG);
        }

        byte[] bodyJsonBytes;
        try {
            bodyJsonBytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // body中需要加密数据的key, 默认key为data {"data":"base64加密字符串"}
        String bodyJsonKey = properties.getBodyKey();
        // 如果key不存在，那么直接返回对body进行加密的结果
        if (StrUtil.isBlank(bodyJsonKey)) {
            return Utils.encrypt(bodyJsonBytes, record);
        }
        // 包装返回对象，使用指定key映射加密数据
        Map<String, Object> data = new HashMap<>(2);
        data.put(bodyJsonKey, Utils.encrypt(bodyJsonBytes, record));
        return data;
    }
}
