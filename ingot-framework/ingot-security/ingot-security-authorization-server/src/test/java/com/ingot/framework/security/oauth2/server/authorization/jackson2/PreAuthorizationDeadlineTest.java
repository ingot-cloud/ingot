package com.ingot.framework.security.oauth2.server.authorization.jackson2;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.jackson2.InSecurityJackson2Modules;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.OAuth2PreAuthorizationAuthenticationTokenConverter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * Auth预授权序列化保持原始绝对截止，内部响应输出UTC Z。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class PreAuthorizationDeadlineTest {

    @Test
    void roundtripKeepsOriginalDeadlineAndConverterReturnsUtc() throws Exception {
        var user = InUser.stateless(1L, null, "client", "standard", "0", "operator", List.of(), List.of(), Map.of());
        long deadline = Instant.now().getEpochSecond() + 17;
        var token = new OAuth2PreAuthorizationCodeRequestAuthenticationToken(user, Map.of(), List.of(), 600, deadline);
        var mapper = new ObjectMapper();
        InSecurityJackson2Modules.registerModules(mapper, getClass().getClassLoader());
        var restored = mapper.readValue(mapper.writeValueAsBytes(token),
                OAuth2PreAuthorizationCodeRequestAuthenticationToken.class);
        assertEquals(deadline, restored.getAuthenticationExpiresAt());
        assertEquals(600, restored.getTimeToLive());
        var converted = new OAuth2PreAuthorizationAuthenticationTokenConverter().convert(restored);
        assertEquals(Instant.ofEpochSecond(deadline).toString(),
                ((Map<?, ?>) converted.get(R.DATA)).get(BffConstants.AUTHENTICATION_EXPIRES_AT));
    }

}
