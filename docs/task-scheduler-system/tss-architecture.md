# TSS 任务调度架构设计

## 设计目标

1. **统一接口** - 业务层使用统一的 `@ScheduledTask` 注解或 `IScheduledTask` 接口
2. **易于切换** - 通过配置切换 Spring 或 XXL-Job，业务代码零改动
3. **无缝集成** - Spring 环境默认可用，XXL-Job 提供高级特性
4. **代码复用** - 公共逻辑统一管理

## 架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                       业务层 (Business Layer)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ OrderTask    │  │ DataSyncTask │  │ ReportTask   │ ...  │
│  │ @ScheduledTask│  │ IScheduledTask│  │ 原生@XxlJob  │      │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘      │
│         │                 │                                  │
│         └─────────────────┘                                  │
│                           ▼                                  │
├─────────────────────────────────────────────────────────────┤
│                   抽象层 (Abstraction Layer)                 │
│              ┌────────────────────────┐                      │
│              │   TaskScheduler (接口)  │                      │
│              └────────────────────────┘                      │
│                   - registerTask()                           │
│                   - unregisterTask()                         │
│                   - getAllTasks()                            │
│              ┌────────────────────────┐                      │
│              │  TaskManagement (接口)  │                      │
│              └────────────────────────┘                      │
│                   - triggerTask()                            │
│                   - pauseTask()                              │
│                   - updateTaskCron()                         │
│              ┌────────────────────────┐                      │
│              │   TaskRegistry (接口)   │                      │
│              └────────────────────────┘                      │
│                   - register()                               │
│                   - getHandler()                             │
├─────────────────────────────────────────────────────────────┤
│                  实现层 (Implementation Layer)               │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │ SpringTaskScheduler  │      │ XxlJobTaskScheduler  │    │
│  │ (Spring实现)         │      │ (XXL-Job实现)        │    │
│  └──────────┬───────────┘      └──────────┬───────────┘    │
│             │                              │                │
│             ▼                              ▼                │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │ ThreadPoolTask       │      │ 动态注册独立Handler   │    │
│  │ Scheduler            │      │ (每个任务一个Handler) │    │
│  └──────────────────────┘      └──────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 核心类图

```
                    ┌─────────────────┐
                    │  «interface»    │
                    │  TaskScheduler  │
                    ├─────────────────┤
                    │ + registerTask()│
                    │ + unregisterTask│
                    └────────▲────────┘
                             │
                  ┌──────────┴──────────┐
                  │                     │
        ┌─────────┴────────┐  ┌────────┴─────────┐
        │SpringTaskScheduler│  │XxlJobTaskScheduler│
        └──────────────────┘  └──────────────────┘
                  │                     │
                  │依赖                 │依赖
                  ▼                     ▼
        ┌──────────────────┐  ┌──────────────────┐
        │   TaskRegistry   │  │   TaskRegistry   │
        │ ┌──────────────┐ │  │ ┌──────────────┐ │
        │ │ TaskHandler  │ │  │ │ TaskHandler  │ │
        │ │ TaskHandler  │ │  │ │ TaskHandler  │ │
        │ └──────────────┘ │  │ └──────────────┘ │
        └──────────────────┘  └──────────────────┘
```

## 模块依赖关系

```
ingot-tss-common（抽象层）
    ├── @ScheduledTask 注解
    ├── IScheduledTask 接口
    ├── TaskScheduler 接口
    ├── TaskManagement 接口
    ├── TaskRegistry 接口
    ├── TaskContext（上下文）
    └── TaskResult（结果）

ingot-tss-spring（Spring实现）
    ├── 依赖: ingot-tss-common
    ├── 依赖: ingot-core
    ├── SpringTaskScheduler
    ├── SpringTaskManagement
    ├── ScheduledTaskAnnotationProcessor
    └── MethodInvokerTaskHandler

ingot-tss-xxljob（XXL-Job实现）
    ├── 依赖: ingot-tss-common
    ├── 依赖: ingot-tss-spring（复用处理器）
    ├── 依赖: ingot-core
    ├── 依赖: xxl-job-core
    ├── XxlJobTaskScheduler（动态注册独立Handler）
    ├── XxlJobTaskManagement
    ├── ScheduledTaskJobHandler（任务适配器）
    └── XxlJobTaskRegistrationProcessor

业务服务
    ├── 依赖: ingot-tss-spring 或 ingot-tss-xxljob (二选一)
    └── 使用: @ScheduledTask 注解或 IScheduledTask 接口
```

## 设计模式

### 1. 策略模式 (Strategy Pattern)

`TaskScheduler` 接口定义调度策略，不同实现提供不同的调度方式：

```java
// 策略接口
public interface TaskScheduler {
    void registerTask(TaskDefinition taskDefinition);
}

// 具体策略 A - Spring
public class SpringTaskScheduler implements TaskScheduler { }

// 具体策略 B - XXL-Job
public class XxlJobTaskScheduler implements TaskScheduler { }

// 使用策略
@Service
public class FileService {
    private final TaskScheduler taskScheduler;  // 具体使用哪个由配置决定
}
```

### 2. 注册中心模式

`TaskRegistry` 统一管理所有任务处理器：

```java
// 注册中心
public interface TaskRegistry {
    void register(String taskName, TaskHandler handler);
    TaskHandler getHandler(String taskName);
}

// 使用
@ScheduledTask(name = "task1")
public TaskResult task1(TaskContext context) { }

// 框架自动注册
taskRegistry.register("task1", new MethodInvokerTaskHandler(bean, method));
```

### 3. 适配器模式

将不同的调度框架（Spring、XXL-Job）适配到统一的接口：

```java
// 目标接口
public interface TaskScheduler {
    void registerTask(TaskDefinition taskDefinition);
}

// 适配器 A - 适配 Spring
public class SpringTaskScheduler implements TaskScheduler {
    private org.springframework.scheduling.TaskScheduler springScheduler;
    
    @Override
    public void registerTask(TaskDefinition taskDefinition) {
        // 调用 Spring 的 API
        springScheduler.schedule(...);
    }
}

// 适配器 B - 适配 XXL-Job
public class XxlJobTaskScheduler implements TaskScheduler {
    private TaskRegistry taskRegistry;
    
    @Override
    public void registerTask(TaskDefinition taskDefinition) {
        // 注册到 TaskRegistry，通过统一 Handler 路由
        taskRegistry.register(taskName, handler);
    }
}
```

## 两种实现对比

### Spring 实现

**优势：**
- ✅ 无需外部中间件
- ✅ 轻量级，启动快
- ✅ 支持动态修改 Cron
- ✅ 配置简单

**劣势：**
- ❌ 不支持分片执行
- ❌ 无管理界面
- ❌ 执行历史简单（内存存储）
- ❌ 不支持失败重试

**适用场景：** 简单的定时任务，单机或简单集群环境

### XXL-Job 实现

**优势：**
- ✅ 完整的管理界面
- ✅ 支持分片执行
- ✅ 完整的执行历史
- ✅ 失败重试和告警
- ✅ 任务依赖
- ✅ 多种路由策略
- ✅ GLUE 模式

**劣势：**
- ❌ 需要部署 XXL-Job Admin
- ❌ 需要数据库存储任务信息
- ❌ 配置相对复杂

**适用场景：** 复杂调度场景，需要精细化管理和监控

## XXL-Job 集成方式说明

### 方式1：使用 @ScheduledTask 注解（推荐）

业务代码使用抽象层注解，框架自动为每个任务创建独立的 XXL-Job Handler：

```java
// 业务代码
@ScheduledTask(name = "order-cleanup-task")
public TaskResult cleanup(TaskContext context) {
    String params = context.getParams();  // 从 Admin 的"任务参数"获取
    // ...
}
```

在 XXL-Job Admin 中配置：
- JobHandler: `order-cleanup-task`（任务名称，与代码中 name 一致）
- 任务参数: `{"days": 30}`（可选的业务参数）
- Cron: `0 0 2 * * ?`
- 运行模式: BEAN

**特点：**
- ✅ 每个任务独立 Handler，灵活性高
- ✅ 参数通过 Admin 的"任务参数"字段传递
- ✅ 可以无缝切换到 Spring 实现

### 方式2：使用原生 @XxlJob 注解

直接使用 XXL-Job 原生注解，不经过抽象层：

```java
@XxlJob("reportHandler")
public void generateReport() {
    String param = XxlJobHelper.getJobParam();  // 使用 XXL-Job 原生 API
    // ...
}
```

在 XXL-Job Admin 中配置：
- JobHandler: `reportHandler`（与注解中的名称一致）
- 任务参数: 业务参数
- Cron: `0 0 1 * * ?`
- 运行模式: BEAN

**特点：**
- ✅ 使用 XXL-Job 原生 API
- ❌ 无法切换到 Spring 实现

### 方式3：GLUE 模式

在 Admin 后台直接编写代码，不需要在项目中定义。

## 任务执行流程

### Spring 流程

```
应用启动
    ↓
扫描 @ScheduledTask 注解
    ↓
注册到 TaskRegistry
    ↓
注册到 SpringTaskScheduler
    ↓
Spring ThreadPoolTaskScheduler 按 Cron 调度
    ↓
执行 TaskHandler
    ↓
记录执行历史（内存）
```

### XXL-Job 流程

```
应用启动
    ↓
扫描 @ScheduledTask 注解
    ↓
注册到 TaskRegistry
    ↓
为每个任务动态注册独立的 XXL-Job Handler
（通过反射访问 jobHandlerRepository）
    ↓
XXL-Job Admin 配置任务
（JobHandler 填写任务名称）
    ↓
XXL-Job Admin 按 Cron 触发
    ↓
调用对应任务的 Handler
    ↓
执行 TaskHandler
    ↓
返回结果给 XXL-Job Admin
```

## 配置优先级

当多个 TSS 实现同时存在时：

1. 如果只有 ingot-tss-spring → 使用 Spring 实现
2. 如果只有 ingot-tss-xxljob → 使用 XXL-Job 实现
3. 如果两者都有 → 通过 `ingot.tss.type` 配置决定

**推荐做法：** 业务模块只引入一个 TSS 实现依赖。

## 性能考虑

### Spring 实现

- 线程池大小可配置（默认10）
- 执行历史保留最多1000条
- 内存占用小

### XXL-Job 实现

- 执行器线程池由 XXL-Job 管理
- 执行历史存储在数据库
- 支持海量任务调度

## 安全考虑

### 1. 任务访问控制

Spring 实现：
- 通过 Spring Security 控制管理接口访问

XXL-Job 实现：
- 使用 Admin 的权限管理
- 配置 accessToken

### 2. 参数验证

```java
@ScheduledTask(name = "sensitive-task")
public TaskResult sensitiveTask(TaskContext context) {
    // 验证参数
    if (!validate(context.getParams())) {
        return TaskResult.failure("参数验证失败");
    }
    
    // 执行业务逻辑
    return TaskResult.success("完成");
}
```

## 扩展新的调度器

要支持新的调度器（如 Quartz、Elastic-Job），只需：

### 步骤1：创建新模块

```
ingot-framework/
  └── ingot-tss-quartz/
      ├── build.gradle
      └── src/main/java/com/ingot/framework/tss/quartz/
          ├── scheduler/
          │   └── QuartzTaskScheduler.java   (实现 TaskScheduler)
          ├── management/
          │   └── QuartzTaskManagement.java  (实现 TaskManagement)
          └── config/
              └── QuartzTaskAutoConfiguration.java
```

### 步骤2：实现接口

```java
public class QuartzTaskScheduler implements TaskScheduler {
    private Scheduler quartzScheduler;
    
    @Override
    public void registerTask(TaskDefinition taskDefinition) {
        // 调用 Quartz API
        JobDetail jobDetail = JobBuilder.newJob(...)
            .withIdentity(taskDefinition.getTaskName())
            .build();
        
        CronTrigger trigger = TriggerBuilder.newTrigger()
            .withSchedule(CronScheduleBuilder.cronSchedule(taskDefinition.getCron()))
            .build();
        
        quartzScheduler.scheduleJob(jobDetail, trigger);
    }
}
```

### 步骤3：自动配置

```java
@AutoConfiguration
@ConditionalOnProperty(name = "ingot.tss.type", havingValue = "quartz")
public class QuartzTaskAutoConfiguration {
    
    @Bean
    public TaskScheduler quartzTaskScheduler(...) {
        return new QuartzTaskScheduler(...);
    }
}
```

**完成！** 业务代码无需修改。

## 总结

这个架构实现了：

1. ✅ **高内聚低耦合** - 业务层与调度实现完全解耦
2. ✅ **开闭原则** - 对扩展开放，对修改关闭
3. ✅ **依赖倒置** - 依赖抽象（接口）而非具体实现
4. ✅ **单一职责** - 每个类只负责一个功能
5. ✅ **里氏替换** - 所有 TaskScheduler 实现可以互相替换

通过这种设计，可以轻松支持任何任务调度框架，且业务代码完全无感知。
