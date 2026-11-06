# 设计：所有者转交内嵌登录密码

沿用应用强制清除的内嵌确认。`SensitiveConfirmation` 仍只有 `LOGIN_PASSWORD` 会进入账号口令校验，`OPERATION_PASSWORD` 直接失败关闭。

Web 入站使用 `OwnerTransferRequest`，`confirmation.secret` 标 `@InDecryptField`，端点标 `@InCryptoHybridContext`。commons 的 `OwnerTransferInput` 只接收解密后的明文，不依赖 crypto。

`TenantQueryService.transferOwner`：

1. `access.require` 转交权限。
2. 读取当前组织，调用者成员 ID 不是所有者则 `ACTION_DENIED`。
3. `confirmations.require`。
4. 事务内再锁行、核对版本和所有者，然后转交。同一人时在密码通过后返回当前版本，不写审计。
