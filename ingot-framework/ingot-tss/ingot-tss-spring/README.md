# Ingot TSS Spring

## 概述

`ingot-tss-spring` 是基于 Spring 原生调度器实现的任务调度服务，无需依赖任何外部中间件，适合简单的定时任务场景。

## 特性

- ✅ 无需外部中间件
- ✅ 使用 Spring 的 `TaskScheduler`
- ✅ 支持动态 Cron 修改
- ✅ 支持手动触发任务
- ✅ 内存存储执行历史
- ✅ 支持配置文件占位符
- ✅ 轻量级，易于使用

## 依赖

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-tss-spring')
}
```

## 配置

```yaml
ingot:
  tss:
    type: spring  # 使用 Spring 调度器（默认值，可不配）
    spring:
      threadPool:
        size: 10  # 线程池大小
        threadNamePrefix: "tss-task-"
        awaitTerminationMillis: 60
        waitForTasksToCompleteOnShutdown: true
```

## 使用示例

### 1. 注解方式

```java
@Service
public class OrderTask {
    
    @ScheduledTask(
        name = "order-cleanup-task",
        description = "订单清理任务",
        cron = "0 0 2 * * ?",  // 每天凌晨2点执行
        group = "order"
    )
    public TaskResult cleanupOrders(TaskContext context) {
        log.info("开始清理过期订单");
        int count = orderService.deleteExpiredOrders();
        return TaskResult.success("清理完成，共 " + count + " 条");
    }
    
    @ScheduledTask(
        name = "order-sync-task",
        description = "订单同步任务",
        cron = "${task.order.sync.cron:0 */10 * * * ?}",  // 支持配置文件占位符
        group = "order"
    )
    public TaskResult syncOrders(TaskContext context) {
        // 从上下文获取参数
        String params = context.getParams();
        
        // 业务逻辑...
        return TaskResult.success("同步完成");
    }
}
```

### 2. 接口方式

```java
@Component
public class DataSyncTask implements IScheduledTask {
    
    @Autowired
    private DataService dataService;
    
    @Override
    public TaskResult execute(TaskContext context) {
        log.info("开始数据同步");
        dataService.sync();
        return TaskResult.success("同步完成");
    }
    
    @Override
    public TaskConfig getConfig() {
        return TaskConfig.builder()
            .name("data-sync-task")
            .description("数据同步任务")
            .cron("0 */10 * * * ?")  // 每10分钟执行一次
            .enabled(true)
            .group("data")
            .build();
    }
}
```

### 3. 任务管理

```java
@Service
@RequiredArgsConstructor
public class TaskManagementService {
    private final TaskManagement taskManagement;
    
    /**
     * 手动触发任务
     */
    public TaskResult trigger(String taskName, String params) {
        return taskManagement.triggerTask(taskName, params);
    }
    
    /**
     * 更新任务执行时间
     */
    public boolean updateCron(String taskName, String newCron) {
        return taskManagement.updateTaskCron(taskName, newCron);
    }
    
    /**
     * 暂停任务
     */
    public boolean pause(String taskName) {
        return taskManagement.pauseTask(taskName);
    }
    
    /**
     * 恢复任务
     */
    public boolean resume(String taskName) {
        return taskManagement.resumeTask(taskName);
    }
    
    /**
     * 查询执行历史
     */
    public List<TaskExecutionRecord> getHistory(String taskName) {
        return taskManagement.getExecutionHistory(taskName, 1, 50);
    }
}
```

## 配置文件占位符

支持在 Cron 表达式中使用配置文件占位符：

```java
@ScheduledTask(
    name = "my-task",
    cron = "${task.my.cron:0 0 1 * * ?}"  // 从配置文件读取，默认每天1点
)
public TaskResult myTask(TaskContext context) {
    return TaskResult.success("完成");
}
```

配置文件：
```yaml
task:
  my:
    cron: 0 0 3 * * ?  # 修改为每天3点执行
```

## 动态修改 Cron

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskManagement taskManagement;
    
    @PostMapping("/{taskName}/cron")
    public ApiResult<Boolean> updateCron(
            @PathVariable String taskName,
            @RequestParam String cron) {
        boolean success = taskManagement.updateTaskCron(taskName, cron);
        return ApiResult.ok(success);
    }
}
```

## 执行历史

Spring 实现使用内存存储执行历史（最多1000条）：

```java
// 查询执行历史
List<TaskExecutionRecord> history = taskManagement.getExecutionHistory("my-task", 1, 20);

for (TaskExecutionRecord record : history) {
    System.out.println("执行时间: " + record.getExecuteTime());
    System.out.println("耗时: " + record.getDuration() + "ms");
    System.out.println("是否成功: " + record.isSuccess());
    System.out.println("结果: " + record.getMessage());
}
```

## 限制

与 XXL-Job 相比，Spring 实现有以下限制：

- ❌ 不支持分片执行
- ❌ 不支持失败重试
- ❌ 不支持任务依赖
- ❌ 执行历史简单（内存存储）
- ❌ 无管理界面

**适用场景**：简单的定时任务，不需要复杂的调度功能。

## 切换到 XXL-Job

如果后续需要更强大的调度功能，可以轻松切换到 XXL-Job：

**步骤1：修改依赖**
```gradle
dependencies {
    // implementation project(':ingot-framework:ingot-tss-spring')
    implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

**步骤2：修改配置**
```yaml
ingot:
  tss:
    type: xxljob
    xxljob:
      admin:
        addresses: http://localhost:8080/xxl-job-admin
      executor:
        app-name: ${spring.application.name}
```

**步骤3：在 XXL-Job Admin 中配置任务**
- JobHandler: `order-cleanup-task`（与代码中的 name 一致）
- Cron: `0 0 2 * * ?`

**业务代码无需任何修改！**

## 技术实现

- **ThreadPoolTaskScheduler**：Spring 的线程池任务调度器
- **BeanPostProcessor**：扫描和处理 @ScheduledTask 注解
- **ConcurrentHashMap**：线程安全的任务存储
- **ConcurrentLinkedQueue**：线程安全的执行历史队列

## 相关文档

- [Common 抽象层](../ingot-tss-common/README.md)
- [XXL-Job 实现](../../ingot-tss-xxljob/README.md)
- [使用示例](../../../docs/tss-usage-example.md)
- [架构设计](../../../docs/tss-architecture.md)
