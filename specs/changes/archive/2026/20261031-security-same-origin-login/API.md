# 同源双入口登录 API（权威契约）

> 状态：completed；2026-10-31。接口已实施并完成本地功能验收，生产环境验收另行记录。
> 权威位置：ingot/specs/changes/archive/2026/20261031-security-same-origin-login/API.md。
> ingot-admin 配套 change 的 API.md 是逐字副本，禁止独立演化。

## 1. 通用约定

- 浏览器使用当前 origin 的 /api 前缀；下文是服务端路径。平台只用 /platform/，租户只用 /tenant/，{entry} 不是第三种可选入口。
- Gateway 由受信 Host/端口和注册表确定 appId/domain；BFF 核对路径入口。浏览器不提供 appId、domain、clientId、redirectUri、redirect_uri、loginUrl、completionUrl、adminOrigin 或绝对 returnTo；提交这些字段返回 BFF_INVALID_REQUEST，不静默接受。
- 响应沿用 R<T>，成功码与既有 R 实现一致；下列 JSON 仅表示 data。所有认证响应 Cache-Control: no-store，不通过日志或查询参数回显密码、Token、CSRF、会话 ID。
- 时间点响应为 ISO-8601 UTC Z；expiresAt 是服务端最终期限。JWT/OAuth 数字时间和配置秒数不改语义。
- transactionId、绑定 ID、会话 ID 由服务端密码学随机生成，至少 128 bit；CSRF 至少 192 bit。ID 不作为用户身份或权限证明。
- 除 csrf 自举外，状态修改请求携带 X-CSRF-Token，匹配当前应用绑定 Cookie 与服务端记录。写请求严格校验 Origin 精确等于本应用 origin；拒绝缺失、null、冲突来源，不用 Referer 覆盖不匹配 Origin。
- csrf 自举仅接受同源 application/json POST，校验精确 Origin，不开放 CORS。GET 不签发 Cookie、不绑定事务、不续认证期限；读取还须校验本应用绑定/会话。GET 有 Origin 时必须匹配，无 Origin 时采用同源 Fetch Metadata 或精确 Referer，并始终校验 Cookie。
- 新配置与状态采用命名常量/枚举；Java 业务枚举遵循 CommonStatusEnum/EnumUtils 契约。已在本轮实现。
- 登录/事务/resume 路径不依赖正式会话，不向 Auth 透传已有 Bearer；不能把旧账号会话当成新认证结果。
- 固定受信 defaultReturnTo 为 /；原业务深链只在管理台同源 sessionStorage 保存，前端规范化为本应用相对路径。拒绝 //、反斜杠及绝对 URL。
- 正常登录不返回浏览器 ticket/completionUrl，不接受旧 ticket 换会话。

## 2. POST /bff/auth/csrf

输入：空 JSON。唯一允许缺 CSRF 的自举接口；仍须通过 §1 的 JSON、Origin 与限流校验。
有效绑定：校验 appId 后复用 ID/CSRF，不因新开页面删除旧绑定。过期/不存在：创建全新匿名绑定；不采纳客户端指定绑定 ID，不把匿名绑定升级为正式 sessionId。
响应设置/更新当前应用绑定 Cookie；其有效期至少覆盖后续事务。新建事务及成功登录可延长绑定，不缩短已有更长有效期。

    {"csrfToken":"<opaque>","expiresAt":"2026-10-31T04:10:00Z"}

expiresAt 表示绑定/CSRF 当前有效期限；客户端缓存不代表服务端仍有效。绑定存储不可用返回 503，不签发无服务端状态的成功结果。读到旧正式会话不会自动完成新登录。

## 3. POST /bff/auth/{entry}/transactions

输入：空 JSON；要求本域绑定与 CSRF。仅在用户点击登录或明确重试未认证准备时调用。
服务端创建 LOGIN 事务，记录 appId/domain、唯一 bindingId、PKCE/state、硬截止时间；原子延长绑定 Redis/Cookie 到至少事务硬截止时间。同源不再区分 adminBindingId/loginBindingId。

    {"transactionId":"<opaque>","stage":"LOGIN","expiresAt":"2026-10-31T04:10:00Z"}

不返回 loginUrl。事务默认 600 秒，自创建开始，不因 GET、页面焦点或滑块重试续期。
每个 appId/bindingId 最多 5 个未终结事务，超过返回 429 BFF_LOGIN_CAPACITY；由 BFF 命名配置 max-active-transactions 控制，默认 5，正整数。
重复创建是不同事务，不影响其他标签页；前端负责合并一次点击的创建请求。限流、容量与账号失败保护独立，不能靠重建绕过。

## 4. GET /bff/auth/{entry}/transactions/{id}

仅允许持有创建时绑定的当前应用浏览器读取；不能认领 URL 中别人的事务。绑定、应用不符返回 403，不披露候选或结果。
LOGIN：返回 transactionId/stage/expiresAt。
AUTHENTICATING 或 COMMITTING：返回 transactionId/stage/expiresAt/retryAfterMs（固定建议 1000，客户端最多等待 15 秒）。
SELECT_TENANT：

    {"transactionId":"<opaque>","stage":"SELECT_TENANT","expiresAt":"2026-10-31T04:08:00Z","allows":[{"id":"<string-id>","name":"组织名"}]}

COMPLETED：结果记录存在且未过恢复期限时返回：

    {"transactionId":"<opaque>","stage":"COMPLETED","returnTo":"/","sessionEstablished":false,"recoveryExpiresAt":"2026-10-31T04:01:00Z"}

sessionEstablished 只有当前正式 Cookie 正好对应本次结果、appId、有效 sid 和当前会话代次时才为 true；“已有其他有效会话”仍为 false。不返回 sessionId、sid、密码、Token 或 Auth Cookie。
FAILED：返回 transactionId/stage，前端新建事务重试；不回显认证秘密。
过期/不存在返回 410 BFF_TRANSACTION_EXPIRED。已完成结果过恢复期不再靠该事务恢复；如当前已有正式会话，按普通 me/bootstrap 校验。
本接口只读，COMPLETED 的 expires 由独立 recoveryExpiresAt 表示，不重新延长认证或恢复窗口。

## 5. POST /bff/auth/{entry}/login

要求 whole HYBRID + X-CSRF-Token + 当前绑定；In-Ca-Sig 按既有指纹策略提供。

    {"transactionId":"<opaque>","username":"<account>","password":"<password>"}

网关 412 挑战仍在本请求前执行；请求 JSON 不新增验证码字段。验证码由 In-Vc-Scope/In-Vc-Pass-Token 提交。
只允许 LOGIN 原子推进到 AUTHENTICATING。相同事务处理中返回 409 BFF_TRANSACTION_BUSY；失败认证转 FAILED，新的用户提交必须新建事务，账号失败计数不重置。
平台：只认证 PLATFORM，不返回租户候选；租户零候选返回 BFF_IDENTITY_UNAVAILABLE，单候选仍重验成员资格并继续认证，多候选返回 SELECT_TENANT。
多候选响应见 §4，尚不设置正式 Cookie。选择期限取事务硬截止、Auth 预授权/state 真实可用期限的最早值。
平台/单候选成功：服务端完成 Auth authorize/token，原子提交本次正式会话结果，再设置当前应用正式 Cookie：

    {"transactionId":"<opaque>","stage":"COMPLETED","returnTo":"/","recoveryExpiresAt":"2026-10-31T04:01:00Z"}

COMPLETED 才表示本次认证已完成；收到响应仍需以实际携带 Cookie 的 me/受保护请求确认交付，之后装配业务。
已完成或终结事务不得重新认证；需读状态并按 §7 恢复，不重放密码。账户/密码/初始改密等既有业务 code/message 保持，不全部改成 BFF 错误。

## 6. POST /bff/auth/tenant/select

    {"transactionId":"<opaque>","tenantId":"<string-id>"}

要求当前绑定与 CSRF。仅 SELECT_TENANT 可原子进入 AUTHENTICATING；候选 ID 必须属于该事务且 Auth 重新校验实时资格。
成功响应与 §5 COMPLETED 相同，直接设置本域正式 Cookie。不返回浏览器 ticket；平台入口调用拒绝。
资格撤销、选择窗口过期不能恢复为已认证状态；需在本域重新认证。用户取消选择可退回表单，放弃事务到期回收；不改变旧正式会话。

## 7. POST /bff/auth/{entry}/transactions/{id}/resume（仅异常恢复）

输入：空 JSON；要求同一 appId/bindingId、有效 CSRF、原完成请求的设备指纹约束。不是匿名 ticket 兑换接口。
仅 COMPLETED、恢复窗口内、对应正式会话仍有效且仍是该绑定当前代次、未退出/撤销/被新身份替换时，允许重新下发同一个正式 Cookie；不再校验密码、不再调用 token、不创建第二个会话。
成功响应同 §5。窗口默认 60 秒，由命名配置 completion-recovery-ttl 控制，必须为正且不超过 session-ttl；实际截止还受正式会话期限约束。不滑动续期。
超过期限/结果被替换/退出/撤销返回 409 BFF_RESULT_UNAVAILABLE；需新事务重新认证，不重建旧身份。绑定不符返回 403，不允许将结果重新绑定。
前端仅在网络结果不确定且 GET 确认本次 COMPLETED、sessionEstablished=false 时调用一次。再次网络不确定可重新读状态；不能无界自动重试。
本接口已按批准的同结果恢复约束实施并验证；生产故障演练另由发布验收记录。

## 8. 会话、退出与辅助接口

- GET /bff/auth/me：原字段保留，appId/domain 与入口相符；无有效正式会话返回既有 401。校验有效 sid，不能只读到 BFF 残键就认定在线。收到实际正式 Cookie 的 me 可幂等确认该会话已交付，不续认证时限、不新建身份。
- DELETE /bff/auth/logout：沿用来源检查与既有 CSRF 不匹配仍尽力清本地 Cookie 的退出兜底；只撤销当前应用 sid、清当前应用正式/绑定 Cookie，并使该结果不可 resume；不删除另一应用 Cookie。
- GET /crypto/public-keys、GET /vc/image、POST /vc/image/check、GET /iam/v1/me/bootstrap、强制改密接口沿用现有业务契约。
- POST /bff/auth/{entry}/complete 删除；新入口即使收到 ticket 参数也拒绝，不接受旧交接协议。旧通用 /bff/auth/login 不新增别名。
- /auth/start、/auth/complete、/oauth2/challenge 是页面兼容路由，不是新 API；只清理旧参数并进入本域 /login/，不消费旧 ticket、不跨域跳转。

## 9. 稳定错误与前端处理

| code / HTTP | 含义 | 处理 |
|---|---|---|
| BFF_INVALID_REQUEST / 400 | 禁止字段、非法参数 | 留页提示，不自动重放 |
| BFF_ENTRY_MISMATCH / 403 | 应用/来源/入口不符 | 阻断，不尝试另一入口 |
| BFF_BINDING_MISMATCH / 403 | 绑定缺失、过期或不属于事务 | 未认证准备可重新 csrf+新事务一次；已认证结果不得重新绑定 |
| BFF_CSRF_INVALID / 403 | 有效绑定下 CSRF 不符 | 原地重新获取 CSRF；确认事务仍属当前绑定后有限重试 |
| BFF_TRANSACTION_EXPIRED / 410 | 事务或 Auth 认证窗口失效 | 未认证阶段重建一次；已验证阶段回表单重新认证 |
| BFF_TRANSACTION_BUSY / 409 | 同事务正在处理 | 只读状态，最多 15 秒；不并发提交密码 |
| BFF_TRANSACTION_CONFLICT / 409 | 不允许的状态推进 | 读状态或用户明确重新认证 |
| BFF_RESULT_UNAVAILABLE / 409 | 完成结果不能恢复 | 当前页重新认证，不激活旧结果 |
| BFF_LOGIN_CAPACITY / 429 | 未终结事务超过容量 | 提示稍后/关闭旧流程，不循环创建 |
| BFF_AUTH_UNAVAILABLE / 503 | Auth/BFF 认证依赖暂不可用 | 结果不确定先查状态；不伪装过期，不清旧身份 |
| CHALLENGE_REQUIRED / 412 | 既有挑战要求 | 全局挑战组件处理 |
| 既有凭证/锁定/改密错误 | 账号业务失败 | 沿用原 code/message 和安全规则 |

通用 Redis 失败使用既有 503/服务不可用映射，不返回伪造成功。前端不按错误文案匹配状态，不对 403/503 全局重定向。以上 code 为本次已实施契约。

## 10. Auth 内部接入有效期

BFF→Auth 的 pre_authorize/authorize/token 方法、授权码和PKCE语义保留，浏览器不直接调用。
pre_authorize 成功的内部 data 增加 authenticationExpiresAt（ISO-8601 UTC Z），由Auth计算本次预授权SecurityContext及待授权state真实可用期限的最早值；不是客户端提交值，也不是授权码尚未生成时虚构的code期限。
BFF将其与事务硬截止取最早值形成SELECT_TENANT的expiresAt。已经到期返回明确过期；新BFF收到缺失、无时区或不可解析期限则503，不猜测“还能等十分钟”。其他OAuth客户端原有grant、回调和业务返回不改变，此附加字段不得携带Token等秘密。
具体Auth接入/框架响应DTO及RemoteAuthTokenService同步属于T04；联合发布验证，不能让新BFF与未提供此字段的旧Auth混跑。
