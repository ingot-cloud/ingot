# 在线用户管理查询方案

**版本**：v1.0  
**日期**：2025-12-17  
**类型**：功能扩展 - 管理端查询

---

## 📋 需求分析

### 原有方法的局限性

```java
// 现有方法
List<Long> getOnlineUsers(Long tenantId, String clientId, long offset, long limit);
```

**问题**：
- ❌ tenantId 和 clientId 是必传参数，无法查询所有租户
- ❌ 只返回用户ID列表，缺少详细信息
- ❌ 不支持 username、ipAddress 等条件查询
- ❌ 不支持设备类型、认证类型筛选

### 管理端实际需求

- ✅ 支持多条件组合查询（tenantId、clientId、userId、username、ipAddress、deviceType 等）
- ✅ 所有条件均可选，支持全局查询
- ✅ 返回完整的 `OnlineToken` 对象
- ✅ 支持分页和排序
- ✅ 支持强制下线功能

---

## 🎯 解决方案

### 1. 核心组件

#### OnlineTokenQuery（查询条件）

```java
@Data
@Builder
public class OnlineTokenQuery implements Serializable {
    
    // ========== 查询条件（全部可选）==========
    
    /**
     * 租户ID（可选）
     */
    private Long tenantId;
    
    /**
     * 客户端ID（可选）
     */
    private String clientId;
    
    /**
     * 用户ID（可选）
     */
    private Long userId;
    
    /**
     * 用户名（可选，模糊匹配）
     */
    private String username;
    
    /**
     * IP 地址（可选，模糊匹配）
     */
    private String ipAddress;
    
    /**
     * 设备类型（可选：PC、Mobile、Tablet）
     */
    private String deviceType;
    
    /**
     * 认证类型（可选：UNIQUE、DEFAULT）
     */
    private String authType;
    
    /**
     * 用户类型（可选：SYS、TENANT）
     */
    private String userType;
    
    // ========== 分页和排序 ==========
    
    /**
     * 当前页码（从 1 开始，默认 1）
     */
    @Builder.Default
    private int page = 1;
    
    /**
     * 每页大小（默认 20）
     */
    @Builder.Default
    private int size = 20;
    
    /**
     * 排序字段（可选：issuedAt、expiresAt，默认 issuedAt）
     */
    @Builder.Default
    private String sortBy = "issuedAt";
    
    /**
     * 排序方向（可选：asc、desc，默认 desc）
     */
    @Builder.Default
    private String sortOrder = "desc";
}
```

#### PageResult（分页结果）

```java
@Data
public class PageResult<T> implements Serializable {
    
    /**
     * 当前页码
     */
    private int page;
    
    /**
     * 每页大小
     */
    private int size;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 总页数
     */
    private int pages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 是否有上一页
     */
    private boolean hasPrevious;
    
    /**
     * 是否有下一页
     */
    private boolean hasNext;
}
```

### 2. 服务接口

#### OnlineTokenService 新增方法

```java
public interface OnlineTokenService {
    
    /**
     * 查询在线 Token（分页、多条件）
     * 适用于管理端的在线用户管理功能
     *
     * @param query 查询条件
     * @return 在线 Token 分页结果
     */
    PageResult<OnlineToken> queryOnlineTokens(OnlineTokenQuery query);
    
    // ... 其他方法保持不变 ...
}
```

### 3. 实现策略

#### 查询优化（三级策略）

```java
/**
 * 策略1: 精确查询（最优）
 * 条件：userId + tenantId + clientId
 * 性能：O(1)
 * 说明：从用户 JTI 集合直接获取
 */
if (query.getUserId() != null && query.getTenantId() != null && clientId != null) {
    String userSetKey = TOKEN_USER_SET_PREFIX + buildUserKey(...);
    Set<Object> userJtis = redisTemplate.opsForSet().members(userSetKey);
    // 直接返回该用户的所有 Token
}

/**
 * 策略2: 租户范围查询（较优）
 * 条件：tenantId + clientId
 * 性能：O(N)，N = 租户在线用户数
 * 说明：从在线用户 ZSet 获取所有用户ID，再获取 JTI
 */
if (query.getTenantId() != null && clientId != null) {
    String onlineKey = ONLINE_USER_PREFIX + buildTenantClientKey(...);
    Set<Object> userIds = redisTemplate.opsForZSet().range(onlineKey, 0, -1);
    // 遍历用户ID，获取所有 JTI
}

/**
 * 策略3: 全局扫描（较慢）
 * 条件：无 tenantId 或 clientId
 * 性能：O(M)，M = 全局 Token 数量
 * 说明：扫描所有 token:jti:* 的 key
 * 建议：限制使用，或添加索引优化
 */
Set<String> keys = redisTemplate.keys(TOKEN_JTI_PREFIX + "*");
// 扫描所有 Token
```

---

## 💻 使用示例

### 1. Controller 层

```java
@RestController
@RequestMapping("/api/admin/online-users")
@RequiredArgsConstructor
public class OnlineUserController {
    
    private final OnlineTokenService onlineTokenService;
    
    /**
     * 查询在线用户（分页）
     */
    @GetMapping
    public R<PageResult<OnlineToken>> queryOnlineUsers(OnlineTokenQuery query) {
        PageResult<OnlineToken> result = onlineTokenService.queryOnlineTokens(query);
        return R.ok(result);
    }
    
    /**
     * 获取在线用户详情
     */
    @GetMapping("/{jti}")
    public R<OnlineToken> getOnlineUser(@PathVariable String jti) {
        Optional<OnlineToken> tokenOpt = onlineTokenService.getByJti(jti);
        if (tokenOpt.isEmpty()) {
            return R.fail("User not online");
        }
        return R.ok(tokenOpt.get());
    }
    
    /**
     * 强制单个 Token 下线
     */
    @PostMapping("/kick-token")
    public R<Void> kickToken(@RequestParam String jti) {
        onlineTokenService.removeByJti(jti);
        log.info("Kicked token offline: jti={}", jti);
        return R.ok();
    }
    
    /**
     * 强制用户所有 Token 下线
     */
    @PostMapping("/kick-user")
    public R<Void> kickUser(
            @RequestParam Long userId,
            @RequestParam Long tenantId,
            @RequestParam String clientId) {
        
        onlineTokenService.removeByUser(userId, tenantId, clientId);
        log.info("Kicked user offline: userId={}, tenantId={}, clientId={}", 
                userId, tenantId, clientId);
        return R.ok();
    }
}
```

### 2. 前端调用示例

#### 查询所有在线用户

```javascript
// GET /api/admin/online-users?page=1&size=20
const response = await fetch('/api/admin/online-users?page=1&size=20', {
    headers: {
        'Authorization': 'Bearer ' + accessToken
    }
});

const result = await response.json();
console.log(result);

// 响应示例：
{
    "code": 200,
    "data": {
        "page": 1,
        "size": 20,
        "total": 150,
        "pages": 8,
        "hasPrevious": false,
        "hasNext": true,
        "records": [
            {
                "jti": "1234567890",
                "userId": 12345,
                "tenantId": 1,
                "principalName": "zhangsan",
                "clientId": "web-client",
                "authType": "UNIQUE",
                "userType": "SYS",
                "ipAddress": "183.14.132.117",
                "deviceType": "PC",
                "os": "macOS 14.1",
                "browser": "Chrome 120.0",
                "location": "上海市",
                "issuedAt": "2025-12-17T10:30:25Z",
                "expiresAt": "2025-12-17T11:00:25Z"
            },
            // ... more records
        ]
    }
}
```

#### 按租户查询

```javascript
// GET /api/admin/online-users?tenantId=1&page=1&size=20
const response = await fetch('/api/admin/online-users?tenantId=1&page=1&size=20');
```

#### 按用户名模糊查询

```javascript
// GET /api/admin/online-users?username=zhang&page=1&size=20
const response = await fetch('/api/admin/online-users?username=zhang&page=1&size=20');
```

#### 按设备类型查询

```javascript
// GET /api/admin/online-users?deviceType=Mobile&page=1&size=20
const response = await fetch('/api/admin/online-users?deviceType=Mobile&page=1&size=20');
```

#### 组合查询

```javascript
// GET /api/admin/online-users?tenantId=1&clientId=web&deviceType=PC&username=zhang&page=1&size=20
const response = await fetch(
    '/api/admin/online-users?' + new URLSearchParams({
        tenantId: 1,
        clientId: 'web',
        deviceType: 'PC',
        username: 'zhang',
        page: 1,
        size: 20
    })
);
```

### 3. 完整的前端管理页面

```typescript
interface OnlineUser {
    jti: string;
    userId: number;
    tenantId: number;
    principalName: string;
    clientId: string;
    authType: string;
    userType: string;
    ipAddress: string;
    deviceType: string;
    os: string;
    browser: string;
    location: string;
    issuedAt: string;
    expiresAt: string;
}

interface QueryParams {
    tenantId?: number;
    clientId?: string;
    userId?: number;
    username?: string;
    ipAddress?: string;
    deviceType?: string;
    authType?: string;
    userType?: string;
    page: number;
    size: number;
    sortBy?: string;
    sortOrder?: string;
}

// React 组件示例
function OnlineUserManagement() {
    const [users, setUsers] = useState<OnlineUser[]>([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        page: 1,
        size: 20,
        total: 0,
        pages: 0
    });
    
    // 查询条件
    const [query, setQuery] = useState<QueryParams>({
        page: 1,
        size: 20
    });
    
    // 查询在线用户
    const fetchOnlineUsers = async () => {
        setLoading(true);
        try {
            const params = new URLSearchParams();
            Object.entries(query).forEach(([key, value]) => {
                if (value != null && value !== '') {
                    params.append(key, String(value));
                }
            });
            
            const response = await fetch(`/api/admin/online-users?${params}`, {
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
                }
            });
            
            const result = await response.json();
            if (result.code === 200) {
                setUsers(result.data.records);
                setPagination({
                    page: result.data.page,
                    size: result.data.size,
                    total: result.data.total,
                    pages: result.data.pages
                });
            }
        } catch (error) {
            console.error('Failed to fetch online users:', error);
        } finally {
            setLoading(false);
        }
    };
    
    // 强制下线
    const kickUser = async (jti: string) => {
        if (!confirm('确定要强制下线该用户吗？')) {
            return;
        }
        
        try {
            const response = await fetch('/api/admin/online-users/kick-token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
                },
                body: `jti=${jti}`
            });
            
            const result = await response.json();
            if (result.code === 200) {
                alert('下线成功');
                fetchOnlineUsers(); // 刷新列表
            }
        } catch (error) {
            console.error('Failed to kick user:', error);
        }
    };
    
    useEffect(() => {
        fetchOnlineUsers();
    }, [query]);
    
    return (
        <div className="online-user-management">
            <h2>在线用户管理</h2>
            
            {/* 查询条件 */}
            <div className="query-form">
                <input
                    type="text"
                    placeholder="用户名"
                    value={query.username || ''}
                    onChange={e => setQuery({...query, username: e.target.value, page: 1})}
                />
                <input
                    type="text"
                    placeholder="IP地址"
                    value={query.ipAddress || ''}
                    onChange={e => setQuery({...query, ipAddress: e.target.value, page: 1})}
                />
                <select
                    value={query.deviceType || ''}
                    onChange={e => setQuery({...query, deviceType: e.target.value, page: 1})}
                >
                    <option value="">所有设备</option>
                    <option value="PC">PC</option>
                    <option value="Mobile">Mobile</option>
                    <option value="Tablet">Tablet</option>
                </select>
                <button onClick={fetchOnlineUsers}>查询</button>
            </div>
            
            {/* 统计信息 */}
            <div className="stats">
                <span>在线用户数：{pagination.total}</span>
            </div>
            
            {/* 用户列表 */}
            <table className="user-table">
                <thead>
                    <tr>
                        <th>用户名</th>
                        <th>租户ID</th>
                        <th>IP地址</th>
                        <th>设备</th>
                        <th>操作系统</th>
                        <th>浏览器</th>
                        <th>登录时间</th>
                        <th>过期时间</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    {users.map(user => (
                        <tr key={user.jti}>
                            <td>{user.principalName}</td>
                            <td>{user.tenantId}</td>
                            <td>{user.ipAddress} {user.location && `(${user.location})`}</td>
                            <td>
                                {user.deviceType === 'PC' && '💻'}
                                {user.deviceType === 'Mobile' && '📱'}
                                {user.deviceType === 'Tablet' && '📱'}
                                {user.deviceType}
                            </td>
                            <td>{user.os}</td>
                            <td>{user.browser}</td>
                            <td>{new Date(user.issuedAt).toLocaleString()}</td>
                            <td>{new Date(user.expiresAt).toLocaleString()}</td>
                            <td>
                                <button onClick={() => kickUser(user.jti)}>
                                    强制下线
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            
            {/* 分页 */}
            <div className="pagination">
                <button
                    disabled={!pagination.hasPrevious}
                    onClick={() => setQuery({...query, page: query.page - 1})}
                >
                    上一页
                </button>
                <span>{pagination.page} / {pagination.pages}</span>
                <button
                    disabled={pagination.page >= pagination.pages}
                    onClick={() => setQuery({...query, page: query.page + 1})}
                >
                    下一页
                </button>
            </div>
        </div>
    );
}
```

---

## 📊 性能对比

| 场景 | 原方法 | 新方法 | 说明 |
|------|-------|--------|------|
| **查询特定租户** | ✅ O(N) | ✅ O(N) | 性能相当 |
| **查询特定用户** | ❌ 不支持 | ✅ O(1) | 新方法更优 |
| **按用户名查询** | ❌ 不支持 | ✅ O(M) | M = Token总数 |
| **全局查询** | ❌ 不支持 | ⚠️ O(M) | 建议限制使用 |
| **灵活性** | ❌ 固定参数 | ✅ 自由组合 | 新方法更灵活 |

---

## 🎯 最佳实践

### 1. 查询优化建议

```java
// ✅ 推荐：指定租户和客户端
OnlineTokenQuery query = OnlineTokenQuery.builder()
        .tenantId(1L)
        .clientId("web-client")
        .page(1)
        .size(20)
        .build();

// ⚠️ 谨慎：全局查询（数据量大时性能较低）
OnlineTokenQuery query = OnlineTokenQuery.builder()
        .page(1)
        .size(20)
        .build();

// ✅ 最优：精确查询
OnlineTokenQuery query = OnlineTokenQuery.builder()
        .userId(12345L)
        .tenantId(1L)
        .clientId("web-client")
        .build();
```

### 2. 分页大小限制

```java
// 实现中已限制最大 size 为 1000
int size = Math.max(1, Math.min(query.getSize(), 1000));
```

### 3. 排序字段

```java
// 支持两种排序字段
- "issuedAt"  // 登录时间（默认）
- "expiresAt" // 过期时间

// 支持两种排序方向
- "asc"  // 升序
- "desc" // 降序（默认）
```

---

## ✅ 总结

### 核心改进

1. **新增 `queryOnlineTokens` 方法**：
   - ✅ 支持多条件组合查询
   - ✅ 所有条件均可选
   - ✅ 返回完整 `OnlineToken` 对象
   - ✅ 支持分页和排序

2. **保留现有方法**：
   - ✅ `getOnlineUsers`：用于特定场景的统计
   - ✅ 向后兼容，不影响现有代码

3. **查询策略优化**：
   - ✅ 三级查询策略（精确 > 租户 > 全局）
   - ✅ 自动选择最优查询路径
   - ✅ 性能和灵活性的平衡

### 使用场景

| 场景 | 推荐方法 |
|------|---------|
| 管理端用户查询 | ✅ `queryOnlineTokens` |
| 租户在线统计 | ✅ `getOnlineUsers` + `getOnlineUserCount` |
| 用户设备管理 | ✅ `queryOnlineTokens` (指定 userId) |
| 强制下线 | ✅ `removeByJti` / `removeByUser` |

---

**文档版本**：v1.0  
**最后更新**：2025-12-17  
**维护者**：Ingot Cloud Team
