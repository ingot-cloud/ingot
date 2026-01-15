# TSS 任务调度快速开始

## 5 分钟快速上手

### 1. 添加依赖

在你的服务模块 `build.gradle` 中添加依赖（二选一）：

```gradle
dependencies {
    // 使用 Spring 原生调度（推荐新手）
    implementation project(':ingot-framework:ingot-tss-spring')
    
    // 或使用 XXL-Job（需要部署 Admin）
    // implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

### 2. 添加配置（可选）

**Spring（默认，可不配置）：**
```yaml
ingot:
  tss:
    type: spring
```

**XXL-Job：**
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

### 3. 编写任务代码

```java
package com.your.package.task;

import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyFirstTask {
    
    @ScheduledTask(
        name = "hello-world-task",
        description = "我的第一个定时任务",
        cron = "0 */1 * * * ?",  // 每分钟执行一次
        group = "demo"
    )
    public TaskResult execute(TaskContext context) {
        log.info("Hello, Task Scheduling!");
        return TaskResult.success("执行完成");
    }
}
```

### 4. 启动应用

```bash
./gradlew :your-service:bootRun
```

**Spring 环境：** 任务会自动按 Cron 执行

**XXL-Job 环境：** 需要在 Admin 中配置任务
- JobHandler: `hello-world-task`（任务名称）
- 任务参数: 可选参数（如需传参）
- Cron: `0 */1 * * * ?`
- 运行模式: BEAN

### 5. 查看日志

```
[task-1] INFO  - 注册 @ScheduledTask: hello-world-task, cron: 0 */1 * * * ?
[task-1] INFO  - Hello, Task Scheduling!
[task-1] INFO  - 任务执行完成: hello-world-task, 耗时: 2ms, 结果: 执行完成
```

## 完成！

现在你已经成功创建了第一个定时任务！

## 进阶使用

### 1. 带参数的任务

```java
@ScheduledTask(name = "cleanup-task")
public TaskResult cleanup(TaskContext context) {
    // 从上下文获取参数
    String params = context.getParams();
    
    // 业务逻辑...
    return TaskResult.success("清理完成");
}
```

**传递参数方式：**

**代码触发：**
```java
taskManagement.triggerTask("cleanup-task", "{\"days\": 30}");
```

**XXL-Job Admin 配置：**
- JobHandler: `cleanup-task`
- 任务参数: `{"days": 30}`（在这里填写参数）

### 2. 使用接口方式

```java
@Component
public class MyTask implements IScheduledTask {
    
    @Override
    public TaskResult execute(TaskContext context) {
        // 任务逻辑
        return TaskResult.success("完成");
    }
    
    @Override
    public TaskConfig getConfig() {
        return TaskConfig.builder()
            .name("my-task")
            .cron("0 0 1 * * ?")
            .enabled(true)
            .build();
    }
}
```

### 3. 任务管理

```java
@Autowired
private TaskManagement taskManagement;

// 手动触发
taskManagement.triggerTask("my-task", null);

// 暂停任务
taskManagement.pauseTask("my-task");

// 恢复任务
taskManagement.resumeTask("my-task");

// 修改执行时间
taskManagement.updateTaskCron("my-task", "0 0 2 * * ?");

// 查询执行历史
List<TaskExecutionRecord> history = 
    taskManagement.getExecutionHistory("my-task", 1, 10);
```

## 切换调度器

从 Spring 切换到 XXL-Job，或反之：

**步骤1：** 修改 `build.gradle`
```gradle
dependencies {
    // implementation project(':ingot-framework:ingot-tss-spring')
    implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

**步骤2：** 修改 `application.yml`
```yaml
ingot:
  tss:
    type: xxljob
    xxljob:
      admin:
        addresses: http://localhost:8080/xxl-job-admin
```

**步骤3：** 在 XXL-Job Admin 中配置任务
- JobHandler: 填写任务名称（与代码中 @ScheduledTask 的 name 一致）
- 任务参数: 填写业务参数（可选）
- Cron: 配置执行时间
- 运行模式: BEAN

**业务代码完全不用改！** ✅

## 常用 Cron 表达式

```
每分钟：    0 */1 * * * ?
每5分钟：   0 */5 * * * ?
每小时：    0 0 * * * ?
每天1点：   0 0 1 * * ?
每周一9点： 0 0 9 ? * MON
每月1号：   0 0 0 1 * ?
```

## 更多资源

- [完整架构设计](../../../docs/tss-architecture.md)
- [详细使用示例](../../../docs/tss-usage-example.md)
- [Spring 实现文档](../../ingot-tss-spring/README.md)
- [XXL-Job 实现文档](../../ingot-tss-xxljob/README.md)

## 获取帮助

如有问题，请查看：
- [架构文档](../../../docs/tss-architecture.md)
- 项目 README
- 或联系开发团队
