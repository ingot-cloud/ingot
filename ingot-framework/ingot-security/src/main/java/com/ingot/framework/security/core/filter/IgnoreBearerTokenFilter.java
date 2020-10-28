package com.ingot.framework.security.core.filter;

import com.ingot.framework.core.constants.SecurityConstants;
import com.ingot.framework.security.utils.ResourcePermitUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * <p>Description  : IgnoreBearerTokenFilter.
 *                   为了避免已经permit不需要鉴权的请求携带了Authorization，从而
 *                   导致OAuth2AuthenticationProcessingFilter对Bearer Token
 *                   进行校验，从而可能触发失效等异常。
 *                   </p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/4.</p>
 * <p>Time         : 5:32 PM.</p>
 */
@Slf4j
@AllArgsConstructor
public class IgnoreBearerTokenFilter extends OncePerRequestFilter {
    private final ResourcePermitUtils resourcePermitUtils;

    @Override protected void doFilterInternal(@NonNull HttpServletRequest request,
                                              @NonNull HttpServletResponse response,
                                              @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.info(">>> IgnoreBearerTokenFilter, {}", requestURI);

        if (!resourcePermitUtils.resourcePermit(requestURI)){
            filterChain.doFilter(request, response);
            return;
        }

        log.info(">>> IgnoreBearerTokenFilter - ignore bearer token url - {}", requestURI);

        // delegate, ignore authorization
        HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(request) {
            @Override public String getHeader(String name) {
                if (StringUtils.equals(name, HttpHeaders.AUTHORIZATION)){
                    String header = super.getHeader(name);
                    log.info(">>> IgnoreBearerTokenFilter - request invoke getHeader('Authorization')");
                    return org.springframework.util.StringUtils.startsWithIgnoreCase(header, SecurityConstants.OAUTH2_BEARER_TYPE) ? "" : header;
                }
                return super.getHeader(name);
            }

            @Override public Enumeration<String> getHeaders(String name) {
                if (StringUtils.equals(name, HttpHeaders.AUTHORIZATION)){
                    log.info(">>> IgnoreBearerTokenFilter - request invoke getHeaders('Authorization')");
                    Enumeration<String> superHeaders = super.getHeaders(name);
                    return new Enumeration<String>() {
                        @Override public boolean hasMoreElements() {
                            return superHeaders.hasMoreElements();
                        }

                        @Override public String nextElement() {
                            String superValue = superHeaders.nextElement();
                            return org.springframework.util.StringUtils.startsWithIgnoreCase(superValue, SecurityConstants.OAUTH2_BEARER_TYPE) ? "" : superValue;
                        }
                    };
                }
                return super.getHeaders(name);
            }
        };

        filterChain.doFilter(requestWrapper, response);
    }
}
