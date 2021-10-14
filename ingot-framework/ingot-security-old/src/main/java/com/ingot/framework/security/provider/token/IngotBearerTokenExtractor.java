package com.ingot.framework.security.provider.token;

import javax.servlet.http.HttpServletRequest;

import com.ingot.framework.security.service.ResourcePermitService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;

/**
 * <p>Description  : IngotBearerTokenExtractor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/19.</p>
 * <p>Time         : 5:14 下午.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotBearerTokenExtractor extends BearerTokenExtractor {
    private final ResourcePermitService resourcePermitService;

    @Override
    public Authentication extract(HttpServletRequest request) {
        boolean result = resourcePermitService.resourcePermit(request.getRequestURI());
        return result ? null : super.extract(request);
    }
}
