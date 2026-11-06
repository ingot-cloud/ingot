# 所有者转交内嵌登录密码

> 状态：completed

## 元数据

- Change ID：`20261021-tenant-owner-transfer-stepup`
- 领域：`iam`
- 负责人：`jy`
- 创建日期：`2026-10-21`

## 目标

租户转交所有者必须在同一次写请求里校验当前账号的登录密码。缺密码、密码错误或未实现的确认种类都失败关闭，不改所有者。

## 范围

包含：

- `OwnerTransferInput` 增加必填 `confirmation`
- 入站 `secret` 字段级 HYBRID 解密
- 改库前调用 `SensitiveConfirmationGuard`

不包含：

- 单独的验密接口或可复用票据
- 操作密码
- 解散组织

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [接口](./API.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-11-06（用户确认产品验收通过并要求归档）。
- 关联实现提交：0ece086c。
- 更新的 current：[iam/tenant-settings](../../../../current/iam/tenant-settings/README.md)。
- 最终交付：同一请求按当前所有者身份验登录密码，目标为本人时同样先验密。
