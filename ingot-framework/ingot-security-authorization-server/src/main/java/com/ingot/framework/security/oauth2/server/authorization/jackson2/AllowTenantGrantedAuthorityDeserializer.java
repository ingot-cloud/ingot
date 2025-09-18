package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.security.core.authority.AllowTenantGrantedAuthority;

/**
 * <p>Description  : AllowTenantGrantedAuthorityDeserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/14.</p>
 * <p>Time         : 11:24 AM.</p>
 */
public class AllowTenantGrantedAuthorityDeserializer extends JsonDeserializer<AllowTenantGrantedAuthority> {

    @Override
    public AllowTenantGrantedAuthority deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode root = mapper.readTree(parser);
        return deserialize(parser, mapper, root);
    }

    private AllowTenantGrantedAuthority deserialize(JsonParser parser, ObjectMapper mapper, JsonNode root)
            throws JsonProcessingException {
        String authority = JsonNodeUtils.findStringValue(root, "authority");
        AllowTenantDTO allow = mapper.readValue(authority, new TypeReference<>() {
        });
        return new AllowTenantGrantedAuthority(allow);
    }
}
