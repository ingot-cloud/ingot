# 双入口 BFF 登录契约

> 修订日期：2026-09-18。状态：implementing。用户已确认两套入口、不传重定向 URL、Host→appId 与 Nacos 回跳。Cookie 策略跟 `require-https`，注册表仅 Gateway/BFF 加载。主 change 既有 IAM 证据保留。本文是 BFF 登录、跳转与会话的权威契约；端到端调用链见 `docs/modules/authorization-server/BFF-AUTH-FLOW.md`。IAM 管理面仍以 API/OpenAPI 为准。

## 1. 范围与已锁定决策

- 两个独立托管的登录应用：apps/auth 为租户入口，apps/auth-platform 为平台入口。
- 一份 apps/admin 源码，按平台/租户配置分别构建并部署；生产同一主域下四个独立 HTTPS 子域。不是每租户一份管理台。
- BFF 一个实现、两组固定域入口。domain 只由服务端路径确定；不接受浏览器指定、不按账号或 tenant 是否为空推断，不在失败后跨域降级。
- 平台和租户独立认证、同浏览器同时在线；普通退出只作用当前应用对应 Auth sid。全局账号禁用及安全撤销仍依既有全局规则。
- 租户零候选失败，单候选由 BFF 自动执行完整租户认证，多候选展示选择；切租户重新认证。平台允许无任何租户资格且永不返回租户候选。
- 浏览器不传任何重定向类绝对 URL（`redirect_uri` / `redirectUri` / loginUrl / completionUrl / adminOrigin）。回跳地址只由 **Gateway 注入的 appId** 读取 Nacos 应用注册表得到。Origin/Referer 只做与注册 origin 的一致性校验，不是查找键。
- 保留凭证检查、初始改密、账号锁定、挑战、HYBRID 加密、授权码与 PKCE。Token 只在服务端，不对 SPA 开放 password grant，不新增 SSO、跨主域、扫码或平台代入租户能力。Member 与第三方既有认证不改变。
- 本轮还包含全部 IAM 前端 API、页面及验收对齐；页面范围见 FRONTEND，前端落点和覆盖矩阵由前端 change 管理。

## 2. 配置与信任来源

### 2.1 应用注册表（Nacos）

BFF 与 Gateway 共用 `ingot.bff.apps` 与 `ingot.bff.require-https`，写在 **仅二者 import** 的 Nacos `in-bff-apps.yml`。不要放进全服务 `in-common.yml`。TTL / 指纹等 BFF 独占项仍在 `in-service-bff.yml`。每行字段：

- `appId`：稳定键。本轮仅 `platform-admin`、`tenant-admin`。**不等于** IAM `applicationId`。
- `domain`：`PLATFORM` 或 `TENANT`，与入口路径锁定，不能被请求改写。
- `adminOrigin` / `loginOrigin`：精确 origin（scheme + host + 非默认 port），生产必须 HTTPS、同一主域。
- `completionPath`：固定 `/auth/complete`。
- `loginPath`：固定 `/oauth2/challenge`。
- `startPath`：固定 `/auth/start`。
- `defaultReturnTo`：固定 `/`。
- `oauthClientId` / `oauthScope` / `oauthRedirectUri`：该应用独立 OAuth 公开客户端。本轮 `oauthClientId` 为 `in-bff-platform`、`in-bff-tenant`；`oauthRedirectUri` 是 Auth 注册的**协议回调**（如 `https://bff.example/bff/auth/platform/callback`），与业务 returnTo、完成页独立，必须精确匹配注册值。禁止把浏览器返回路径拼进 OAuth redirectUri。种子脚本见后端 `databases/bff_client_init.sql`。

本轮只交付两行。同 `domain` 再增加产品时只加注册行（新 `appId` + 新 origin 对），**不**新增 `{entry}` 字符串、不让前端传 `frontendId` 或 URL。`{entry}` 仅为文档占位，实际路由只注册 `platform`、`tenant`。

`require-https: true`（内网测试、内网生产、公网生产）启动失败条件：缺行、appId/origin 重复、非 HTTPS origin、跨主域、空列表。**禁止**把 `http://IP:端口` 配成 origin。已部署环境用四个不同 HTTPS 主机名、同一主域、边缘终止 TLS；浏览器信任内网 CA。Cookie 名称与 `Secure` 只跟该开关，不跟 `request.isSecure()`，也不再提供 `cookie.domain` 配置。

`require-https: false` **仅本机 DEV**：四个不同 hostname（Cookie 按 host 隔离），端口仍用 Vite 开发端口：

| 站点 | origin |
|---|---|
| 租户管理台 | `http://tenant.localhost:5798` |
| 租户登录 | `http://tenant-login.localhost:1798` |
| 平台管理台 | `http://platform.localhost:5799` |
| 平台登录 | `http://platform-login.localhost:1799` |

`*.localhost` 直接回环，不必写 `/etc/hosts`。不要配 `Domain=.localhost`，也不要用光杆 `http://localhost:端口` 或 `http://IP:端口` 当作注册 origin。OAuth `oauth-redirect-uri` 仍是网关协议回调（如 `http://localhost:5400/bff/auth/{entry}/callback`），浏览器不访问。端到端步骤见 `docs/modules/authorization-server/BFF-AUTH-FLOW.md`。局域网互访应绑同样主机名（最好 HTTPS）。IP+HTTP 不是受支持的部署形态。

### 2.2 Host → appId（权威）

四站点同源代理 `/api` 到 Gateway。Gateway 在入口：

1. 剥离客户端伪造的内部头（含下文 `In-Inner-Bff-App-Id` / `In-Inner-Bff-Entry`）以及不可信的 `X-Forwarded-Host` 对应用身份的影响。
2. 用 **TLS/HTTP 的实际 Host**（经可信转发策略规范化后的主机名，不含用户可改的 X-Forwarded-Host）匹配注册表的 `adminOrigin` 或 `loginOrigin`。
3. 写入内部头：`In-Inner-Bff-App-Id`、`In-Inner-Bff-Entry`（`admin` 或 `login`）。未匹配或冲突则拒绝，不转发 BFF。
4. 网络层限制绕过 Gateway 直连 BFF；BFF 若收不到内部头或头与路径不匹配，返回 `BFF_ENTRY_MISMATCH`。

查找回跳配置的键是 **注入的 appId**，不是 Origin。Origin/Referer 必须精确等于该行对应站点 origin（管理台请求对 `adminOrigin`，登录站请求对 `loginOrigin`）；缺少或不匹配则拒绝。禁止 `startsWith` 前缀匹配，禁止白名单为空时放行。

### 2.3 浏览器不得传的字段

请求中出现 `domain`、OAuth 参数、绝对 `redirectUri`/`redirect_uri`/`loginUrl`/`completionUrl` 均视为非法输入（`BFF_INVALID_REQUEST`），不得覆盖服务端决策。

`returnTo` **本轮不进 BFF 接口**。BFF 落地页始终为该应用 `defaultReturnTo`（`/`）。登录前深链由管理台同源 `sessionStorage` 保存，complete 成功后仅允许恢复经本地规范化的相对 path（拒绝 `//`、反斜杠、绝对 URL）；BFF 不接收、不回显该 path。

登录站无事务时，跳转目标只能是 **该登录产物构建期配对且与注册表一致的 `adminOrigin + startPath`**，禁止用 query/host 覆盖。

### 2.4 Cookie 与部署矩阵

| 环境 | Nacos group | `require-https` | Origin | 正式 / 绑定 Cookie |
|---|---|---|---|---|
| 本机开发 | `DEV_GROUP` | `false` | `http://tenant.localhost:5798` 等四个 `*.localhost` origin（见 §2.1） | `IN_SESSION` / `IN_AUTH_BINDING`（host-only、无 Secure） |
| 内网测试 | `TEST_GROUP` | `true` | 内网 DNS 四个 HTTPS 主机名，同一主域 | `__Host-IN_SESSION` / `__Host-IN_AUTH_BINDING` + Secure |
| 内网生产 | 独立 group 或覆盖后的 `PROD_GROUP` | `true` | 内网 HTTPS 主机名（不要复用公网域名） | 同上 |
| 公网生产 | `PROD_GROUP` | `true` | 现有公网四子域 | 同上 |

共同属性：HttpOnly、SameSite=Lax、Path=/、无 Domain。下文表格里的 `__Host-IN_*` 指 **HTTPS 环境** 名称；DEV HTTP 用上表无前缀名。Gateway JWT 中继只读正式会话 Cookie，不读绑定 Cookie。

## 3. 浏览器接口

以下为服务端路径，浏览器同源增加 `/api` 前缀。响应沿用 R<T>，下列结构指 data。HTTP 状态按第 3.3 节。新增契约不计入既有 96/161 IAM 控制器快照。

| 方法与路径 | 允许入口（Host） | 输入 | 成功 data / 效果 |
|---|---|---|---|
| POST /bff/auth/csrf | 注册管理台或登录站 | 空 JSON；CSRF 首次可豁免本接口 | `{csrfToken}`；建立当前 host 的 `__Host-IN_AUTH_BINDING`，no-store |
| POST /bff/auth/{entry}/transactions | 该域管理台 | `{}` | `{transactionId, loginUrl}`；`loginUrl` 仅由注册 `loginOrigin + loginPath + ?tx=` 不透明事务 ID 构成 |
| GET /bff/auth/{entry}/transactions/{id} | 对应登录站 | 不透明事务 ID | `{transactionId, stage, expiresAt, allows?}`；恢复事务并绑定登录站浏览器 |
| POST /bff/auth/platform/login | 平台登录站 | `{transactionId, username, password}`，whole HYBRID + CSRF 头 | LoginResult，只允许 READY |
| POST /bff/auth/tenant/login | 租户登录站 | 同上 | LoginResult，SELECT_TENANT 或 READY |
| POST /bff/auth/tenant/select | 租户登录站 | `{transactionId, tenantId}` + CSRF 头 | LoginResult，READY |
| POST /bff/auth/{entry}/complete | 目标管理台 | `{ticket}` + CSRF 头 + 本站设备指纹 | `{returnTo}`（恒为注册 `defaultReturnTo`），设置本 host 正式 `__Host-IN_SESSION` |
| GET /bff/auth/me | 管理台 | 正式 Cookie | `{appId, domain, tenantId, userId, clientId}`；tenantId 平台为 null |
| DELETE /bff/auth/logout | 管理台 | 正式 Cookie；CSRF 尽量带上 | 空成功 data。清本 host 正式 Cookie 与绑定 Cookie；能读到会话则尽量撤销当前应用 sid。CSRF 不匹配时仍清 Cookie 并返回成功，前端必须离开管理台 |

`loginUrl` 形状：`{loginOrigin}{loginPath}?tx={transactionId}`。`transactionId` 至少 128 bit 密码学随机、URL-safe。`completionUrl` 形状：`{adminOrigin}{completionPath}?ticket={ticket}`。ticket 至少 128 bit、一次性、默认 60 秒且不超过事务剩余期限。二者都不含 returnTo、domain、admin 以外的 host。

LoginResult 为判别联合：

- `{stage: "SELECT_TENANT", transactionId, allows: [{id, name}]}`：只在租户分支出现；ID 为字符串，仅披露有效候选的必要展示字段。
- `{stage: "READY", transactionId, completionUrl}`：凭证及域身份认证已完成，但管理台正式会话尚未建立；不能在登录站据此请求 IAM。
- 事务读取 stage 为 `LOGIN`、`SELECT_TENANT` 或 `READY`。READY 恢复时可返回仍有效的原 completionUrl，不重新签发/延长 ticket；已消费或已过期事务拒绝恢复。
- PLATFORM 响应不含候选；内部共享 DTO 如有 allows，固定 []。不以 allows 空/非空推断成功或页面阶段。
- 选择阶段是 Auth/IAM 语义（TENANT 且无 tenant 时无 AuthorizationContext）。BFF 对**单候选**自动继续 authorize，仍须重验有效成员资格，不得因列表长度为 1 跳过校验。

### 3.1 CSRF、CORS、Cookie 隔离

- CSRF：`POST /bff/auth/csrf` 建立绑定并签发 token；除该接口外所有状态修改请求带 `X-CSRF-Token`，并与本 host 的绑定 Cookie（§2.4）核对。GET 不设绑定 Cookie（避免预取）。顶层页面 GET 不执行登录/退出。若请求已带绑定 Cookie，服务端先删除 Redis 中该 binding，再签发新绑定（轮换，避免前端缓存的旧 token 与 Cookie 不一致）。管理台创建事务必须重新 `POST csrf`，不得因 sessionStorage 仍有 token 而跳过。登录站把 CSRF 与当前 `transactionId` 绑定：新事务或缓存不属于该事务时必须重新 `POST csrf` 并覆盖 `X-CSRF-Token`；同一事务内挑战重试复用。事务过期或 `BFF_BINDING_MISMATCH` 跳回配对 `/auth/start` 前清除本 origin 的 CSRF 缓存。
- Origin 精确匹配；拒绝凭证式通配 CORS。每个 origin 只能调用上表「允许入口」列中的接口：登录站不能 `complete`/`logout`/`me`，租户登录不能打 `platform/login`，管理台不能提交账号密码。
- 同主域子站仍为 same-site，不能只靠 SameSite 防 CSRF。
- 登录站不得共享管理台正式 Cookie。绑定 Cookie 与正式会话 Cookie 名称、Redis 前缀均不同；Gateway 只对正式 `__Host-IN_SESSION` 做 JWT 中继。

### 3.2 错误码与前端映射

新协议错误在 R 的稳定 `code` 中区分，并使用对应 HTTP 状态：

| code | HTTP | 前端处理 |
|---|---|---|
| BFF_INVALID_REQUEST | 400 | 提示参数错误，不跳登录 |
| BFF_RETURN_TO_INVALID | 400 | 同非法请求（本轮无 returnTo 入参时仅内部规范化失败使用） |
| BFF_TICKET_INVALID | 400 | 完成页停止，引导本站 `/auth/start` |
| BFF_TRANSACTION_EXPIRED | 410 | 重新 `/auth/start` |
| BFF_TRANSACTION_CONFLICT | 409 | 提示阶段冲突，可重新启动事务 |
| BFF_ENTRY_MISMATCH | 403 | 停止，不跨域 |
| BFF_BINDING_MISMATCH | 403 | 登录站清除 CSRF 后重新配对 `/auth/start`；管理台完成页停止并引导本站 `/auth/start`。不跨管理域 |
| BFF_IDENTITY_UNAVAILABLE | 403 | 提示身份不可用；**不**展示另一域资格 |
| （无正式会话） | 401 / 既有 S0401 | **仅管理台**进入本站 `/auth/start`，禁止跳 env 登录 URL |
| （认证依赖不可用） | 503 | 提示稍后重试，**不**登出、不伪装无候选 |

既有凭证错误、412 挑战、锁定、初始改密保持原契约，不硬改为 `BFF_*`。BFF 对 Auth `preAuthorize` 的 `InFeignException` 原样回传 `code`/`message`（如 `S0400`「用户名或密码错误」），不得改写成 `S0500`。错误不回显账号在另一域的资格。

管理台 401/登出/改密后的再认证：**只**导航到本站 `/auth/start`。禁止 `VITE_APP_LOGIN_URI?redirect_uri=`。已有正式会话再访 `/auth/start`：若会话有效则落地 `defaultReturnTo`（前端可再读 sessionStorage 深链），不新建事务。用户显式退出：不保存当前页为 returnTo，start 即使仍能 `me()` 也必须新建事务进入登录站，不得弹回刚才的业务页。用户显式「切换账号/组织」同样创建新事务。

## 4. 登录事务与完成流程

1. Admin 在 `/auth/start`：若无有效正式会话，则每次重新 `POST csrf`（轮换绑定并删除旧 Redis 键）再 `POST transactions`；不传 returnTo。不得复用上次缓存在前端的 CSRF token。
2. BFF 按注入 appId 固定 domain/origins/OAuth client，生成事务 ID、独立 OAuth state/PKCE，保存管理台浏览器绑定，返回注册表构造的 `loginUrl`。
3. 登录页无 `tx` 时，跳转到配对 `adminOrigin + startPath` 再建事务；禁止登录站绕过管理台绑定直接发正式会话。
4. 登录页先按当前 `tx` 决定是否 `POST csrf`（新事务或 CSRF 缓存不属于该事务则签发），再 `GET transactions/{id}` 恢复事务，然后按入口提交凭证。每次认证新建 Auth 预授权会话，不复用另一域 Auth 会话。挑战重试保持同一事务与同一 CSRF，不重复换 Token。HYBRID 登录 POST 必须带 CSRF 头。
5. 平台 pre_authorize 显式 `domain=PLATFORM`；authorize **不带** `org`，以绑定的预授权域及合法平台成员为依据。平台携带 tenant/org 拒绝。
6. 租户 pre_authorize 显式 `domain=TENANT` 且无 tenant；无 AuthorizationContext。零候选失败；单候选自动、多个候选显式选择，authorize 时均重新校验有效成员。候选在选择前被撤销则失败。仅平台身份走租户入口 → `BFF_IDENTITY_UNAVAILABLE`，不降级。
7. Auth RPC 贯穿 domain、state、client、oauthRedirectUri 与 PKCE。完成授权码换取后 Token 暂存**事务**（不得写入正式 BffSession，不得占用 accessToken 字段存 PKCE）。
8. 签发绑定事务/appId/目标管理台浏览器绑定的一次性 ticket；`completionUrl` 仅为注册 adminOrigin + completionPath + ticket。不是 Access Token 或正式 sessionId。
9. 完成页在执行其他网络请求前读取 ticket 并立即 `history.replaceState` 清 URL。complete **复用** start 时的管理台绑定 Cookie 与 CSRF（有缓存则不再 `POST csrf`，轮换会导致 `BFF_BINDING_MISMATCH`），携带本站设备指纹同源 POST complete。登录页与完成页均 `Referrer-Policy: no-referrer`，不加载第三方资源。
10. BFF 原子消费 ticket、验证事务与目标绑定，创建全新正式 sessionId，设置本 host `__Host-IN_SESSION`，返回 `defaultReturnTo`。Admin 获取 bootstrap，验证 domain/appId 与部署相符后恢复路径。无目标页权限进入首个可见页；无菜单显示已登录无功能。前端再按需恢复 sessionStorage 深链。

LoginTransaction 独立存储（Redis 前缀与正式会话不同），包含 appId/domain、两站点浏览器绑定、stage、PKCE/state、Auth cookie、候选、待交接凭据、ticket、到期时间。默认 10 分钟。多标签页各有事务。

TTL 关系：事务 10 分钟，覆盖打开登录页到提交凭证的等待；ticket 60 秒且不超过事务剩余。preAuthorize 之后的 Auth SecurityContext 跟随客户端 access-token TTL（BFF 客户端种子为 7200 秒）。OAuth 授权码与 Redis state 索引跟随客户端 `authorization-code-time-to-live`（BFF 客户端种子为 300 秒，且不得再被硬编码截短到更短）。authorize / token 因预授权会话、state 或授权码失效而失败时，BFF 返回 `BFF_TRANSACTION_EXPIRED`，登录站重启配对 `/auth/start`，不得伪装 `BFF_IDENTITY_UNAVAILABLE`。零候选、仅平台身份走租户入口仍是 `BFF_IDENTITY_UNAVAILABLE`。任一先到期则待交接 Token 必须撤销，不能只删 Redis。complete 成功后把管理台 CSRF 绑定 TTL 延长到 `session-ttl`，以便会话期内退出仍能通过 CSRF。

状态推进及 ticket 消费必须原子；同事务并发认证不重复换码。不同事务成功后可依次替换当前同应用 Cookie，最后成功完成者成为该 host 当前身份。取消/超时不提前撤销原正式会话；成功替换后撤销被替换的同应用 sid。网络丢失不重新消费已用 ticket；客户端先 `GET /me`，无有效会话则重新 `/auth/start`。

## 5. 正式会话、跳转和安全

正式 `BffSession` 增加 `appId`/`domain`，与可信 tenantId/account/member 身份及 Auth sid 关联。Gateway：

- 只读取当前环境正式会话 Cookie 对应 Redis（§2.4）；绑定 Cookie 与事务键 **不得** 触发 JWT 中继。
- 白名单覆盖 csrf、transactions、login、select、complete；这些路径即使带绑定 Cookie 也不注入 Bearer。
- 核对会话 `appId`/`domain` 与当前 Host 注入值；冲突拒绝。
- Cookie 链路：TENANT 会话在请求未带 Tenant 时注入 `session.tenantId`；请求 Tenant 与会话冲突则拒绝。PLATFORM 会话 **不注入** Tenant 头。

Cookie 属性与名称见 §2.4。两个 host 同名也不共享。会话设备指纹在 complete 时绑定管理台指纹，不能复制登录站指纹；顶层导航到完成页本身无需指纹 header。

既有 Bearer 客户端仍按自身认证规则校验，不借本次改造扩大权限。

OAuth 精确回调与禁止开放跳转依据 [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)。CSRF + 精确 Origin 依据 [OWASP CSRF](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)。事务/登录/完成页面及接口 no-store；代理及应用日志屏蔽 ticket、密码、Token、Cookie、完整敏感 query。

## 6. 部署、兼容与可观测性

新系统不提供旧 `/bff/auth/login` 租户别名，移除旧前端 OAuth query、任意 redirectUri 和 session 预授权链路。不改变 Auth 对其他合法客户端的既有缺省域兼容；BFF 新入口永不使用缺省域。

发布顺序：Auth/BFF/Gateway 配置及实现 → 四站点产物 → 真实联调 → 开放入口。每个站点独立产物/镜像；切流时清除旧带 Domain 的 Cookie 以及与当前环境名称不一致的会话 Cookie，旧会话不迁移。回退采用一致版本集，不恢复空白名单放行。内网正式部署：四主机名 HTTPS（可用同一 443 + `server_name`），使用端安装内网 CA；不要用 IP+HTTP。

观测登录阶段失败、域不匹配、事务过期、交接重放、bootstrap 失败，只记录脱敏关联 ID。生产域名是部署输入；预检验证四域、两 client、精确协议回调、内部头注入。

## 7. 验收（全部待执行）

| 编号 | 场景与通过标准 |
|---|---|
| L01 | 平台无租户资格可完成认证；共享 allows=[]；无平台资格不返回租户列表、不降级 |
| L02 | 租户零/单/多候选分别失败/自动完整认证/选择；仅平台身份不能进租户 |
| L03 | 两域同时在线，独立 sid/Cookie；登录、退出一域不破坏另一域；全局禁用仍生效 |
| L04 | 临时事务不能调 IAM；请求伪造 appId/domain/Tenant、错误站点或内部头均拒绝 |
| L05 | 前端传绝对返回地址被拒；合法默认页可恢复；深链仅本站相对 path |
| L06 | ticket 跨浏览器/应用/域、过期、重放拒绝；事务并发不重复换码，多标签页返回不串线 |
| L07 | 选租户前成员被暂停/移出则失败；单候选仍重验；切租户成功清旧上下文，失败/取消保留原会话 |
| L08 | CSRF/来源校验、挑战重试、锁定、初始改密、Auth 参数绑定保持有效；503 不伪装身份失败 |
| L09 | 完成页清 ticket、无第三方请求、日志脱敏；管理台指纹绑定正确，顶层跳转不误拒绝 |
| L10 | bootstrap 域不符阻断；无菜单不循环登录；旧请求不能回填新身份；401 只进本站 `/auth/start` |
| L11 | 四个不同 host 的 HTTPS 环境验证生产 Cookie 属性；四产物启动、client 配置及回退预检通过 |
| L12 | Member/第三方认证回归通过；旧 BFF 无域入口不可用，管理台不直连 Auth、不接收 Token |

需后端单元/集成及真实浏览器证据；不能用 Mock 或已有 A28 直连 Auth 证据替代 L01–L12。前端全量 IAM 另映射 F01–F09 与前端验收矩阵。未完成不勾选 T16/T17，不更新 current。
