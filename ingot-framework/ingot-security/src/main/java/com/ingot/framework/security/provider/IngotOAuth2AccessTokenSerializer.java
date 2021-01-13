package com.ingot.framework.security.provider;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.ingot.framework.security.provider.token.IngotOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

/**
 * <p>Description  : IngotOAuth2AccessTokenSerializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/26.</p>
 * <p>Time         : 5:20 下午.</p>
 */
public class IngotOAuth2AccessTokenSerializer extends StdSerializer<IngotOAuth2AccessToken> {

    public IngotOAuth2AccessTokenSerializer() {
        super(IngotOAuth2AccessToken.class);
    }

    @Override
    public void serialize(IngotOAuth2AccessToken token, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonGenerationException {
        jgen.writeStartObject();
        jgen.writeStringField(IngotOAuth2AccessToken.ACCESS_TOKEN, token.getValue());
        jgen.writeStringField(IngotOAuth2AccessToken.TOKEN_TYPE, token.getTokenType());
        OAuth2RefreshToken refreshToken = token.getRefreshToken();
        if (refreshToken != null) {
            jgen.writeStringField(IngotOAuth2AccessToken.REFRESH_TOKEN, refreshToken.getValue());
        }
        Date expiration = token.getExpiration();
        if (expiration != null) {
            long now = System.currentTimeMillis();
            jgen.writeNumberField(IngotOAuth2AccessToken.EXPIRES_IN, (expiration.getTime() - now) / 1000);
        }
        Set<String> scope = token.getScope();
        if (scope != null && !scope.isEmpty()) {
            StringBuilder scopes = new StringBuilder();
            for (String s : scope) {
                Assert.hasLength(s, "Scopes cannot be null or empty. Got " + scope + "");
                scopes.append(s);
                scopes.append(" ");
            }
            jgen.writeStringField(IngotOAuth2AccessToken.SCOPE, scopes.substring(0, scopes.length() - 1));
        }
        // 不返回 additional information
//        Map<String, Object> additionalInformation = token.getAdditionalInformation();
//        for (String key : additionalInformation.keySet()) {
//            jgen.writeObjectField(key, additionalInformation.get(key));
//        }
        jgen.writeEndObject();
    }
}
