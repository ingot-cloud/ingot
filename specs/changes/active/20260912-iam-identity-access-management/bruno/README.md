# IAM T16 第二批 Bruno 集合

Bruno → **Open Collection** → 选本目录。右上角环境选 **local**。

把 `environments/local.bru` 里的 `tenantA` 改成你第一批创建的**组织 A ID**。其余 token / 成员 ID 由请求脚本写入，不必手填。

## 执行顺序

按文件夹序号跑，每个文件夹内按请求序号跑：

1. `00-auth` — 登录并加载组织 A 上下文
2. `01-A01b-identity-lifecycle` — 暂停平台 / 移出租户 / 禁用账号
3. `02-A02-entitlement-vs-assignment` — 开通 ≠ 业务授权
4. `03-A04-shared-role-deltas` — 共享角色 + ADD/REMOVE/REPLACE_SCOPE + 恢复基础
5. `04-A26-owner-transfer` — 所有者转交（会改组织 A 所有者，必须最后）

标题含 `expect-fail` 的请求**预期失败**（断言已写在 Tests）。

## 约定

- Auth：`http://localhost:5100/oauth2/token`
- Gateway：`http://localhost:7980/iam/v1/...`
- OAuth Basic：`ingot` / `ingot`
- 平台请求不带 `Tenant`
- 租户请求必须带 `Tenant: {{tenantA}}`
- 成功信封 `code=S0200`；密码登录成功体是 OAuth2 `access_token`，不包在 `R` 里

A02 / A04 的业务断言打在**普通成员 Y** 上，不要用所有者（所有者有 SYSTEM 治理角色，会掩盖差异）。

重置密码会标记 `mustChangePassword=true`，刚重置的账号在有效期内仍可登录；集合用登录成败判断身份是否还在，不要求先改密。

`expect-fail` 请求：暂停后的平台登录、移出后的租户登录、禁用后的任意登录、旧所有者改设置、旧 version 再转交。
