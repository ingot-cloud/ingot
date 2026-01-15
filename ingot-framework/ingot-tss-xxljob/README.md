# Ingot TSS XXL-Job

## 概述

`ingot-tss-xxljob` 是基于 XXL-Job 实现的任务调度服务，支持分片执行、失败重试、任务依赖等高级特性，适合复杂的调度场景。

## 特性

- ✅ 基于 XXL-Job 3.x
- ✅ 支持混合模式（@ScheduledTask + 原生 @XxlJob）
- ✅ 每个任务独立 Handler
- ✅ 支持分片执行
- ✅ 完整的管理界面
- ✅ 执行历史和日志
- ✅ 失败重试和告警
- ✅ 任务依赖和路由策略
- ✅ GLUE 模式支持

## 依赖

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-tss-xxljob')
}
```

## 配置

```yaml
ingot:
  tss:
    type: xxljob  # 使用 XXL-Job 调度器
    xxljob:
      enabled: true
      admin:
        addresses: http://localhost:8080/xxl-job-admin
        access-token: your-access-token
      executor:
        app-name: ${spring.application.name}
        address:   # 执行器地址（可选，自动注册）
        ip:        # 执行器IP（可选）
        port: 9999 # 执行器端口
        log-path: /ingot-data/xxl-job
        log-retention-days: 30
```

## 重要说明

**每个 @ScheduledTask 任务会自动注册为独立的 XXL-Job Handler**

- Handler 名称 = 任务名称（@ScheduledTask 的 name 属性）
- 在 XXL-Job Admin 中，JobHandler 直接填写任务名称
- 参数通过 Admin 的"任务参数"功能传递

## 使用方式

### 方式1：使用 @ScheduledTask 注解（推荐）

业务代码使用抽象层注解，框架自动为每个任务创建独立的 XXL-Job Handler：

```java
@Service
public class OrderTask {
    
    @ScheduledTask(
        name = "order-cleanup-task",      // 任务名称 = Handler 名称
        description = "订单清理任务"
    )
    public TaskResult cleanupOrders(TaskContext context) {
        // 支持分片执行
        int shardIndex = context.getShardIndex();
        int shardTotal = context.getShardTotal();
        
        log.info("执行分片: {}/{}", shardIndex, shardTotal);
        
        // 业务逻辑...
        List<Order> orders = orderService.findBySharding(shardIndex, shardTotal);
        int count = orderService.cleanup(orders);
        
        return TaskResult.success("清理完成，共 " + count + " 条");
    }
    
    @ScheduledTask(
        name = "order-sync-task",
        description = "订单同步任务"
    )
    public TaskResult syncOrders(TaskContext context) {
        // 从上下文获取参数（在 Admin 后台配置或手动触发时传入）
        String params = context.getParams();
        
        // 业务逻辑...
        return TaskResult.success("同步完成");
    }
}
```

**在 XXL-Job Admin 中配置：**

**任务1：订单清理**
- 执行器：选择你的应用名称（如：pms-service）
- **JobHandler：`order-cleanup-task`**（与代码中的 name 一致）
- **任务参数：**（可选，如果需要传参数）
- Cron：`0 0 2 * * ?`（每天凌晨2点）
- 运行模式：BEAN
- 路由策略：第一个（单机）或分片广播（分片）

**任务2：订单同步**
- 执行器：选择你的应用名称
- **JobHandler：`order-sync-task`**（与代码中的 name 一致）
- **任务参数：`{"type": "daily"}`**（可选参数，JSON 格式）
- Cron：`0 */10 * * * ?`（每10分钟）
- 运行模式：BEAN

> ⚠️ **关键点：**
> - **JobHandler 填写任务名称**（与代码中 @ScheduledTask 的 name 属性一致）
> - **每个任务都是独立的 Handler**，灵活性最高
> - **参数通过"任务参数"字段传递**，不是放在 JobHandler 里

### 方式2：使用原生 @XxlJob 注解

如果需要使用 XXL-Job 特有功能，可以直接使用原生注解：

```java
@Component
public class ReportTask {
    
    @XxlJob("reportGenerateHandler")
    public void generateReport() {
        // 使用 XXL-Job 原生 API
        String param = XxlJobHelper.getJobParam();
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        
        XxlJobHelper.log("开始生成报表，分片: {}/{}", shardIndex, shardTotal);
        
        try {
            // 业务逻辑...
            XxlJobHelper.handleSuccess("报表生成完成");
        } catch (Exception e) {
            XxlJobHelper.handleFail("报表生成失败: " + e.getMessage());
        }
    }
}
```

在 XXL-Job Admin 中配置：
- JobHandler：`reportGenerateHandler`（原生注解的名称）
- 任务参数：可选参数
- Cron：`0 0 1 * * ?`
- 运行模式：BEAN

> 注意：使用原生 @XxlJob 注解时，JobHandler 填写注解中定义的名称，参数通过 XxlJobHelper.getJobParam() 获取

### 方式3：GLUE 模式

在 XXL-Job Admin 后台直接编写代码，不需要在项目中定义任何 Handler：

1. 在 Admin 中创建任务
2. 运行模式选择：GLUE(Java)
3. 在线编写代码：

```java
import com.xxl.job.core.context.XxlJobHelper;

public class DemoGlueJob {
    public void execute() throws Exception {
        XxlJobHelper.log("GLUE 模式执行");
        
        // 业务逻辑（可以引入项目中的类）
        String param = XxlJobHelper.getJobParam();
        
        XxlJobHelper.handleSuccess("执行完成");
    }
}
```

## 混合使用示例

在同一个项目中可以混合使用三种方式：

```java
@Service
public class TaskService {
    
    // 方式1：使用抽象层（推荐，可切换到Spring）
    @ScheduledTask(name = "task1", description = "任务1")
    public TaskResult task1(TaskContext context) {
        return TaskResult.success("task1 完成");
    }
    
    // 方式2：XXL-Job 原生注解（仅XXL-Job）
    @XxlJob("task2Handler")
    public void task2() {
        XxlJobHelper.handleSuccess("task2 完成");
    }
    
    // 方式3：GLUE模式在Admin后台编写
}
```

在 XXL-Job Admin 中配置：
- `task1` - JobHandler: `task1`（抽象层，独立 Handler）
- `task2Handler` - JobHandler: `task2Handler`（原生注解名）
- `glueTask` - GLUE 模式（后台编写，无需代码）

**参数传递示例：**
```
任务1：
- JobHandler: task1
- 任务参数: {"param1": "value1"}

任务2（原生）：
- JobHandler: task2Handler  
- 任务参数: some-param-value
```

## 高级特性

### 1. 分片执行

```java
@ScheduledTask(name = "big-data-task")
public TaskResult processBigData(TaskContext context) {
    int shardIndex = context.getShardIndex();  // 当前分片索引（0开始）
    int shardTotal = context.getShardTotal();  // 总分片数
    
    // 根据分片处理数据
    List<Data> dataList = dataService.findBySharding(shardIndex, shardTotal);
    
    for (Data data : dataList) {
        // 处理数据...
    }
    
    return TaskResult.success("分片 " + shardIndex + " 处理完成");
}
```

在 Admin 中配置：
- 路由策略：**分片广播**
- 任务会在所有执行器实例上执行，每个实例处理不同分片

### 2. 参数传递

```java
@ScheduledTask(name = "param-task")
public TaskResult processWithParams(TaskContext context) {
    String params = context.getParams();
    
    // 解析参数
    JSONObject json = JSONObject.parseObject(params);
    String type = json.getString("type");
    
    // 业务逻辑...
    return TaskResult.success("处理完成");
}
```

在 Admin 中：
- 配置任务参数：`{"type": "daily"}`
- 或手动触发时传入参数

### 3. 失败重试

在 XXL-Job Admin 中配置：
- 失败重试次数：3
- 任务失败后会自动重试

### 4. 任务依赖

在 XXL-Job Admin 中配置：
- 子任务ID：配置依赖的任务
- 当前任务执行成功后，自动触发子任务

### 5. 路由策略

XXL-Job 支持多种路由策略：
- **第一个**：只在第一个在线执行器执行
- **最后一个**：只在最后一个在线执行器执行
- **轮询**：按顺序轮询执行器
- **随机**：随机选择执行器
- **一致性HASH**：根据参数HASH
- **最不经常使用**：选择使用频率最低的执行器
- **最近最久未使用**：选择最久未使用的执行器
- **故障转移**：失败后转移到其他执行器
- **忙碌转移**：忙碌时转移到其他执行器
- **分片广播**：所有执行器执行，每个处理不同分片

## 切换到 Spring

如果后续想切换回 Spring 原生调度：

**步骤1：修改依赖**
```gradle
dependencies {
    // implementation project(':ingot-framework:ingot-tss-xxljob')
    implementation project(':ingot-framework:ingot-tss-spring')
}
```

**步骤2：修改配置**
```yaml
ingot:
  tss:
    type: spring  # 从 xxljob 改为 spring
```

**业务代码无需修改！**（前提是使用的是 @ScheduledTask 注解）

## 注意事项

1. **Cron 配置位置**：使用 @ScheduledTask 时，Cron 在 XXL-Job Admin 后台配置，注解中的 cron 可以不填
2. **任务标识**：任务名称（@ScheduledTask.name）必须在整个执行器中唯一
3. **Handler 注册**：每个 @ScheduledTask 会注册为一个独立的 JobHandler
4. **原生注解共存**：可以同时使用 @ScheduledTask 和 @XxlJob
5. **GLUE 模式独立**：GLUE 模式在 Admin 后台使用，与代码无关

## XXL-Job Admin 操作指南

### 1. 创建执行器

- 执行器管理 → 新增执行器
- AppName：填写配置中的 `app-name`
- 注册方式：自动注册

### 2. 创建任务

- 任务管理 → 新增
- 执行器：选择上面创建的执行器
- JobHandler：填写代码中的任务名称
- Cron：配置执行时间
- 运行模式：BEAN

### 3. 查看执行日志

- 任务管理 → 操作 → 执行日志
- 可以看到详细的执行记录

### 4. 手动触发

- 任务管理 → 操作 → 执行一次
- 可以传入参数

## 相关文档

- [Common 抽象层](../ingot-tss-common/README.md)
- [Spring 实现](../ingot-tss-spring/README.md)
- [XXL-Job 官方文档](https://www.xuxueli.com/xxl-job/)
- [使用示例](../../docs/tss-usage-example.md)
- [架构设计](../../docs/tss-architecture.md)
