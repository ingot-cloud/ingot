# 故障排查快速参考卡片 🚀

> 服务假死时的快速诊断指南 - 打印此页面贴在工位上！

## ⚠️ 重要提示：微服务多实例场景

本参考基于 **8C16G 服务器运行多个微服务**（Gateway + Auth + PMS + Member 等）的场景。配置已针对资源共享进行优化：
- 单服务 JVM：**2G**（不是 6G）
- 单服务连接池：**50**（不是 100）
- 单服务线程池：**120**（不是 256）

详见：[微服务资源规划指南](./MICROSERVICES-RESOURCE-PLANNING.md) 和 [配置对比文档](./CONFIGURATION-COMPARISON.md)

## ⚡ 30 秒快速诊断

```bash
# 1. 运行自动诊断脚本（推荐）
./bin/troubleshoot.sh ingot-pms

# 2. 查看诊断报告
cat troubleshoot_*/REPORT.md
```

---

## 🔥 应急处理（1分钟内）

### 服务完全假死
```bash
# 立即重启
docker restart ingot-pms

# 如果重启失败，强制重启
docker kill ingot-pms && docker start ingot-pms
```

### 服务响应慢但未死
```bash
# 查看实时资源使用
docker stats ingot-pms

# 查看实时日志
docker logs -f --tail 100 ingot-pms
```

---

## 🔍 五步排查法（15分钟）

### 第 1 步：看日志（2分钟）
```bash
# 查找异常
docker logs --tail 1000 ingot-pms | grep -i "exception\|error" | tail -n 20

# 查找 OOM
docker logs --tail 1000 ingot-pms | grep -i "OutOfMemoryError"

# 查找超时
docker logs --tail 1000 ingot-pms | grep -i "timeout" | tail -n 10
```

**常见关键字**：
- `OutOfMemoryError` → 内存溢出
- `Connection refused` → 连接失败
- `Timeout` → 超时
- `Cannot get connection` → 连接池耗尽

### 第 2 步：看线程（3分钟）
```bash
# 进入容器
docker exec -it ingot-pms sh

# 导出线程堆栈
PID=$(pgrep java)
jstack $PID > /tmp/thread.txt

# 统计线程状态
grep "java.lang.Thread.State" /tmp/thread.txt | sort | uniq -c

# 查找死锁
grep -A 20 "deadlock" /tmp/thread.txt
```

**关注指标**：
- `BLOCKED > 10` → 锁竞争严重
- `WAITING > 100` → 可能连接池/线程池耗尽
- `Found deadlock` → 死锁

### 第 3 步：看内存（3分钟）
```bash
# 堆内存使用
jmap -heap $PID | grep -A 10 "Heap Usage"

# 对象统计（前10）
jmap -histo:live $PID | head -n 15
```

**关注指标**：
- 老年代使用率 `> 90%` → 内存即将耗尽
- 对象数量异常多 → 可能内存泄漏

### 第 4 步：看 GC（3分钟）
```bash
# GC 统计
jstat -gcutil $PID 1000 5
```

**关注指标**：
- `FGC` 频繁（> 10次/小时）→ Full GC 过多
- `FGCT` 很大 → Full GC 耗时长
- `OU` 接近 100% → 老年代满

### 第 5 步：看连接（4分钟）
```bash
# 数据库连接
# 访问 Druid 监控：http://your-server:5200/druid/index.html

# TCP 连接
netstat -antp | grep 5200 | wc -l
netstat -n | awk '/^tcp/ {++S[$NF]} END {for(a in S) print a, S[a]}'
```

**关注指标**：
- Druid 活跃连接 ≈ max-active → 连接池耗尽
- `TIME_WAIT > 1000` → 短连接过多
- `CLOSE_WAIT > 100` → 连接未正确关闭

---

## 📊 关键指标阈值速查表

### 单个服务指标（微服务场景）

| 指标 | 正常 | 警告 | 危险 | 操作 |
|------|------|------|------|------|
| **CPU（单服务）** | < 40% | 40-60% | > 60% | 找到热点线程（jstack） |
| **堆内存（单服务）** | < 1.4G | 1.4-1.7G | > 1.7G | 检查内存泄漏 |
| **Full GC** | < 1次/时 | 1-10次/时 | > 10次/时 | 调整 JVM 参数 |
| **Full GC 耗时** | < 500ms | 500ms-1s | > 1s | 优化代码 |
| **响应时间 P95** | < 500ms | 500ms-1s | > 1s | 优化慢接口 |
| **错误率** | < 0.01% | 0.01-0.1% | > 0.1% | 查日志定位原因 |
| **DB 连接使用率** | < 35/50 | 35-45/50 | > 45/50 | 增加连接池或优化 SQL |
| **BLOCKED 线程** | < 5 | 5-10 | > 10 | 查找锁竞争 |

### 整机资源指标（所有服务总和）

| 指标 | 正常 | 警告 | 危险 | 操作 |
|------|------|------|------|------|
| **总 CPU** | < 60% | 60-75% | > 75% | 扩容或优化服务 |
| **总内存** | < 12G | 12-14G | > 14G | 扩容或减少服务 |
| **MySQL 连接数** | < 200 | 200-300 | > 300 | 检查连接泄漏 |

---

## 💊 常见问题速查

### 问题 1：内存溢出（OOM）
```bash
# 现象
日志中出现：java.lang.OutOfMemoryError

# 快速处理
1. docker restart ingot-pms
2. 临时增加内存：docker update --memory=8g ingot-pms
3. 分析堆转储找根因

# 长期方案
- 增加 JVM 堆内存（Xmx）
- 使用 MAT 分析内存泄漏
- 优化代码减少对象创建
```

### 问题 2：连接池耗尽
```bash
# 现象
- 日志：Cannot get connection within timeout
- Druid 监控：活跃连接 = max-active

# 快速处理
1. 查找慢 SQL：访问 Druid 监控
2. 杀掉慢查询：SHOW PROCESSLIST; KILL <id>;
3. 重启服务

# 长期方案
- 增加连接池大小（max-active: 100）
- 优化慢 SQL，添加索引
- 启用连接泄漏检测（remove-abandoned）
```

### 问题 3：CPU 100%
```bash
# 现象
- top 显示 Java 进程 CPU 100%
- 服务响应极慢

# 快速处理
1. 找到热点线程
   top -Hp $(pgrep java) -n 1
2. 转换线程 ID 为十六进制
   printf "%x\n" <线程ID>
3. 导出线程堆栈并查找
   jstack $(pgrep java) | grep -A 30 <十六进制ID>

# 常见原因
- 死循环
- 正则表达式回溯
- 频繁 GC
```

### 问题 4：死锁
```bash
# 现象
- 服务假死，部分接口不响应
- jstack 显示 deadlock

# 快速处理
1. 重启服务
2. 导出线程堆栈分析死锁位置

# 长期方案
- 调整锁顺序，避免循环等待
- 使用 tryLock() 设置超时
- 减少锁的粒度
```

### 问题 5：慢 SQL
```bash
# 现象
- 响应时间长
- Druid 监控显示慢 SQL

# 快速处理
1. 访问 Druid：http://localhost:5200/druid/sql.html
2. 查看慢 SQL 列表
3. 使用 EXPLAIN 分析执行计划

# 优化方法
- 添加索引
- 优化查询条件
- 分页查询
- 避免 SELECT *
```

---

## 🛠️ 常用命令速记

### Docker 相关
```bash
# 查看资源
docker stats --no-stream <容器名>

# 查看日志
docker logs -f --tail 100 <容器名>

# 进入容器
docker exec -it <容器名> sh

# 重启容器
docker restart <容器名>

# 复制文件
docker cp <容器名>:/path/to/file ./
```

### JVM 诊断
```bash
# 找 Java 进程
PID=$(pgrep java)

# 线程堆栈
jstack $PID

# 堆信息
jmap -heap $PID

# 对象统计
jmap -histo:live $PID | head -n 20

# GC 统计
jstat -gcutil $PID 1000 10

# 堆转储（会暂停应用）
jmap -dump:live,format=b,file=heap.hprof $PID
```

### 系统监控
```bash
# CPU
top -bn1 | head -20

# 内存
free -h

# 磁盘 IO
iostat -x 1 5

# 网络连接
netstat -antp | grep <端口>
ss -s
```

---

## 📋 故障处理检查清单

### 发生故障时（按顺序执行）

- [ ] **0-1 分钟**：确认故障范围
  - 访问健康检查接口
  - 查看监控大盘
  - 确认是否单机故障

- [ ] **1-3 分钟**：初步诊断
  - 查看容器资源使用
  - 查看最近日志（异常、错误）
  - 确认数据库、Redis 是否正常

- [ ] **3-10 分钟**：深入分析
  - 运行诊断脚本
  - 导出线程堆栈
  - 检查 GC 情况
  - 检查数据库连接

- [ ] **10-15 分钟**：决策处理
  - 如果可恢复：重启服务
  - 如果是负载问题：扩容或限流
  - 如果是代码问题：回滚版本

- [ ] **15-30 分钟**：执行恢复
  - 执行恢复方案
  - 验证服务正常
  - 保存现场数据

### 恢复后（事后处理）

- [ ] 保存诊断数据（堆转储、线程堆栈、日志）
- [ ] 分析根本原因
- [ ] 制定长期优化方案
- [ ] 更新运维文档
- [ ] 团队复盘

---

## 📞 紧急联系方式

```
运维团队：emergency@example.com
告警系统：监控大盘 URL
故障文档：/docs/TROUBLESHOOTING-SERVICE-HANG.md
诊断脚本：./bin/troubleshoot.sh
```

---

## 🎯 QPS 快速计算

```
QPS = 并发用户数 ÷ (思考时间 + 响应时间)

示例：1000 用户，5秒思考，200ms响应
QPS = 1000 ÷ (5 + 0.2) ≈ 192

单机能力参考（8C16G，微服务多实例共存）：
- 单个服务简单查询：500-800 QPS
- 单个服务中等复杂：300-500 QPS  
- 单个服务复杂业务：100-300 QPS
- 整机能力（4服务）：400-600 QPS

1000 用户建议：
- 2 台应用服务器（每台运行 Gateway+Auth+PMS+Member）
- 1 台数据库服务器（MySQL+Redis）
- 负载均衡（Nginx）
```

---

## 📚 完整文档

详细内容请查看：
- **故障排查指南**：`docs/TROUBLESHOOTING-SERVICE-HANG.md`
- **微服务资源规划**：`docs/MICROSERVICES-RESOURCE-PLANNING.md` ⭐
- **配置对比**：`docs/CONFIGURATION-COMPARISON.md` ⭐
- **配置示例**：`docs/config-examples/`
- **诊断脚本**：`bin/troubleshoot.sh`
- **优化 Dockerfile**：`ingot-service/ingot-pms/ingot-pms-provider/src/main/docker/prod/Dockerfile`

---

**最后更新**：2025-12-24  
**版本**：v1.0  
**适用场景**：8C16G 服务器，Java 17，Spring Boot 3.x，**微服务多实例共存**

---

💡 **提示**：将此页面打印出来，出现故障时按照步骤操作，可大大缩短恢复时间！

