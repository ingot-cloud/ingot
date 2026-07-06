package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

import static com.ingot.framework.security.oauth2.server.authorization.jackson2.JsonNodeUtils.*;

/**
 * <p>Description  : {@link InUser} Deserializer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/26.</p>
 * <p>Time         : 4:52 下午.</p>
 * <p>
 * 仅从 JSON 还原 {@link InUser} 的业务核心字段，meta 不参与序列化，
 * 因此无状态会话恢复时 meta 为 {@code null}（符合预期：meta 只在登录流程生效）。
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

        List<Long> deptList = JsonNodeUtils.findValue(
                root, InUserFieldNames.DEPT_IDS, LONG_LIST, mapper);
        Map<Long, List<Long>> tenantDeptsList = JsonNodeUtils.findValue(
                root, InUserFieldNames.TENANT_DEPT_IDS, LONG_LIST_MAP, mapper);

        return InUser.stateless(id, tenantId, clientId, tokenAuthType, userType, username, authorities,
                deptList, tenantDeptsList);
    }
}
