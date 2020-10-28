package com.ingot.framework.security.core.filter;

import com.ingot.framework.core.context.ContextHolder;
import com.ingot.framework.security.core.exception.UserForbiddenException;
import com.ingot.framework.security.core.exception.UserTokenEmptyException;
import com.ingot.framework.security.core.exception.UserTokenInvalidException;
import com.ingot.framework.security.core.exception.UserTokenSignBackException;
import com.ingot.framework.security.model.dto.UserTokenDto;
import com.ingot.framework.security.provider.service.AuthenticationService;
import com.ingot.framework.security.provider.service.UserAccessTokenRedisService;
import com.ingot.framework.security.utils.ResourcePermitUtils;
import com.ingot.framework.security.utils.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.ingot.framework.core.constants.SecurityConstants.AUTH_TYPE_UNIQUE;


/**
 * <p>Description  : UserAuthenticationFilter. 通过 IngotGateway 注解控制是否注入该用户鉴权 filter</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/13.</p>
 * <p>Time         : 2:42 PM.</p>
 */
@Slf4j
@AllArgsConstructor
public class UserAuthenticationFilter extends OncePerRequestFilter {
    private final UserAccessTokenRedisService userAccessTokenRedisService;
    private final ResourcePermitUtils resourcePermitUtils;
    private final AuthenticationService authenticationService;

    @Override protected void doFilterInternal(@NonNull HttpServletRequest request,
                                              @NonNull HttpServletResponse response,
                                              @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String url = request.getRequestURI();
        String token = SecurityUtils.getBearerTokenValue(SecurityUtils.getBearerToken(request).orElse(""));

        if (resourcePermitUtils.userPermit(url)){
            log.info(">>> UserAuthenticationFilter ====> IgnoreUserAuthentication 忽略认证.url={}", url);
            filterChain.doFilter(request, response);
            return;
        }

        log.info("{} at filter chain; firing Filter: 'UserAuthenticationFilter'", url);

        log.info(">>> UserAuthenticationFilter ====>  用户鉴权拦截器. token={}", token);
        if (StringUtils.isEmpty(token)){
            log.error(">>> UserAuthenticationFilter ====> Token不能为空");
            throw new UserTokenEmptyException();
        }
        UserTokenDto user = userAccessTokenRedisService.getAccessToken(token);
        if (user == null){
            log.error(">>> UserAuthenticationFilter ====> 获取用户信息失败，用户 token 失效");
            throw new UserTokenInvalidException();
        }

        String authType = user.getAuthType();
        // 如果当前鉴权类型为唯一，那么需要判断使用token是否和当前登录用户token相同，不同则签退
        if (StringUtils.endsWithIgnoreCase(authType, AUTH_TYPE_UNIQUE)){
            if (!org.apache.commons.codec.binary.StringUtils.equals(token, user.getAccessToken())){
                log.error(">>> UserAuthenticationFilter ====> 用户已被签退");
                throw new UserTokenSignBackException();
            }
        }

        log.info(">>> UserAuthenticationFilter ====>  用户鉴权拦截器. user={}", user);

        // 校验用户权限
        log.info(">>> UserAuthenticationFilter 开始验证用户权限, user={}", user.getUsername());
        boolean authResult = authenticationService.authenticate(SecurityUtils.getAuthentication(), request);
        log.info(">>> UserAuthenticationFilter 验证用户权限结束, user={}, 结果={}",
                user.getUsername(), authResult);
        if (!authResult) {
            throw new UserForbiddenException();
        }

        ContextHolder.setToken(user.getAccessToken());

        filterChain.doFilter(request, response);

        ContextHolder.clear();
    }
}
