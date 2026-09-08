# Requirements

## 用户场景

1. 业务接口通过 `OssService.getObjectURL` 或 `@OssUrl` 把库内路径转成临时访问 URL。OSS 正常时仍返回预签名 URL。
2. OSS 未启动、连接被拒、网络超时或签名失败时，接口按时返回，对应字段仍是库内原始路径。
3. 登录拉用户详情时转换租户头像：预签名失败不得让整次请求 500。
4. 运维可为 MinIO 配置 `ingot.oss.minio.region` 与 `ingot.oss.minio.connect-timeout`；不配则使用默认 region 与 3 秒连接超时。

## 业务规则

1. `getObjectURL` 两个重载语义一致：`null` / 空串原样返回；成功返回临时 URL；解析失败、SDK 异常、超时打 warn 后返回入参原值。
2. 降级只作用于 URL 转换。`uploadFile` / `getFile` 的对象读写失败仍抛异常。
3. `uploadFile` 对象写入成功后若预签名失败，结果 URL 为 `bucket/fileName`，上传本身成功。
4. MinIO 必须设置 region，避免预签名阶段同步调用 `GetBucketLocation`。
5. 只缩短 MinIO **connectTimeout**；read / write timeout 保持 SDK 默认，避免大文件传输被误伤。
6. 默认 region 为 `us-east-1`，与现有 RustFS 缺省一致，禁止调用点裸写该字面量。

## 边界与非目标

- 回退后的原始路径对浏览器可能不可直接访问；业务读接口可用性优先于临时 URL。
- 不改 RustFS 预签名实现（已是本地签名）。
- 不改 `@OssUrl` 注解契约，不改业务调用点。
- 不引入预签名缓存或异步签 URL。

## 验收标准

- [x] OSS 未启动或连接被拒：带 `@OssUrl` 的接口和登录仍返回，头像字段为原始路径
- [x] MinIO 配置 region 后，`getObjectURL` 不再依赖 `GetBucketLocation`
- [x] 连接黑洞时最差约 `connect-timeout`（默认 3s）后回退，而不是数分钟
- [x] 预签名成功时行为不变
- [x] 上传 / 下载不因本次变更缩短 read / write timeout
- [x] `getObjectURL` 空值、解析失败、SDK 异常的单测覆盖回退
