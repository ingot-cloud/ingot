package com.ingot.framework.security.crypto.web;

import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.annotation.InEncrypt;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.model.CryptoInfoRecord;
import com.ingot.framework.security.crypto.model.CryptoType;
import com.ingot.framework.security.crypto.utils.CryptoUtils;
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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

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
public class InEncryptResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    private final InCryptoProperties properties;
    private final ObjectMapper objectMapper;
    private final HybridCryptoService hybridCryptoService;

    @Override
    public boolean supports(@NonNull MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return AnnotationUtil.hasAnnotation(returnType.getMethod(), InEncrypt.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request,
                                  @NonNull ServerHttpResponse response) {
        CryptoInfoRecord record = CryptoUtils.getEncryptInfo(returnType);
        if (record == null) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_CONFIG);
        }

        if (record.type() == CryptoType.HYBRID) {
            return hybridEncrypt(body);
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
            return CryptoUtils.encrypt(bodyJsonBytes, record);
        }
        // 包装返回对象，使用指定key映射加密数据
        Map<String, Object> data = new HashMap<>(2);
        data.put(bodyJsonKey, CryptoUtils.encrypt(bodyJsonBytes, record));
        return data;
    }

    /**
     * 信封加密（HYBRID）响应加密。复用请求 CEK 与 AAD（由拦截器/请求体解密阶段写入 request attribute）。
     * 默认 DATA_ONLY：保留 R 结构仅加密 data；FULL：整体加密。响应头 Md/Kv 由拦截器统一回带。
     */
    private Object hybridEncrypt(Object body) {
        byte[] cek = attribute(HybridContext.ATTR_CEK);
        byte[] aad = attribute(HybridContext.ATTR_AAD);
        if (cek == null) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_HEADER_MISSING);
        }
        if (body == null) {
            return null;
        }

        InCryptoProperties.Hybrid hybrid = properties.getHybrid();
        if (hybrid.getResponseWrap() == InCryptoProperties.ResponseWrap.DATA_ONLY
                && body instanceof R<?> r) {
            Object dataObj = r.getData();
            if (dataObj == null) {
                return body;
            }
            String encrypted = hybridCryptoService.encrypt(cek, toBytes(dataObj), aad);
            @SuppressWarnings("unchecked")
            R<Object> raw = (R<Object>) r;
            raw.data(encrypted);
            return raw;
        }

        // FULL 或非 R 结构：整体加密
        return hybridCryptoService.encrypt(cek, toBytes(body), aad);
    }

    private byte[] toBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] attribute(String name) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
        return value instanceof byte[] bytes ? bytes : null;
    }
}
