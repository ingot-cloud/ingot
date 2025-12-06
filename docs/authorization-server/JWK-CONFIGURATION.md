# JWK 密钥管理配置说明

## 概述

本项目使用 Redis 存储 JWK（JSON Web Key）密钥，支持集群部署和密钥轮换。

- **授权服务器**：持有私钥，用于签发 JWT
- **资源服务器**：只持有公钥，用于验证 JWT
- **密钥加密**：Redis 中的私钥使用 AES-256-GCM 加密存储

## 配置说明

### 1. 环境变量配置

在启动授权服务器时，设置环境变量：

```bash
# 生成一个随机的 master key（首次部署时）
export INGOT_JWK_MASTER_KEY="your-random-base64-key-here"

# 启动授权服务器
java -jar ingot-service-auth.jar
```

**生成 Master Key 的方法**：

```java
// 在 Java 代码中生成（只需执行一次）
String masterKey = AESUtil.generateMasterKey();
System.out.println("Master Key: " + masterKey);
```

或使用命令行：

```bash
openssl rand -base64 32
```

### 2. 配置文件配置

在 `application.yml` 或 `application-dev.yml` 中：

```yaml
ingot:
  security:
    jwk:
      # 主密钥（用于加密 Redis 中的私钥）
      # 建议从环境变量读取，不要硬编码
      master-key: ${INGOT_JWK_MASTER_KEY}
      
      # 是否启用私钥加密（默认 true）
      enable-encryption: true
      
      # 密钥生命周期（默认 90 天）
      key-lifetime: 90d
      
      # 密钥轮换宽限期（默认 7 天）
      # 在此期间，旧密钥仍可用于验证 JWT
      key-grace-period: 7d
      
      # 最大活跃密钥数量（默认 3 个）
      max-active-keys: 3
      
      # 资源服务器 JWK 缓存刷新间隔（默认 5 分钟）
      cache-refresh-interval: 5m
```

### 3. 配置中心配置

如果使用 Nacos 等配置中心：

1. 在 Nacos 中创建配置 `ingot-auth-security.yml`
2. 添加敏感配置：

```yaml
ingot:
  security:
    jwk:
      master-key: ${INGOT_JWK_MASTER_KEY}  # 从环境变量读取
      enable-encryption: true
```

3. 在 `application.yml` 中引用：

```yaml
spring:
  config:
    import:
      - optional:nacos:ingot-auth-security.yml
```

## 集群部署说明

### 架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Redis (共享存储)                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Key: in:security:jwk:key:{keyId}:pub (公钥)      │  │
│  │ Key: in:security:jwk:key:{keyId}:pri (加密私钥)  │  │
│  │ Key: in:security:jwk:key:{keyId}:created         │  │
│  │ Key: in:security:jwk:key-ids (活跃密钥集合)      │  │
│  │ Key: in:security:jwk:current-key-id (当前密钥)   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                          ▲
                          │
        ┌─────────────────┴──────────────────┐
        │                                    │
┌───────▼───────┐                   ┌────────▼────────┐
│  授权服务器-1  │                   │  授权服务器-2   │
│  (持有私钥)    │                   │  (持有私钥)     │
│  - 签发 JWT    │                   │  - 签发 JWT     │
│  - 密钥轮换    │                   │  - 密钥轮换     │
│  - JWKS 端点   │                   │  - JWKS 端点    │
└───────┬───────┘                   └────────┬────────┘
        │                                    │
        └─────────────────┬──────────────────┘
                          │
        ┌─────────────────┴──────────────────┐
        │                                    │
┌───────▼────────┐                  ┌───────▼────────┐
│ 资源服务器-1    │                  │ 资源服务器-2    │
│ (只持有公钥)    │                  │ (只持有公钥)    │
│ - 验证 JWT      │                  │ - 验证 JWT      │
│ - 定期刷新公钥  │                  │ - 定期刷新公钥  │
└────────────────┘                  └────────────────┘
```

### 集群部署步骤

1. **部署 Redis**
   ```bash
   # 使用 Redis Cluster 或 Redis Sentinel 保证高可用
   ```

2. **配置 Master Key**
   ```bash
   # 所有节点使用相同的 master key
   export INGOT_JWK_MASTER_KEY="your-master-key"
   ```

3. **启动授权服务器集群**
   ```bash
   # 节点 1
   java -jar ingot-auth.jar --server.port=8080
   
   # 节点 2
   java -jar ingot-auth.jar --server.port=8081
   ```
   
   - 第一个启动的节点会自动生成密钥
   - 后续节点会从 Redis 读取已有密钥
   - 密钥轮换由任一节点自动执行

4. **启动资源服务器**
   ```bash
   # 资源服务器不需要 master key（只读公钥）
   java -jar ingot-pms-provider.jar
   ```

## 端点说明

### JWKS 端点

**URL**: `http://授权服务器地址/.well-known/jwks.json`

**示例**:
```bash
curl http://localhost:8080/.well-known/jwks.json
```

**响应**:
```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "kid": "9c7a8f3e-1234-5678-9abc-def012345678",
      "n": "xGOt-qnS..."
    }
  ]
}
```

## 故障排查

### 常见问题

1. **资源服务器无法验证 JWT**
   - 检查 Redis 连接是否正常
   - 检查资源服务器是否能访问 Redis
   - 检查密钥是否存在：`redis-cli get in:security:jwk:current-key-id`

2. **授权服务器无法解密私钥**
   - 检查 master key 是否正确
   - 检查 master key 是否在所有节点保持一致
   - 查看日志中的解密错误信息

3. **集群节点密钥不一致**
   - 不应该出现这种情况（都从 Redis 读取）
   - 检查是否使用了不同的 Redis 实例
   - 检查 Redis 的主从同步是否正常

### 调试命令

```bash
# 查看 Redis 中的密钥信息
redis-cli

# 查看当前密钥 ID
GET in:security:jwk:current-key-id

# 查看所有活跃密钥 ID
SMEMBERS in:security:jwk:key-ids

# 查看某个密钥的公钥
GET in:security:jwk:key:{keyId}:pub

# 查看某个密钥的创建时间
GET in:security:jwk:key:{keyId}:created
```

## 技术细节

### 加密算法

- **对称加密**: AES-256-GCM
- **密钥派生**: SHA-256
- **IV 长度**: 12 字节（GCM 推荐）
- **认证标签**: 128 位

### 密钥格式

- **公钥**: Base64 编码的 X.509 格式
- **私钥**: Base64 编码的 PKCS#8 格式（加密后存储）

### 性能优化

- 资源服务器缓存公钥（默认 5 分钟刷新）
- 使用 Redis 管道减少网络往返
- 密钥轮换异步执行，不影响请求

## 参考资料

- [RFC 7517 - JSON Web Key (JWK)](https://tools.ietf.org/html/rfc7517)
- [RFC 7518 - JSON Web Algorithms (JWA)](https://tools.ietf.org/html/rfc7518)
- [OAuth 2.0 Authorization Server Metadata](https://tools.ietf.org/html/rfc8414)

