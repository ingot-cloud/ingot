package com.ingot.cloud.gateway.captcha;

import cn.hutool.core.util.StrUtil;
import com.anji.captcha.service.CaptchaService;
import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCProcessor;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : CaptchaVCProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/27.</p>
 * <p>Time         : 4:55 PM.</p>
 */
@Component(VCConstants.BEAN_NAME_PROCESSOR_IMAGE)
public class CaptchaVCProcessor implements VCProcessor {
    private static final String TOKEN_ENDPOINT = "/auth" + SecurityConstants.TOKEN_ENDPOINT_URI;
    private final DefaultCaptchaVCProcessor defaultCaptchaVCProcessor;

    public CaptchaVCProcessor(CaptchaService captchaService) {
        this.defaultCaptchaVCProcessor = new DefaultCaptchaVCProcessor(captchaService);
    }

    @Override
    public Mono<ServerResponse> handle(ServerRequest request, VCGenerator generator) {
        return defaultCaptchaVCProcessor.handle(request, generator);
    }

    @Override
    public Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // token端点单独处理，判断grant_type
        // 目前仅password需要进行验证码验证
        if (StrUtil.equals(TOKEN_ENDPOINT, path)) {
            String grantType = request.getQueryParams().getFirst("grant_type");
            if (!StrUtil.equals(grantType, SecurityConstants.GrantType.PASSWORD)) {
                return chain.filter(exchange);
            }
        }

        return defaultCaptchaVCProcessor.checkOnly(type, exchange, chain);
    }

    @Override
    public Mono<ServerResponse> check(VCType type, ServerRequest request) {
        return defaultCaptchaVCProcessor.check(type, request);
    }
}
