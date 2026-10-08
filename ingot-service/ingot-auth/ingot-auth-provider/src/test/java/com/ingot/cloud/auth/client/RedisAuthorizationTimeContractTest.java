package com.ingot.cloud.auth.client;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.data.redis.config.InRedisTemplateConfiguration;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.authorization.OnlineSessionRegistration;
import com.ingot.framework.security.oauth2.server.authorization.RedisOAuth2AuthorizationService;
import com.ingot.framework.security.oauth2.server.authorization.RedisOnlineTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>真实 Redis 验证授权码、令牌索引、会话续期、类型信息、TTL 和撤销与 API 编码隔离。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@EnabledIfEnvironmentVariable(named = "TIME_TEST_REDIS_HOST", matches = ".+")
class RedisAuthorizationTimeContractTest {
    @Test
    void authorizationAndSessionInstantsSurviveRoundTripRefreshAndRevocation() {
        var config = new RedisStandaloneConfiguration(System.getenv("TIME_TEST_REDIS_HOST"),
                Integer.parseInt(System.getenv().getOrDefault("TIME_TEST_REDIS_PORT", "6379")));
        String password = System.getenv("TIME_TEST_REDIS_PASSWORD");
        if (password != null && !password.isEmpty()) config.setPassword(password);
        var factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        factory.start();
        var template = new InRedisTemplateConfiguration().redisTemplate(factory);
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                String prefix = "time-contract-" + UUID.randomUUID();
                var client = RegisteredClient.withId(prefix).clientId(prefix)
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("https://example.test/callback").scope("probe").build();
                var sessions = new RedisOnlineTokenService(template, new InSecurityProperties());
                var authorizations = new RedisOAuth2AuthorizationService(template, sessions,
                        new InMemoryRegisteredClientRepository(client));
                Instant issued = Instant.now();
                Instant accessUntil = issued.plusSeconds(120);
                Instant sessionUntil = issued.plusSeconds(600);
                var code = new OAuth2AuthorizationCode(prefix + "-code", issued, issued.plusSeconds(60));
                var access = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, prefix + "-access",
                        issued, accessUntil, Set.of("probe"));
                var refresh = new OAuth2RefreshToken(prefix + "-refresh", issued, sessionUntil);
                var authorization = OAuth2Authorization.withRegisteredClient(client).id(prefix)
                        .principalName(prefix).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizedScopes(Set.of("probe")).attribute("deadline", sessionUntil.toEpochMilli())
                        .token(code).token(access).token(refresh).build();
                var user = InUser.stateless(987654321L, 987654321L, prefix, "standard", "0", prefix,
                        List.of(), List.of(), Map.of()).toBuilder().meta(Map.of("deadline", sessionUntil.toEpochMilli())).build();
                try {
                    authorizations.save(authorization);
                    sessions.save(user, new OnlineSessionRegistration(prefix, prefix + "-jti", accessUntil, sessionUntil));
                    var loaded = authorizations.findByToken(code.getTokenValue(), new OAuth2TokenType("code"));
                    assertNotNull(loaded);
                    assertEquals(code.getIssuedAt(), loaded.getToken(OAuth2AuthorizationCode.class).getToken().getIssuedAt());
                    assertEquals(sessionUntil, loaded.getRefreshToken().getToken().getExpiresAt());
                    assertEquals(prefix, authorizations.findByToken(access.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN).getId());
                    assertEquals(prefix, authorizations.findByToken(refresh.getTokenValue(), OAuth2TokenType.REFRESH_TOKEN).getId());
                    var online = sessions.getBySid(prefix).orElseThrow();
                    assertEquals(sessionUntil, online.getExpiresAt());
                    assertEquals(sessionUntil.toEpochMilli(), (Long) loaded.getAttribute("deadline"));
                    Instant firstIssuedAt = online.getIssuedAt();
                    Long ttl = template.getExpire(RedisKeyConstants.OnlineToken.sidKey(prefix));
                    assertNotNull(ttl);
                    assertTrue(ttl > 120 && ttl <= 600);

                    var renewedAccess = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, prefix + "-renewed-access",
                            issued.plusSeconds(1), issued.plusSeconds(121), Set.of("probe"));
                    authorization = OAuth2Authorization.from(loaded).token(renewedAccess).build();
                    authorizations.save(authorization);
                    sessions.save(user, new OnlineSessionRegistration(prefix, prefix + "-renewed-jti",
                            renewedAccess.getExpiresAt(), sessionUntil));
                    var renewed = sessions.getBySid(prefix).orElseThrow();
                    assertEquals(prefix, renewed.getSid());
                    assertEquals(prefix + "-renewed-jti", renewed.getJti());
                    assertEquals(firstIssuedAt, renewed.getIssuedAt());
                    assertEquals(sessionUntil, renewed.getExpiresAt());
                    assertEquals(prefix, authorizations.findByToken(renewedAccess.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN).getId());
                    authorizations.remove(authorization);
                    assertNull(authorizations.findById(prefix));
                    assertNull(authorizations.findByToken(refresh.getTokenValue(), OAuth2TokenType.REFRESH_TOKEN));
                    assertNull(authorizations.findByToken(renewedAccess.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN));
                    assertTrue(sessions.getBySid(prefix).isEmpty());
                    assertTrue(sessions.listSids(987654321L, prefix, 987654321L).isEmpty());
                } finally {
                    authorizations.remove(authorization);
                    // 所有键都带独立随机前缀；清理刷新前旧索引以及异常路径残留。
                    String onlineKey = RedisKeyConstants.OnlineToken.onlineUserKey(987654321L, prefix);
                    template.opsForSet().remove(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY, onlineKey);
                    template.delete(List.of(RedisKeyConstants.OnlineToken.sidKey(prefix),
                            RedisKeyConstants.OnlineToken.userSetKey(987654321L, prefix, 987654321L), onlineKey));
                    for (String token : List.of(code.getTokenValue(), access.getTokenValue(), refresh.getTokenValue(), prefix + "-renewed-access")) {
                        template.delete("oauth2:token:" + com.ingot.framework.commons.utils.DigestUtil.sha256(token));
                    }
                }
            }
        } finally {
            TimeZone.setDefault(original);
            factory.destroy();
        }
    }
}
