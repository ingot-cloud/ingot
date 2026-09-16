package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * <p>{@link OnlineTokenService} 的 Redis 实现，以 sid 为主键并维护用户集合、在线用户与 IP 三类查询索引。</p>
 *
 * <p>存储结构见 {@link RedisKeyConstants.OnlineToken}：sid 键是唯一权威主数据，其余
 * userSet / ip / online 均为可重建的索引。任何在线态判定都必须回到 sid 主数据，
 * 不能只信索引存在。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @implNote 会话主数据 TTL 对齐 Refresh Token 寿命。集合类索引采用「只延长不缩短」的
 * TTL 策略，避免多会话场景下被最短寿命的会话拖垮；{@code SADD} 新建的 key 无过期，
 * {@link #extendExpire} 必须把 {@code TTL=-1} 补成有限过期。
 */
@Slf4j
@RequiredArgsConstructor
public class RedisOnlineTokenService implements OnlineTokenService {

    /**
     * {@code getExpire}：键不存在。
     */
    private static final long KEY_NOT_FOUND = -2L;

    /**
     * ZSet 成员解析失败时的占位值。
     */
    private static final long INVALID_USER_ID = -1L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final InSecurityProperties properties;

    @Override
    public void save(InUser user, OnlineSessionRegistration registration) {
        long sessionTtl = remainingSeconds(registration.sessionExpiresAt());
        if (sessionTtl <= 0) {
            log.warn("[RedisOnlineTokenService] 会话已过期，跳过落库: sid={}", registration.sid());
            return;
        }

        String sid = registration.sid();
        OnlineToken previous = getBySid(sid).orElse(null);
        if (previous != null
                && (previous.getAuthorizationContext() != null || user.getAuthorizationContext() != null)
                && (!Objects.equals(previous.getAuthorizationContext(), user.getAuthorizationContext())
                    || !Objects.equals(previous.getUserId(), user.getId())
                    || !Objects.equals(previous.getTenantId(), user.getTenantId())
                    || !Objects.equals(previous.getClientId(), user.getClientId()))) {
            throw new IllegalArgumentException("IAM 身份切换必须建立新的认证会话");
        }
        OnlineToken session = buildSession(user, registration, previous);
        Long tenantId = session.getTenantId();
        String clientId = session.getClientId();
        Long userId = session.getUserId();

        redisTemplate.opsForValue().set(RedisKeyConstants.OnlineToken.sidKey(sid),
                session, sessionTtl, TimeUnit.SECONDS);

        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(tenantId, clientId, userId);
        redisTemplate.opsForSet().add(userSetKey, sid);
        extendExpire(userSetKey, sessionTtl);

        markUserOnline(tenantId, clientId, userId, registration.sessionExpiresAt());
        indexByIp(session, sessionTtl);

        log.debug("[RedisOnlineTokenService] 会话已落库: sid={}, jti={}, userId={}, authType={}, "
                        + "sessionTtl={}s, renew={}",
                sid, session.getJti(), userId, session.getAuthType(), sessionTtl, previous != null);
    }

    @Override
    public Optional<OnlineToken> getBySid(String sid) {
        if (StrUtil.isEmpty(sid)) {
            return Optional.empty();
        }
        Object value = redisTemplate.opsForValue().get(RedisKeyConstants.OnlineToken.sidKey(sid));
        return value instanceof OnlineToken session ? Optional.of(session) : Optional.empty();
    }

    @Override
    public List<String> listSids(Long tenantId, String clientId, Long userId) {
        if (!isValidUserScope(tenantId, clientId, userId)) {
            return Collections.emptyList();
        }
        return onlineSids(RedisKeyConstants.OnlineToken.userSetKey(tenantId, clientId, userId));
    }

    @Override
    public List<String> listSids(Long tenantId, Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Long indexTenant = RedisKeyConstants.OnlineToken.indexTenantId(tenantId);
        List<String> sids = new ArrayList<>();
        for (String clientId : listClientIds(indexTenant)) {
            sids.addAll(listSids(indexTenant, clientId, userId));
        }
        return sids;
    }

    @Override
    public List<String> listSidsByIp(Long tenantId, String ip) {
        if (StrUtil.isEmpty(ip)) {
            return Collections.emptyList();
        }
        return onlineSids(RedisKeyConstants.OnlineToken.ipSetKey(tenantId, ip));
    }

    @Override
    public List<OnlineToken> listUserSessions(Long tenantId, String clientId, Long userId) {
        return loadSessions(listSids(tenantId, clientId, userId));
    }

    @Override
    public List<OnlineToken> listUserSessions(Long tenantId, Long userId) {
        return loadSessions(listSids(tenantId, userId));
    }

    @Override
    public List<OnlineToken> listUserSessions(Long tenantId, String clientId, Collection<Long> userIds) {
        if (StrUtil.isEmpty(clientId) || userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> distinct = userIds.stream().filter(id -> id != null).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyList();
        }
        if (distinct.size() == 1) {
            return listUserSessions(tenantId, clientId, distinct.getFirst());
        }

        List<String> setKeys = distinct.stream()
                .map(userId -> RedisKeyConstants.OnlineToken.userSetKey(tenantId, clientId, userId))
                .toList();
        List<Object> memberSets = redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(RedisOperations operations) {
                for (String setKey : setKeys) {
                    operations.opsForSet().members(setKey);
                }
                return null;
            }
        });

        List<String> sids = new ArrayList<>();
        if (memberSets != null) {
            for (int i = 0; i < setKeys.size() && i < memberSets.size(); i++) {
                if (memberSets.get(i) instanceof Set<?> members && !members.isEmpty()) {
                    sids.addAll(liveSids(setKeys.get(i), members));
                }
            }
        }
        return loadSessions(sids);
    }

    @Override
    public boolean isOnlineSid(String sid) {
        if (StrUtil.isEmpty(sid)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyConstants.OnlineToken.sidKey(sid)));
    }

    @Override
    public void removeBySid(String sid) {
        if (StrUtil.isEmpty(sid)) {
            return;
        }

        OnlineToken session = getBySid(sid).orElse(null);
        redisTemplate.delete(RedisKeyConstants.OnlineToken.sidKey(sid));
        if (session == null) {
            log.debug("[RedisOnlineTokenService] 会话主数据不存在，仅确认删除: sid={}", sid);
            return;
        }

        Long tenantId = session.getTenantId();
        String clientId = session.getClientId();
        Long userId = session.getUserId();

        if (StrUtil.isNotEmpty(session.getIpAddress())) {
            String ipSetKey = RedisKeyConstants.OnlineToken.ipSetKey(tenantId, session.getIpAddress());
            redisTemplate.opsForSet().remove(ipSetKey, sid);
            Long remaining = redisTemplate.opsForSet().size(ipSetKey);
            if (remaining == null || remaining == 0) {
                redisTemplate.delete(ipSetKey);
            }
        }

        String userSetKey = RedisKeyConstants.OnlineToken.userSetKey(tenantId, clientId, userId);
        redisTemplate.opsForSet().remove(userSetKey, sid);
        List<OnlineToken> remaining = loadSessions(onlineSids(userSetKey));
        if (remaining.isEmpty()) {
            redisTemplate.delete(userSetKey);
            redisTemplate.opsForZSet()
                    .remove(RedisKeyConstants.OnlineToken.onlineUserKey(tenantId, clientId), userId);
        } else {
            Instant latest = remaining.stream()
                    .map(OnlineToken::getExpiresAt)
                    .filter(expiresAt -> expiresAt != null)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            if (latest != null) {
                redisTemplate.opsForZSet().add(
                        RedisKeyConstants.OnlineToken.onlineUserKey(tenantId, clientId),
                        userId, latest.toEpochMilli());
            }
        }

        log.info("[RedisOnlineTokenService] 会话已删除: sid={}, userId={}, tenantId={}, clientId={}",
                sid, userId, tenantId, clientId);
    }

    @Override
    public List<Long> getOnlineUsers(Long tenantId, String clientId, long offset, long limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        String onlineKey = RedisKeyConstants.OnlineToken.onlineUserKey(tenantId, clientId);
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(onlineKey, offset, offset + limit - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        double now = Instant.now().toEpochMilli();
        List<Long> userIds = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            if (tuple.getScore() == null || tuple.getScore() <= now) {
                continue;
            }
            long userId = NumberUtil.parseLong(StrUtil.toString(tuple.getValue()), INVALID_USER_ID);
            if (userId != INVALID_USER_ID) {
                userIds.add(userId);
            }
        }
        return userIds;
    }

    @Override
    public long getOnlineUserCount(Long tenantId, String clientId) {
        String onlineKey = RedisKeyConstants.OnlineToken.onlineUserKey(tenantId, clientId);
        Long count = redisTemplate.opsForZSet()
                .count(onlineKey, Instant.now().toEpochMilli(), Double.MAX_VALUE);
        return count == null ? 0L : count;
    }

    @Override
    public long cleanExpiredOnlineUsers(Long tenantId, String clientId) {
        String onlineKey = RedisKeyConstants.OnlineToken.onlineUserKey(tenantId, clientId);
        long removed = removeExpiredMembers(onlineKey, tenantId, clientId);
        if (removed > 0) {
            log.info("[RedisOnlineTokenService] 已清理过期在线用户: tenantId={}, clientId={}, count={}",
                    tenantId, clientId, removed);
        }
        return removed;
    }

    @Override
    public long cleanAllExpiredOnlineUsers() {
        Set<Object> registered = redisTemplate.opsForSet()
                .members(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY);
        if (registered == null || registered.isEmpty()) {
            return 0L;
        }

        long total = 0L;
        for (Object member : registered) {
            String onlineKey = String.valueOf(member);
            Long tenantId = RedisKeyConstants.OnlineToken.parseTenantId(onlineKey);
            String clientId = RedisKeyConstants.OnlineToken.parseClientId(onlineKey, tenantId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey))) {
                total += removeExpiredMembers(onlineKey, tenantId, clientId);
            }
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey))) {
                redisTemplate.opsForSet()
                        .remove(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY, onlineKey);
            }
        }

        if (total > 0) {
            log.info("[RedisOnlineTokenService] 已清理全部过期在线用户: registry={}, count={}",
                    registered.size(), total);
        }
        return total;
    }

    private OnlineToken buildSession(InUser user, OnlineSessionRegistration registration, OnlineToken previous) {
        Instant now = Instant.now();

        OnlineToken.OnlineTokenBuilder builder = OnlineToken.builder()
                .sid(registration.sid())
                .jti(registration.jti())
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .authorizationContext(user.getAuthorizationContext())
                .principalName(user.getUsername())
                .clientId(user.getClientId())
                .authType(user.getTokenAuthType())
                .userType(user.getUserType())
                .authorities(new HashSet<>(InAuthorityUtils.authorityListToSet(
                        user.getAuthorities(), RedisKeyConstants.OnlineToken.indexTenantId(user.getTenantId()))))
                .deptIds(user.getDeptIds() == null ? List.of() : List.copyOf(user.getDeptIds()))
                .expiresAt(registration.sessionExpiresAt())
                .lastAccessAt(now);

        if (previous == null) {
            LoginInfoExtractor.LoginInfo loginInfo = LoginInfoExtractor.extract(null);
            return builder.issuedAt(now)
                    .ipAddress(loginInfo.getIpAddress())
                    .userAgent(loginInfo.getUserAgent())
                    .deviceType(loginInfo.getDeviceType())
                    .os(loginInfo.getOs())
                    .browser(loginInfo.getBrowser())
                    .location(loginInfo.getLocation())
                    .build();
        }

        return builder.issuedAt(previous.getIssuedAt())
                .ipAddress(previous.getIpAddress())
                .userAgent(previous.getUserAgent())
                .deviceType(previous.getDeviceType())
                .os(previous.getOs())
                .browser(previous.getBrowser())
                .location(previous.getLocation())
                .attributes(previous.getAttributes())
                .build();
    }

    private void markUserOnline(Long tenantId, String clientId, Long userId, Instant sessionExpiresAt) {
        String onlineKey = RedisKeyConstants.OnlineToken.onlineUserKey(tenantId, clientId);
        double score = sessionExpiresAt.toEpochMilli();
        Double current = redisTemplate.opsForZSet().score(onlineKey, userId);
        if (current == null || current < score) {
            redisTemplate.opsForZSet().add(onlineKey, userId, score);
        }
        redisTemplate.opsForSet().add(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY, onlineKey);
    }

    /**
     * 写入同 IP 会话索引；集合达到 {@link InSecurityProperties.Session#getIpSetMaxMembers()} 后跳过。
     */
    private void indexByIp(OnlineToken session, long sessionTtl) {
        if (StrUtil.isEmpty(session.getIpAddress())) {
            return;
        }
        String ipSetKey = RedisKeyConstants.OnlineToken.ipSetKey(session.getTenantId(), session.getIpAddress());
        int cap = properties.getSession().getIpSetMaxMembers();
        if (cap > 0) {
            Long size = redisTemplate.opsForSet().size(ipSetKey);
            if (size != null && size >= cap) {
                log.warn("[RedisOnlineTokenService] IP 会话集合已达上限，跳过索引: key={}, size={}, cap={}, sid={}",
                        ipSetKey, size, cap, session.getSid());
                return;
            }
        }
        redisTemplate.opsForSet().add(ipSetKey, session.getSid());
        extendExpire(ipSetKey, sessionTtl);
    }

    private List<String> listClientIds(Long tenantId) {
        Set<Object> registered = redisTemplate.opsForSet()
                .members(RedisKeyConstants.OnlineToken.ONLINE_REGISTRY);
        if (registered == null || registered.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> clientIds = new ArrayList<>();
        for (Object member : registered) {
            String clientId = RedisKeyConstants.OnlineToken
                    .parseClientId(String.valueOf(member), tenantId);
            if (StrUtil.isNotEmpty(clientId)) {
                clientIds.add(clientId);
            }
        }
        return clientIds;
    }

    private List<OnlineToken> loadSessions(List<String> sids) {
        if (sids == null || sids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keys = sids.stream().map(RedisKeyConstants.OnlineToken::sidKey).toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        List<OnlineToken> sessions = new ArrayList<>(sids.size());
        if (values != null) {
            for (Object value : values) {
                if (value instanceof OnlineToken session) {
                    sessions.add(session);
                }
            }
        }
        sessions.sort(Comparator.comparing(OnlineToken::getIssuedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return sessions;
    }

    /**
     * 读取 sid 集合并剔除主数据已消亡的成员。
     */
    private List<String> onlineSids(String setKey) {
        Set<Object> members = redisTemplate.opsForSet().members(setKey);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return liveSids(setKey, members);
    }

    /**
     * 用主数据 {@code MGET} 过滤仍在线的 sid，墓碑从集合中 {@code SREM}；集合空了则删除 key。
     */
    private List<String> liveSids(String setKey, Set<?> members) {
        List<String> sids = new ArrayList<>(members.size());
        for (Object member : members) {
            if (member != null) {
                sids.add(String.valueOf(member));
            }
        }
        if (sids.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = sids.stream().map(RedisKeyConstants.OnlineToken::sidKey).toList();
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        List<String> live = new ArrayList<>(sids.size());
        List<Object> dead = new ArrayList<>();
        for (int i = 0; i < sids.size(); i++) {
            Object value = values != null && i < values.size() ? values.get(i) : null;
            if (value instanceof OnlineToken) {
                live.add(sids.get(i));
            } else {
                dead.add(sids.get(i));
            }
        }
        if (!dead.isEmpty()) {
            redisTemplate.opsForSet().remove(setKey, dead.toArray());
            Long remaining = redisTemplate.opsForSet().size(setKey);
            if (remaining == null || remaining == 0) {
                redisTemplate.delete(setKey);
            }
        }
        return live;
    }

    /**
     * 移除在线用户 ZSet 中 score 已早于当前时间的成员，并删除对应的用户会话集合。
     */
    private long removeExpiredMembers(String onlineKey, Long tenantId, String clientId) {
        double now = Instant.now().toEpochMilli();
        Set<Object> expired = redisTemplate.opsForZSet().rangeByScore(onlineKey, 0, now);
        if (expired != null && tenantId != null && StrUtil.isNotEmpty(clientId)) {
            for (Object member : expired) {
                long userId = NumberUtil.parseLong(StrUtil.toString(member), INVALID_USER_ID);
                if (userId != INVALID_USER_ID) {
                    redisTemplate.delete(RedisKeyConstants.OnlineToken.userSetKey(tenantId, clientId, userId));
                }
            }
        }
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(onlineKey, 0, now);
        return removed == null ? 0L : removed;
    }

    /**
     * 延长键的过期时间；已有更长 TTL 时不做变更。
     *
     * <p>{@code TTL=-1}（存在但未设过期）视为尚未补 TTL，必须写入本次寿命。
     * {@code TTL=-2}（键不存在）跳过。</p>
     */
    private void extendExpire(String key, long ttlSeconds) {
        Long current = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (current != null && current == KEY_NOT_FOUND) {
            return;
        }
        if (current != null && current >= ttlSeconds) {
            return;
        }
        redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    private boolean isValidUserScope(Long tenantId, String clientId, Long userId) {
        return userId != null && StrUtil.isNotEmpty(clientId);
    }

    private long remainingSeconds(Instant expiresAt) {
        return ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
    }
}
