# 对象存储访问 URL

> 能力域：`framework` / `oss`

## 摘要

`OssService.getObjectURL` 把库内存储路径转成带过期时间的预签名 URL。签名失败或超时时返回原始路径，业务读接口不因 OSS 不可用而失败。

## 边界

- **含**：路径解析、预签名、失败回退、MinIO region 与连接超时。
- **不含**：对象上传 / 下载的 fail-open；预签名缓存。

## 所有者

- 契约：`ingot-framework/ingot-commons`（`OssService`）
- 实现：`ingot-framework/ingot-oss/ingot-oss-common`、`ingot-oss-minio`、`ingot-oss-rustfs`

## 关联模块

- `ingot-oss-common`：`AbstractS3OssService`、`OssDefaults`、`@OssUrl`
- `ingot-oss-minio`：MinIO 客户端与配置
- `ingot-oss-rustfs`：RustFS / AWS SDK 预签名

## 文档索引

- [SPEC](./SPEC.md)：URL 生成契约、降级与 MinIO 超时
- 来源变更：`specs/changes/archive/2026/20260908-framework-oss-presign-fallback/`
