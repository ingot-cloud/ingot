package com.ingot.cloud.gateway.captcha;

import cn.hutool.core.util.StrUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.reactive.WebUtil;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCException;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCProcessor;
import com.ingot.framework.vc.module.reactive.ReactorUtils;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import com.ingot.cloud.gateway.security.PassTokenStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关 Captcha 处理器：保留原有登录验码逻辑，并在风控挑战场景下签发 PassToken。
 *
 * <p>当 {@code POST /vc/image/check?_vc_scope=...} 且挑战域开启时，验码成功后响应
 * {@code data._vc_pass_token}，供业务请求重试携带。</p>
 */
@Component(VCConstants.BEAN_NAME_PROCESSOR_IMAGE)
public class CaptchaVCProcessor implements VCProcessor {
    private static final String TOKEN_ENDPOINT = "/auth" + SecurityConstants.TOKEN_ENDPOINT_URI;
    private static final String TOKEN_PRE_AUTHORIZE = "/auth" + SecurityConstants.PRE_AUTHORIZE_URI;
    private final DefaultCaptchaVCProcessor defaultCaptchaVCProcessor;
    private final CaptchaService captchaService;
    private final ObjectProvider<ChallengePolicyService> challengeProvider;
    private final PassTokenStore passTokenStore;

    public CaptchaVCProcessor(CaptchaService captchaService,
                              ObjectProvider<ChallengePolicyService> challengeProvider,
                              PassTokenStore passTokenStore) {
        this.captchaService = captchaService;
        this.defaultCaptchaVCProcessor = new DefaultCaptchaVCProcessor(captchaService);
        this.challengeProvider = challengeProvider;
        this.passTokenStore = passTokenStore;
    }

    @Override
    public Mono<ServerResponse> handle(ServerRequest request, VCGenerator generator) {
        return defaultCaptchaVCProcessor.handle(request, generator);
    }

    @Override
    public Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (StrUtil.equals(TOKEN_ENDPOINT, path)) {
            String grantType = request.getQueryParams().getFirst("grant_type");
            if (!StrUtil.equals(grantType, SecurityConstants.GrantType.PASSWORD)) {
                return chain.filter(exchange);
            }
        }

        if (StrUtil.equals(TOKEN_PRE_AUTHORIZE, path)) {
            String preGrantType = request.getQueryParams().getFirst("pre_grant_type");
            if (StrUtil.equals(preGrantType, SecurityConstants.PreAuthorizationGrantType.SESSION)) {
                return chain.filter(exchange);
            }
        }

        return defaultCaptchaVCProcessor.checkOnly(type, exchange, chain);
    }

    @Override
    public Mono<ServerResponse> check(VCType type, ServerRequest request) {
        try {
            String pointJson = ReactorUtils.getFromRequest(request, "pointJson");
            String token = ReactorUtils.getFromRequest(request, "token");

            CaptchaVO vo = new CaptchaVO();
            vo.setPointJson(pointJson);
            vo.setToken(token);
            vo.setBrowserInfo(WebUtil.getRemoteIP(request));
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            ResponseModel responseModel = captchaService.check(vo);

            String scope = request.queryParam(VCConstants.QUERY_PARAMS_SCOPE).orElse(null);
            return issuePassToken(scope)
                    .map(passToken -> buildCheckResponse(responseModel, scope, passToken))
                    .flatMap(ReactorUtils::successResponse);
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    private Mono<String> issuePassToken(String scope) {
        if (scope == null || scope.isBlank()) {
            return Mono.empty();
        }
        ChallengePolicyService service = challengeProvider.getIfAvailable();
        if (service == null) {
            return Mono.empty();
        }
        ChallengePolicy policy = service.findByScope(scope);
        if (policy == null) {
            return Mono.empty();
        }
        return passTokenStore.issue(scope, policy.getPassTokenTtlSec(), policy.getPassTokenRemaining());
    }

    private static R<?> buildCheckResponse(ResponseModel captchaResult, String scope, String passToken) {
        if (passToken == null) {
            return R.ok(captchaResult);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("captcha", captchaResult);
        data.put(VCConstants.QUERY_PARAMS_PASS_TOKEN, passToken);
        data.put(VCConstants.QUERY_PARAMS_SCOPE, scope);
        return R.ok(data);
    }
}
