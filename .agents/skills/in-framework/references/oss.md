# OSS

头像、附件、图标等对象字段入库只保存相对路径，响应再签发时效 URL。不要在库里持久化带 host 的预签名链接，也不要在前端自己拼对象地址。

## 写入

1. 客户端可以提交预签名 URL 或 `bucket/objectName`。
2. 入库前规范成 `bucket/objectName`：IAM 写路径用 `IamOssPaths.store(...)`；请求 DTO 字段标 `@OssSaveUrl`（`com.ingot.framework.oss.common.OssSaveUrl`），由 `OssSaveUrlDeserializer` 还原路径。
3. DTO 在 `ingot-commons`、不能依赖 OSS 模块时，在消费模块用 Jackson mixin 挂 `@OssSaveUrl`，参考 `IamOssJacksonConfiguration.SaveAvatar`。
4. 禁止把带 query 的时效链接原样写入数据库。

## 读取

1. 库存值是 `bucket/objectName`（可空）。
2. 响应字段标 `@OssUrl`（`com.ingot.framework.oss.common.OssUrl`），由 `OssUrlSerializer` 调用 `OssService.getObjectURL` 签发时效链接。
3. commons DTO 同样用 mixin 挂 `@OssUrl`，参考 `IamOssJacksonConfiguration.ReadAvatar`。
4. 签发失败时序列化器会回退原值；业务代码不要再手写一层 URL 拼接。

## 前端

前端把接口返回的时效 URL 直接交给 `in-avatar` / `in-detail-identity` / `<img>`。上传成功后把服务返回的 `url` 写入表单，提交时原样回传。对象键用业务目录 + 文件名。不要根据相对路径拼 host，也不要把过期链接缓存成长期地址。

## 检查

- 新字段同时覆盖写路径（`store` 或 `@OssSaveUrl`）和读路径（`@OssUrl` 或 mixin）。
- 创建接口如果接受头像，初始化插入必须写入规范化后的路径，不要只在 PATCH 里落库。
- 补一条往返测试：请求预签名 URL → 库存 `bucket/objectName`；响应库存路径 → 时效 URL。
