# Java 服务假死问题排查指南

> 适用于 8C16G 服务器环境，基于 ingot-pms 服务配置

## 目录

- [1. 问题现象](#1-问题现象)
- [2. 排查步骤](#2-排查步骤)
- [3. 常见原因分析](#3-常见原因分析)
- [4. QPS 计算与容量规划](#4-qps-计算与容量规划)
- [5. 配置优化建议](#5-配置优化建议)
- [6. 监控与预防](#6-监控与预防)
- [7. 应急处理方案](#7-应急处理方案)

---

## 1. 问题现象

### 典型症状

- 应用无响应或响应极慢（超过30秒）
- 健康检查接口超时
- CPU 使用率异常（过高或过低）
- 内存持续增长
- 数据库连接耗尽
- 日志停止输出或大量异常

---

## 2. 排查步骤

### 2.1 第一步：快速诊断（5分钟内）

#### 检查容器状态

```bash
# 查看容器是否运行
docker ps -a | grep ingot-pms

# 查看容器资源使用情况
docker stats ingot-pms --no-stream

# 查看容器日志（最后1000行）
docker logs --tail 1000 ingot-pms

# 实时查看日志
docker logs -f ingot-pms
```

#### 检查进程状态

```bash
# 进入容器
docker exec -it ingot-pms sh

# 查看 Java 进程
ps aux | grep java

# 查看进程状态详情
top -p $(pgrep java)
```

### 2.2 第二步：JVM 诊断（10-15分钟）

#### 使用 jstack 分析线程状态

```bash
# 进入容器
docker exec -it ingot-pms sh

# 找到 Java 进程 PID
PID=$(pgrep java)

# 导出线程堆栈（连续3次，间隔3秒）
jstack $PID > /app/logs/jstack_1.log
sleep 3
jstack $PID > /app/logs/jstack_2.log
sleep 3
jstack $PID > /app/logs/jstack_3.log

# 复制到宿主机分析
docker cp ingot-pms:/app/logs/jstack_1.log ./
```

**重点关注：**

- **BLOCKED** 状态的线程数量
- **WAITING** 状态的线程（特别是数据库连接等待）
- 死锁（Deadlock）
- 大量相同堆栈的线程（可能的热点）

```bash
# 统计线程状态
grep "java.lang.Thread.State" jstack_1.log | sort | uniq -c

# 查找死锁
grep -A 20 "Found one Java-level deadlock" jstack_1.log

# 查找 BLOCKED 线程
grep -B 5 "BLOCKED" jstack_1.log
```

#### 使用 jmap 分析内存

```bash
# 查看堆内存使用情况
jmap -heap $PID

# 查看对象统计（按大小排序，前20个）
jmap -histo:live $PID | head -n 20

# 如果怀疑内存泄漏，导出堆转储（会暂停应用）
jmap -dump:live,format=b,file=/app/logs/heapdump.hprof $PID
docker cp ingot-pms:/app/logs/heapdump.hprof ./
```

**内存分析工具：**
- Eclipse MAT（Memory Analyzer Tool）
- VisualVM
- JProfiler

#### 使用 jstat 查看 GC 情况

```bash
# 每秒输出一次 GC 统计，共10次
jstat -gc $PID 1000 10

# 查看 GC 详细信息
jstat -gcutil $PID 1000 10
```

**关注指标：**

- **FGC**（Full GC 次数）：频繁 Full GC 是性能杀手
- **FGCT**（Full GC 总时间）：单次 Full GC 超过 1 秒需要关注
- **GCT**（GC 总时间）：占用总运行时间比例
- 老年代使用率（OU）：持续接近 100% 表示内存泄漏

### 2.3 第三步：数据库连接池检查

#### Druid 监控页面

访问：`http://your-server:5200/druid/index.html`

**关注指标：**

- **活跃连接数**：接近或等于 max-active (30) 说明连接池耗尽
- **等待线程数**：大于 0 说明有线程在等待连接
- **慢 SQL**：执行时间超过 1 秒的 SQL
- **错误率**：连接获取失败比例

#### 命令行检查

```bash
# 查看 MySQL 连接数
docker exec -it mysql mysql -uroot -p -e "show processlist;"

# 查看当前连接数统计
docker exec -it mysql mysql -uroot -p -e "show status like 'Threads_connected';"

# 查看最大连接数
docker exec -it mysql mysql -uroot -p -e "show variables like 'max_connections';"

# 查看锁等待
docker exec -it mysql mysql -uroot -p -e "show engine innodb status\G" | grep -A 20 "TRANSACTIONS"
```

### 2.4 第四步：系统资源检查

```bash
# CPU 使用情况
top -bn1 | head -n 20

# 内存使用情况
free -h

# 磁盘 IO
iostat -x 1 5

# 网络连接
netstat -antp | grep 5200 | wc -l  # 当前连接数
ss -s  # 连接统计

# 查看 TCP 连接状态
netstat -n | awk '/^tcp/ {++S[$NF]} END {for(a in S) print a, S[a]}'
```

### 2.5 第五步：应用日志分析

```bash
# 查找异常
docker logs ingot-pms 2>&1 | grep -i "exception\|error" | tail -n 100

# 查找超时
docker logs ingot-pms 2>&1 | grep -i "timeout" | tail -n 50

# 查找数据库相关问题
docker logs ingot-pms 2>&1 | grep -i "connection\|jdbc\|sql" | tail -n 50

# 查找 OOM
docker logs ingot-pms 2>&1 | grep -i "OutOfMemoryError"

# 统计错误类型
docker logs ingot-pms 2>&1 | grep "Exception" | awk -F: '{print $NF}' | sort | uniq -c | sort -rn
```

---

## 3. 常见原因分析

### 3.1 JVM 相关问题

| 问题 | 现象 | 原因 | 解决方案 |
|------|------|------|----------|
| **Full GC 频繁** | CPU 高，响应慢 | 堆内存不足，老年代频繁回收 | 增加堆内存，优化对象创建 |
| **Young GC 耗时长** | 停顿时间长 | 年轻代过大，Minor GC 时间长 | 调整年轻代大小（-XX:NewRatio） |
| **内存泄漏** | 内存持续增长，最终 OOM | 对象无法被回收 | 使用 MAT 分析堆转储，定位泄漏点 |
| **线程堆栈溢出** | StackOverflowError | 递归调用过深 | 检查递归逻辑，增加栈大小（-Xss） |

### 3.2 数据库连接池问题

| 问题 | 现象 | 原因 | 解决方案 |
|------|------|------|----------|
| **连接池耗尽** | 大量 WAITING，响应超时 | 慢 SQL 占用连接，max-active 过小 | 优化 SQL，增加连接池大小 |
| **连接泄漏** | 连接持续增长 | 连接未正确释放 | 检查代码，启用 removeAbandoned |
| **连接超时** | 频繁创建连接 | 数据库响应慢，连接建立超时 | 检查网络，优化数据库性能 |
| **死锁** | 请求挂起 | SQL 死锁 | 分析 InnoDB 状态，优化事务 |

### 3.3 线程问题

| 问题 | 现象 | 原因 | 解决方案 |
|------|------|------|----------|
| **线程池满** | 请求排队或拒绝 | 并发请求超过线程池容量 | 增加线程池大小，优化业务逻辑 |
| **死锁** | 应用假死 | 多个线程相互等待锁 | 分析 jstack，调整锁顺序 |
| **CPU 100%** | 响应慢 | 死循环或计算密集 | jstack 找到热点线程，优化代码 |

### 3.4 网络问题

| 问题 | 现象 | 原因 | 解决方案 |
|------|------|------|----------|
| **TIME_WAIT 过多** | 连接数耗尽 | 短连接过多 | 调整 TCP 参数，使用长连接 |
| **网络延迟** | 响应慢 | 带宽不足，网络拥塞 | 优化网络，使用 CDN |
| **防火墙/限流** | 连接失败 | 被限流或防火墙拦截 | 检查配置，调整限流策略 |

---

## 4. QPS 计算与容量规划

### 4.1 QPS 基础概念

**QPS（Queries Per Second）**：每秒查询率，即每秒处理的请求数量。

```
QPS = 总请求数 / 总时间（秒）
```

### 4.2 单机 QPS 估算

Java 单机能支撑的 QPS 取决于多个因素：

#### 影响因素

| 因素 | 影响 | 说明 |
|------|------|------|
| **业务复杂度** | 高影响 | 简单查询 > 复杂业务逻辑 > 批量计算 |
| **数据库操作** | 高影响 | 无DB > 单次查询 > 多次查询 > 复杂事务 |
| **外部调用** | 高影响 | 每次外部调用增加 50-200ms |
| **CPU 核心数** | 中影响 | 计算密集型与核心数成正比 |
| **内存大小** | 中影响 | 影响缓存和并发能力 |
| **网络带宽** | 低影响 | 大数据量传输时影响显著 |

#### 参考值（8C16G 服务器）

| 场景 | 单机 QPS | 响应时间 | 说明 |
|------|----------|----------|------|
| **纯内存操作** | 5000-10000 | <10ms | 无 IO，纯计算或缓存 |
| **单次简单 DB 查询** | 1000-3000 | 20-50ms | 单表查询，有索引 |
| **单次复杂 DB 查询** | 200-500 | 100-200ms | 多表 JOIN，复杂逻辑 |
| **包含外部调用** | 100-300 | 200-500ms | 1-2 次 HTTP/RPC 调用 |
| **复杂业务逻辑** | 50-200 | 500ms-2s | 多次 DB 访问 + 外部调用 |

### 4.3 并发用户数与 QPS 关系

```
QPS = 并发用户数 × (1 / 平均响应时间)
```

或

```
并发用户数 = QPS × 平均响应时间
```

#### 示例计算：1000 个并发用户

假设场景：
- 用户行为：浏览、查询、操作混合
- 平均思考时间：5秒（用户操作间隔）
- 平均响应时间：200ms（0.2秒）

```
单个用户 QPS = 1 / (5 + 0.2) ≈ 0.19 QPS
总 QPS = 1000 × 0.19 ≈ 190 QPS
```

**实际场景分析：**

| 场景 | 思考时间 | 响应时间 | 单用户 QPS | 1000 用户总 QPS |
|------|----------|----------|-----------|----------------|
| **轻度使用** | 10s | 200ms | 0.098 | 98 |
| **中度使用** | 5s | 200ms | 0.192 | 192 |
| **重度使用** | 3s | 200ms | 0.313 | 313 |
| **压力峰值** | 1s | 200ms | 0.833 | 833 |

### 4.4 容量规划建议

#### 针对 1000 用户场景

**保守估算：**
- 平均 QPS：200
- 峰值 QPS：600-800（3-4倍峰值系数）
- 单机能力：1000 QPS（中等复杂度业务）
- **建议配置：2 台服务器（8C16G）**

**容量计算公式：**

```
所需服务器数量 = (峰值 QPS / 单机 QPS) × 安全系数

安全系数 = 1.5-2.0（预留 50%-100% 余量）

示例：
所需服务器 = (800 / 1000) × 2 = 1.6 ≈ 2 台
```

#### 性能目标设定

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **平均响应时间** | < 200ms | 用户无感知 |
| **95 分位响应时间** | < 500ms | 95% 请求在此时间内完成 |
| **99 分位响应时间** | < 1s | 可接受的慢请求 |
| **错误率** | < 0.1% | 每 1000 次请求小于 1 次错误 |
| **CPU 使用率** | < 70% | 预留余量应对突发流量 |
| **内存使用率** | < 80% | 避免频繁 GC |

### 4.5 压力测试

#### 使用 JMeter 进行压测

```bash
# 安装 JMeter
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz

# 创建测试计划
# 1. 添加线程组：模拟并发用户
# 2. 添加 HTTP 请求：配置接口
# 3. 添加监听器：聚合报告、查看结果树

# 命令行运行
jmeter -n -t test_plan.jmx -l result.jtl -e -o report
```

**压测步骤：**

1. **基线测试**：单用户，确认功能正常
2. **负载测试**：逐步增加并发（50 → 100 → 200 → 500）
3. **压力测试**：找到系统极限（持续增加直到崩溃）
4. **稳定性测试**：70% 负载运行 2-8 小时
5. **峰值测试**：模拟突发流量（2-3 倍平均负载）

#### 使用 wrk 进行简单压测

```bash
# 安装 wrk
git clone https://github.com/wg/wrk.git
cd wrk && make

# 压测示例：12 线程，400 连接，持续 30 秒
./wrk -t12 -c400 -d30s http://your-server:5200/your-api

# 查看结果
# Latency：延迟分布
# Req/Sec：每秒请求数（QPS）
# Transfer/sec：每秒传输数据量
```

---

## 5. 配置优化建议

### 5.1 JVM 参数优化（8C16G 服务器）

#### 当前配置问题

```dockerfile
ENV JAVA_OPTS="-server \
               -Xms2g \
               -Xmx2g \    # ❌ 偏小，建议 4-8G
               ...
```

#### 推荐配置

```dockerfile
ENV JAVA_OPTS="-server \
               # ========== 内存配置 ==========
               -Xms6g \
               -Xmx6g \
               -XX:MetaspaceSize=256m \
               -XX:MaxMetaspaceSize=512m \
               -XX:MaxDirectMemorySize=1g \
               \
               # ========== GC 配置 ==========
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:G1HeapRegionSize=16m \
               -XX:InitiatingHeapOccupancyPercent=40 \
               -XX:G1ReservePercent=15 \
               -XX:ConcGCThreads=2 \
               -XX:ParallelGCThreads=6 \
               \
               # ========== 容器支持 ==========
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               \
               # ========== 性能优化 ==========
               -XX:+AlwaysPreTouch \
               -XX:-UseBiasedLocking \
               -XX:+OptimizeStringConcat \
               -XX:+UseStringDeduplication \
               \
               # ========== 诊断配置 ==========
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/heapdump_$(date +%Y%m%d_%H%M%S).hprof \
               -XX:+ExitOnOutOfMemoryError \
               -XX:ErrorFile=/app/logs/hs_err_pid%p.log \
               \
               # ========== GC 日志 ==========
               -Xlog:gc*,gc+heap=info,gc+age=trace,safepoint:file=/app/logs/gc_%t.log:time,level,tags:filecount=10,filesize=20M \
               \
               # ========== 应用配置 ==========
               -Dspring.profiles.active=prod \
               -Duser.timezone=Asia/Shanghai \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"
```

#### 参数说明

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| **-Xms/-Xmx** | 6g | 16G 内存的 37.5%，预留系统和其他开销 |
| **MaxGCPauseMillis** | 200 | G1 目标停顿时间，平衡吞吐量和延迟 |
| **InitiatingHeapOccupancyPercent** | 40 | 堆使用 40% 时启动并发标记，降低 Full GC 风险 |
| **MaxDirectMemorySize** | 1g | 限制堆外内存，防止容器 OOM |
| **UseStringDeduplication** | 启用 | 减少重复字符串内存占用 |

### 5.2 数据库连接池优化

#### 当前配置问题

```yaml
druid:
  initial-size: 10
  min-idle: 10
  max-active: 30      # ❌ 偏小，高并发时不够
  max-wait: 3000      # ❌ 3 秒等待过长
  remove-abandoned-timeout: 180  # ❌ 3 分钟过长
```

#### 推荐配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      # ========== 连接池大小 ==========
      initial-size: 20          # 初始连接数，建议与 min-idle 相同
      min-idle: 20              # 最小空闲连接，应对基线负载
      max-active: 100           # 最大连接数，根据 QPS 计算
      
      # ========== 连接等待 ==========
      max-wait: 5000            # 最大等待 5 秒
      
      # ========== 连接检测 ==========
      test-on-borrow: false     # 获取时不测试，影响性能
      test-on-return: false     # 归还时不测试
      test-while-idle: true     # 空闲时检测，推荐
      validation-query: SELECT 1
      validation-query-timeout: 3
      
      # ========== 连接回收 ==========
      time-between-eviction-runs-millis: 60000    # 每分钟检测一次
      min-evictable-idle-time-millis: 300000      # 5 分钟未使用则回收
      max-evictable-idle-time-millis: 900000      # 15 分钟强制回收
      
      # ========== 泄漏检测 ==========
      remove-abandoned: true                       # 启用泄漏检测
      remove-abandoned-timeout: 60                 # 60 秒未归还视为泄漏
      log-abandoned: true                          # 记录泄漏日志
      
      # ========== 性能优化 ==========
      pool-prepared-statements: true               # 启用 PSCache
      max-pool-prepared-statement-per-connection-size: 20
      
      # ========== 慢SQL监控 ==========
      filter:
        stat:
          enabled: true
          slow-sql-millis: 1000                    # 慢 SQL 阈值：1 秒
          log-slow-sql: true
          merge-sql: true
      
      # ========== 监控配置 ==========
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        allow: 127.0.0.1,192.168.0.0/16            # 限制访问 IP
        login-username: admin
        login-password: ${DRUID_PASSWORD:admin}    # 使用环境变量
        reset-enable: false                         # 禁止重置统计
      
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: '*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*'
        session-stat-enable: true
        session-stat-max-count: 1000
      
      # ========== 连接参数 ==========
      connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=1000
      connect-properties:
        # 网络超时
        socketTimeout: 30000                       # Socket 读取超时：30 秒
        connectTimeout: 10000                      # 连接超时：10 秒
        # 字符集
        useUnicode: true
        characterEncoding: utf8mb4
        # 性能优化
        useServerPrepStmts: true                   # 使用服务端预编译
        cachePrepStmts: true                       # 缓存 PreparedStatement
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        # 连接保活
        tcpKeepAlive: true
        # 时区
        serverTimezone: Asia/Shanghai
```

#### 连接池大小计算

```
max-active = (期望 QPS × 单次请求平均 DB 查询次数 × 单次查询平均耗时) × 安全系数

示例：
- 期望 QPS：500
- 平均每次请求查询数据库：2 次
- 单次查询耗时：20ms（0.02s）
- 安全系数：1.5

max-active = (500 × 2 × 0.02) × 1.5 = 30

但考虑峰值和慢SQL，建议设置为 100
```

### 5.3 Tomcat/Undertow 线程池配置

```yaml
server:
  port: 5200
  # ========== Undertow 配置（推荐） ==========
  undertow:
    threads:
      # IO 线程数 = CPU 核心数
      io: 8
      # 工作线程数 = IO 线程数 × 8（根据业务调整）
      worker: 256
    # 每个 buffer 的大小
    buffer-size: 1024
    # 是否使用直接内存
    direct-buffers: true
    # 最大HTTP POST内容大小
    max-http-post-size: 10485760
  
  # ========== 连接超时 ==========
  connection-timeout: 30000
  
  # ========== 压缩配置 ==========
  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json,application/xml
    min-response-size: 1024
```

**或使用 Tomcat：**

```yaml
server:
  tomcat:
    threads:
      # 最小工作线程
      min-spare: 50
      # 最大工作线程
      max: 500
    # 最大连接数
    max-connections: 10000
    # 等待队列长度
    accept-count: 200
    # 连接超时
    connection-timeout: 30000
    # 保持连接超时
    keep-alive-timeout: 60000
    # 最大保持连接请求数
    max-keep-alive-requests: 100
```

### 5.4 Spring Boot 异步配置

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(20);
        
        // 最大线程数
        executor.setMaxPoolSize(100);
        
        // 队列容量
        executor.setQueueCapacity(500);
        
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("async-task-");
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}
```

### 5.5 网关限流配置优化

#### 当前配置问题

```yaml
redis-rate-limiter.replenishRate: 200     # ❌ 200 QPS 不够
redis-rate-limiter.burstCapacity: 300     # ❌ 容量偏小
```

#### 推荐配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ingot-service-pms
          uri: lb://ingot-service-pms
          predicates:
            - Path=/pms/**
          filters:
            - StripPrefix=1
            # ========== 限流配置 ==========
            - name: RequestRateLimiter
              args:
                key-resolver: '#{@remoteAddrKeyResolver}'
                # 稳定速率：每秒放入令牌数（根据后端能力设置）
                redis-rate-limiter.replenishRate: 500
                # 突发容量：令牌桶大小（通常是稳定速率的 1.5-2 倍）
                redis-rate-limiter.burstCapacity: 1000
                # 每次请求消费的令牌数
                redis-rate-limiter.requestedTokens: 1
            
            # ========== 熔断配置 ==========
            - name: CircuitBreaker
              args:
                name: pmsFallback
                fallbackUri: forward:/fallback/pms
            
            # ========== 重试配置 ==========
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                methods: GET
                backoff:
                  firstBackoff: 50ms
                  maxBackoff: 500ms
                  factor: 2
                  basedOnPreviousValue: false
      
      # ========== 全局超时 ==========
      httpclient:
        connect-timeout: 3000
        response-timeout: 30s
        pool:
          # 连接池配置
          type: elastic
          max-connections: 1000
          max-idle-time: 30s
          max-life-time: 60s
          eviction-interval: 30s
```

### 5.6 MySQL 数据库优化

```sql
-- 查看当前配置
SHOW VARIABLES LIKE 'max_connections';
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';

-- 推荐配置（my.cnf）
[mysqld]
# ========== 连接配置 ==========
max_connections = 500                      # 最大连接数
max_connect_errors = 1000                  # 最大连接错误次数
wait_timeout = 600                         # 非交互连接超时（10 分钟）
interactive_timeout = 600                  # 交互连接超时（10 分钟）

# ========== InnoDB 配置 ==========
innodb_buffer_pool_size = 8G               # 缓冲池大小（物理内存的 50-70%）
innodb_buffer_pool_instances = 4           # 缓冲池实例数
innodb_log_file_size = 512M                # 日志文件大小
innodb_log_buffer_size = 16M               # 日志缓冲大小
innodb_flush_log_at_trx_commit = 2         # 事务提交刷新策略（性能优先）
innodb_flush_method = O_DIRECT             # 刷新方法
innodb_io_capacity = 2000                  # IO 能力
innodb_io_capacity_max = 4000              # 最大 IO 能力
innodb_read_io_threads = 8                 # 读 IO 线程
innodb_write_io_threads = 8                # 写 IO 线程

# ========== 查询缓存（MySQL 5.7）==========
# query_cache_type = 1
# query_cache_size = 64M

# ========== 临时表 ==========
tmp_table_size = 64M                       # 内存临时表大小
max_heap_table_size = 64M                  # 堆表最大大小

# ========== 慢查询日志 ==========
slow_query_log = 1                         # 启用慢查询日志
long_query_time = 1                        # 慢查询阈值：1 秒
slow_query_log_file = /var/log/mysql/slow.log
log_queries_not_using_indexes = 1          # 记录未使用索引的查询

# ========== 其他 ==========
character_set_server = utf8mb4             # 字符集
collation_server = utf8mb4_unicode_ci      # 排序规则
max_allowed_packet = 64M                   # 最大包大小
```

---

## 6. 监控与预防

### 6.1 APM 监控

#### 推荐工具

1. **Prometheus + Grafana**（开源）
2. **Skywalking**（分布式追踪）
3. **Elastic APM**（ELK 生态）
4. **Pinpoint**（分布式追踪）

#### Spring Boot Actuator 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env,loggers,threaddump,heapdump
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

### 6.2 关键指标监控

#### JVM 指标

```yaml
# 内存
jvm.memory.used
jvm.memory.max
jvm.memory.committed

# GC
jvm.gc.pause                    # GC 停顿时间
jvm.gc.count                    # GC 次数
jvm.gc.memory.allocated         # 分配内存速率
jvm.gc.memory.promoted          # 晋升老年代速率

# 线程
jvm.threads.live                # 活动线程数
jvm.threads.daemon              # 守护线程数
jvm.threads.peak                # 峰值线程数
jvm.threads.states              # 线程状态分布
```

#### 应用指标

```yaml
# HTTP 请求
http.server.requests            # 请求数、响应时间、状态码

# 数据库
hikaricp.connections.active     # 活动连接数
hikaricp.connections.idle       # 空闲连接数
hikaricp.connections.pending    # 等待连接数

# 线程池
executor.active                 # 活动线程
executor.pool.size              # 线程池大小
executor.queue.size             # 队列大小
```

#### 系统指标

```yaml
system.cpu.usage                # CPU 使用率
process.cpu.usage               # 进程 CPU 使用率
system.load.average.1m          # 1 分钟负载
disk.free                       # 磁盘剩余
```

### 6.3 告警规则

```yaml
# Prometheus AlertManager 规则示例
groups:
  - name: java_service_alerts
    interval: 30s
    rules:
      # JVM 内存告警
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "服务 {{ $labels.application }} 内存使用率过高"
          description: "当前使用率: {{ $value | humanizePercentage }}"
      
      # GC 频繁告警
      - alert: FrequentGC
        expr: rate(jvm_gc_pause_seconds_count[5m]) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "服务 {{ $labels.application }} GC 频繁"
          description: "5 分钟内 GC {{ $value }} 次/秒"
      
      # 响应时间告警
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "服务 {{ $labels.application }} 响应时间过长"
          description: "P95 响应时间: {{ $value }}s"
      
      # 错误率告警
      - alert: HighErrorRate
        expr: (rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.application }} 错误率过高"
          description: "错误率: {{ $value | humanizePercentage }}"
      
      # 数据库连接池告警
      - alert: DatabaseConnectionPoolExhausted
        expr: (hikaricp_connections_active / hikaricp_connections_max) > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.application }} 数据库连接池即将耗尽"
          description: "当前使用率: {{ $value | humanizePercentage }}"
      
      # CPU 使用率告警
      - alert: HighCPUUsage
        expr: process_cpu_usage > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "服务 {{ $labels.application }} CPU 使用率过高"
          description: "当前使用率: {{ $value | humanizePercentage }}"
```

### 6.4 日志优化

```yaml
logging:
  level:
    root: INFO
    com.ingot: DEBUG
    # SQL 日志
    com.ingot.pms.mapper: DEBUG
    # 慢 SQL
    druid.sql.Statement: DEBUG
  
  # 日志格式
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n'
    file: '%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{50} - %msg%n'
  
  # 日志文件
  file:
    name: /app/logs/application.log
    max-size: 100MB
    max-history: 30
    total-size-cap: 10GB
  
  # Logback 配置
  config: classpath:logback-spring.xml
```

**Logback 配置示例（logback-spring.xml）：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 彩色日志依赖 -->
    <conversionRule conversionWord="clr" converterClass="org.springframework.boot.logging.logback.ColorConverter" />
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr([%5p]) %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n</pattern>
        </encoder>
    </appender>
    
    <!-- 应用日志 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/application.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>/app/logs/application-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
    </appender>
    
    <!-- 错误日志 -->
    <appender name="ERROR" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>/app/logs/error-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>60</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- 慢 SQL 日志 -->
    <appender name="SLOW_SQL" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/slow-sql.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>/app/logs/slow-sql-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <logger name="druid.sql.Statement" level="DEBUG" additivity="false">
        <appender-ref ref="SLOW_SQL" />
    </logger>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
        <appender-ref ref="ERROR" />
    </root>
</configuration>
```

### 6.5 健康检查

#### Dockerfile 健康检查

```dockerfile
# 安装 curl
RUN yum install -y curl || apt-get update && apt-get install -y curl

# 健康检查
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --retries=3 \
            --start-period=90s \
  CMD curl -f http://localhost:5200/actuator/health/liveness || exit 1
```

#### Kubernetes 健康检查

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: ingot-pms
spec:
  containers:
  - name: ingot-pms
    image: ingot-pms:latest
    
    # 存活探针：容器是否运行
    livenessProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 5200
      initialDelaySeconds: 90
      periodSeconds: 30
      timeoutSeconds: 10
      failureThreshold: 3
    
    # 就绪探针：容器是否准备好接收流量
    readinessProbe:
      httpGet:
        path: /actuator/health/readiness
        port: 5200
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3
    
    # 启动探针：容器是否启动成功
    startupProbe:
      httpGet:
        path: /actuator/health/liveness
        port: 5200
      initialDelaySeconds: 0
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 30  # 最多等待 300 秒
```

---

## 7. 应急处理方案

### 7.1 服务假死快速恢复

#### 方案 1：重启容器（推荐）

```bash
# 优雅重启（推荐）
docker restart ingot-pms

# 强制重启（如果优雅重启失败）
docker kill ingot-pms
docker start ingot-pms
```

#### 方案 2：回滚到上一个版本

```bash
# 拉取上一个稳定版本
docker pull your-registry/ingot-pms:stable

# 停止当前容器
docker stop ingot-pms

# 启动稳定版本
docker run -d --name ingot-pms \
  --restart=always \
  -p 5200:5200 \
  -v /data/ingot-data:/ingot-data \
  -v /data/logs:/app/logs \
  your-registry/ingot-pms:stable
```

#### 方案 3：临时扩容

```bash
# 启动第二个实例（不同端口）
docker run -d --name ingot-pms-2 \
  --restart=always \
  -p 5201:5200 \
  -v /data/ingot-data:/ingot-data \
  -v /data/logs-2:/app/logs \
  your-registry/ingot-pms:latest

# 更新网关配置，添加新实例到负载均衡
```

### 7.2 数据库连接池耗尽处理

```bash
# 1. 立即杀掉慢查询
docker exec -it mysql mysql -uroot -p -e "SHOW PROCESSLIST;" | grep "Query" | awk '{print $1}' | xargs -I {} docker exec -it mysql mysql -uroot -p -e "KILL {};"

# 2. 临时增加连接池大小（需要重启）
# 修改配置文件，然后重启服务

# 3. 清理泄漏连接（Druid 自动清理）
# 访问 Druid 监控页面查看连接状态
```

### 7.3 内存溢出处理

```bash
# 1. 立即导出堆转储（如果服务还能响应）
docker exec -it ingot-pms sh -c 'jmap -dump:live,format=b,file=/app/logs/emergency_heap.hprof $(pgrep java)'

# 2. 增加内存限制（Docker）
docker update --memory=8g --memory-swap=8g ingot-pms

# 3. 重启服务
docker restart ingot-pms

# 4. 分析堆转储找到内存泄漏根源
# 使用 Eclipse MAT 或 VisualVM
```

### 7.4 CPU 100% 处理

```bash
# 1. 找到占用 CPU 的线程
docker exec -it ingot-pms sh -c 'top -Hp $(pgrep java) -n 1'

# 2. 转换线程 ID 为十六进制
# 假设线程 ID 为 12345
printf "%x\n" 12345  # 输出：3039

# 3. 导出线程堆栈
docker exec -it ingot-pms sh -c 'jstack $(pgrep java) | grep -A 30 3039'

# 4. 分析堆栈找到问题代码，修复后发布
```

### 7.5 限流降级

#### 网关层限流

```yaml
# 临时降低限流阈值
redis-rate-limiter.replenishRate: 100  # 降低到 100 QPS
redis-rate-limiter.burstCapacity: 150
```

#### 应用层降级

```java
// 使用 Sentinel 进行降级
@SentinelResource(
    value = "complexBusinessLogic",
    fallback = "fallbackMethod",
    blockHandler = "blockHandler"
)
public Result complexBusinessLogic() {
    // 复杂业务逻辑
}

// 降级方法：返回简化结果
public Result fallbackMethod(Throwable e) {
    return Result.error("服务繁忙，请稍后再试");
}

// 限流方法：返回限流提示
public Result blockHandler(BlockException e) {
    return Result.error("访问量过大，请稍后再试");
}
```

### 7.6 应急预案检查清单

#### 发生故障时

- [ ] **1 分钟内**：确认故障范围（单机/集群）
- [ ] **3 分钟内**：查看监控大盘（CPU/内存/QPS）
- [ ] **5 分钟内**：查看日志和错误信息
- [ ] **10 分钟内**：决定处理方案（重启/扩容/降级）
- [ ] **15 分钟内**：执行恢复操作
- [ ] **30 分钟内**：确认服务恢复正常
- [ ] **事后**：根因分析，编写故障报告

#### 恢复后

- [ ] 保存现场数据（堆转储、线程堆栈、日志）
- [ ] 分析根本原因
- [ ] 制定长期优化方案
- [ ] 更新运维文档
- [ ] 团队复盘

---

## 8. 总结

### 8.1 核心要点

1. **监控优先**：先建立完善的监控体系
2. **容量规划**：基于压测数据进行容量规划
3. **优化配置**：JVM、连接池、线程池合理配置
4. **快速响应**：建立标准化的故障排查流程
5. **持续优化**：定期分析性能瓶颈并优化

### 8.2 针对 1000 用户场景的建议

| 项目 | 推荐配置 |
|------|----------|
| **服务器数量** | 2 台（8C16G），支持 1000-2000 QPS |
| **JVM 内存** | 6G 堆内存 + 1G 堆外内存 |
| **数据库连接池** | max-active: 100 |
| **Undertow 线程池** | worker: 256 |
| **网关限流** | 500 QPS 稳定速率，1000 QPS 突发容量 |
| **负载均衡** | Nginx/Gateway，轮询或最少连接 |

### 8.3 性能优化优先级

1. **高优先级**
   - 优化慢 SQL（单次查询 > 100ms）
   - 增加数据库索引
   - 启用缓存（Redis）
   - 合理配置 JVM 内存

2. **中优先级**
   - 优化数据库连接池
   - 调整线程池大小
   - 启用 HTTP 压缩
   - 代码层面优化（减少循环、批量操作）

3. **低优先级**
   - JVM 参数微调
   - 更换 Web 容器（Tomcat → Undertow）
   - 静态资源 CDN

### 8.4 参考资料

- [Oracle JVM Performance Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [Spring Boot Production-Ready Features](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Alibaba Druid Wiki](https://github.com/alibaba/druid/wiki)
- [MySQL Performance Tuning](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)

---

## 附录

### A. 常用命令速查

```bash
# ========== Docker ==========
docker stats --no-stream ingot-pms
docker logs --tail 1000 -f ingot-pms
docker exec -it ingot-pms sh

# ========== JVM 诊断 ==========
jps -lv                          # 查看 Java 进程
jstack <pid>                     # 线程堆栈
jmap -heap <pid>                 # 堆信息
jmap -histo:live <pid>           # 对象统计
jstat -gc <pid> 1000 10          # GC 统计

# ========== 系统监控 ==========
top -Hp <pid>                    # 线程级 CPU
free -h                          # 内存
iostat -x 1 5                    # 磁盘 IO
netstat -antp | grep 5200        # 网络连接

# ========== 日志分析 ==========
grep -i "exception" application.log | tail -n 50
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -n 10
```

### B. 监控指标参考值

| 指标 | 正常范围 | 警告阈值 | 危险阈值 |
|------|----------|----------|----------|
| CPU 使用率 | < 60% | 60-80% | > 80% |
| 内存使用率（堆） | < 70% | 70-85% | > 85% |
| Full GC 频率 | < 1次/小时 | 1-10次/小时 | > 10次/小时 |
| Full GC 耗时 | < 500ms | 500ms-1s | > 1s |
| 平均响应时间 | < 200ms | 200-500ms | > 500ms |
| P95 响应时间 | < 500ms | 500ms-1s | > 1s |
| 错误率 | < 0.01% | 0.01-0.1% | > 0.1% |
| 数据库连接使用率 | < 70% | 70-90% | > 90% |
| 线程池使用率 | < 70% | 70-90% | > 90% |

---

**文档版本**：v1.0  
**最后更新**：2025-12-24  
**适用版本**：Java 17, Spring Boot 3.x  
**作者**：ingot-cloud 团队

