package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UsernamePasswordAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

import static com.ingot.framework.security.oauth2.server.authorization.jackson2.JsonNodeUtils.GRANTED_AUTH_COLL;
import static com.ingot.framework.security.oauth2.server.authorization.jackson2.JsonNodeUtils.INGOT_USER;

/**
 * <p>Description  : OAuth2UsernamePasswordAuthenticationTokenDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/11.</p>
 * <p>Time         : 5:50 下午.</p>
 */
@Slf4j
final class OAuth2UsernamePasswordAuthenticationTokenDeserializer
        extends JsonDeserializer<OAuth2UsernamePasswordAuthenticationToken> {
    @Override
    public OAuth2UsernamePasswordAuthenticationToken deserialize(
            JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode root = mapper.readTree(parser);
        return deserialize(parser, mapper, root);
    }

    private OAuth2UsernamePasswordAuthenticationToken deserialize(JsonParser parser, ObjectMapper mapper, JsonNode root)
            throws JsonParseException {
        IngotUser principal = JsonNodeUtils.findValue(root, "principal", INGOT_USER, mapper);
        Object credentials = JsonNodeUtils.findObjectNode(root, "credentials");
        Collection<? extends GrantedAuthority> authorities = JsonNodeUtils.findValue(
                root, "authorities", GRANTED_AUTH_COLL, mapper);
        return new OAuth2UsernamePasswordAuthenticationToken(principal, credentials, authorities, null);
    }
}
