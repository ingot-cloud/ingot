# 租户组织设置与所有者转交

## 资料与权限

`GET /v1/tenant/settings` 读取当前组织资料，`PUT /v1/tenant/settings` 按更新权限修改名称和 Logo。组织从可信当前身份恢复，更新校验乐观锁。Logo 保存对象路径，响应签发访问地址。

## 所有者转交

`POST /v1/tenant/settings/owner-transfer` 接受 `expectedVersion`、`newOwnerMemberId` 和必填 `confirmation`。确认类型为 `LOGIN_PASSWORD`，`secret` 使用字段级 HYBRID 加密。服务先检查转交资格和当前所有者身份，再验证登录密码。缺失、空密码、密码错误或不支持的确认类型均返回 `STEP_UP_FAILED`，数据库与授权缓存保持原状态。

写入时在事务中锁定组织，复查版本、当前所有者及新旧成员资格。目标为本人时仍先验密，成功后直接返回当前版本。实际转交写入新所有者并记录审计，提交后失效授权；普通角色分配保留。拥有者权限见 [租户即时授权](../tenant-live-authorization/SPEC.md)。
