package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

import java.io.IOException;
import java.util.Map;

/**
 * <p>Description  : OAuth2AuthorizationCodeRequestHttpMessageConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/9.</p>
 * <p>Time         : 4:15 PM.</p>
 */
public class OAuth2AuthorizationCodeRequestHttpMessageConverter extends AbstractHttpMessageConverter<OAuth2AuthorizationCodeRequestAuthenticationToken> {
    private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<Map<String, Object>>() {
    };
    private final GenericHttpMessageConverter<Object> jsonMessageConverter = HttpMessageConverters.getJsonMessageConverter();
    private final Converter<OAuth2AuthorizationCodeRequestAuthenticationToken, Map<String, Object>> tokenConverter = new OAuth2AuthorizationCodeRequestAuthenticationTokenConverter();

    @Override
    protected boolean supports(@NonNull Class<?> clazz) {
        return OAuth2AuthorizationCodeRequestAuthenticationToken.class.isAssignableFrom(clazz);
    }

    @Override
    protected OAuth2AuthorizationCodeRequestAuthenticationToken readInternal(@NonNull Class<? extends OAuth2AuthorizationCodeRequestAuthenticationToken> clazz,
                                                                             @NonNull HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        // 不使用该方法
        return null;
    }

    @Override
    protected void writeInternal(@NonNull OAuth2AuthorizationCodeRequestAuthenticationToken token,
                                 @NonNull HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            Map<String, Object> parameters = this.tokenConverter.convert(token);
            assert parameters != null;
            this.jsonMessageConverter.write(parameters, STRING_OBJECT_MAP.getType(),
                    MediaType.APPLICATION_JSON, outputMessage);
        } catch (Exception ex) {
            throw new HttpMessageNotWritableException(
                    "An error occurred writing the OAuth 2.0 Access Token Response: " + ex.getMessage(), ex);
        }
    }
}
