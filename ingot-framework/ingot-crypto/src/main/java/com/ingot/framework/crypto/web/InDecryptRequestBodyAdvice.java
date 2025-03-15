package com.ingot.framework.crypto.web;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.crypto.InCryptoProperties;
import com.ingot.framework.crypto.annotation.InDecrypt;
import com.ingot.framework.crypto.model.CryptoErrorCode;
import com.ingot.framework.crypto.model.CryptoHttpInputMessage;
import com.ingot.framework.crypto.model.CryptoInfoRecord;
import com.ingot.framework.crypto.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * <p>Description  : IngotDecryptRequestBodyAdvice.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 11:35 AM.</p>
 */
@Slf4j
@Order(1)
@ControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class InDecryptRequestBodyAdvice implements RequestBodyAdvice {
    private final InCryptoProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(@NonNull MethodParameter methodParameter,
                            @NonNull Type targetType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return AnnotationUtil.hasAnnotation(methodParameter.getMethod(), InDecrypt.class);
    }

    @Override
    @NonNull
    public HttpInputMessage beforeBodyRead(@NonNull HttpInputMessage inputMessage,
                                           @NonNull MethodParameter methodParameter,
                                           @NonNull Type targetType,
                                           @NonNull Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        InputStream messageBody = inputMessage.getBody();
        if (messageBody.available() <= 0) {
            return inputMessage;
        }

        CryptoInfoRecord record = CryptoUtils.getDecryptInfo(methodParameter);
        if (record == null) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_CONFIG);
        }

        byte[] bodyByteArray = StreamUtils.copyToByteArray(messageBody);
        // body中加密信息的key, 默认key为data {"data":"加密后的实际请求数据"}
        String bodyCryptoKey = properties.getBodyKey();

        byte[] decryptedBody = null;
        // 如果不存在，表示请求体整体加密
        if (StrUtil.isBlank(bodyCryptoKey)) {
            decryptedBody = CryptoUtils.decrypt(bodyByteArray, record);
        } else {
            Map<String, Object> data = objectMapper.readValue(bodyByteArray, Map.class);
            String cryptoContent = (String) data.get(bodyCryptoKey);
            if (cryptoContent != null) {
                decryptedBody = CryptoUtils.decrypt(cryptoContent.getBytes(StandardCharsets.UTF_8), record);
            }
        }
        if (decryptedBody == null) {
            log.error("[IngotDecryptRequestBodyAdvice] 解密异常，检查相关配置");
            CryptoUtils.throwError(CryptoErrorCode.DECRYPT_ERROR);
        }
        InputStream inputStream = new ByteArrayInputStream(decryptedBody);
        return new CryptoHttpInputMessage(inputStream, inputMessage.getHeaders());
    }

    @Override
    @NonNull
    public Object afterBodyRead(@NonNull Object body,
                                @NonNull HttpInputMessage inputMessage,
                                @NonNull MethodParameter parameter,
                                @NonNull Type targetType,
                                @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body,
                                  @NonNull HttpInputMessage inputMessage,
                                  @NonNull MethodParameter parameter,
                                  @NonNull Type targetType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }
}
