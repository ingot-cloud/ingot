# JTI 获取和在线用户清理方案

**版本**：v4.1  
**日期**：2025-12-17  
**类型**：企业级最佳实践

---

## 定期清理过期的在线用户

### Score 改为使用过期时间

```java
// 使用过期时间作为 score
double score = expiresAt.toEpochMilli();
```

### 优点

| 优点 | 说明 |
|------|------|
| ✅ **自动排序** | 按过期时间排序，最晚过期的在前 |
| ✅ **易于清理** | 使用 `ZREMRANGEBYSCORE` 批量删除 |
| ✅ **精确统计** | 可以精确统计当前在线用户数 |
| ✅ **性能优异** | Redis 原生命令，O(log N + M) |

### 定时任务实现

#### Spring @Scheduled 方式

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OnlineUserCleanupTask {
    
    private final OnlineTokenService onlineTokenService;
    
    /**
     * 每5分钟清理一次过期的在线用户
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanExpiredOnlineUsers() {
        log.info("[OnlineUserCleanupTask] Starting cleanup expired online users");
        
        long startTime = System.currentTimeMillis();
        
        // 清理所有租户的过期用户
        long removed = onlineTokenService.cleanAllExpiredOnlineUsers();
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("[OnlineUserCleanupTask] Cleanup completed: removed={}, duration={}ms",
                removed, duration);
    }
}
```

#### XXL-JOB 方式

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OnlineUserCleanupJobHandler {
    
    private final OnlineTokenService onlineTokenService;
    
    /**
     * XXL-JOB 清理任务
     */
    @XxlJob("cleanExpiredOnlineUsers")
    public void cleanExpiredOnlineUsers() {
        log.info("[OnlineUserCleanupJob] Starting cleanup");
        
        long removed = onlineTokenService.cleanAllExpiredOnlineUsers();
        
        log.info("[OnlineUserCleanupJob] Completed: removed={}", removed);
        
        // 返回执行结果
        XxlJobHelper.handleSuccess("Cleaned " + removed + " expired users");
    }
    
    /**
     * 清理指定租户的过期用户
     */
    @XxlJob("cleanExpiredOnlineUsersByTenant")
    public void cleanExpiredOnlineUsersByTenant() {
        // 从 XXL-JOB 参数中获取 tenantId 和 clientId
        String param = XxlJobHelper.getJobParam();
        String[] parts = param.split(":");
        
        if (parts.length != 2) {
            XxlJobHelper.handleFail("Invalid param: " + param);
            return;
        }
        
        Long tenantId = Long.valueOf(parts[0]);
        String clientId = parts[1];
        
        long removed = onlineTokenService.cleanExpiredOnlineUsers(tenantId, clientId);
        
        XxlJobHelper.handleSuccess("Cleaned " + removed + " expired users for tenant " + tenantId);
    }
}
```

### 清理方法实现细节

#### cleanAllExpiredOnlineUsers()

```java
public long cleanAllExpiredOnlineUsers() {
    // 1. 扫描所有 online:user:* 的 key
    Set<String> keys = redisTemplate.keys(ONLINE_USER_PREFIX + "*");
    if (keys == null || keys.isEmpty()) {
        return 0;
    }
    
    long totalRemoved = 0;
    long now = Instant.now().toEpochMilli();
    
    // 2. 逐个清理
    for (String key : keys) {
        // 删除所有 score < now 的成员（已过期）
        Long removed = redisTemplate.opsForZSet()
            .removeRangeByScore(key, 0, now);
        
        if (removed != null) {
            totalRemoved += removed;
        }
    }
    
    if (totalRemoved > 0) {
        log.info("Cleaned expired online users: total={}", totalRemoved);
    }
    
    return totalRemoved;
}
```

#### cleanExpiredOnlineUsers(tenantId, clientId)

```java
public long cleanExpiredOnlineUsers(Long tenantId, String clientId) {
    String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(tenantId, clientId);
    
    // 删除所有已过期的用户（score < now）
    long now = Instant.now().toEpochMilli();
    Long removed = redisTemplate.opsForZSet()
        .removeRangeByScore(onlineKey, 0, now);
    
    if (removed != null && removed > 0) {
        log.info("Cleaned expired online users: tenantId={}, clientId={}, count={}",
                tenantId, clientId, removed);
    }
    
    return removed != null ? removed : 0;
}
```

### 改进后的统计方法

#### getOnlineUserCount()

```java
public long getOnlineUserCount(Long tenantId, String clientId) {
    String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(tenantId, clientId);
    
    // 只统计未过期的用户（score > now）
    long now = Instant.now().toEpochMilli();
    Long count = redisTemplate.opsForZSet()
        .count(onlineKey, now, Double.MAX_VALUE);
    
    return count != null ? count : 0;
}
```

**改进点**：
- ✅ 使用 `ZCOUNT` 只统计未过期的用户
- ✅ 实时精确统计，无需清理

#### getOnlineUsers()

```java
public List<Long> getOnlineUsers(Long tenantId, String clientId, long offset, long limit) {
    String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(tenantId, clientId);
    
    // 按 score 降序获取（最晚过期的在前）
    Set<TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
        .reverseRangeWithScores(onlineKey, offset, offset + limit - 1);
    
    if (tuples == null || tuples.isEmpty()) {
        return Collections.emptyList();
    }
    
    long now = Instant.now().toEpochMilli();
    List<Long> userIds = new ArrayList<>();
    
    for (TypedTuple<Object> tuple : tuples) {
        // 过滤掉已过期的用户
        if (tuple.getScore() != null && tuple.getScore() > now) {
            if (tuple.getValue() instanceof Long userId) {
                userIds.add(userId);
            }
        }
    }
    
    return userIds;
}
```

**改进点**：
- ✅ 查询时过滤已过期用户
- ✅ 保证返回的都是在线用户

---

## 📊 性能对比

### 清理性能

| 指标 | 值 |
|------|-----|
| 扫描所有 key | O(N)，N = key 数量 |
| 单个 key 清理 | O(log M + K)，M = 成员数，K = 删除数 |
| **总复杂度** | **O(N × log M)** |
| **10 个租户** | <10ms |
| **100 个租户** | <100ms |

### 统计性能

| 操作 | 旧方案 | 新方案 | 改进 |
|------|-------|--------|------|
| 统计在线数 | `ZCARD` | `ZCOUNT` | 精确统计 |
| 查询在线列表 | 全部返回 | 过滤返回 | 准确性↑ |

---

## 🔧 配置建议

### 定时任务配置

```yaml
# Spring Scheduled
spring:
  task:
    scheduling:
      pool:
        size: 2
      thread-name-prefix: online-user-cleanup-

# 推荐 Cron 表达式
# 每5分钟：0 */5 * * * ?
# 每小时：  0 0 * * * ?
# 每天凌晨：0 0 0 * * ?
```

### XXL-JOB 配置

```
任务名称：cleanExpiredOnlineUsers
Cron：0 */5 * * * ?
运行模式：BEAN
JobHandler：cleanExpiredOnlineUsers
阻塞处理策略：单机串行
路由策略：轮询
```

---

## 📝 最佳实践总结

### JTI 获取方案

| 方案 | 推荐度 | 说明 |
|------|-------|------|
| **客户端解析 JWT** | ⭐⭐⭐⭐⭐ | **企业级推荐**，标准做法 |
| 登录响应返回 | ⭐⭐⭐ | 需要客户端额外存储 |
| UserInfo 接口 | ⭐⭐ | 需要额外网络请求 |

### ZSet Score 方案

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| **expiresAt** | 可清理，可精确统计 | - | ⭐⭐⭐⭐⭐ |
| loginTs | 展示友好 | 无法清理 | ⭐⭐ |
| lastActiveTs | 实时活跃度 | 频繁更新 | ⭐ |

### 定时清理策略

| 频率 | 场景 | 推荐 |
|------|------|------|
| **每5分钟** | 常规场景 | ⭐⭐⭐⭐⭐ |
| 每小时 | 用户量小 | ⭐⭐⭐ |
| 每天凌晨 | 不重要 | ⭐⭐ |

---

## 🎉 总结

### 两个关键改进

1. **JTI 获取**：✅ 客户端从 JWT 解析，标准且高效
2. **ZSet Score**：✅ 使用过期时间，支持定期清理

### 最终方案

```java
// 1. 保存时使用过期时间作为 score
double score = expiresAt.toEpochMilli();
redisTemplate.opsForZSet().add(onlineKey, userId, score);

// 2. 统计时只统计未过期的
long count = redisTemplate.opsForZSet()
    .count(onlineKey, now, Double.MAX_VALUE);

// 3. 定期清理过期的
Long removed = redisTemplate.opsForZSet()
    .removeRangeByScore(onlineKey, 0, now);
```

---

**版本**：v4.1  
**状态**：完成

