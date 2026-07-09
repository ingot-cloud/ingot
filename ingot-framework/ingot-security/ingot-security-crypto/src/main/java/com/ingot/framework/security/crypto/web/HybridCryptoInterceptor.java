package com.ingot.framework.security.crypto.web;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.annotation.InCryptoHybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridContext;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.hybrid.HybridKeyManager;
import com.ingot.framework.security.crypto.model.CryptoErrorCode;
import com.ingot.framework.security.crypto.utils.CryptoUtils;
import com.ingot.framework.security.replay.ReplayGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * <p>信封加密处理拦截器，在请求进入 Controller 前统一建立加解密上下文。</p>
 *
 * <p>{@code preHandle} 阶段解析隐晦协议头、执行防重放校验、以 RSA-OAEP 解包 CEK，
 * 并将 CEK 与 AAD 存入 request attribute，供请求体解密与响应体加密复用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 对 GET（无请求体）同样生效，因此 GET 响应亦可加密；仅处理标记了 HYBRID 的处理器方法。
 */
@Slf4j
@RequiredArgsConstructor
public class HybridCryptoInterceptor implements HandlerInterceptor {
    private final InCryptoProperties properties;
    private final HybridKeyManager hybridKeyManager;
    private final HybridCryptoService hybridCryptoService;
    private final ObjectProvider<ReplayGuard> replayGuardProvider;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!isHybrid(handlerMethod)) {
            return true;
        }

        InCryptoProperties.Hybrid hybrid = properties.getHybrid();
        InCryptoProperties.Headers headerNames = hybrid.getHeaders();

        String mode = request.getHeader(headerNames.getMode());
        if (StrUtil.isBlank(mode)) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_HEADER_MISSING);
        }

        String kid = request.getHeader(headerNames.getKid());
        String wrappedKey = request.getHeader(headerNames.getKey());
        String nonce = request.getHeader(headerNames.getNonce());
        String ts = request.getHeader(headerNames.getTimestamp());
        String alg = request.getHeader(headerNames.getAlg());
        String enc = request.getHeader(headerNames.getEnc());

        if (StrUtil.hasBlank(kid, wrappedKey)) {
            CryptoUtils.throwError(CryptoErrorCode.CRYPTO_HEADER_MISSING);
        }
        hybridCryptoService.checkAlgorithm(alg, enc);

        ReplayGuard replayGuard = replayGuardProvider.getIfAvailable();
        if (replayGuard != null) {
            replayGuard.check(hybrid.getReplayNamespace(), nonce, parseTs(ts));
        }

        byte[] cek = hybridKeyManager.unwrapCek(kid, wrappedKey);
        byte[] aad = HybridContext.buildAad(mode, kid, nonce, ts);
        request.setAttribute(HybridContext.ATTR_CEK, cek);
        request.setAttribute(HybridContext.ATTR_AAD, aad);
        request.setAttribute(HybridContext.ATTR_ACTIVE, Boolean.TRUE);

        // 统一在此回带模式标记与激活 kid：整体与字段级模式均适用，
        // 供前端感知响应已加密并跟进密钥轮换。
        response.addHeader(headerNames.getMode(), hybrid.getModeValue());
        if (StrUtil.isNotBlank(hybrid.getActiveKid())) {
            response.addHeader(headerNames.getKid(), hybrid.getActiveKid());
        }
        return true;
    }

    private boolean isHybrid(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), InCryptoHybridContext.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), InCryptoHybridContext.class);
    }

    private long parseTs(String ts) {
        try {
            return Long.parseLong(StrUtil.blankToDefault(ts, "0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
