package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationRequestAuthenticationToken;
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

import java.io.IOException;
import java.util.Map;

/**
 * <p>Description  : OAuth2PreAuthHttpMessageConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/29.</p>
 * <p>Time         : 10:57 AM.</p>
 */
public class OAuth2PreAuthHttpMessageConverter extends AbstractHttpMessageConverter<OAuth2PreAuthorizationRequestAuthenticationToken> {
    private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<Map<String, Object>>() {
    };
    private final GenericHttpMessageConverter<Object> jsonMessageConverter = HttpMessageConverters.getJsonMessageConverter();
    private final Converter<OAuth2PreAuthorizationRequestAuthenticationToken, Map<String, Object>> tokenConverter = new OAuth2PreAuthorizationAuthenticationTokenConverter();

    @Override
    protected boolean supports(@NonNull Class<?> clazz) {
        return OAuth2PreAuthorizationRequestAuthenticationToken.class.isAssignableFrom(clazz);
    }

    @Override
    @NonNull
    protected OAuth2PreAuthorizationRequestAuthenticationToken readInternal(@NonNull Class<? extends OAuth2PreAuthorizationRequestAuthenticationToken> clazz,
                                                                            @NonNull HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        // 不使用该方法
        return OAuth2PreAuthorizationRequestAuthenticationToken.unauthenticated();
    }

    @Override
    protected void writeInternal(@NonNull OAuth2PreAuthorizationRequestAuthenticationToken token,
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
