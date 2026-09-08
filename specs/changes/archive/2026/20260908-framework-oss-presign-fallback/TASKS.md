# Tasks

## 决策

- 降级放在 `AbstractS3OssService`，不改业务调用点。
- 失败 / 超时 / 解析失败一律返回入参原值。
- MinIO 同时设 region 与 connectTimeout；不改 read/write timeout。
- 默认 region 抽到 `OssDefaults`，RustFS 复用。

## 实施任务

- [x] T1：新增 `OssDefaults`；`AbstractS3OssService.getObjectURL` 空值短路 + 异常回退；更新 `OssService` 失败语义
  - 依赖：无
  - 验收：空值、解析失败、SDK 异常返回原值；成功返回预签名 URL
- [x] T2：MinIO `region` / `connect-timeout` 配置与客户端装配；RustFS region 默认改引用常量
  - 依赖：T1
  - 验收：`MinioClient` 带 region；`setTimeout` 仅缩短 connect
- [x] T3：`ingot-oss-common` 单测
  - 依赖：T1
  - 验收：成功 / 空值 / 非法路径 / 异常回退用例通过

## 验证任务

- [x] V1：相关模块编译与单测通过

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
