# OSS 预签名失败回退原始路径

> 状态：completed

## 元数据

- Change ID：`20260908-framework-oss-presign-fallback`
- 领域：`framework`（oss）
- 负责人：jy
- 创建日期：2026-09-08
- 目标发布日期：TBD

## 目标

`OssService.getObjectURL` 在预签名失败或超时时返回库内原始路径，避免 OSS 不可用或网络黑洞拖死业务读接口。MinIO 客户端固定 region 并缩短连接超时，使预签名不再依赖 `GetBucketLocation`。

## 范围

### 包含

- `AbstractS3OssService.getObjectURL` 空值短路与异常回退
- `OssService` 失败语义契约
- MinIO `region`、`connect-timeout` 配置与客户端装配
- 默认 region 抽到 `ingot-oss-common` 常量，RustFS 复用

### 不包含

- 上传 / 下载真实 IO 的 fail-open
- 缩短 MinIO read / write timeout
- 改 `IdentityUtil` 或去掉 `OssUrlSerializer` 的第二层 catch
- 预签名 URL 缓存

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-09-08
- 关联提交或 PR：工作区实施（随代码一并提交）
- 更新的 current capability：`specs/current/framework/oss/`
- 与原设计的差异：无
- 取消原因：—
