package com.ingot.framework.security.crypto.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.annotation.InDecrypt;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.model.CryptoHttpInputMessage;
import com.ingot.framework.security.crypto.model.CryptoInfoRecord;
import com.ingot.framework.security.crypto.model.CryptoType;
import com.ingot.framework.security.crypto.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

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
    private final HybridCryptoService hybridCryptoService;

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
        CryptoInfoRecord record = CryptoUtils.getDecryptInfo(methodParameter);
        if (record == null) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_CONFIG);
        }

        if (record.type() == CryptoType.HYBRID) {
            return hybridDecrypt(inputMessage);
        }

        InputStream messageBody = inputMessage.getBody();
        if (messageBody.available() <= 0) {
            return inputMessage;
        }

        byte[] bodyByteArray = StreamUtils.copyToByteArray(messageBody);
        // body中加密信息的key, 默认key为data {"data":"加密后的实际请求数据"}
        String bodyCryptoKey = properties.getBodyKey();

        byte[] decryptedBody = null;
        // 如果不存在，表示请求体整体加密
        if (StrUtil.isBlank(bodyCryptoKey)) {
            decryptedBody = CryptoUtils.decrypt(bodyByteArray, record);
        } else {
            String cryptoContent = extractCipherContent(bodyByteArray, bodyCryptoKey);
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

    /**
     * 信封加密（HYBRID）请求体解密。协议头解析、防重放与 CEK 解包由
     * {@link HybridCryptoInterceptor} 在 preHandle 阶段完成，这里仅用上下文中的 CEK/AAD 解密密文。
     */
    private HttpInputMessage hybridDecrypt(HttpInputMessage inputMessage) throws IOException {
        byte[] cek = attribute(HybridContext.ATTR_CEK);
        byte[] aad = attribute(HybridContext.ATTR_AAD);
        if (cek == null) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_HEADER_MISSING);
        }

        byte[] bodyByteArray = StreamUtils.copyToByteArray(inputMessage.getBody());
        if (bodyByteArray.length == 0) {
            return inputMessage;
        }

        HttpHeaders headers = inputMessage.getHeaders();
        String bodyCryptoKey = properties.getBodyKey();
        byte[] cipherBytes;
        if (StrUtil.isBlank(bodyCryptoKey)) {
            cipherBytes = bodyByteArray;
        } else {
            String cryptoContent = extractCipherContent(bodyByteArray, bodyCryptoKey);
            if (cryptoContent == null) {
                return inputMessage;
            }
            cipherBytes = cryptoContent.getBytes(StandardCharsets.UTF_8);
        }

        byte[] decryptedBody = hybridCryptoService.decrypt(cek, cipherBytes, aad);
        return new CryptoHttpInputMessage(new ByteArrayInputStream(decryptedBody), headers);
    }

    private byte[] attribute(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
        return value instanceof byte[] bytes ? bytes : null;
    }

    /**
     * 从请求体 JSON 中按 {@link InCryptoProperties#getBodyKey()} 提取密文载荷。
     */
    @SuppressWarnings("unchecked")
    private String extractCipherContent(byte[] bodyByteArray, String bodyCryptoKey) throws IOException {
        Map<String, Object> data = objectMapper.readValue(bodyByteArray, Map.class);
        Object cryptoContent = data.get(bodyCryptoKey);
        return cryptoContent == null ? null : cryptoContent.toString();
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
