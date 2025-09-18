package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.utils.SocialUtil;
import com.ingot.framework.security.core.userdetails.UsernameUri;
import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.MultiValueMap;

/**
 * <p>Description  : OAuth2UserDetailsSocialAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 12:00 PM.</p>
 */
public class OAuth2UserDetailsSocialAuthenticationConverter extends OAuth2UserDetailsAuthenticationConverter {
    @Override
    protected AuthorizationGrantType getGrantType() {
        return InAuthorizationGrantType.SOCIAL;
    }

    @Override
    protected Authentication createUnauthenticated(HttpServletRequest request, Authentication clientPrincipal) {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        String socialType = OAuth2EndpointUtils.getParameter(parameters, InOAuth2ParameterNames.SOCIAL_TYPE);
        String code = OAuth2EndpointUtils.getParameter(parameters, InOAuth2ParameterNames.SOCIAL_CODE);
        String userTypeValue = OAuth2EndpointUtils.getParameter(parameters, InOAuth2ParameterNames.USER_TYPE);
        UserTypeEnum userType = UserTypeEnum.getEnum(userTypeValue);
        if (userType == null) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(InOAuth2ParameterNames.USER_TYPE);
        }

        String tenant = parameters.getFirst(InOAuth2ParameterNames.TENANT);
        String principal = UsernameUri.of(
                SocialUtil.uniqueCode(socialType, code), userTypeValue, getGrantType().getValue(), tenant).getValue();
        return OAuth2UserDetailsAuthenticationToken
                .unauthenticated(principal,
                        null, getGrantType(), clientPrincipal);
    }
}
