# 对象存储访问 URL SPEC

> 记录当前已验收并在线生效的框架事实。本能力只覆盖访问 URL 生成，不描述完整 OSS 模块。

## 1. 契约

`OssService.getObjectURL(url)` / `getObjectURL(url, expiredSeconds)`：

- 入参兼容全路径与 `bucket/objectName`。
- `null` 或空串原样返回，不调用存储客户端。
- 成功返回带过期时间的预签名 URL。
- 路径解析失败、SDK 异常、连接超时：记录 warn，返回入参原值。不向调用方抛异常。

实现落点：`AbstractS3OssService`。`@OssUrl` 序列化与业务显式调用共享该契约。

`uploadFile` / `getFile` 的对象读写失败仍抛异常。上传对象成功后若预签名失败，结果 URL 为 `bucket/fileName`。

## 2. MinIO 预签名

`MinioClient` 必须设置 `region`（默认 `OssDefaults.DEFAULT_REGION`）。预签名使用本地 HMAC，不调用 `GetBucketLocation`。

连接超时默认 `OssDefaults.DEFAULT_CONNECT_TIMEOUT`（3s）。read / write timeout 保持 `OssDefaults.DEFAULT_TRANSFER_TIMEOUT`（5 分钟）。

| YAML | 含义 |
|---|---|
| `ingot.oss.minio.region` | 签名区域 |
| `ingot.oss.minio.connect-timeout` | TCP 连接超时，例如 `3s` |

## 3. RustFS 预签名

`S3Presigner.presignGetObject` 为本地签名。`ingot.oss.rustfs.region` 默认同 `OssDefaults.DEFAULT_REGION`。失败回退仍由 `AbstractS3OssService` 承担。

## 4. 常量

跨实现默认值集中在 `com.ingot.framework.oss.common.OssDefaults`，禁止在调用点复制 region / 超时字面量。
