package com.ingot.framework.vc.module.captcha;

import cn.hutool.core.util.StrUtil;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.*;
import com.ingot.framework.vc.module.servlet.ServletUtils;
import com.ingot.framework.vc.module.servlet.VCProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : DefaultCaptchaVCProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 3:11 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCaptchaVCProvider implements VCProvider {
    private final CaptchaService captchaService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void create(ServletWebRequest request, VCGenerator generator) {
        try {
            CaptchaVO vo = new CaptchaVO();
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            ResponseModel responseModel = captchaService.get(vo);
            ServletUtils.successResponse(request, objectMapper, R.ok(responseModel));
        } catch (Exception e) {
            throw new VCException(VCStatusCode.Send,
                    IngotVCMessageSource.getAccessor()
                            .getMessage("vc.common.sendError"));
        }
    }

    @Override
    public void checkOnly(ServletWebRequest request, VCType type) {
        String code = ServletUtils.getCode(request);
        InnerCheck.check(StrUtil.isNotEmpty(code), "vc.check.image.illegalArgs");

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaVerification(code);
        vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
        InnerCheck.check(captchaService.verification(vo).isSuccess(), "vc.check.image.checkFailure");
    }

    @Override
    public void check(ServletWebRequest request, VCType type) {
        try {
            String pointJson = ServletUtils.getFromRequest(request, "pointJson");
            String token = ServletUtils.getFromRequest(request, "token");

            CaptchaVO vo = new CaptchaVO();
            vo.setPointJson(pointJson);
            vo.setToken(token);
            vo.setCaptchaType(VCConstants.IMAGE_CODE_TYPE);
            ResponseModel responseModel = captchaService.check(vo);
            ServletUtils.successResponse(request, objectMapper, R.ok(responseModel));
        } catch (Exception e) {
            throw new VCException(VCStatusCode.Check,
                    IngotVCMessageSource.getAccessor()
                            .getMessage("vc.common.checkError"));
        }
    }
}
