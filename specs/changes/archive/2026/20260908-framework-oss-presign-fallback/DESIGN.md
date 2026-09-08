# Design

## 方案摘要

降级落在 `AbstractS3OssService.getObjectURL`，所有调用方（含 `OssUrlSerializer`、`IdentityUtil`、`uploadFile` 回写 URL）共享同一契约。MinIO 客户端补充 region 与连接超时：前者去掉预签名的远端 region 查询，后者限制黑洞网络的等待上限。

```text
调用方 → getObjectURL
           ├─ 空值 → 原样返回
           ├─ 预签名成功 → 临时 URL
           └─ 解析/SDK/超时 → warn + 原始路径
```

## 数据模型与接口

### `OssService.getObjectURL`

公共契约变更为 fail-soft：不再因预签名失败向调用方抛运行时异常。入参格式仍兼容全路径与 `bucket/objectName`。

### 配置

| YAML | 类型 | 默认 | 含义 |
|---|---|---|---|
| `ingot.oss.minio.region` | `String` | `OssDefaults.DEFAULT_REGION`（`us-east-1`） | 写入 `MinioClient.builder().region` |
| `ingot.oss.minio.connect-timeout` | `Duration` | `OssDefaults.DEFAULT_CONNECT_TIMEOUT`（3s） | 仅连接超时 |

RustFS `ingot.oss.rustfs.region` 字段默认值改为引用同一常量，YAML 字面量不变。

### 新增类型

`com.ingot.framework.oss.common.OssDefaults`：默认 region、连接超时、传输超时（5 分钟，对应 MinIO SDK 默认 read/write，供 `setTimeout` 使用）。

## 数据流与失败处理

1. `getObjectURL`：空值短路 → `OssPathParser.parse` → `S3Client.getPresignedObjectUrl`。任一异常记录 warn 并返回原值。
2. MinIO 启动：`region` 非空；`setTimeout(connect, DEFAULT_TRANSFER_TIMEOUT, DEFAULT_TRANSFER_TIMEOUT)`。
3. `OssUrlSerializer` 保留 catch，作为第二层兜底，不再是唯一降级点。
4. `uploadFile` / `getFile` 的 `putObject` / `getObject` 失败路径不变。

## 迁移与回滚

- 无数据迁移。新增配置项均有默认值，现有 YAML 可不改。
- 回滚：恢复旧 `getObjectURL`（失败抛异常）与未设 region 的 MinIO 客户端。

## 测试策略

- 单元测试：stub `S3Client`，覆盖成功、空值、非法路径、SDK 异常。
- 不在本变更做真实 MinIO 集成测试。
