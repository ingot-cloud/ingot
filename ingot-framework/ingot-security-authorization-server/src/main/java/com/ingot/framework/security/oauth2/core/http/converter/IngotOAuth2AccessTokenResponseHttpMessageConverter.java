package com.ingot.framework.security.oauth2.core.http.converter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.oauth2.core.endpoint.MapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

/**
 * <p>Description  : IngotOAuth2AccessTokenResponseHttpMessageConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/24.</p>
 * <p>Time         : 3:54 下午.</p>
 */
public class IngotOAuth2AccessTokenResponseHttpMessageConverter
        extends AbstractHttpMessageConverter<OAuth2AccessTokenResponse> {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<Map<String, Object>>() {
    };

    private final GenericHttpMessageConverter<Object> jsonMessageConverter = HttpMessageConverters.getJsonMessageConverter();

    protected Converter<Map<String, String>, OAuth2AccessTokenResponse> tokenResponseConverter = new MapOAuth2AccessTokenResponseConverter();

    protected Converter<OAuth2AccessTokenResponse, Map<String, Object>> tokenResponseParametersConverter = new IngotOAuth2AccessTokenResponseMapConverter();

    public IngotOAuth2AccessTokenResponseHttpMessageConverter() {
        super(DEFAULT_CHARSET, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return OAuth2AccessTokenResponse.class.isAssignableFrom(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected OAuth2AccessTokenResponse readInternal(Class<? extends OAuth2AccessTokenResponse> clazz,
                                                     HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
        try {
            // gh-6463: Parse parameter values as Object in order to handle potential JSON
            // Object and then convert values to String
            Map<String, Object> tokenResponseParameters = (Map<String, Object>) this.jsonMessageConverter
                    .read(STRING_OBJECT_MAP.getType(), null, inputMessage);
            // @formatter:off
            return this.tokenResponseConverter.convert(tokenResponseParameters
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, (entry) -> String.valueOf(entry.getValue()))));
            // @formatter:on
        } catch (Exception ex) {
            throw new HttpMessageNotReadableException(
                    "An error occurred reading the OAuth 2.0 Access Token Response: " + ex.getMessage(), ex,
                    inputMessage);
        }
    }

    @Override
    protected void writeInternal(OAuth2AccessTokenResponse tokenResponse, HttpOutputMessage outputMessage)
            throws HttpMessageNotWritableException {
        try {
            Map<String, Object> tokenResponseParameters = this.tokenResponseParametersConverter.convert(tokenResponse);
            this.jsonMessageConverter.write(tokenResponseParameters, STRING_OBJECT_MAP.getType(),
                    MediaType.APPLICATION_JSON, outputMessage);
        } catch (Exception ex) {
            throw new HttpMessageNotWritableException(
                    "An error occurred writing the OAuth 2.0 Access Token Response: " + ex.getMessage(), ex);
        }
    }

}
