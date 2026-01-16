package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * Redis 实现的在线 Token 服务 <br>
 * 采用完整索引策略：<br>
 * 1. token:jti:{jti} → OnlineToken对象（主数据）<br>
 * 2. token:user:{tenantId}:{clientId}:{userId} → jti（唯一登录索引，仅唯一登录时存）<br>
 * 3. token:user:set:{tenantId}:{clientId}:{userId} → Set<jti>（用户所有有效 token，支持强制下线）<br>
 * 4. online:user:{tenantId}:{clientId} → ZSet<userId, loginTs>（在线用户列表，支持统计和分页）<br>
 *
 * <p>Author: wangchao</p>
 * <p>Date: 2024/12/17</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RedisOnlineTokenService implements OnlineTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Token主Key前缀
     * 格式：token:jti:{jti}
     */
    private static final String TOKEN_JTI_PREFIX = "token:jti:";

    /**
     * 用户唯一登录索引Key前缀（仅唯一登录使用）
     * 格式：token:user:{tenantId}:{clientId}:{userId}
     */
    private static final String TOKEN_USER_UNIQUE_PREFIX = "token:user:";

    /**
     * 用户所有Token集合Key前缀（用于强制下线）
     * 格式：token:user:set:{tenantId}:{clientId}:{userId}
     */
    private static final String TOKEN_USER_SET_PREFIX = "token:user:set:";

    /**
     * 在线用户ZSet Key前缀（用于统计和分页）
     * 格式：online:user:{tenantId}:{clientId}
     */
    private static final String ONLINE_USER_PREFIX = "online:user:";

    @Override
    public void save(InUser user, String jti, Instant expiresAt) {
        long ttl = calculateTTL(expiresAt);
        if (ttl <= 0) {
            log.warn("[RedisOnlineTokenService] Token already expired, skip saving");
            return;
        }

        Set<String> authorities = new HashSet<>(InAuthorityUtils.authorityListToSet(
                user.getAuthorities(), user.getTenantId()
        ));

        // 提取登录信息（IP、User-Agent等）
        // 注意：传入 null，LoginInfoExtractor 会自动从 Spring RequestContextHolder 获取
        LoginInfoExtractor.LoginInfo loginInfo = LoginInfoExtractor.extract(null);

        // 构建 OnlineToken
        OnlineToken onlineToken = OnlineToken.builder()
                .jti(jti)
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .principalName(user.getUsername())
                .clientId(user.getClientId())
                .authType(user.getTokenAuthType())
                .userType(user.getUserType())
                .authorities(authorities)
                .issuedAt(Instant.now())
                .expiresAt(expiresAt)
                // 登录信息
                .ipAddress(loginInfo.getIpAddress())
                .userAgent(loginInfo.getUserAgent())
                .deviceType(loginInfo.getDeviceType())
                .os(loginInfo.getOs())
                .browser(loginInfo.getBrowser())
                .location(loginInfo.getLocation())
                .build();

        // 判断登录类型
        TokenAuthTypeEnum authType = TokenAuthTypeEnum.getEnum(user.getTokenAuthType());
        boolean isUnique = (authType == TokenAuthTypeEnum.UNIQUE);

        // 如果是唯一登录，需要先踢掉旧的 token
        if (isUnique) {
            kickOldTokenIfUnique(user);
        }

        // 1. 保存主数据
        String jtiKey = TOKEN_JTI_PREFIX + jti;
        redisTemplate.opsForValue().set(jtiKey, onlineToken, ttl, TimeUnit.SECONDS);

        // 2. 如果是唯一登录，保存唯一登录索引
        if (isUnique) {
            String uniqueKey = TOKEN_USER_UNIQUE_PREFIX + buildUserKey(user);
            redisTemplate.opsForValue().set(uniqueKey, jti, ttl, TimeUnit.SECONDS);
        }

        // 3. 添加到用户 Token 集合（用于强制下线）
        String userSetKey = TOKEN_USER_SET_PREFIX + buildUserKey(user);
        redisTemplate.opsForSet().add(userSetKey, jti);
        // 设置 TTL（使用当前 token 的 TTL，会被后续更长的 token 自动延长）
        redisTemplate.expire(userSetKey, ttl, TimeUnit.SECONDS);

        // 4. 添加到在线用户 ZSet（用于统计和分页）
        String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(user.getTenantId(), user.getClientId());
        // 使用过期时间作为 score（便于定期清理过期用户）
        double score = expiresAt.toEpochMilli();
        redisTemplate.opsForZSet().add(onlineKey, user.getId(), score);

        log.debug("[RedisOnlineTokenService] Saved online token: userId={}, jti={}, authType={}, ttl={}s, ip={}, device={}",
                user.getId(), jti, authType, ttl, onlineToken.getIpAddress(), onlineToken.getDeviceType());
    }

    @Override
    public Optional<OnlineToken> getByUser(Long userId, Long tenantId, String clientId) {
        if (userId == null || tenantId == null || StrUtil.isEmpty(clientId)) {
            return Optional.empty();
        }

        // 1. 通过用户唯一登录索引查找 JTI（仅唯一登录有此索引）
        String uniqueKey = TOKEN_USER_UNIQUE_PREFIX + buildUserKey(userId, tenantId, clientId);
        Object jtiObj = redisTemplate.opsForValue().get(uniqueKey);

        if (jtiObj != null) {
            // 唯一登录模式，直接返回
            return getByJti(String.valueOf(jtiObj));
        }

        // 2. 如果没有唯一登录索引，从用户 Token 集合中获取最新的一个
        String userSetKey = TOKEN_USER_SET_PREFIX + buildUserKey(userId, tenantId, clientId);
        Set<Object> jtis = redisTemplate.opsForSet().members(userSetKey);

        if (jtis == null || jtis.isEmpty()) {
            log.debug("[RedisOnlineTokenService] No online token for user: userId={}, tenantId={}, clientId={}",
                    userId, tenantId, clientId);
            return Optional.empty();
        }

        // 获取最新的 token（按 issuedAt 排序）
        return jtis.stream()
                .map(jti -> getByJti(String.valueOf(jti)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(OnlineToken::getIssuedAt));
    }

    @Override
    public Optional<OnlineToken> getByJti(String jti) {
        if (StrUtil.isEmpty(jti)) {
            return Optional.empty();
        }

        String key = TOKEN_JTI_PREFIX + jti;
        Object value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            log.debug("[RedisOnlineTokenService] Found online token by jti: {}", jti);
            return Optional.of((OnlineToken) value);
        }

        log.debug("[RedisOnlineTokenService] Online token not found by jti: {}", jti);
        return Optional.empty();
    }

    @Override
    public void removeByUser(Long userId, Long tenantId, String clientId) {
        if (userId == null || tenantId == null || StrUtil.isEmpty(clientId)) {
            return;
        }

        // 1. 获取用户所有 token
        String userSetKey = TOKEN_USER_SET_PREFIX + buildUserKey(userId, tenantId, clientId);
        Set<Object> jtis = redisTemplate.opsForSet().members(userSetKey);

        if (jtis == null || jtis.isEmpty()) {
            log.debug("[RedisOnlineTokenService] No tokens to remove for user: userId={}, tenantId={}, clientId={}",
                    userId, tenantId, clientId);
            return;
        }

        // 2. 删除所有主数据
        jtis.forEach(jti -> {
            String jtiKey = TOKEN_JTI_PREFIX + jti;
            redisTemplate.delete(jtiKey);
        });

        // 3. 删除唯一登录索引（如果存在）
        String uniqueKey = TOKEN_USER_UNIQUE_PREFIX + buildUserKey(userId, tenantId, clientId);
        redisTemplate.delete(uniqueKey);

        // 4. 删除用户 Token 集合
        redisTemplate.delete(userSetKey);

        // 5. 从在线用户 ZSet 中移除
        String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(tenantId, clientId);
        redisTemplate.opsForZSet().remove(onlineKey, userId);

        log.info("[RedisOnlineTokenService] Forced offline all tokens: userId={}, tenantId={}, clientId={}, count={}",
                userId, tenantId, clientId, jtis.size());
    }

    @Override
    public void removeByJti(String jti) {
        if (StrUtil.isEmpty(jti)) {
            return;
        }

        // 1. 获取完整信息
        Optional<OnlineToken> tokenOpt = getByJti(jti);
        if (tokenOpt.isEmpty()) {
            log.debug("[RedisOnlineTokenService] Token not found for removal: jti={}", jti);
            return;
        }

        OnlineToken token = tokenOpt.get();

        // 2. 删除主数据
        String jtiKey = TOKEN_JTI_PREFIX + jti;
        redisTemplate.delete(jtiKey);

        // 3. 从用户 Token 集合中移除
        String userSetKey = TOKEN_USER_SET_PREFIX + buildUserKey(token.getUserId(), token.getTenantId(), token.getClientId());
        redisTemplate.opsForSet().remove(userSetKey, jti);

        // 4. 如果是唯一登录且是当前 token，删除唯一登录索引
        TokenAuthTypeEnum authType = TokenAuthTypeEnum.getEnum(token.getAuthType());
        if (authType == TokenAuthTypeEnum.UNIQUE) {
            String uniqueKey = TOKEN_USER_UNIQUE_PREFIX + buildUserKey(token.getUserId(), token.getTenantId(), token.getClientId());
            Object currentJti = redisTemplate.opsForValue().get(uniqueKey);
            if (jti.equals(String.valueOf(currentJti))) {
                redisTemplate.delete(uniqueKey);
            }
        }

        // 5. 检查用户是否还有其他 token，如果没有则从在线用户 ZSet 中移除
        Set<Object> remainingJtis = redisTemplate.opsForSet().members(userSetKey);
        if (remainingJtis == null || remainingJtis.isEmpty()) {
            String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(token.getTenantId(), token.getClientId());
            redisTemplate.opsForZSet().remove(onlineKey, token.getUserId());
        }

        log.info("[RedisOnlineTokenService] Removed online token: jti={}, userId={}", jti, token.getUserId());
    }

    @Override
    public boolean isOnline(String jti) {
        if (StrUtil.isEmpty(jti)) {
            return false;
        }

        String key = TOKEN_JTI_PREFIX + jti;
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取在线用户列表（分页）
     *
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @param offset   偏移量
     * @param limit    数量
     * @return 用户ID列表
     */
    @Override
    public List<Long> getOnlineUsers(Long tenantId, String clientId, long offset, long limit) {
        String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(tenantId, clientId);

        // 按 score 降序获取（最晚过期的在前，即最新登录的）
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(onlineKey, offset, offset + limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        long now = Instant.now().toEpochMilli();
        List<Long> userIds = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            // 过滤掉已过期的用户
            if (tuple.getScore() != null && tuple.getScore() > now) {
                Object userId = tuple.getValue();
                Long value = NumberUtil.parseLong(StrUtil.toString(userId), -1L);
                if (value > -1) {
                    userIds.add(value);
                }
            }
        }

        return userIds;
    }

    /**
     * 获取在线用户总数
     *
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @return 在线用户数
     */
    @Override
    public long getOnlineUserCount(Long tenantId, String clientId) {
        String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(tenantId, clientId);

        // 只统计未过期的用户
        long now = Instant.now().toEpochMilli();
        Long count = redisTemplate.opsForZSet().count(onlineKey, now, Double.MAX_VALUE);
        return count != null ? count : 0;
    }

    /**
     * 清理过期的在线用户（定时任务调用）
     *
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @return 清理的用户数
     */
    @Override
    public long cleanExpiredOnlineUsers(Long tenantId, String clientId) {
        String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(tenantId, clientId);

        // 删除所有已过期的用户（score < now）
        long now = Instant.now().toEpochMilli();
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(onlineKey, 0, now);

        if (removed != null && removed > 0) {
            log.info("[RedisOnlineTokenService] Cleaned expired online users: tenantId={}, clientId={}, count={}",
                    tenantId, clientId, removed);
        }

        return removed != null ? removed : 0;
    }

    /**
     * 清理所有租户的过期在线用户（定时任务调用）
     *
     * @return 清理的总用户数
     */
    @Override
    public long cleanAllExpiredOnlineUsers() {
        // 扫描所有 online:user:* 的 key
        Set<String> keys = redisTemplate.keys(ONLINE_USER_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        long totalRemoved = 0;
        long now = Instant.now().toEpochMilli();

        for (String key : keys) {
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
            if (removed != null) {
                totalRemoved += removed;
            }
        }

        if (totalRemoved > 0) {
            log.info("[RedisOnlineTokenService] Cleaned all expired online users: total={}", totalRemoved);
        }

        return totalRemoved;
    }

    /**
     * 获取用户所有在线 Token
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param clientId 客户端ID
     * @return Token列表
     */
    @Override
    public List<OnlineToken> getUserAllTokens(Long userId, Long tenantId, String clientId) {
        String userSetKey = TOKEN_USER_SET_PREFIX + buildUserKey(userId, tenantId, clientId);
        Set<Object> jtis = redisTemplate.opsForSet().members(userSetKey);

        if (jtis == null || jtis.isEmpty()) {
            return Collections.emptyList();
        }

        List<OnlineToken> tokens = new ArrayList<>();
        for (Object jti : jtis) {
            getByJti(String.valueOf(jti)).ifPresent(tokens::add);
        }

        // 按登录时间倒序排序
        tokens.sort(Comparator.comparing(OnlineToken::getIssuedAt).reversed());
        return tokens;
    }

    /**
     * 唯一登录时踢掉旧 token
     */
    private void kickOldTokenIfUnique(InUser user) {
        String uniqueKey = TOKEN_USER_UNIQUE_PREFIX + buildUserKey(user);
        Object oldJti = redisTemplate.opsForValue().get(uniqueKey);

        if (oldJti != null) {
            log.info("[RedisOnlineTokenService] Kicking old token for unique login: userId={}, oldJti={}",
                    user.getId(), oldJti);
            removeByJti(String.valueOf(oldJti));
        }
    }

    /**
     * 构建用户索引Key
     */
    private String buildUserKey(InUser user) {
        return buildUserKey(user.getId(), user.getTenantId(), user.getClientId());
    }

    /**
     * 构建用户索引Key
     * 格式：{tenantId}:{clientId}:{userId}
     */
    private String buildUserKey(Long userId, Long tenantId, String clientId) {
        return String.format("%d:%s:%d", tenantId, clientId, userId);
    }

    /**
     * 构建租户客户端Key
     * 格式：{tenantId}:{clientId}
     */
    private String buildTenantClientKey(Long tenantId, String clientId) {
        return String.format("%d:%s", tenantId, clientId);
    }

    /**
     * 计算TTL（秒）
     */
    private long calculateTTL(Instant expiresAt) {
        if (expiresAt == null) {
            return 3600; // 默认1小时
        }
        return ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
    }
}
