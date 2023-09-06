package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * <p>Description  : OAuth2PreAuthorizationCodeRequestAuthenticationTokenMixin.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/6.</p>
 * <p>Time         : 9:54 AM.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonDeserialize(using = OAuth2PreAuthorizationCodeRequestAuthenticationTokenDeserializer.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class OAuth2PreAuthorizationCodeRequestAuthenticationTokenMixin {
}
