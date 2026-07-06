package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : AllowTenantDTODeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/6.</p>
 * <p>Time         : 9:59 AM.</p>
 */
@Slf4j
final class AllowTenantDTODeserializer extends JsonDeserializer<TenantMainDTO> {
    @Override
    public TenantMainDTO deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode root = mapper.readTree(parser);
        return deserialize(parser, mapper, root);
    }

    private TenantMainDTO deserialize(JsonParser parser, ObjectMapper mapper, JsonNode root)
            throws JsonParseException {
        String id = JsonNodeUtils.findStringValue(root, "id");
        String name = JsonNodeUtils.findStringValue(root, "name");
        String avatar = JsonNodeUtils.findStringValue(root, "avatar");
        Boolean main = JsonNodeUtils.findBooleanValue(root, "main");
        TenantMainDTO result = new TenantMainDTO();
        result.setId(id);
        result.setName(name);
        result.setAvatar(avatar);
        result.setMain(main);
        return result;
    }
}
