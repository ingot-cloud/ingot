package com.ingot.framework.security.provider.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ingot.framework.security.service.ResourcePermitService;
import com.ingot.framework.security.service.TokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.filter.OncePerRequestFilter;


/**
 * <p>Description  : UserAuthenticationFilter. 通过 IgnoreUserAuthentication 注解控制是否注入该用户鉴权 filter</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/13.</p>
 * <p>Time         : 2:42 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class UserAuthenticationFilter extends OncePerRequestFilter {
    private final ResourcePermitService resourcePermitService;
    private final TokenService tokenService;

    @Override protected void doFilterInternal(@NonNull HttpServletRequest request,
                                              @NonNull HttpServletResponse response,
                                              @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String url = request.getRequestURI();
        if (resourcePermitService.userPermit(url)){
            log.info(">>> UserAuthenticationFilter ====> IgnoreUserAuthentication 忽略认证.url={}", url);
            filterChain.doFilter(request, response);
            return;
        }

        // 获取当前token
        OAuth2AccessToken token = tokenService.getToken(request);

        // 校验token
        tokenService.checkAuthentication(token);

        filterChain.doFilter(request, response);
    }
}
