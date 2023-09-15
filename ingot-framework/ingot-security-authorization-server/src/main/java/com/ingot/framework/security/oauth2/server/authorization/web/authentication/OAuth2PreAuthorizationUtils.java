package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.core.endpoint.PreAuthorizationGrantType;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 * <p>Description  : OAuth2PreAuthorizationUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/15.</p>
 * <p>Time         : 5:18 PM.</p>
 */
public class OAuth2PreAuthorizationUtils {

    public static boolean hadOAuth2PreAuthorizationCodeRequestAuthenticationToken(Authentication authentication,
                                                                                  HttpServletRequest request) {
        if (!(authentication instanceof OAuth2PreAuthorizationCodeRequestAuthenticationToken token)) {
            return false;
        }

        if (!token.isAuthenticated()) {
            return false;
        }

        // 检查grantType类型, 必须为PreAuthorizationGrantType.SESSION
        String preGrantType = request.getParameter(IngotOAuth2ParameterNames.PRE_GRANT_TYPE);
        return StrUtil.equals(preGrantType, PreAuthorizationGrantType.SESSION.value());
    }
}
