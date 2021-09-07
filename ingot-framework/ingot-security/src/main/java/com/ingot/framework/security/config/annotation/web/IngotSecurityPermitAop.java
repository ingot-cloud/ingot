package com.ingot.framework.security.config.annotation.web;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.SecurityConstants;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Description  : IngotSecurityPermitAop. 内部请求不鉴权</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/12.</p>
 * <p>Time         : 10:39 AM.</p>
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class IngotSecurityPermitAop {
    private final HttpServletRequest request;

    @SneakyThrows
    @Around("@annotation(permit)")
    public Object around(ProceedingJoinPoint point, Permit permit) {
        // 内部模式需要校验接口来源
        if (permit.model() == PermitModel.INNER) {
            final String header = request.getHeader(SecurityConstants.HEADER_FROM);
            if (!StrUtil.equals(SecurityConstants.HEADER_FROM_INSIDE_VALUE, header)) {
                log.warn(">>> IngotSecurityPermitAop 访问接口 {} 没有权限", point.getSignature().getName());
                throw new AccessDeniedException("Access is denied");
            }
        }

        return point.proceed();
    }
}
