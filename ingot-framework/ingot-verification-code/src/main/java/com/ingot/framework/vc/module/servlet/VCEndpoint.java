package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCException;
import com.ingot.framework.vc.common.VCType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>Description  : VCEndpoint.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 4:08 PM.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class VCEndpoint {
    private final VCProviderManager providerManager;
    private final VCFailureHandler failureHandler;

    @Permit
    @GetMapping(VCConstants.PATH_PREFIX + "/{type}")
    @ResponseBody
    public void createCode(@PathVariable String type,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        try {
            providerManager.create(VCType.getEnum(type), new ServletWebRequest(request, response));
        } catch (VCException e) {
            try {
                failureHandler.onFailure(request, response, e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Permit
    @PostMapping(VCConstants.PATH_PREFIX + "/{type}/check")
    @ResponseBody
    public void checkCode(@PathVariable String type,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        try {
            providerManager.check(VCType.getEnum(type), new ServletWebRequest(request, response));
        } catch (VCException e) {
            try {
                failureHandler.onFailure(request, response, e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
