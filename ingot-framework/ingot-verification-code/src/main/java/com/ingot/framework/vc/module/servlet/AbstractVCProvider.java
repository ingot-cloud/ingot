package com.ingot.framework.vc.module.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.Utils;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.common.VCErrorCode;
import com.ingot.framework.vc.common.VCType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * <p>Description  : AbstractVCProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 1:47 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractVCProvider implements VCProvider {
    private final VCRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送校验码，由子类实现
     *
     * @param request      the request
     * @param validateCode the validate code
     * @throws Exception the exception
     */
    protected abstract void send(ServletWebRequest request, VC validateCode) throws Exception;

    @Override
    public void create(ServletWebRequest request, VCGenerator generator) {
        VC code = generator.generate();
        try {
            send(request, code);
        } catch (Exception e) {
            log.error("[验证码] - 验证码发送失败", e);
            Utils.throwVCException(VCErrorCode.Send, "vc.common.sendError");
        }
        save(request, code);
    }

    @Override
    public void checkOnly(ServletWebRequest request, VCType type) {
        VC codeInCache = repository.get(ServletUtils.getReceiver(request), type);
        String codeInRequest = ServletUtils.getCode(request);
        Utils.checkCode(codeInRequest, codeInCache);
    }

    @Override
    public void check(ServletWebRequest request, VCType type) {
        checkOnly(request, type);
        try {
            ServletUtils.successResponse(request, objectMapper, R.ok(Boolean.TRUE));
        } catch (Exception e) {
            log.error("[验证码] - 验证码校验失败", e);
            Utils.throwVCException(VCErrorCode.Check, "vc.common.checkError");
        }
    }

    /**
     * 保存校验码
     */
    private void save(ServletWebRequest request, VC validateCode) {
        repository.save(ServletUtils.getReceiver(request), validateCode);
    }
}
