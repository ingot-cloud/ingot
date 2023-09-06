package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * <p>Description  : OAuth2PreAuthorizationCodeRequestAuthenticationTokenDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/6.</p>
 * <p>Time         : 9:48 AM.</p>
 */
@Slf4j
final class OAuth2PreAuthorizationCodeRequestAuthenticationTokenDeserializer
        extends JsonDeserializer<OAuth2PreAuthorizationCodeRequestAuthenticationToken> {

    @Override
    public OAuth2PreAuthorizationCodeRequestAuthenticationToken deserialize(
            JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode root = mapper.readTree(parser);
        return deserialize(parser, mapper, root);
    }

    private OAuth2PreAuthorizationCodeRequestAuthenticationToken deserialize(JsonParser parser, ObjectMapper mapper, JsonNode root)
            throws JsonParseException {
        OAuth2UserDetailsAuthenticationToken principal = JsonNodeUtils.findValue(root, "principal",
                new TypeReference<>() {
                }, mapper);
        List<AllowTenantDTO> allows = JsonNodeUtils.findValue(
                root, "allowList", new TypeReference<>() {
                }, mapper);
        return OAuth2PreAuthorizationCodeRequestAuthenticationToken
                .authenticated(principal, allows);
    }
}
