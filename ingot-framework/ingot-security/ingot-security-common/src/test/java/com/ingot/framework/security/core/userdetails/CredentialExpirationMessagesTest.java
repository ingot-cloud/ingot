package com.ingot.framework.security.core.userdetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.security.CredentialExpirationReason;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.InSecurityMessageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证场景提示、RPC 透传及运行中修改／删除配置，同时保持认证阻断。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CredentialExpirationMessagesTest {
    private static final String INITIAL_DEFAULT = "初始密码或重置后的密码已过期，请联系管理员重置密码后再登录。";
    private static final String PASSWORD_DEFAULT = "密码已过期，当前无法登录，请联系管理员重置密码后再登录。";
    private static final String UNKNOWN_DEFAULT = "登录密码已过期，请联系管理员重置密码后再登录。";

    private final Map<String, Object> liveConfig = new HashMap<>();
    private final MockEnvironment environment = new MockEnvironment();
    private final InUserDetailsChecker checker;

    CredentialExpirationMessagesTest() {
        environment.getPropertySources().addFirst(new MapPropertySource("nacos-messages", liveConfig));
        checker = new InUserDetailsChecker(new InSecurityMessageSource(),
                new CredentialExpirationMessageResolver(environment));
    }

    @ParameterizedTest
    @EnumSource(CredentialExpirationReason.class)
    void missingConfigurationUsesSceneDefault(CredentialExpirationReason reason) {
        assertEquals(defaultMessage(reason), expiredMessage(reason.getValue()));
    }

    @ParameterizedTest
    @EnumSource(CredentialExpirationReason.class)
    void changesAndDeletionAreVisibleToTheSameChecker(CredentialExpirationReason reason) {
        String key = propertyKey(reason);
        environment.withProperty("support", "不应替换到文案中的环境变量");
        liveConfig.put(key, "联系管理员 {0}，${support} 只是文字");
        assertEquals("联系管理员 {0}，${support} 只是文字", expiredMessage(reason.getValue()));
        liveConfig.put(key, "请联系服务台重置密码");
        assertEquals("请联系服务台重置密码", expiredMessage(reason.getValue()));
        liveConfig.remove(key);
        assertEquals(defaultMessage(reason), expiredMessage(reason.getValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n\t"})
    void blankOverridesUseEachSceneDefault(String blank) {
        for (CredentialExpirationReason reason : CredentialExpirationReason.values()) {
            liveConfig.put(propertyKey(reason), blank);
            assertEquals(defaultMessage(reason), expiredMessage(reason.getValue()));
        }
    }

    @Test
    void overridesAreIndependent() {
        liveConfig.put(propertyKey(CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED), "初始密码自定义");
        liveConfig.put(propertyKey(CredentialExpirationReason.PASSWORD_EXPIRED), "普通密码自定义");
        liveConfig.put(propertyKey(CredentialExpirationReason.UNKNOWN), "通用提示自定义");
        assertEquals("初始密码自定义", expiredMessage(CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue()));
        assertEquals("普通密码自定义", expiredMessage(CredentialExpirationReason.PASSWORD_EXPIRED.getValue()));
        assertEquals("通用提示自定义", expiredMessage(null));
    }

    @ParameterizedTest
    @MethodSource("unrecognizedReasons")
    void malformedOrMissingReasonStillBlocksWithGenericMessage(Object reason) {
        assertEquals(UNKNOWN_DEFAULT, expiredMessage(reason));
    }

    static Stream<Object> unrecognizedReasons() {
        return Stream.of(null, "future-reason", "", 42, List.of("INITIAL_PASSWORD_EXPIRED"),
                Map.of("value", "INITIAL_PASSWORD_EXPIRED"), CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED);
    }

    @Test
    void invalidConfigurationStillBlocksUsingDefault() {
        liveConfig.put(propertyKey(CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED), Map.of("nested", "value"));
        assertEquals(INITIAL_DEFAULT, expiredMessage(CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue()));
    }

    @Test
    void rpcJsonRetainsReasonAndLockMetadata() throws Exception {
        UserDetailsResponse response = new UserDetailsResponse();
        response.setId(1L);
        response.setUsername("user");
        response.setPassword("hash");
        response.setUserType(UserTypeEnum.ADMIN.getValue());
        response.setEnabled(true);
        response.setLocked(false);
        response.setCredentialsNonExpired(false);
        response.setMeta(Map.of(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON,
                CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue(), InUserMetaKeys.FAILED_LOGIN_COUNT, 2));
        ObjectMapper mapper = new ObjectMapper();
        UserDetailsResponse decoded = mapper.readValue(mapper.writeValueAsBytes(response), UserDetailsResponse.class);
        OAuth2UserDetailsService service = username -> null;
        InUserDetails user = service.parse(R.ok(decoded));
        assertEquals(INITIAL_DEFAULT,
                assertThrows(CredentialsExpiredException.class, () -> checker.check(user)).getMessage());
        assertEquals(2, ((InUser) user).getMetaValue(InUserMetaKeys.FAILED_LOGIN_COUNT, Integer.class));
    }

    @Test
    void reasonNeverChangesAdmissionOrOtherStatusChecks() {
        Map<String, Object> reason = Map.of(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON,
                CredentialExpirationReason.INITIAL_PASSWORD_EXPIRED.getValue());
        assertDoesNotThrow(() -> checker.check(user(true, true, true, true, reason)));
        assertThrows(DisabledException.class, () -> checker.check(user(false, true, false, true, reason)));
        assertThrows(AccountExpiredException.class, () -> checker.check(user(true, false, false, true, reason)));
        assertEquals("账号已被锁定，请联系管理员",
                assertThrows(LockedException.class, () -> checker.check(user(true, true, true, false, reason))).getMessage());
    }

    @Test
    void plainUserAndLegacyConstructorUseGenericDefault() {
        var user = User.withUsername("user").password("hash").credentialsExpired(true).authorities("login").build();
        assertEquals(UNKNOWN_DEFAULT,
                assertThrows(CredentialsExpiredException.class, () -> new InUserDetailsChecker().check(user)).getMessage());
    }

    private String expiredMessage(Object reason) {
        Map<String, Object> meta = reason == null ? Map.of() : Map.of(InUserMetaKeys.CREDENTIAL_EXPIRATION_REASON, reason);
        return assertThrows(CredentialsExpiredException.class,
                () -> checker.check(user(true, true, false, true, meta))).getMessage();
    }

    private InUser user(boolean enabled, boolean accountValid, boolean credentialsValid, boolean unlocked,
                        Map<String, Object> meta) {
        return InUser.userDetails(1L, UserTypeEnum.ADMIN.getValue(), null, "user", "hash",
                enabled, accountValid, credentialsValid, unlocked, List.of(), meta, List.of(), Map.of());
    }

    private String propertyKey(CredentialExpirationReason reason) {
        return LoginFailureMessageProperties.PREFIX + switch (reason) {
            case INITIAL_PASSWORD_EXPIRED -> ".initial-password-expired";
            case PASSWORD_EXPIRED -> ".password-expired";
            case UNKNOWN -> ".credentials-expired";
        };
    }

    private String defaultMessage(CredentialExpirationReason reason) {
        return switch (reason) {
            case INITIAL_PASSWORD_EXPIRED -> INITIAL_DEFAULT;
            case PASSWORD_EXPIRED -> PASSWORD_DEFAULT;
            case UNKNOWN -> UNKNOWN_DEFAULT;
        };
    }
}
