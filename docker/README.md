# Ingot Cloud Docker 部署方案

基于 Docker Compose 和 Docker Swarm 的微服务部署解决方案，支持单机和多机集群部署。

## 目录结构

```
docker/
├── docker-compose.yml          # Swarm 集群部署配置
├── docker-compose.standalone.yml # 单机部署配置
├── .env                        # Docker Compose 环境变量（从模板复制）
│
├── env-templates/              # Docker Compose 配置模板
│   ├── README.md              # 配置说明
│   ├── env.4c8g.template      # 4核8G配置
│   ├── env.8c16g.template     # 8核16G配置 ⭐推荐
│   └── env.16c32g.template    # 16核32G配置
│
├── services-env/               # 服务环境变量配置
│   ├── common.env             # 公共配置 ⭐必须修改
│   ├── auth.env               # Auth服务配置
│   ├── gateway.env            # Gateway服务配置
│   ├── member.env             # Member服务配置
│   └── pms.env                # PMS服务配置
│
├── swarm-deploy.sh             # Swarm一键部署脚本
├── standalone-deploy.sh        # 单机一键部署脚本
│
└── run-*.sh                    # 单个服务部署脚本
```

## 配置分层说明

### 1. Docker Compose 配置（.env）

控制容器运行参数：
- 镜像版本
- 资源限制（CPU、内存）
- 副本数量
- 网络配置
- 运行环境（dev/test/prod）

**文件位置：** `env-templates/*.template`

### 2. 服务环境变量（services-env/）

控制服务业务逻辑：
- 数据库连接（MySQL）
- 缓存连接（Redis）
- 配置中心（Nacos）
- 业务参数（加密密钥等）

**文件位置：** `services-env/*.env`

## 快速开始

### 方式一：单机部署

使用 `standalone-deploy.sh` 一键部署：

```bash
# 1. 复制 Docker Compose 配置（8核16G推荐）
cp env-templates/env.8c16g.template .env

# 2. 修改服务环境变量（必须修改）
vim services-env/common.env
# 修改以下配置：
# - MYSQL_HOST, MYSQL_USERNAME, MYSQL_PASSWORD
# - REDIS_HOST, REDIS_PASSWORD
# - NACOS_SERVER_ADDR, NACOS_USERNAME, NACOS_PASSWORD
# - JASYPT_PASSWORD, CRYPTO_AES_KEY

# 3. 创建网络
./standalone-deploy.sh network-create

# 4. 一键部署所有服务
./standalone-deploy.sh deploy

# 5. 查看服务状态
./standalone-deploy.sh status

# 6. 查看服务日志
./standalone-deploy.sh logs ingot-gateway
```

**单机部署脚本命令：**
```bash
# 服务部署
./standalone-deploy.sh deploy                      # 部署所有服务

# 服务启动/停止/重启
./standalone-deploy.sh start                       # 启动所有服务
./standalone-deploy.sh start ingot-gateway         # 启动单个服务
./standalone-deploy.sh stop                        # 停止所有服务
./standalone-deploy.sh stop ingot-auth             # 停止单个服务
./standalone-deploy.sh restart                     # 重启所有服务
./standalone-deploy.sh restart ingot-member        # 重启单个服务

# 状态查看
./standalone-deploy.sh status                      # 查看服务状态
./standalone-deploy.sh logs <service>              # 查看服务日志

# 清理
./standalone-deploy.sh down                        # 停止并移除服务
```

### 方式二：多机集群部署

使用 `swarm-deploy.sh` 进行集群部署：

**Manager 节点：**
```bash
# 1. 准备配置
cp env-templates/env.8c16g.template .env
vim services-env/common.env

# 2. 初始化 Swarm
./swarm-deploy.sh init-manager

# 3. 部署服务（默认使用 docker-compose.yml）
./swarm-deploy.sh deploy

# 4. 或使用自定义配置文件部署
./swarm-deploy.sh deploy docker-compose.custom.yml
```

**Worker 节点：**
```bash
# 使用 Manager 输出的 token 加入
./swarm-deploy.sh init-worker "docker swarm join --token SWMTKN-xxx..."
```

**Swarm 集群管理命令：**
```bash
./swarm-deploy.sh deploy [compose-file]    # 部署服务（可指定配置文件）
./swarm-deploy.sh update                   # 滚动更新服务
./swarm-deploy.sh scale <service> <n>      # 扩缩容服务
./swarm-deploy.sh status                   # 查看服务状态
./swarm-deploy.sh logs <service>           # 查看服务日志
./swarm-deploy.sh stop                     # 停止并移除服务栈
```

## 配置说明

### 必须修改的配置

在 `services-env/common.env` 中：

```bash
# 数据库
MYSQL_HOST=your-mysql-host
MYSQL_USERNAME=ingot_user
MYSQL_PASSWORD=YourStrongPassword123!

# Redis
REDIS_HOST=your-redis-host  
REDIS_PASSWORD=YourRedisPassword123!

# Nacos（支持单节点或集群）
NACOS_SERVER_ADDR=nacos1:8848,nacos2:8848,nacos3:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=YourNacosPassword123!

# 加密密钥
JASYPT_PASSWORD=YourJasyptPassword123!
CRYPTO_AES_KEY=your32charactersecretkey123456
CRYPTO_AES_GCM_KEY=your32charactersecretkey123456

# MinIO
MINIO_URL=http://minio-server:9000
MINIO_SECRET_KEY=YourMinioSecretKey123!

# Druid监控
DRUID_PASSWORD=DruidAdmin@2024
```

### 服务端口分配

| 服务 | 服务端口 | 管理端口 | 说明 |
|------|------|------|------|
| Gateway | 7980 | 8980 | 对外网关 |
| Auth | 5100 | 6100 | 认证服务 |
| Member | 5300 | 6300 | 会员服务 |
| PMS | 5200 | 6200 | 权限服务 |

## 资源配置（8核16G 推荐）

### 服务配置

| 服务 | 副本数 | CPU | 内存 | 说明 |
|------|-------|-----|------|------|
| Gateway | 2 | 1.5核 | 2.5G | 对外服务，双副本高可用 |
| Auth | 1 | 2核 | 3G | 认证服务 |
| Member | 1 | 2核 | 3G | 业务服务 |
| PMS | 1 | 2核 | 3G | 业务服务 |

### 资源分配

- **CPU总计：** 9核（允许超分配）
- **内存总计：** 15GB（预留1GB给系统）

## 常用操作

### 服务管理

```bash
# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f ingot-gateway

# 重启服务
docker-compose restart ingot-gateway

# 停止服务
docker-compose down
```

### Swarm 集群管理

```bash
# 查看状态
./swarm-deploy.sh status

# 扩容 Gateway 到 3 个副本
./swarm-deploy.sh scale ingot-gateway 3

# 查看日志
./swarm-deploy.sh logs ingot-gateway

# 滚动更新
./swarm-deploy.sh update

# 停止服务
./swarm-deploy.sh stop
```

### 访问服务

| 服务 | 访问地址                  | 说明 |
|------|-----------------------|------|
| Nginx | http://localhost      | 负载均衡（推荐） |
| Gateway | http://localhost:7980 | 直接访问 |
| Auth | http://localhost:5100 | 认证服务 |
| Member | http://localhost:5300 | 会员服务 |
| PMS | http://localhost:5200 | 权限服务 |

### 健康检查

```bash
# 直接访问服务
curl http://localhost:7980/actuator/health  # Gateway
curl http://localhost:19000/actuator/health # Auth管理端口
curl http://localhost:15300/actuator/health # Member管理端口
curl http://localhost:15200/actuator/health # PMS管理端口
```

## 性能优化

### 数据库连接池（单服务）

```bash
# services-env/common.env
DB_POOL_MAX_ACTIVE=80          # 最大连接数
DB_POOL_MIN_IDLE=5             # 最小空闲连接
DB_SLOW_SQL_MILLIS=2000        # 慢SQL阈值（毫秒）
```

### Redis 连接池（单服务）

```bash
# services-env/common.env
REDIS_POOL_MAX_ACTIVE=50       # 最大连接数
REDIS_POOL_MIN_IDLE=5          # 最小空闲连接
```

### Tomcat 配置（单服务）

```bash
# services-env/auth.env (或其他服务)
TOMCAT_THREADS_MAX=400         # 最大线程数
TOMCAT_MAX_CONNECTIONS=8000    # 最大连接数
```

## 安全配置

### 强烈建议修改的密码（8项）

在 `services-env/common.env` 中：

```bash
1. MYSQL_PASSWORD         # MySQL密码
2. REDIS_PASSWORD         # Redis密码
3. NACOS_PASSWORD         # Nacos密码
4. JASYPT_PASSWORD        # Jasypt加密密钥
5. CRYPTO_AES_KEY         # AES密钥（32位）
6. CRYPTO_AES_GCM_KEY     # AES-GCM密钥（32位）
7. MINIO_SECRET_KEY       # MinIO密钥
8. DRUID_PASSWORD         # Druid监控密码
```

### 生成随机密码

```bash
# 生成随机密码
openssl rand -base64 32

# 生成32位密钥
openssl rand -hex 16
```

## 🔍 故障排查

### 服务无法启动

```bash
# 查看日志
docker-compose logs <service-name>

# 查看详细错误
docker service ps ingot_<service> --no-trunc
```

### 网络连接问题

```bash
# 测试连通性
docker exec ingot-gateway ping ingot-auth

# 检查网络
docker network inspect ingot-overlay
```

## 注意事项

1. ✅ **配置文件分离**
    - `.env` → Docker Compose 配置
    - `services-env/*.env` → 服务业务配置

2. ✅ **环境变量优先级**
   ```
   Docker environment > env_file > 默认值
   ```

3. ✅ **Nacos 配置组**
    - DEV_GROUP → 开发环境
    - TEST_GROUP → 测试环境
    - PROD_GROUP → 生产环境

4. ✅ **端口不冲突**
    - 确保 80、443、7980 端口未被占用

5. ✅ **数据卷挂载**
    - `/ingot-data` → 数据目录

---

**版本：** v1.0.0  
**更新日期：** 2026-01-02  
**维护者：** Ingot Cloud Team
