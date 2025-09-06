package com.ingot.framework.security.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.UrlUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * <p>Description  : InLoginUrlAuthenticationEntryPoint.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/5.</p>
 * <p>Time         : 2:50 PM.</p>
 */
@Slf4j
public class InLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    /**
     * @param loginFormUrl URL where the login page can be found. Should either be
     *                     relative to the web-app context path (include a leading {@code /}) or an absolute
     *                     URL.
     */
    public InLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        String loginForm = determineUrlToUseForThisRequest(request, response, authException);
        // 非绝对路径，使用原有逻辑
        if (!UrlUtils.isAbsoluteUrl(loginForm)) {
            super.commence(request, response, authException);
            return;
        }

        StringBuffer requestUrl = request.getRequestURL();
        if (StrUtil.isNotEmpty(request.getQueryString())) {
            requestUrl.append("?").append(request.getQueryString());
        }

        String redirectUri = URLEncoder.encode(requestUrl.toString(), StandardCharsets.UTF_8);
        String loginUrl = loginForm + "?" + OAuth2ParameterNames.REDIRECT_URI + "=" + redirectUri +
                "&" + InOAuth2ParameterNames.SESSION_ID + "=" + request.getSession(false).getId();
        this.redirectStrategy.sendRedirect(request, response, loginUrl);
    }
}
