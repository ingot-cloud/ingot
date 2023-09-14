package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.core.userdetails.IngotUserFieldNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;

import static com.ingot.framework.security.oauth2.server.authorization.jackson2.JsonNodeUtils.GRANTED_AUTH_COLL;

/**
 * <p>Description  : IngotUserDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 4:52 下午.</p>
 */
@Slf4j
final class IngotUserDeserializer extends JsonDeserializer<IngotUser> {
    @Override
    public IngotUser deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode root = mapper.readTree(parser);
        return deserialize(parser, mapper, root);
    }

    private IngotUser deserialize(JsonParser parser, ObjectMapper mapper, JsonNode root)
            throws JsonParseException {
        Long id = JsonNodeUtils.findNumberValue(root, IngotUserFieldNames.ID).longValue();
        Number tenantIdNumber = JsonNodeUtils.findNumberValue(root, IngotUserFieldNames.TENANT_ID);
        Long tenantId = tenantIdNumber != null ? tenantIdNumber.longValue() : null;
        String clientId = JsonNodeUtils.findStringValue(root, IngotUserFieldNames.CLIENT_ID);
        String tokenAuthType = JsonNodeUtils.findStringValue(root, IngotUserFieldNames.TOKEN_AUTH_TYPE);
        String userType = JsonNodeUtils.findStringValue(root, IngotUserFieldNames.USER_TYPE);
        String username = JsonNodeUtils.findStringValue(root, IngotUserFieldNames.USERNAME);
        Collection<? extends GrantedAuthority> authorities = JsonNodeUtils.findValue(
                root, IngotUserFieldNames.AUTHORITIES, GRANTED_AUTH_COLL, mapper);
        return IngotUser.stateless(id, tenantId, clientId, tokenAuthType, username, userType, authorities);
    }
}
