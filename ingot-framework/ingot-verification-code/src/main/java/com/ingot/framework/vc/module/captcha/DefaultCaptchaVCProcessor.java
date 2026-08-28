package com.ingot.framework.vc.module.captcha;

import cn.hutool.core.util.StrUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.reactive.WebUtil;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.InnerCheck;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCErrorCode;
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
 * <p>图形 / 滑块验证码的默认 WebFlux 处理器，直接调用 anji {@link CaptchaService}，
 * 不经过 OTP 的 {@code AbstractVCProcessor} / {@code VCRepository}。</p>
 *
 * <p>拉码对应 {@code GET /vc/image}，验码对应 {@code POST /vc/image/check}。
 * 网关挑战编排（PassToken）由调用方在验码成功后完成，本类只负责引擎结果。</p>
 *
 * @author wangchao
 * @since 2023/6/26
 */
@RequiredArgsConstructor
public class DefaultCaptchaVCProcessor implements VCProcessor {
    private final CaptchaService captchaService;

    /**
     * 向 anji 申请一张图形 / 滑块挑战。
     *
     * @param request   当前请求，用于取客户端 IP
     * @param generator 未使用；captcha 由 anji 生成
     * @return 成功时 data 为 anji {@link ResponseModel}；拉码失败为错误流
     */
    @Override
    public Mono<ServerResponse> handle(ServerRequest request, VCGenerator generator) {
        try {
            CaptchaVO vo = new CaptchaVO();
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            vo.setBrowserInfo(WebUtil.getClientIP(request));
            ResponseModel responseModel = captchaService.get(vo);

            InnerCheck.check(responseModel.isSuccess(), VCErrorCode.Illegal,
                    "vc.check.image.fetchFailure",
                    new String[]{responseModel.getRepCode(), responseModel.getRepMsg()});
            return ReactorUtils.successResponse(R.ok(responseModel));
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    /**
     * 随业务请求带码校验（遗留 {@code verifyUrls} 路径）。登录安全触发已改为 412 + PassToken。
     *
     * @param type     验证码类型
     * @param exchange 当前请求
     * @param chain    后续过滤器
     * @return 校验通过则继续链路；失败为错误流
     */
    @Override
    public Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain) {
        try {
            String code = ReactorUtils.getCode(exchange.getRequest());
            InnerCheck.check(StrUtil.isNotEmpty(code), "vc.check.image.illegalArgs");

            CaptchaVO vo = new CaptchaVO();
            vo.setCaptchaVerification(code);
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            vo.setBrowserInfo(WebUtil.getClientIP(exchange.getRequest()));
            InnerCheck.check(captchaService.verification(vo).isSuccess(), "vc.check.image.checkFailure");
            return chain.filter(exchange);
        } catch (VCException e) {
            return Mono.error(e);
        }
    }

    /**
     * 校验滑块轨迹 / 图形结果；失败不返回成功体。
     *
     * @param type    验证码类型
     * @param request 含 {@code pointJson}、{@code token}
     * @return 成功时 data 为 anji {@link ResponseModel}
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
            return ReactorUtils.successResponse(R.ok(responseModel));
        } catch (VCException e) {
            return Mono.error(e);
        }
    }
}
