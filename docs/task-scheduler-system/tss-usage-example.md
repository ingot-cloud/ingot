# TSS 任务调度使用示例

## 概述

本文档展示如何在 Ingot Cloud 项目中使用任务调度服务，以及如何在 Spring 和 XXL-Job 之间切换。

## 依赖配置

### 使用 Spring 原生调度

在你的服务模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-tss-spring')
}
```

### 使用 XXL-Job

在你的服务模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

> **注意：** 两者选其一即可，业务代码完全兼容。

## 配置文件

### Spring 配置 (application.yml)

```yaml
ingot:
  tss:
    type: spring  # 使用 Spring 调度器（默认值）
```

### XXL-Job 配置 (application.yml)

```yaml
ingot:
  tss:
    type: xxljob  # 使用 XXL-Job 调度器
    xxljob:
      enabled: true
      admin:
        addresses: http://localhost:8080/xxl-job-admin
        access-token: default_token
      executor:
        app-name: ${spring.application.name}
        port: 0  # 0 表示自动分配端口
        log-path: /data/applogs
        log-retention-days: 30
```

## 基础使用示例

### 1. 简单定时任务

```java
package com.ingot.cloud.pms.task;

import com.ingot.framework.tss.common.annotation.ScheduledTask;
import com.ingot.framework.tss.common.context.TaskContext;
import com.ingot.framework.tss.common.result.TaskResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCleanupTask {
    
    private final OrderService orderService;
    
    /**
     * 订单清理任务
     */
    @ScheduledTask(
        name = "order-cleanup-task",
        description = "清理过期订单",
        cron = "0 0 2 * * ?",  // 每天凌晨2点执行
        group = "order"
    )
    public TaskResult cleanupExpiredOrders(TaskContext context) {
        log.info("开始清理过期订单");
        
        // 执行业务逻辑
        int count = orderService.deleteExpiredOrders();
        
        log.info("清理完成，共清理 {} 条记录", count);
        
        return TaskResult.success("清理完成，共 " + count + " 条记录");
    }
}
```

### 2. 带参数的任务

```java
@Service
@RequiredArgsConstructor
public class DataSyncTask {
    
    private final DataService dataService;
    
    @ScheduledTask(
        name = "data-sync-task",
        description = "数据同步任务",
        cron = "0 */10 * * * ?",  // 每10分钟执行
        group = "data"
    )
    public TaskResult syncData(TaskContext context) {
        // 从上下文获取参数
        String params = context.getParams();
        
        log.info("开始数据同步，参数: {}", params);
        
        // 执行同步
        dataService.sync(params);
        
        return TaskResult.success("同步完成");
    }
}
```

### 3. 配置文件占位符

```java
@Service
public class ReportTask {
    
    @ScheduledTask(
        name = "daily-report-task",
        description = "每日报表生成",
        cron = "${task.report.cron:0 0 1 * * ?}",  // 支持配置文件覆盖
        group = "report"
    )
    public TaskResult generateDailyReport(TaskContext context) {
        // 业务逻辑...
        return TaskResult.success("报表生成完成");
    }
}
```

配置文件：
```yaml
task:
  report:
    cron: 0 0 3 * * ?  # 修改为每天凌晨3点
```

### 4. 实现接口方式

```java
@Component
@RequiredArgsConstructor
public class DatabaseBackupTask implements IScheduledTask {
    
    private final DatabaseService databaseService;
    
    @Override
    public TaskResult execute(TaskContext context) {
        log.info("开始数据库备份");
        
        try {
            databaseService.backup();
            return TaskResult.success("备份完成");
        } catch (Exception e) {
            log.error("备份失败", e);
            return TaskResult.failure("备份失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TaskConfig getConfig() {
        return TaskConfig.builder()
            .name("database-backup-task")
            .description("数据库备份任务")
            .cron("0 0 0 * * ?")  // 每天午夜执行
            .enabled(true)
            .group("maintenance")
            .build();
    }
}
```

## XXL-Job 特有功能

### 1. 分片执行

```java
@Service
public class BigDataProcessTask {
    
    @ScheduledTask(
        name = "big-data-process-task",
        description = "大数据处理任务"
    )
    public TaskResult processBigData(TaskContext context) {
        // 获取分片信息（只有 XXL-Job 支持）
        int shardIndex = context.getShardIndex();  // 当前分片索引（0开始）
        int shardTotal = context.getShardTotal();  // 总分片数
        
        if (context.isSharding()) {
            log.info("执行分片: {}/{}", shardIndex, shardTotal);
            
            // 根据分片处理数据
            List<Data> dataList = dataService.findBySharding(shardIndex, shardTotal);
            
            for (Data data : dataList) {
                // 处理数据...
            }
            
            return TaskResult.success("分片 " + shardIndex + " 处理完成");
        } else {
            log.warn("非分片执行模式");
            return TaskResult.failure("请使用分片执行模式");
        }
    }
}
```

在 XXL-Job Admin 中配置分片：
- JobHandler: `big-data-process-task`（任务名称）
- 任务参数: 可选参数
- 路由策略: **分片广播**
- Cron: `0 0 1 * * ?`

### 2. 使用原生 @XxlJob 注解

```java
@Component
@RequiredArgsConstructor
public class ComplexTask {
    
    private final ServiceA serviceA;
    
    /**
     * 使用 XXL-Job 原生注解
     */
    @XxlJob("complexTaskHandler")
    public void executeComplexTask() {
        // 使用 XXL-Job 原生 API
        String param = XxlJobHelper.getJobParam();
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        
        XxlJobHelper.log("开始执行复杂任务，参数: {}", param);
        
        try {
            // 业务逻辑...
            XxlJobHelper.handleSuccess("执行完成");
        } catch (Exception e) {
            XxlJobHelper.handleFail("执行失败: " + e.getMessage());
        }
    }
}
```

在 XXL-Job Admin 中配置：
- JobHandler: `complexTaskHandler`（与注解一致）
- Cron: `0 */5 * * * ?`

## 任务管理示例

### 1. Controller 接口

```java
package com.ingot.cloud.pms.controller;

import com.ingot.framework.core.model.common.ApiResult;
import com.ingot.framework.tss.common.management.TaskManagement;
import com.ingot.framework.tss.common.model.TaskExecutionRecord;
import com.ingot.framework.tss.common.model.TaskStatusInfo;
import com.ingot.framework.tss.common.result.TaskResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "任务管理")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskManagementController {
    
    private final TaskManagement taskManagement;
    
    @Operation(summary = "手动触发任务")
    @PostMapping("/{taskName}/trigger")
    public ApiResult<TaskResult> triggerTask(
            @PathVariable String taskName,
            @RequestParam(required = false) String params) {
        TaskResult result = taskManagement.triggerTask(taskName, params);
        return ApiResult.ok(result);
    }
    
    @Operation(summary = "暂停任务")
    @PostMapping("/{taskName}/pause")
    public ApiResult<Boolean> pauseTask(@PathVariable String taskName) {
        boolean success = taskManagement.pauseTask(taskName);
        return ApiResult.ok(success);
    }
    
    @Operation(summary = "恢复任务")
    @PostMapping("/{taskName}/resume")
    public ApiResult<Boolean> resumeTask(@PathVariable String taskName) {
        boolean success = taskManagement.resumeTask(taskName);
        return ApiResult.ok(success);
    }
    
    @Operation(summary = "更新 Cron 表达式")
    @PostMapping("/{taskName}/cron")
    public ApiResult<Boolean> updateCron(
            @PathVariable String taskName,
            @RequestParam String cron) {
        boolean success = taskManagement.updateTaskCron(taskName, cron);
        return ApiResult.ok(success);
    }
    
    @Operation(summary = "查询执行历史")
    @GetMapping("/{taskName}/history")
    public ApiResult<List<TaskExecutionRecord>> getHistory(
            @PathVariable String taskName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TaskExecutionRecord> history = 
            taskManagement.getExecutionHistory(taskName, page, size);
        return ApiResult.ok(history);
    }
    
    @Operation(summary = "查询任务状态")
    @GetMapping("/{taskName}/status")
    public ApiResult<TaskStatusInfo> getStatus(@PathVariable String taskName) {
        TaskStatusInfo status = taskManagement.getTaskStatus(taskName);
        return ApiResult.ok(status);
    }
}
```

### 2. Service 层封装

```java
@Service
@RequiredArgsConstructor
public class TaskManagementService {
    
    private final TaskManagement taskManagement;
    
    /**
     * 手动触发任务（业务逻辑）
     */
    public void triggerOrderCleanup() {
        TaskResult result = taskManagement.triggerTask("order-cleanup-task", null);
        
        if (!result.isSuccess()) {
            throw new RuntimeException("触发失败: " + result.getMessage());
        }
    }
    
    /**
     * 根据条件触发数据同步
     */
    public void triggerDataSync(String syncType) {
        String params = String.format("{\"type\": \"%s\"}", syncType);
        taskManagement.triggerTask("data-sync-task", params);
    }
    
    /**
     * 临时调整任务执行时间
     */
    public void adjustTaskSchedule(String taskName, String newCron) {
        boolean success = taskManagement.updateTaskCron(taskName, newCron);
        
        if (!success) {
            throw new RuntimeException("更新失败");
        }
    }
}
```

## 高级用法

### 1. 任务依赖（XXL-Job）

```java
// 主任务
@ScheduledTask(name = "main-task")
public TaskResult mainTask(TaskContext context) {
    // 主任务逻辑
    return TaskResult.success("主任务完成");
}

// 子任务
@ScheduledTask(name = "sub-task")
public TaskResult subTask(TaskContext context) {
    // 子任务逻辑
    return TaskResult.success("子任务完成");
}
```

在 XXL-Job Admin 中配置：
- **主任务**：
  - JobHandler: `main-task`（任务名称）
  - 任务参数: 可选参数
  - 子任务ID: 配置子任务的 ID
  
- **子任务**：
  - JobHandler: `sub-task`（任务名称）
  - 任务参数: 可选参数

主任务执行成功后，会自动触发子任务。

### 2. 失败重试（XXL-Job）

在 XXL-Job Admin 中配置：
- 失败重试次数: 3
- 任务失败后会自动重试

业务代码无需修改。

### 3. 任务编排

```java
@Service
@RequiredArgsConstructor
public class TaskOrchestrationService {
    
    private final TaskManagement taskManagement;
    
    /**
     * 编排多个任务按顺序执行
     */
    public void executeWorkflow() {
        // 步骤1：数据准备
        TaskResult step1 = taskManagement.triggerTask("data-prepare-task", null);
        if (!step1.isSuccess()) {
            throw new RuntimeException("步骤1失败");
        }
        
        // 步骤2：数据处理
        TaskResult step2 = taskManagement.triggerTask("data-process-task", null);
        if (!step2.isSuccess()) {
            throw new RuntimeException("步骤2失败");
        }
        
        // 步骤3：数据清理
        TaskResult step3 = taskManagement.triggerTask("data-cleanup-task", null);
        if (!step3.isSuccess()) {
            throw new RuntimeException("步骤3失败");
        }
        
        log.info("工作流执行完成");
    }
}
```

## 实际场景示例

### 场景1：电商订单清理

```java
@Service
@RequiredArgsConstructor
public class EcommerceOrderTask {
    
    private final OrderService orderService;
    private final NotificationService notificationService;
    
    /**
     * 清理超过30天的已取消订单
     */
    @ScheduledTask(
        name = "cleanup-cancelled-orders",
        description = "清理已取消订单",
        cron = "0 0 3 * * ?",  // 每天凌晨3点
        group = "order"
    )
    public TaskResult cleanupCancelledOrders(TaskContext context) {
        // 获取30天前的时间戳
        long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        
        // 查询并删除
        List<Order> orders = orderService.findCancelledOrdersBefore(thirtyDaysAgo);
        int count = orderService.batchDelete(orders);
        
        // 发送通知
        notificationService.send("订单清理完成，共清理 " + count + " 条");
        
        return TaskResult.success("清理完成", count);
    }
    
    /**
     * 自动确认收货（超过7天未确认）
     */
    @ScheduledTask(
        name = "auto-confirm-receipt",
        description = "自动确认收货",
        cron = "0 0 1 * * ?",  // 每天凌晨1点
        group = "order"
    )
    public TaskResult autoConfirmReceipt(TaskContext context) {
        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        
        List<Order> orders = orderService.findDeliveredOrdersBefore(sevenDaysAgo);
        int count = orderService.batchConfirm(orders);
        
        return TaskResult.success("自动确认完成，共 " + count + " 笔订单");
    }
}
```

### 场景2：数据统计任务（支持分片）

```java
@Service
@RequiredArgsConstructor
public class StatisticsTask {
    
    private final UserService userService;
    private final StatisticsService statisticsService;
    
    /**
     * 用户活跃度统计（支持分片）
     */
    @ScheduledTask(
        name = "user-activity-statistics",
        description = "用户活跃度统计"
    )
    public TaskResult calculateUserActivity(TaskContext context) {
        if (context.isSharding()) {
            // XXL-Job 分片执行
            int shardIndex = context.getShardIndex();
            int shardTotal = context.getShardTotal();
            
            log.info("分片执行: {}/{}", shardIndex, shardTotal);
            
            // 根据分片查询用户
            List<User> users = userService.findBySharding(shardIndex, shardTotal);
            
            // 统计每个用户的活跃度
            for (User user : users) {
                int activityScore = statisticsService.calculate(user.getId());
                user.setActivityScore(activityScore);
                userService.update(user);
            }
            
            return TaskResult.success("分片 " + shardIndex + " 处理完成，共 " + users.size() + " 个用户");
        } else {
            // Spring 执行（处理所有用户）
            List<User> users = userService.findAll();
            
            for (User user : users) {
                int activityScore = statisticsService.calculate(user.getId());
                user.setActivityScore(activityScore);
                userService.update(user);
            }
            
            return TaskResult.success("处理完成，共 " + users.size() + " 个用户");
        }
    }
}
```

在 XXL-Job Admin 中配置分片：
- JobHandler: `user-activity-statistics`（任务名称）
- 任务参数: 可选参数
- 路由策略: **分片广播**
- Cron: `0 0 2 * * ?`

### 场景3：定时提醒

```java
@Service
@RequiredArgsConstructor
public class ReminderTask {
    
    private final MembershipService membershipService;
    private final EmailService emailService;
    
    /**
     * 会员到期提醒
     */
    @ScheduledTask(
        name = "membership-expiry-reminder",
        description = "会员到期提醒",
        cron = "0 0 9 * * ?",  // 每天早上9点
        group = "notification"
    )
    public TaskResult sendExpiryReminder(TaskContext context) {
        // 查询7天内到期的会员
        List<Member> expiringMembers = membershipService.findExpiringWithin(7);
        
        int count = 0;
        for (Member member : expiringMembers) {
            // 发送提醒邮件
            emailService.sendExpiryReminder(member);
            count++;
        }
        
        return TaskResult.success("发送提醒完成，共 " + count + " 人");
    }
}
```

## 任务监控和管理

### 1. 查询任务状态

```java
@Service
@RequiredArgsConstructor
public class TaskMonitoringService {
    
    private final TaskManagement taskManagement;
    
    /**
     * 检查所有任务状态
     */
    public Map<String, TaskStatusInfo> checkAllTasks() {
        List<String> taskNames = Arrays.asList(
            "order-cleanup-task",
            "data-sync-task",
            "report-task"
        );
        
        Map<String, TaskStatusInfo> statusMap = new HashMap<>();
        
        for (String taskName : taskNames) {
            TaskStatusInfo status = taskManagement.getTaskStatus(taskName);
            statusMap.put(taskName, status);
        }
        
        return statusMap;
    }
    
    /**
     * 查询任务执行历史（仅 Spring 实现）
     */
    public void printTaskHistory(String taskName) {
        List<TaskExecutionRecord> history = 
            taskManagement.getExecutionHistory(taskName, 1, 10);
        
        for (TaskExecutionRecord record : history) {
            System.out.printf("[%s] %s - %s - %dms\n",
                new Date(record.getExecuteTime()),
                record.isSuccess() ? "SUCCESS" : "FAILURE",
                record.getMessage(),
                record.getDuration()
            );
        }
    }
}
```

### 2. 动态调整任务

```java
@Service
@RequiredArgsConstructor
public class DynamicTaskAdjustmentService {
    
    private final TaskManagement taskManagement;
    
    /**
     * 根据业务负载调整任务执行频率
     */
    public void adjustByLoad(double load) {
        String cron;
        
        if (load > 0.8) {
            // 高负载：每30分钟执行
            cron = "0 */30 * * * ?";
        } else if (load > 0.5) {
            // 中负载：每15分钟执行
            cron = "0 */15 * * * ?";
        } else {
            // 低负载：每5分钟执行
            cron = "0 */5 * * * ?";
        }
        
        taskManagement.updateTaskCron("data-sync-task", cron);
        log.info("根据负载 {} 调整任务频率为: {}", load, cron);
    }
}
```

## 切换调度器

从 Spring 切换到 XXL-Job，或反之。

### 从 Spring 切换到 XXL-Job

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

为每个 @ScheduledTask 创建任务：
- JobHandler: 填写任务名称（如 `order-cleanup-task`）
- 任务参数: 可选的业务参数（如 `{"days": 30}`）
- Cron: 配置执行时间

**业务代码无需任何修改！**

### 从 XXL-Job 切换到 Spring

**步骤1：修改依赖**
```gradle
dependencies {
    implementation project(':ingot-framework:ingot-tss-spring')
    // implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

**步骤2：修改配置**
```yaml
ingot:
  tss:
    type: spring  # 或直接删除此配置，默认就是 spring
```

**业务代码无需任何修改！**

> **注意：** 如果你使用了 @XxlJob 原生注解，这些任务在 Spring 环境下不会执行。

## 参数传递详解

### 使用 @ScheduledTask 时如何传参

**业务代码：**
```java
@ScheduledTask(name = "order-sync-task")
public TaskResult sync(TaskContext context) {
    String params = context.getParams();  // 从 Admin 的"任务参数"获取
    // ...
}
```

**XXL-Job Admin 配置：**
- JobHandler: `order-sync-task`
- **任务参数**: `{"type": "daily", "scope": "all"}`

**手动触发时传参：**
- 点击"执行一次"
- 在弹出框中输入参数：`{"type": "manual"}`

### 使用原生 @XxlJob 时如何传参

**业务代码：**
```java
@XxlJob("myHandler")
public void execute() {
    String params = XxlJobHelper.getJobParam();  // 使用 XXL-Job API
    // ...
}
```

**XXL-Job Admin 配置：**
- JobHandler: `myHandler`
- 任务参数: `your-params`

## 最佳实践

### 1. 任务命名规范

建议使用小写字母加连字符的格式：

```
order-cleanup-task
data-sync-task
user-activity-statistics-task
```

### 2. 任务分组

按业务模块划分：

```
order    - 订单相关任务
data     - 数据相关任务
report   - 报表相关任务
notification - 通知相关任务
```

### 3. 返回结果

始终返回 TaskResult：

```java
@ScheduledTask(name = "my-task")
public TaskResult myTask(TaskContext context) {
    try {
        // 业务逻辑
        return TaskResult.success("完成");
    } catch (Exception e) {
        log.error("任务执行失败", e);
        return TaskResult.failure("失败: " + e.getMessage(), e);
    }
}
```

### 4. 参数传递

使用 JSON 格式传递复杂参数：

**方式1：通过代码触发**
```java
// 传递参数
String params = "{\"type\": \"daily\", \"scope\": \"all\"}";
taskManagement.triggerTask("my-task", params);
```

**方式2：通过 XXL-Job Admin**
- 在任务配置中的"任务参数"字段填写：`{"type": "daily"}`
- 或手动触发时输入参数

**接收参数：**
```java
@ScheduledTask(name = "my-task")
public TaskResult myTask(TaskContext context) {
    String params = context.getParams();  // 获取参数
    JSONObject json = JSONObject.parseObject(params);
    String type = json.getString("type");
    // ...
}
```

## 故障排查

### 1. 任务不执行

**Spring 环境：**
- 检查 Cron 表达式是否正确
- 检查任务是否已注册：查看启动日志
- 检查是否启用：`@ScheduledTask(enabled = true)`

**XXL-Job 环境：**
- 检查 Admin 中任务是否配置
- 检查执行器是否在线
- 检查 JobHandler 是否填写正确（应该是任务名称）
- 检查任务是否已启动（状态为"运行中"）

### 2. 参数传递

**@ScheduledTask 任务：**
- JobHandler：填写任务名称（如：`order-sync-task`）
- 任务参数：填写业务参数（如：`{"type": "daily"}`）

**原生 @XxlJob 任务：**
- JobHandler：填写注解中的名称（如：`myHandler`）
- 任务参数：按 XXL-Job 规范传递

### 3. 分片不生效

确保在 XXL-Job Admin 中选择了**分片广播**路由策略。

## 总结

通过这个抽象层设计，你可以：

1. ✅ 业务代码与调度实现解耦
2. ✅ 轻松在 Spring 和 XXL-Job 之间切换
3. ✅ 统一的接口和使用方式
4. ✅ 支持简单到复杂的各种调度场景
