package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;

/**
 * <p>Description  : {@link OAuth2UserDetailsAuthenticationToken} Mixin.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/11.</p>
 * <p>Time         : 5:03 下午.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonDeserialize(using = OAuth2UserDetailsAuthenticationTokenDeserializer.class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class OAuth2UserDetailsAuthenticationTokenMixin {
}
