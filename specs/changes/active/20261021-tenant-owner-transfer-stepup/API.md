# 接口：所有者转交内嵌登录密码

`POST /v1/tenant/settings/owner-transfer`

```json
{
  "expectedVersion": "0",
  "newOwnerMemberId": "102",
  "confirmation": { "kind": "LOGIN_PASSWORD", "secret": "登录密码" }
}
```

`confirmation` 必填。`secret` 传输为字段级 HYBRID 密文，服务端解密后再验。成功仍返回 `CreatedResource`。失败不改变 `owner_member_id`。
