package com.ingot.framework.vc.module.captcha;

import cn.hutool.core.util.StrUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.InnerCheck;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCException;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.module.reactive.ReactorUtils;
import com.ingot.framework.vc.module.reactive.VCProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : DefaultCaptchaVCProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/26.</p>
 * <p>Time         : 4:26 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultCaptchaVCProcessor implements VCProcessor {
    private final CaptchaService captchaService;

    @Override
    public Mono<ServerResponse> handle(ServerRequest request, VCGenerator generator) {
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
        ResponseModel responseModel = captchaService.get(vo);
        try {
            InnerCheck.check(responseModel.isSuccess(), "vc.check.image.checkFailure");
            return ReactorUtils.successResponse(R.ok(responseModel));
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain) {
        try {
            String code = ReactorUtils.getCode(exchange.getRequest());
            InnerCheck.check(StrUtil.isNotEmpty(code), "vc.check.image.illegalArgs");

            CaptchaVO vo = new CaptchaVO();
            vo.setCaptchaVerification(code);
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            InnerCheck.check(captchaService.verification(vo).isSuccess(), "vc.check.image.checkFailure");
            return chain.filter(exchange);
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<ServerResponse> check(VCType type, ServerRequest request) {
        try {
            String pointJson = ReactorUtils.getFromRequest(request, "pointJson");
            String token = ReactorUtils.getFromRequest(request, "token");

            CaptchaVO vo = new CaptchaVO();
            vo.setPointJson(pointJson);
            vo.setToken(token);
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            ResponseModel responseModel = captchaService.check(vo);
            return ReactorUtils.successResponse(R.ok(responseModel));
        } catch (VCException e) {
            return Mono.error(e);
        }
    }
}
