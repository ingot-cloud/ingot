package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisOnlineTokenService} 键写入、TTL、墓碑收敛与删除时的索引回写。
 *
 * @author jy
 * @since 1.0.0
 */
class RedisOnlineTokenServiceTest {

    private static final String SID = "session-1";
    private static final String SID_OTHER = "session-2";
    private static final String NEW_JTI = "jwt-new";
    private static final long USER_ID = 9L;
    private static final long TENANT_ID = 1L;
    private static final String CLIENT_ID = "web";
    private static final String IP = "10.0.0.7";

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final SetOperations<String, Object> setOps = mock(SetOperations.class);
    @SuppressWarnings("unchecked")
    private final ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);

    private final InSecurityProperties properties = new InSecurityProperties();
    private RedisOnlineTokenService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        service = new RedisOnlineTokenService(redisTemplate, properties);
    }

    @Test
    void save_writesSessionAndUserSetWithoutJtiOrUniqueIndex() {
        service.save(user(), registration(NEW_JTI));

        verify(valueOps).set(eq(RedisKeyConstants.OnlineToken.sidKey(SID)),
                any(OnlineToken.class), anyLong(), eq(TimeUnit.SECONDS));
        verify(setOps).add(RedisKeyConstants.OnlineToken.userSetKey(TENANT_ID, CLIENT_ID, USER_ID), SID);
        verify(setOps).add(eq(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY),
                eq(RedisKeyConstants.OnlineToken.onlineUserKey(TENANT_ID, CLIENT_ID)));
        verify(zSetOps).add(eq(RedisKeyConstants.OnlineToken.onlineUserKey(TENANT_ID, CLIENT_ID)),
                eq(USER_ID), anyDouble());
        verify(valueOps, never()).set(anyString(), eq(SID), anyLong(), any(TimeUnit.class));
    }

    @Test
    void save_userSetWithoutTtl_setsExpire() {
        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(TENANT_ID, CLIENT_ID, USER_ID);
        when(redisTemplate.getExpire(userSetKey, TimeUnit.SECONDS)).thenReturn(-1L);

        service.save(user(), registration(NEW_JTI));

        verify(redisTemplate).expire(eq(userSetKey), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void save_userSetTtl_onlyExtends() {
        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(TENANT_ID, CLIENT_ID, USER_ID);
        when(redisTemplate.getExpire(userSetKey, TimeUnit.SECONDS)).thenReturn(Long.MAX_VALUE);

        service.save(user(), registration(NEW_JTI));

        verify(redisTemplate, never()).expire(eq(userSetKey), anyLong(), any(TimeUnit.class));
    }

    @Test
    void save_onRenew_keepsIssuedAtAndDoesNotWriteJtiIndex() {
        OnlineToken previous = existingSession();
        when(valueOps.get(RedisKeyConstants.OnlineToken.sidKey(SID))).thenReturn(previous);

        service.save(user(), registration(NEW_JTI));

        verify(valueOps).set(eq(RedisKeyConstants.OnlineToken.sidKey(SID)),
                any(OnlineToken.class), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void save_ipSetAtCap_skipsSadd() {
        String ipSetKey = RedisKeyConstants.OnlineToken.ipSetKey(TENANT_ID, IP);
        OnlineToken previous = existingSession();
        previous.setIpAddress(IP);
        when(valueOps.get(RedisKeyConstants.OnlineToken.sidKey(SID))).thenReturn(previous);
        when(setOps.size(ipSetKey)).thenReturn(1000L);

        service.save(user(), registration(NEW_JTI));

        verify(setOps, never()).add(ipSetKey, SID);
    }

    @Test
    void removeBySid_clearsIndexesAndOnlineMarkWhenLastSession() {
        OnlineToken session = existingSession();
        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(TENANT_ID, CLIENT_ID, USER_ID);
        when(valueOps.get(RedisKeyConstants.OnlineToken.sidKey(SID))).thenReturn(session);
        when(setOps.members(userSetKey)).thenReturn(Set.of());

        service.removeBySid(SID);

        verify(redisTemplate).delete(RedisKeyConstants.OnlineToken.sidKey(SID));
        verify(setOps).remove(userSetKey, SID);
        verify(zSetOps).remove(RedisKeyConstants.OnlineToken.onlineUserKey(TENANT_ID, CLIENT_ID), USER_ID);
        verify(redisTemplate).delete(userSetKey);
    }

    @Test
    void removeBySid_rewritesOnlineScoreWhenOtherSessionsRemain() {
        Instant remainingExpiry = Instant.parse("2026-08-21T00:00:00Z");
        OnlineToken session = existingSession();
        OnlineToken other = existingSession();
        other.setSid(SID_OTHER);
        other.setExpiresAt(remainingExpiry);

        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(TENANT_ID, CLIENT_ID, USER_ID);
        when(valueOps.get(RedisKeyConstants.OnlineToken.sidKey(SID))).thenReturn(session);
        when(setOps.members(userSetKey)).thenReturn(Set.of(SID_OTHER));
        when(valueOps.multiGet(List.of(RedisKeyConstants.OnlineToken.sidKey(SID_OTHER))))
                .thenReturn(List.of(other));

        service.removeBySid(SID);

        verify(zSetOps, never()).remove(
                RedisKeyConstants.OnlineToken.onlineUserKey(TENANT_ID, CLIENT_ID), USER_ID);
        verify(zSetOps).add(RedisKeyConstants.OnlineToken.onlineUserKey(TENANT_ID, CLIENT_ID),
                USER_ID, remainingExpiry.toEpochMilli());
    }

    @Test
    void listSids_removesTombstones() {
        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(TENANT_ID, CLIENT_ID, USER_ID);
        when(setOps.members(userSetKey)).thenReturn(Set.of(SID, "session-dead"));
        when(valueOps.multiGet(any())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            return keys.stream()
                    .map(key -> key.endsWith(SID) ? existingSession() : null)
                    .toList();
        });

        List<String> sids = service.listSids(TENANT_ID, CLIENT_ID, USER_ID);

        assertEquals(List.of(SID), sids);
        verify(setOps).remove(eq(userSetKey), (Object[]) any());
    }

    @Test
    void cleanAllExpiredOnlineUsers_deletesUserSetOfExpiredMembers() {
        String onlineKey = RedisKeyConstants.OnlineToken.onlineUserKey(TENANT_ID, CLIENT_ID);
        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(TENANT_ID, CLIENT_ID, USER_ID);
        when(setOps.members(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY)).thenReturn(Set.of(onlineKey));
        when(redisTemplate.hasKey(onlineKey)).thenReturn(true, false);
        when(zSetOps.rangeByScore(eq(onlineKey), eq(0d), anyDouble())).thenReturn(Set.of(USER_ID));
        when(zSetOps.removeRangeByScore(eq(onlineKey), eq(0d), anyDouble())).thenReturn(1L);

        assertEquals(1L, service.cleanAllExpiredOnlineUsers());
        verify(redisTemplate).delete(userSetKey);
        verify(setOps).remove(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY, onlineKey);
    }

    private OnlineSessionRegistration registration(String jti) {
        Instant now = Instant.now();
        return new OnlineSessionRegistration(SID, jti, now.plusSeconds(60), now.plusSeconds(600));
    }

    private OnlineToken existingSession() {
        return OnlineToken.builder()
                .sid(SID)
                .jti("jwt-old")
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .clientId(CLIENT_ID)
                .authType(TokenAuthTypeEnum.STANDARD.getValue())
                .userType(UserTypeEnum.ADMIN.getValue())
                .issuedAt(Instant.now().minusSeconds(300))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private InUser user() {
        return InUser.stateless(USER_ID, TENANT_ID, CLIENT_ID, TokenAuthTypeEnum.STANDARD.getValue(),
                UserTypeEnum.ADMIN.getValue(), "admin", List.of(), List.of(2L), null);
    }
}
