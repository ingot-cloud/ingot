# TSS 任务调度迁移指南

## 概述

本指南帮助你从现有的任务调度方式迁移到 TSS 抽象层，或在 Spring 和 XXL-Job 之间切换。

## 迁移场景

### 场景 1：从 Spring @Scheduled 迁移到 TSS

#### 现有代码

```java
@Component
public class OrderTask {
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOrders() {
        // 业务逻辑
        log.info("清理订单");
    }
}
```

#### 迁移后代码

```java
@Service
public class OrderTask {
    
    @ScheduledTask(
        name = "order-cleanup-task",
        description = "订单清理任务",
        cron = "0 0 2 * * ?",
        group = "order"
    )
    public TaskResult cleanupOrders(TaskContext context) {
        // 业务逻辑不变
        log.info("清理订单");
        
        // 新增：返回执行结果
        return TaskResult.success("清理完成");
    }
}
```

#### 迁移步骤

1. 将 `@Scheduled` 改为 `@ScheduledTask`
2. 添加 `name`、`description` 等属性
3. 方法参数改为 `TaskContext context`（可选）
4. 返回 `TaskResult`

### 场景 2：从 XXL-Job 原生注解迁移到 TSS

#### 现有代码

```java
@Component
public class DataTask {
    
    @XxlJob("dataSyncHandler")
    public void syncData() {
        String param = XxlJobHelper.getJobParam();
        
        // 业务逻辑
        log.info("同步数据，参数: {}", param);
        
        XxlJobHelper.handleSuccess("同步完成");
    }
}
```

#### 迁移后代码

```java
@Service
public class DataTask {
    
    @ScheduledTask(
        name = "data-sync-task",
        description = "数据同步任务",
        group = "data"
    )
    public TaskResult syncData(TaskContext context) {
        // 从上下文获取参数
        String param = context.getParams();
        
        // 业务逻辑不变
        log.info("同步数据，参数: {}", param);
        
        // 新增：返回结果
        return TaskResult.success("同步完成");
    }
}
```

#### XXL-Job Admin 配置调整

**之前：**
- JobHandler: `dataSyncHandler`
- Cron: `0 */10 * * * ?`

**之后：**
- JobHandler: `ingotTaskHandler`（统一入口）
- 任务参数: `data-sync-task`（任务名称）
- Cron: `0 */10 * * * ?`（不变）

### 场景 3：从 Spring 切换到 XXL-Job

#### 步骤 1：修改依赖

编辑 `build.gradle`：

```gradle
dependencies {
    // 注释或删除 Spring 依赖
    // implementation project(':ingot-framework:ingot-tss-spring')
    
    // 添加 XXL-Job 依赖
    implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

#### 步骤 2：修改配置

编辑 `application.yml`：

```yaml
# 删除或注释 Spring 配置
# ingot:
#   tss:
#     type: spring

# 添加 XXL-Job 配置
ingot:
  tss:
    type: xxljob
    xxljob:
      enabled: true
      admin:
        addresses: http://localhost:8080/xxl-job-admin
        access-token: default_token
      executor:
        app-name: ${spring.application.name}
        port: 9999
        log-path: ./logs/xxl-job
```

#### 步骤 3：在 XXL-Job Admin 中配置任务

为每个 @ScheduledTask 任务创建对应的配置：

1. 登录 XXL-Job Admin
2. 创建执行器（如果不存在）
3. 为每个任务创建配置：
   - JobHandler: `ingotTaskHandler`
   - 任务参数: 填写任务名称（如 `order-cleanup-task`）
   - Cron: 配置执行时间
   - 运行模式: BEAN

#### 步骤 4：重新编译和启动

```bash
./gradlew clean build
./gradlew :your-service:bootRun
```

#### 步骤 5：验证

1. 查看启动日志，确认任务已注册
2. 在 XXL-Job Admin 中查看执行器是否在线
3. 手动触发任务测试

**业务代码无需任何修改！**

### 场景 4：从 XXL-Job 切换到 Spring

#### 步骤 1：修改依赖

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-tss-spring')
    // implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

#### 步骤 2：修改配置

```yaml
ingot:
  tss:
    type: spring  # 或直接删除，默认就是 spring
```

#### 步骤 3：确保 Cron 配置

检查 @ScheduledTask 注解中是否配置了 cron：

```java
@ScheduledTask(
    name = "my-task",
    cron = "0 0 2 * * ?"  // Spring 需要在代码或配置文件中配置 Cron
)
public TaskResult myTask(TaskContext context) { }
```

#### 步骤 4：重新启动

```bash
./gradlew :your-service:bootRun
```

**业务代码无需修改！**

## 兼容性说明

### 完全兼容

以下功能在 Spring 和 XXL-Job 之间完全兼容：

- ✅ `@ScheduledTask` 注解
- ✅ `IScheduledTask` 接口
- ✅ `TaskContext` 上下文
- ✅ `TaskResult` 结果
- ✅ 手动触发任务

### 部分兼容

以下功能在两种实现中行为不同：

| 功能 | Spring | XXL-Job |
|------|--------|---------|
| 分片执行 | 不支持 | 支持 |
| 执行历史 | 内存（1000条） | 数据库（完整） |
| 动态 Cron | 代码更新 | Admin 更新 |
| 任务依赖 | 不支持 | 支持 |
| 失败重试 | 不支持 | 支持 |

### 不兼容

以下功能只在特定实现中可用：

**仅 XXL-Job：**
- 原生 `@XxlJob` 注解
- GLUE 模式
- 路由策略
- 管理界面

**解决方案：** 使用 `@ScheduledTask` 注解保证兼容性

## 常见问题

### Q1: 迁移后任务不执行？

**A:** 检查以下几点：

**Spring 环境：**
- 确认 Cron 表达式已配置（代码或配置文件）
- 查看启动日志，确认任务已注册

**XXL-Job 环境：**
- 确认在 Admin 中已配置任务
- 确认执行器在线
- 确认 JobHandler 和任务参数正确

### Q2: 如何保留执行历史？

**A:**

**Spring 环境：**
- 执行历史保存在内存，最多1000条
- 如需持久化，可以自定义实现

**XXL-Job 环境：**
- 执行历史自动保存在数据库
- 在 Admin 后台查看完整历史

### Q3: 分片功能如何迁移？

**A:**

如果使用了 XXL-Job 的分片功能：

```java
@ScheduledTask(name = "my-task")
public TaskResult myTask(TaskContext context) {
    if (context.isSharding()) {
        // XXL-Job 环境（分片执行）
        int shardIndex = context.getShardIndex();
        // 处理当前分片的数据...
    } else {
        // Spring 环境（全量执行）
        // 处理所有数据...
    }
    
    return TaskResult.success("完成");
}
```

### Q4: 如何混合使用？

**A:**

可以在同一个项目中混合使用：

```java
// 方式1：使用抽象层（可切换）
@ScheduledTask(name = "task1")
public TaskResult task1(TaskContext context) { }

// 方式2：XXL-Job 原生注解（仅XXL-Job）
@XxlJob("task2Handler")
public void task2() { }
```

**注意：** 使用原生 @XxlJob 的任务无法切换到 Spring。

## 迁移检查清单

### Spring → XXL-Job

- [ ] 修改 build.gradle 依赖
- [ ] 修改 application.yml 配置
- [ ] 部署 XXL-Job Admin
- [ ] 在 Admin 中创建执行器
- [ ] 为每个任务创建配置
- [ ] 测试任务执行
- [ ] 检查执行日志

### XXL-Job → Spring

- [ ] 修改 build.gradle 依赖
- [ ] 修改 application.yml 配置
- [ ] 确保 Cron 在代码中配置
- [ ] 移除 Admin 依赖
- [ ] 测试任务执行
- [ ] 检查执行日志

### 原生注解 → 抽象层

- [ ] 将 @Scheduled 或 @XxlJob 改为 @ScheduledTask
- [ ] 添加 name、description 等属性
- [ ] 方法参数改为 TaskContext（可选）
- [ ] 返回值改为 TaskResult
- [ ] 测试功能

## 最佳实践

### 1. 优先使用抽象层

```java
// ✅ 推荐：使用抽象层，可切换
@ScheduledTask(name = "my-task")
public TaskResult myTask(TaskContext context) { }

// ❌ 不推荐：使用原生注解，不可切换
@XxlJob("myTaskHandler")
public void myTask() { }
```

### 2. 统一返回 TaskResult

```java
// ✅ 推荐
@ScheduledTask(name = "my-task")
public TaskResult myTask(TaskContext context) {
    try {
        // 业务逻辑
        return TaskResult.success("完成");
    } catch (Exception e) {
        return TaskResult.failure("失败", e);
    }
}

// ❌ 不推荐
@ScheduledTask(name = "my-task")
public void myTask(TaskContext context) {
    // void 返回
}
```

### 3. 合理使用分组

```java
// 按业务模块分组
@ScheduledTask(name = "task1", group = "order")
@ScheduledTask(name = "task2", group = "data")
@ScheduledTask(name = "task3", group = "report")
```

### 4. 监控和告警

```java
@ScheduledTask(name = "critical-task")
public TaskResult criticalTask(TaskContext context) {
    try {
        // 关键业务逻辑
        return TaskResult.success("完成");
    } catch (Exception e) {
        // 发送告警
        alertService.sendAlert("关键任务失败: " + e.getMessage());
        return TaskResult.failure("失败", e);
    }
}
```

## 性能影响

迁移到 TSS 抽象层对性能的影响：

- **微乎其微**：只是增加了一层接口调用
- **内存占用**：Spring 实现会保存执行历史（最多1000条）
- **线程池**：可配置，默认10个线程

## 回滚方案

如果迁移后发现问题，可以快速回滚：

### Spring → 原生 @Scheduled

```gradle
// 移除 TSS 依赖
// implementation project(':ingot-framework:ingot-tss-spring')

// 添加 Spring Scheduling
@EnableScheduling  // 在配置类上启用
```

```java
// @ScheduledTask 改回 @Scheduled
@Scheduled(cron = "0 0 2 * * ?")
public void myTask() {
    // 业务逻辑不变
}
```

### XXL-Job → 原生 @XxlJob

```gradle
// 移除 TSS 依赖
// implementation project(':ingot-framework:ingot-tss-xxljob')

// 添加 XXL-Job 原生依赖
implementation "com.xuxueli:xxl-job-core:${xxl_job_version}"
```

```java
// @ScheduledTask 改回 @XxlJob
@XxlJob("myTaskHandler")
public void myTask() {
    String param = XxlJobHelper.getJobParam();
    // 业务逻辑不变
    XxlJobHelper.handleSuccess("完成");
}
```

## 总结

从现有任务调度迁移到 TSS 抽象层：

1. **修改注解** - `@Scheduled` 或 `@XxlJob` → `@ScheduledTask`
2. **修改返回值** - `void` → `TaskResult`
3. **添加配置** - 依赖和配置文件
4. **业务逻辑** - 完全不变

整个迁移过程简单快速，且完全可逆。
