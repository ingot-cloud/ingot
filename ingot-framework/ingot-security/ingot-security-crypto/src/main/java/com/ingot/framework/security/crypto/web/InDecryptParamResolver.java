package com.ingot.framework.security.crypto.web;

import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import com.ingot.framework.security.crypto.annotation.InDecrypt;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.model.CryptoInfoRecord;
import com.ingot.framework.security.crypto.model.CryptoType;
import com.ingot.framework.security.crypto.utils.CryptoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * <p>URL 查询参数解密解析器，配合参数上的 {@link InDecrypt} 使用。</p>
 *
 * <p>从 {@link InCryptoProperties#getParamKey()}（默认 {@code data}）读取密文查询参数并解密后反序列化为目标类型。
 * HYBRID 模式复用 {@link HybridCryptoInterceptor} 写入的 CEK/AAD，需配合方法/类上的
 * {@link InCryptoHybridContext}。</p>
 *
 * <pre>{@code
 * @InCryptoHybridContext
 * @GetMapping("/query")
 * public R<?> query(@InDecrypt(CryptoType.HYBRID) MyQueryDTO query) { ... }
 * // GET /query?data=<密文> + X-In-Crypto-* 协议头
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class InDecryptParamResolver implements HandlerMethodArgumentResolver {
    private final InCryptoProperties properties;
    private final ObjectMapper objectMapper;
    private final HybridCryptoService hybridCryptoService;

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

        byte[] plainBytes;
        if (decrypt.value() == CryptoType.HYBRID) {
            plainBytes = hybridDecrypt(text);
        } else {
            plainBytes = CryptoUtils.decrypt(
                    text.getBytes(StandardCharsets.UTF_8),
                    new CryptoInfoRecord(decrypt.value(), decrypt.secretKey())
            );
        }
        return objectMapper.readValue(plainBytes, parameter.getType());
    }

    /**
     * 信封加密（HYBRID）URL 参数解密，复用拦截器写入的 CEK/AAD。
     */
    private byte[] hybridDecrypt(String cipherText) {
        byte[] cek = HybridContext.currentCek();
        if (cek == null) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_HEADER_MISSING);
        }
        return hybridCryptoService.decrypt(
                cek, cipherText.getBytes(StandardCharsets.UTF_8), HybridContext.currentAad());
    }
}
