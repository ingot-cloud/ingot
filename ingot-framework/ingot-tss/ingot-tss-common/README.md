# Ingot TSS Common

## 概述

`ingot-tss-common` 是任务调度服务（Task Scheduling Service）的公共抽象层，提供了统一的接口和基础实现，支持 Spring 原生调度和 XXL-Job 两种实现。

## 核心组件

### 1. 注解

#### @ScheduledTask
定时任务注解，支持 Spring 和 XXL-Job 两种调度器。

```java
@Service
public class OrderTask {
    
    @ScheduledTask(
        name = "order-cleanup-task",     // 任务唯一标识
        description = "订单清理任务",
        cron = "0 0 2 * * ?",             // Cron 表达式
        group = "order"                   // 任务分组
    )
    public TaskResult cleanup(TaskContext context) {
        // 业务逻辑...
        return TaskResult.success("清理完成");
    }
}
```

### 2. 接口

#### IScheduledTask
接口方式定义任务。

```java
@Component
public class DataSyncTask implements IScheduledTask {
    
    @Override
    public TaskResult execute(TaskContext context) {
        // 业务逻辑...
        return TaskResult.success("同步完成");
    }
    
    @Override
    public TaskConfig getConfig() {
        return TaskConfig.builder()
            .name("data-sync-task")
            .cron("0 */10 * * * ?")
            .enabled(true)
            .build();
    }
}
```

### 3. 核心接口

#### TaskScheduler
任务调度器接口，由 Spring 和 XXL-Job 实现。

```java
public interface TaskScheduler {
    void registerTask(TaskDefinition taskDefinition);
    void unregisterTask(String taskName);
    List<TaskDefinition> getAllTasks();
    SchedulerType getSchedulerType();
}
```

#### TaskManagement
任务管理接口，提供任务控制和查询功能。

```java
public interface TaskManagement {
    TaskResult triggerTask(String taskName, String params);
    boolean pauseTask(String taskName);
    boolean resumeTask(String taskName);
    boolean updateTaskCron(String taskName, String cron);
    List<TaskExecutionRecord> getExecutionHistory(String taskName, int page, int size);
    TaskStatusInfo getTaskStatus(String taskName);
}
```

#### TaskRegistry
任务注册中心，统一管理任务处理器。

```java
public interface TaskRegistry {
    void register(String taskName, TaskHandler handler);
    TaskHandler getHandler(String taskName);
    Set<String> getAllTaskNames();
}
```

### 4. 数据模型

#### TaskContext
任务执行上下文，携带任务参数和分片信息。

```java
TaskContext context = TaskContext.builder()
    .taskName("order-cleanup-task")
    .params("{\"days\": 30}")
    .shardIndex(0)
    .shardTotal(5)
    .build();
```

#### TaskResult
任务执行结果。

```java
// 成功
TaskResult.success("处理完成");
TaskResult.success("处理完成", data);

// 失败
TaskResult.failure("处理失败");
TaskResult.failure("处理失败", exception);
```

## 依赖关系

```
ingot-tss-common (抽象层)
    ├── ingot-tss-spring (Spring 实现)
    └── ingot-tss-xxljob (XXL-Job 实现)
```

业务服务只需依赖其中一个实现即可：

```gradle
dependencies {
    // 使用 Spring 原生调度
    implementation project(':ingot-framework:ingot-tss-spring')
    
    // 或使用 XXL-Job
    // implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

## 使用场景

### 1. 简单定时任务

使用 Spring 实现，无需额外中间件：

```java
@ScheduledTask(
    name = "simple-task",
    cron = "0 0 1 * * ?"
)
public TaskResult simpleTask(TaskContext context) {
    // 业务逻辑
    return TaskResult.success("完成");
}
```

配置：
```yaml
ingot:
  tss:
    type: spring
```

### 2. 复杂调度场景

使用 XXL-Job 实现，支持分片、失败重试、任务依赖等高级特性：

```java
@ScheduledTask(name = "complex-task")  // Cron 在 XXL-Job Admin 配置
public TaskResult complexTask(TaskContext context) {
    // 支持分片
    int shardIndex = context.getShardIndex();
    int shardTotal = context.getShardTotal();
    
    // 业务逻辑
    return TaskResult.success("分片 " + shardIndex + " 完成");
}
```

配置：
```yaml
ingot:
  tss:
    type: xxljob
    xxljob:
      admin:
        addresses: http://localhost:8080/xxl-job-admin
```

## 特性对比

| 特性 | Spring | XXL-Job |
|------|--------|---------|
| 无需中间件 | ✅ | ❌ |
| 动态 Cron | ✅ | ✅ |
| 分片执行 | ❌ | ✅ |
| 执行历史 | 简单 | 完整 |
| 失败重试 | ❌ | ✅ |
| 任务依赖 | ❌ | ✅ |

## 设计模式

1. **策略模式**：TaskScheduler 接口，不同实现提供不同策略
2. **注册中心模式**：TaskRegistry 统一管理任务处理器
3. **模板方法模式**：抽象公共逻辑，子类实现具体细节

## 扩展性

如需支持新的调度器（如 Quartz），只需：

1. 实现 `TaskScheduler` 接口
2. 实现 `TaskManagement` 接口
3. 处理 `@ScheduledTask` 注解
4. 注册到 `TaskRegistry`

## 相关文档

- [Spring 实现文档](../../ingot-tss-spring/README.md)
- [XXL-Job 实现文档](../../ingot-tss-xxljob/README.md)
- [使用示例](../../../docs/tss-usage-example.md)
- [架构设计](../../../docs/tss-architecture.md)
