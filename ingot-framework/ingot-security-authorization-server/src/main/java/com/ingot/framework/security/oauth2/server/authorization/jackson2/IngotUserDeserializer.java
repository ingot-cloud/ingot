package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.core.userdetails.IngotUser;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * <p>Description  : IngotUserDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 4:52 下午.</p>
 */
@Slf4j
public class IngotUserDeserializer extends JsonDeserializer<IngotUser> {
    @Override
    public IngotUser deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode root = mapper.readTree(parser);
        return deserialize(parser, mapper, root);
    }

    private IngotUser deserialize(JsonParser parser, ObjectMapper mapper, JsonNode root)
            throws JsonParseException {
        Long id = Long.parseLong(JsonNodeUtils.findStringValue(root, "id"));
        Long deptId = Long.parseLong(JsonNodeUtils.findStringValue(root, "deptId"));
        Integer tenantId = Integer.parseInt(JsonNodeUtils.findStringValue(root, "tenantId"));
        String tokenAuthMethod = JsonNodeUtils.findStringValue(root, "tokenAuthenticationMethod");
        String username = JsonNodeUtils.findStringValue(root, "username");
        return new IngotUser(id, deptId, tenantId, tokenAuthMethod, username);
    }
}
