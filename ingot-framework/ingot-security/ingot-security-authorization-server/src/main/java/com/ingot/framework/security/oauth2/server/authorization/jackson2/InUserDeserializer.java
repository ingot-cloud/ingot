package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
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

import static com.ingot.framework.security.oauth2.server.authorization.jackson2.JsonNodeUtils.*;

/**
 * <p>从认证持久化 JSON 恢复用户与单一成员身份，拒绝无效身份结构。</p>
 *
 * <p>仅保留业务身份和当前权限；登录提示 meta 不参与会话恢复。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Slf4j
final class InUserDeserializer extends JsonDeserializer<InUser> {
    /** {@inheritDoc} */
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

        List<Long> deptList = JsonNodeUtils.findValue(
                root, InUserFieldNames.DEPT_IDS, LONG_LIST, mapper);
        Map<Long, List<Long>> tenantDeptsList = JsonNodeUtils.findValue(
                root, InUserFieldNames.TENANT_DEPT_IDS, LONG_LIST_MAP, mapper);

        return InUser.stateless(id, tenantId, clientId, tokenAuthType, userType, username, authorities,
                deptList, tenantDeptsList).toBuilder()
                .authorizationContext(readIdentity(parser, root)).build();
    }
    private AuthorizationContext readIdentity(JsonParser parser, JsonNode root) throws JsonParseException {
        JsonNode node = root.get(InUserFieldNames.AUTHORIZATION_CONTEXT);
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            if (!node.isObject()) {
                throw new IllegalArgumentException();
            }
            return new AuthorizationContext(AuthorizationDomain.valueOf(requiredText(node, InUserFieldNames.IDENTITY_DOMAIN)),
                    node.hasNonNull(InUserFieldNames.TENANT_ID) ? requiredText(node, InUserFieldNames.TENANT_ID) : null,
                    requiredText(node, InUserFieldNames.IDENTITY_ACCOUNT_ID), requiredText(node, InUserFieldNames.IDENTITY_MEMBER_ID));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException(parser, "IAM 会话身份结构无效");
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException();
        }
        return value.asText();
    }
}
