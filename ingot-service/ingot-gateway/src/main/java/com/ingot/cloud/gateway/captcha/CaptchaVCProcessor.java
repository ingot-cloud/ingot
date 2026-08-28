package com.ingot.cloud.gateway.captcha;

import java.util.LinkedHashMap;
import java.util.Map;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.ingot.cloud.gateway.security.PassTokenStore;
import com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.reactive.WebUtil;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.InVCMessageSource;
import com.ingot.framework.vc.common.InnerCheck;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCErrorCode;
import com.ingot.framework.vc.common.VCException;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.module.captcha.DefaultCaptchaVCProcessor;
import com.ingot.framework.vc.module.reactive.ReactorUtils;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 网关 Captcha 处理器：图形/滑块验码，并在挑战场景下签发 PassToken。
 *
 * <p>注册为 VC 路由 {@code image}（bean 名 {@link VCConstants#BEAN_NAME_PROCESSOR_IMAGE}）。
 * {@code POST /vc/image/check} 无 Header {@link VCConstants#HEADER_SCOPE} 时只返回 captcha
 * 结果；带 scope 时验码成功后签发 PassToken（JSON 键名等于头名，不含嵌套 {@code captcha}）。
 * Redis 不可用或 scope 无策略时 fail-closed，不得成功且无 token。</p>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     challenge:
 *       enabled: true
 *       policy:
 *         policies:
 *           - challenge-type: SLIDER
 *             scope: e2e-anon
 *             pass-token-ttl-sec: 300
 *             pass-token-remaining: 3
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Component(VCConstants.BEAN_NAME_PROCESSOR_IMAGE)
@RequiredArgsConstructor
public class CaptchaVCProcessor implements VCProcessor {
    private final CaptchaService captchaService;
    private final ObjectProvider<ChallengePolicyService> challengeProvider;
    private final PassTokenStore passTokenStore;
    private DefaultCaptchaVCProcessor defaultCaptchaVCProcessor;

    @PostConstruct
    void init() {
        defaultCaptchaVCProcessor = new DefaultCaptchaVCProcessor(captchaService);
    }

    /**
     * 向 anji 申请一张图形 / 滑块挑战。
     *
     * @param request   当前请求
     * @param generator 未使用；captcha 由 anji 生成
     * @return 成功时 data 为 anji 拉码结果
     */
    @Override
    public Mono<ServerResponse> handle(ServerRequest request, VCGenerator generator) {
        return defaultCaptchaVCProcessor.handle(request, generator);
    }

    /**
     * 登录保护已改为 412 + PassToken，不再随业务请求带码校验。
     *
     * @param type     验证码类型
     * @param exchange 当前请求
     * @param chain    后续过滤器
     * @return 直接放行后续链路
     */
    @Override
    public Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange);
    }

    /**
     * 校验滑块/图形结果；带 Header {@link VCConstants#HEADER_SCOPE} 时签发 PassToken。
     *
     * @param type    验证码类型
     * @param request 校验请求，scope 只从 Header 读取
     * @return 成功响应；验码失败或签发失败为错误流
     */
    @Override
    public Mono<ServerResponse> check(VCType type, ServerRequest request) {
        try {
            String pointJson = ReactorUtils.getFromRequest(request, "pointJson");
            String token = ReactorUtils.getFromRequest(request, "token");

            CaptchaVO vo = new CaptchaVO();
            vo.setPointJson(pointJson);
            vo.setToken(token);
            vo.setBrowserInfo(WebUtil.getClientIP(request));
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            ResponseModel responseModel = captchaService.check(vo);
            InnerCheck.check(responseModel.isSuccess(), "vc.check.image.checkFailure");

            String scope = request.headers().firstHeader(VCConstants.HEADER_SCOPE);
            if (scope == null || scope.isBlank()) {
                return ReactorUtils.successResponse(R.ok(responseModel));
            }
            return issuePassToken(scope)
                    .flatMap(passToken -> ReactorUtils.successResponse(
                            buildCheckResponse(scope, passToken)));
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    private Mono<String> issuePassToken(String scope) {
        ChallengePolicyService service = challengeProvider.getIfAvailable();
        if (service == null) {
            return Mono.error(passTokenIssueFailure());
        }
        ChallengePolicy policy = service.findByScope(scope);
        if (policy == null || !ChallengeCaptchaType.isSupported(policy.getChallengeType())) {
            return Mono.error(passTokenIssueFailure());
        }
        return passTokenStore.issue(scope, policy.getPassTokenTtlSec(), policy.getPassTokenRemaining())
                .switchIfEmpty(Mono.error(passTokenIssueFailure()));
    }

    private static VCException passTokenIssueFailure() {
        return new VCException(VCErrorCode.Check,
                InVCMessageSource.getAccessor().getMessage("vc.check.image.passTokenIssueFailure"));
    }

    private static R<?> buildCheckResponse(String scope, String passToken) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(VCConstants.HEADER_PASS_TOKEN, passToken);
        data.put(VCConstants.HEADER_SCOPE, scope);
        return R.ok(data);
    }
}
