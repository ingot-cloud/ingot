package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.core.userdetails.InUserFieldNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;
import java.util.Collection;

import static com.ingot.framework.security.oauth2.server.authorization.jackson2.JsonNodeUtils.GRANTED_AUTH_COLL;

/**
 * <p>Description  : {@link InUser} Deserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 4:52 下午.</p>
 */
@Slf4j
final class InUserDeserializer extends JsonDeserializer<InUser> {
    @Override
    public InUser deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode root = mapper.readTree(parser);
        return deserialize(parser, mapper, root);
    }

    private InUser deserialize(JsonParser parser, ObjectMapper mapper, JsonNode root)
            throws JsonParseException {
        Long id = JsonNodeUtils.findNumberValue(root, InUserFieldNames.ID).longValue();
        Number tenantIdNumber = JsonNodeUtils.findNumberValue(root, InUserFieldNames.TENANT_ID);
        Long tenantId = tenantIdNumber != null ? tenantIdNumber.longValue() : null;
        String clientId = JsonNodeUtils.findStringValue(root, InUserFieldNames.CLIENT_ID);
        String tokenAuthType = JsonNodeUtils.findStringValue(root, InUserFieldNames.TOKEN_AUTH_TYPE);
        String userType = JsonNodeUtils.findStringValue(root, InUserFieldNames.USER_TYPE);
        String username = JsonNodeUtils.findStringValue(root, InUserFieldNames.USERNAME);
        Collection<? extends GrantedAuthority> authorities = JsonNodeUtils.findValue(
                root, InUserFieldNames.AUTHORITIES, GRANTED_AUTH_COLL, mapper);
        return InUser.stateless(id, tenantId, clientId, tokenAuthType, username, userType, authorities);
    }
}
