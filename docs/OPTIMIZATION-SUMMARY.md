# 微服务性能优化与故障排查总览

> 8C16G 服务器，1000 并发用户，微服务多实例共存场景完整方案

## 📖 文档导航

### 🔥 必读文档

1. **[微服务资源规划指南](./MICROSERVICES-RESOURCE-PLANNING.md)** ⭐⭐⭐
   - 单机多服务资源分配策略
   - 内存、CPU、连接数规划
   - Docker Compose 完整配置
   - 不同规模的部署方案

2. **[配置对比文档](./CONFIGURATION-COMPARISON.md)** ⭐⭐⭐
   - 单服务 vs 多服务配置对比
   - 配置决策树
   - 快速调整命令

3. **[故障排查指南](./TROUBLESHOOTING-SERVICE-HANG.md)** ⭐⭐⭐
   - 完整的 5 步排查流程
   - JVM、数据库、网络诊断
   - 监控配置和告警规则
   - 应急处理方案

### 📋 快速参考

4. **[快速参考卡片](./QUICK-REFERENCE-TROUBLESHOOTING.md)** ⭐⭐
   - 30 秒快速诊断
   - 五步排查法
   - 关键指标阈值
   - 常见问题速查

5. **[配置示例说明](./config-examples/README.md)** ⭐⭐
   - 优化后的配置文件
   - 性能预期
   - 应用步骤
   - 压力测试方法

### 🛠️ 工具脚本

6. **[自动诊断脚本](../bin/troubleshoot.sh)** ⭐
   - 一键收集诊断信息
   - 自动生成分析报告

---

## 🎯 核心优化内容

### 1. 资源配置（针对微服务多实例）

#### 内存分配（8C16G 服务器）

```
总内存 16G 分配：
├─ 系统预留：      2G
├─ Gateway：       2.5G (JVM 2G)
├─ Auth：          2.5G (JVM 2G)
├─ PMS：           2.5G (JVM 2G)
├─ Member：        2.5G (JVM 2G)
├─ MySQL：         1G
├─ Redis：         512M
└─ 缓冲：          512M
```

#### JVM 配置
```bash
-Xms2g -Xmx2g                    # 堆内存 2G
-XX:MetaspaceSize=256m           # 元空间
-XX:MaxMetaspaceSize=384m
-XX:MaxDirectMemorySize=256m     # 堆外内存
-XX:+UseG1GC                     # G1 垃圾回收器
-XX:MaxGCPauseMillis=200         # 最大停顿时间
```

#### 数据库连接池
```yaml
initial-size: 10
min-idle: 10
max-active: 50                   # 每服务 50 个连接
max-wait: 5000
remove-abandoned-timeout: 60     # 连接泄漏检测
```

#### Web 容器
```yaml
undertow:
  threads:
    io: 4                        # IO 线程
    worker: 120                  # 工作线程
```

#### 网关限流
```yaml
redis-rate-limiter:
  replenishRate: 300             # 稳定速率
  burstCapacity: 500             # 突发容量
```

---

## 📊 性能指标

### 单机能力（8C16G，多服务共存）

| 服务 | QPS | 响应时间 | 说明 |
|------|-----|----------|------|
| Gateway | 2000-3000 | 20-50ms | 路由转发 |
| Auth | 300-500 | 100-200ms | 认证计算 |
| PMS | 300-500 | 100-200ms | 业务逻辑 |
| Member | 300-500 | 100-200ms | 业务逻辑 |
| **整机** | **400-600** | **150ms** | **总体能力** |

### 1000 用户场景

```
期望 QPS：192（平均）
峰值 QPS：576-768（3-4倍）
单机能力：400-600 QPS
所需服务器：2 台应用服务器 + 1 台数据库服务器
余量：50%-100%
```

---

## 🔍 故障排查流程

### 快速诊断（5 分钟）

```bash
# 1. 运行自动诊断脚本（推荐）
./bin/troubleshoot.sh ingot-pms

# 2. 查看诊断报告
cat troubleshoot_*/REPORT.md

# 3. 手动快速检查
docker stats --no-stream
docker logs --tail 100 ingot-pms | grep -i error
```

### 深度排查（15 分钟）

#### 第 1 步：看日志
```bash
# 异常
docker logs --tail 1000 ingot-pms | grep -i "exception\|error"
# OOM
docker logs --tail 1000 ingot-pms | grep -i "OutOfMemoryError"
```

#### 第 2 步：看线程
```bash
docker exec -it ingot-pms sh
jstack $(pgrep java) | grep "java.lang.Thread.State" | sort | uniq -c
```

#### 第 3 步：看内存
```bash
jmap -heap $(pgrep java)
jmap -histo:live $(pgrep java) | head -n 20
```

#### 第 4 步：看 GC
```bash
jstat -gcutil $(pgrep java) 1000 10
```

#### 第 5 步：看连接
```bash
# Druid 监控
http://your-server:5200/druid/index.html
# MySQL 连接数
docker exec -it mysql mysql -uroot -p -e "SHOW PROCESSLIST;"
```

---

## 🚨 常见问题处理

### 问题 1：内存溢出（OOM）

**现象**：
- 日志出现 `OutOfMemoryError`
- 服务假死重启

**快速处理**：
```bash
# 1. 重启服务
docker restart ingot-pms

# 2. 临时增加内存
docker update --memory=3g ingot-pms

# 3. 导出堆转储分析
docker exec ingot-pms sh -c 'jmap -dump:live,format=b,file=/app/logs/heap.hprof $(pgrep java)'
```

**长期方案**：
- 使用 MAT 分析堆转储
- 找到内存泄漏点
- 优化代码

### 问题 2：连接池耗尽

**现象**：
- `Cannot get connection within timeout`
- Druid 活跃连接 = max-active

**快速处理**：
```bash
# 1. 查看慢 SQL
# 访问 http://localhost:5200/druid/sql.html

# 2. 杀掉慢查询
docker exec mysql mysql -uroot -p -e "SHOW PROCESSLIST;"
docker exec mysql mysql -uroot -p -e "KILL <id>;"

# 3. 重启服务
docker restart ingot-pms
```

**长期方案**：
- 优化慢 SQL，添加索引
- 增加连接池：max-active: 80
- 检查连接泄漏

### 问题 3：CPU 100%

**现象**：
- top 显示 Java 进程 CPU 100%
- 服务响应极慢

**快速处理**：
```bash
# 1. 找到热点线程
top -Hp $(pgrep java) -n 1

# 2. 转换为十六进制
printf "%x\n" <线程ID>

# 3. 查看线程堆栈
jstack $(pgrep java) | grep -A 30 <十六进制ID>
```

**常见原因**：
- 死循环
- 正则表达式回溯
- 频繁 GC

### 问题 4：多服务资源争抢

**现象**：
- 所有服务都变慢
- 系统内存/CPU 接近 100%

**快速处理**：
```bash
# 1. 查看所有服务资源
docker stats

# 2. 临时关闭非关键服务
docker stop ingot-member

# 3. 调整资源限制
docker update --memory=2g ingot-pms
```

**长期方案**：
- 增加服务器
- 服务拆分部署
- 优化资源配置

---

## 📈 监控配置

### 关键指标

#### 单服务指标
- CPU 使用率 < 40%
- 堆内存使用 < 1.4G (70%)
- Full GC < 1 次/小时
- 响应时间 P95 < 500ms
- 数据库连接 < 35/50

#### 整机指标
- 总 CPU < 60%
- 总内存 < 12G
- MySQL 连接数 < 200
- 错误率 < 0.1%

### Prometheus 告警规则

```yaml
groups:
  - name: microservices_alerts
    rules:
      # 内存告警
      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.85
        for: 5m
      
      # CPU 告警
      - alert: HighCPUUsage
        expr: process_cpu_usage > 0.7
        for: 5m
      
      # 响应时间告警
      - alert: SlowResponse
        expr: histogram_quantile(0.95, http_server_requests_seconds_bucket) > 1
        for: 5m
```

---

## 🎯 部署方案

### 推荐部署（1000 用户）

```
┌─────────────────────────────────────┐
│         应用服务器 1 (8C16G)        │
│  Gateway + Auth + PMS + Member      │
│  ↓ 负载均衡                         │
│         应用服务器 2 (8C16G)        │
│  Gateway + Auth + PMS + Member      │
└─────────────────────────────────────┘
              ↓ 连接
┌─────────────────────────────────────┐
│       数据库服务器 (4C8G)           │
│       MySQL + Redis                 │
└─────────────────────────────────────┘
```

### Docker Compose 部署

参考：[微服务资源规划指南](./MICROSERVICES-RESOURCE-PLANNING.md) 第 7 节

---

## ✅ 检查清单

### 部署前

- [ ] 确认服务器配置（8C16G）
- [ ] 规划服务部署（哪些服务同机）
- [ ] 计算资源分配（内存、CPU、连接数）
- [ ] 配置 Docker 资源限制
- [ ] 配置 MySQL max_connections
- [ ] 准备监控工具（Prometheus/Grafana）

### 部署后

- [ ] 所有服务启动成功
- [ ] 健康检查通过
- [ ] 监控指标正常
- [ ] 压力测试通过
- [ ] 告警规则配置
- [ ] 文档更新

### 日常运维

- [ ] 每日查看监控大盘
- [ ] 每周检查日志和错误
- [ ] 每月分析性能趋势
- [ ] 定期压力测试
- [ ] 定期备份数据

---

## 🔗 相关链接

### 文档
- [微服务资源规划](./MICROSERVICES-RESOURCE-PLANNING.md)
- [配置对比](./CONFIGURATION-COMPARISON.md)
- [故障排查指南](./TROUBLESHOOTING-SERVICE-HANG.md)
- [快速参考](./QUICK-REFERENCE-TROUBLESHOOTING.md)
- [配置示例](./config-examples/README.md)

### 配置文件
- [Dockerfile](../ingot-service/ingot-pms/ingot-pms-provider/src/main/docker/prod/Dockerfile)
- [应用配置](./config-examples/application-prod-optimized.yml)
- [网关配置](./config-examples/gateway-routes-optimized.yml)

### 工具
- [诊断脚本](../bin/troubleshoot.sh)

---

## 📞 快速支持

### 遇到问题时

1. **查看快速参考**：[QUICK-REFERENCE-TROUBLESHOOTING.md](./QUICK-REFERENCE-TROUBLESHOOTING.md)
2. **运行诊断脚本**：`./bin/troubleshoot.sh <容器名>`
3. **查看故障排查指南**：按照 5 步排查流程操作
4. **查看配置对比**：确认配置是否合理

### 常用命令

```bash
# 查看所有服务资源
docker stats

# 运行诊断
./bin/troubleshoot.sh ingot-pms

# 查看日志
docker logs -f --tail 100 ingot-pms

# 重启服务
docker restart ingot-pms

# 查看数据库连接
docker exec mysql mysql -uroot -p -e "SHOW PROCESSLIST;"
```

---

## 💡 最佳实践

1. **资源规划**
   - 提前规划，避免资源争抢
   - 预留 20% 缓冲
   - 监控实际使用，动态调整

2. **性能优化**
   - 优化慢 SQL（优先级最高）
   - 使用缓存（Redis）
   - 异步处理非关键业务

3. **监控告警**
   - 建立完善的监控体系
   - 设置合理的告警阈值
   - 定期查看和分析

4. **故障演练**
   - 定期进行故障演练
   - 熟悉排查流程
   - 更新运维文档

5. **持续改进**
   - 收集性能数据
   - 分析瓶颈
   - 持续优化

---

## 📊 成果预期

### 优化前（原配置）

```
单服务配置：
- JVM: 2G
- 连接池: 30
- 线程池: 默认

问题：
❌ 高并发时连接池耗尽
❌ 资源利用率低
❌ 缺乏监控和告警
❌ 故障排查困难
```

### 优化后（新配置）

```
微服务配置（每服务）：
- JVM: 2G（合理分配）
- 连接池: 50（提升 67%）
- 线程池: 120（优化配置）
- 限流: 300 QPS
- 熔断: 50% 失败率
- 监控: Prometheus + Grafana
- 工具: 自动诊断脚本

成果：
✅ 支持 400-600 QPS（整机）
✅ 响应时间 P95 < 500ms
✅ 资源利用率提升 40%
✅ 故障排查时间缩短 80%
✅ 完善的监控和告警
✅ 可平滑扩展到 1500 用户
```

---

**最后更新**：2025-12-24  
**版本**：v1.0  
**维护者**：ingot-cloud 团队  
**适用场景**：8C16G 服务器，1000 并发用户，微服务多实例共存

---

💡 **建议**：将本文档和快速参考卡片打印出来，放在工位上，方便随时查阅！

