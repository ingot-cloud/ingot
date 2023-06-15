package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Description  : VCAPI.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 4:08 PM.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class VCController {
    private final VCProviderManager providerManager;

    @Permit
    @PostMapping(VCConstants.PATH_PREFIX + "/{type}")
    @ResponseBody
    public void createCode(@PathVariable String type,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        providerManager.create(VCType.getEnum(type), new ServletWebRequest(request, response));
    }
}
