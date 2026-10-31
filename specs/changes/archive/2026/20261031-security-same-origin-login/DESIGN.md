# 设计：同源内部登录

> 状态：completed；按已批准设计实施，接口字段以 API.md 为准；未宣称生产已切换。

## 1. 已选择的架构

两个应用入口，每个 origin 内同时提供 /login/、管理路由与同源 /api。一个 BFF 实现使用两个固定应用注册，内部 Auth 客户端保持独立。
不再需要登录域、管理域双绑定、浏览器 ticket、complete 页换会话或跨域准备协议。登录页与管理页进入同一浏览器信任边界；路径不是安全隔离机制。

责任划分：
- Gateway：剥离伪造内部头；可信 Host/端口→appId/domain；按当前应用选择 Cookie；校验正式会话、代次和 sid，匿名认证路径不做 Bearer 中继。
- BFF：同源/CSRF、匿名绑定、事务状态机、Auth 编排、正式会话提交、恢复与清理。
- Auth：继续承担密码、账号策略、平台/租户身份、授权码+PKCE、sid 及撤销。不得用浏览器声明覆盖域或 OAuth 客户端。
- 前端：点击后准备；挑战、选择组织、结果恢复；COMPLETED 后确认正式 Cookie 并 bootstrap；清理密码和旧身份 UI。
- 部署：仅两个管理宿主，可信代理维持真实 Host、scheme/port，应用 identity 不取客户端 X-Forwarded-Host。

## 2. 注册表与入口

应用注册保留 app-id、domain、oauth-client-id、oauth-scope、oauth-redirect-uri、default-return-to。
原 admin-origin/login-origin 合并为 origin；login-path 固定 /login/，default-return-to 固定 /。不再用 startPath/completionPath 编排认证。
平台与租户 origin 必须不同；允许相同 IP 不同端口。按 URI 规范化 scheme、host、默认端口；拒绝 userinfo、query、fragment、非根路径、重复 origin 和缺项。
生产 require-https=true，接受域名或合法 IP HTTPS。false 仅本机 loopback 开发，不作为内网 HTTP 逃生开关。
注册表不再按“同一主域”字符串切割校验；两个应用不共享 Cookie，不需要彼此同站。
Gateway 废弃 admin/login 主机角色推断，剥离并停止信任 In-Inner-Bff-Entry；只由受信映射注入应用身份。BFF 按应用与固定 API 路径判定准入，不使用前端角色头。
旧四站注册在切换版本明确报配置错误，不能同时解释新 origin 与冲突的旧 origin。部署前转换三环境样例；不默默兼容以免误路由。

## 3. Cookie 与 Redis 所有权

- 每应用各有正式 Cookie、匿名绑定 Cookie，名称及属性见 DEPLOYMENT.md。服务端只读取当前应用的一组；另一应用 Cookie 不参与 Redis 查询或身份判断。
- 一份有效匿名绑定可以服务多个事务；/csrf 复用，不因新标签页无条件删除。绑定仍限时，创建事务时原子续到至少事务期限；成功登录续到至少正式会话剩余期限。
- binding ID 不等于正式 sessionId。Auth 成功必须新生成正式 sessionId，不接受用户选择的值。
- Redis 新 schema 使用版本与 appId 命名空间；物理示例为 in:bff:v2:{platform-admin}:binding:<bindingId>、tx:<transactionId>、session:<sessionId>、binding-current:<bindingId>、cleanup。各后缀均带同一应用前缀；tenant-admin 使用自己的前缀。真实 key 统一由 BffRedisKeys 生成；cleanup 为按操作保存的 Hash，配合 cleanup-due 到期索引。
- 每事务记录 appId/domain、bindingId、state、PKCE verifier、Auth cookie、候选、硬截止、认证有效截止、执行租约/栅栏值、提交结果引用。秘密不进入 API。
- binding-current 记录当前提交代次和 sessionId；COMPLETED 结果存服务端 sessionId/sid/代次及 recoveryExpiresAt。API 只返回布尔 sessionEstablished。
- 本轮采用每 appId 一个 Redis Cluster hash tag，同应用绑定、事务、正式会话、当前代次与清理记录同槽，原子提交也能标记同应用被替换的旧会话（包括绑定过期后重新建立绑定的情况）。单机/Sentinel沿用相同逻辑；不执行跨槽Lua。代价是每应用集中一个槽，部署容量验证必须覆盖热点与吞吐；不把两应用逻辑隔离宣传为Redis负载均衡。
- 业务参考缓存框架不用于这类安全状态；Redis 不可用时不能用本地缓存伪造登录成功。

## 4. 状态机与期限

LOGIN → AUTHENTICATING → SELECT_TENANT → AUTHENTICATING → COMMITTING → COMPLETED。
平台/单候选：AUTHENTICATING 直接进入 COMMITTING。
凭证业务失败进入 FAILED；过期逻辑进入 EXPIRED；只有未终结事务计入默认 5 个容量。
AUTHENTICATING/COMMITTING 的网络结果不明记 UNKNOWN 执行记录并查询状态，不重新执行相同授权；对外仍展示处理中，无法确认则 503。
正常认证事务默认 600 秒，从 POST transactions 开始。实际选择期限为硬截止与 Auth 会话/state 有效期的最早值；不用前端时钟延长。API 若无法得知底层精确期限，应由 Auth 接入提供可信期限，不虚构多组织可等待时间。
完成恢复窗口默认 60 秒，独立于已完成认证的原硬截止；不延长身份或会话寿命。绑定、sid、会话被撤销会提前终止恢复。
保留 session-ttl 配置及既有 Auth Token 生命周期，本次不顺带设计自动续登。

## 5. 正常流与长停留

1. GET /login/ 显示表单；不创建事务、不以旧 tx 限制表单。可按需缓存公钥，不预取验证码。
2. 点击后合并同一次提交，POST csrf → POST transactions → POST login；表单 loading 防重入。
3. 网关返回 412 时现有全局挑战处理；重试保持本次事务，重新产生 HYBRID nonce/时间戳/密文，不重放原加密包。
4. Auth 密码失败：保留准确文案，事务 FAILED，下一次用户提交新建事务；账号失败计数继续累积。
5. 多组织返回候选、清理密码内存，SELECT 前重验准入；单候选也不跳过 Auth 的成员资格检查。
6. Auth authorize/token 成功后进入一次性会话提交；login 或 select 响应直接设置本域 Cookie。
7. 前端实际携 Cookie 校验 me/本次事务后 bootstrap；强制改密沿用受限分支，不能先装配业务权限。
8. 无效绑定/CSRF发生在未认证准备时，修复后最多自动重建一次；认证已开始或结果不确定时先读状态，禁止盲目换事务并自动重放密码。
页面焦点、休眠恢复只检查本地状态并清除过期标识；没有正在进行的认证则无需维持服务器事务。不能靠后台心跳使已验证身份永久有效。

## 6. 并发提交与网络恢复

- 进入认证/选择使用 Redis CAS、执行租约及递增栅栏，只有取得该次执行权的工作者可提交结果。租约丢失不自动换主重做 Auth，暂停不确定操作，等待明确执行结果或期限结束；迟到工作者撤销自己产生的未提交 sid。未知远端结果按§7边界处理，不假设已有自动对账RPC。
- 先准备新 sessionId 与结果，再原子写 session、COMPLETED 引用、binding-current 新代次与待处理清理记录；成功后才发 Set-Cookie。存储失败不返回 COMPLETED。
- 同事务重复请求不能重复 authorize/token/createSession。不同事务允许最后成功提交者成为同一绑定当前身份；已完成旧结果不能通过 resume 覆盖新代次。
- 同源浏览器可能按网络到达顺序覆盖 Cookie，不能保证旧 Set-Cookie 永远不迟到。因此 Gateway/BFF 必须拒绝旧代次 Cookie；前端暂停旧 UI，核对最新结果并恢复或要求重新认证，不能继续展示旧身份。
- 原正式会话在“新事务准备、认证失败、提交前故障”时保持。服务端已提交但响应丢失属于结果不确定，不承诺旧会话仍有效；通过本次结果恢复，不将已提交成功伪装成失败回滚。
- 前端最多查询 15 秒；GET COMPLETED 的 sessionEstablished=true 才可确认本次 Cookie 已建立。false 时调用一次受绑定/CSRF/设备约束的 resume，重发同一个会话 Cookie；恢复窗口外重新认证。
- 不对拿到的旧业务 session 做“既然已登录就当新登录成功”；不在 GET 中签发身份。
- 当前正式 Cookie 首次用于 me 或受保护请求时，BFF/Gateway 对本会话原子确认 deliveryConfirmed；这是一次性服务端交付记账，不续期、不签发新身份。Gateway 对成功解析且仍有效的当前代次才确认；不能以请求带了任意 Cookie 当作已交付。
- COMPLETED 恢复记录 60 秒后退出；已确认交付只删事务结果，正式会话继续原寿命；未交付结果条件进入 REVOKING，停止 resume，再经清理记录撤销该 sid/session。不能误撤已确认或被其他事务替换的身份。

## 7. 失败、撤销与可观测性

- Auth 网络/5xx 与明确过期分类；保留 preAuthorize 的业务 code/message。不再把所有 authorize/token 失败统一映射 BFF_TRANSACTION_EXPIRED。
- sid 必须经既有 Auth 权威执行面验证；新登录、恢复遇到无法确认状态的故障 fail-closed，普通业务会话原有宽限规则不在本次放宽。
  - 实施校正（2026-10-31 联调）：正式提交前调用既有 getBySid 核对应用、客户端、租户和到期时间，并从 InnerSessionVO 绑定 userId。Token 公开响应会过滤 `in:` 内部字段，不从该响应推断用户身份，也不新增公开用户字段或跳过 sid 校验。查询失败保留已知 sid 的持久撤销补偿，不签正式 Cookie、不重放认证。
- 同应用旧 sid 在成功提交后可靠撤销；开始或失败不提前踢人。撤销失败写清理记录重试并告警，Gateway 的代次校验立刻拒绝旧 Cookie；不宣称远端已撤销。
- 已知未提交/未交付 sid 必须进入有所有权与幂等键的清理队列，定时重试，不能仅依赖 Redis key 过期通知。确认过的活跃 session 不在结果 TTL 清理范围。
- Auth RPC 完全丢失且未获 sid 时保留 UNKNOWN 执行记录，停止同操作自动授权并告警。当前契约没有按操作ID查询Auth结果的RPC，本轮不虚构自动对账接口；由运维按现有Auth会话/安全记录排查，无法定位的远端状态仍受Auth原生期限约束。不声称已完成不可证明的撤销；这个分布式故障残留边界须随draft评审，不能盲目重试造成第二份凭据。
- 补偿任务只处理本 appId、transactionId、操作代次持有的 sid；退出/禁用等全局业务规则仍由 Auth 执行。
- 日志只记脱敏事务关联、阶段、错误分类、恢复/补偿计数。不记原始 Cookie、sessionId、密码、PKCE verifier、Auth Cookie、Token、CSRF。指标覆盖准备失败、认证耗时、代次冲突、恢复成功/拒绝、待清理积压。

## 8. 安全边界

- 来源、CSRF、绑定、事务应用、实时身份、会话代次和 sid 分层校验；nonce/PKCE 不替代自定义状态关联。
- 不开放 CORS，不接受父域共享 Cookie，未知 appId/Host、伪造内部头与路径域不匹配拒绝；生产 BFF 不可绕过 Gateway 直连。
- 纯 IP 双端口是同一可信主机上的应用分离，不提供 Cookie 发送隔离；不能混放互不信任服务。AppID 隔离不等于能够抵御拥有同 IP 服务控制权的攻击者。
- 登录页同源后具备管理 origin 的脚本能力；统一 CSP/依赖治理，不加载第三方脚本，避免 XSS、点击劫持与不安全重定向。
- 客户端不能提供已认证结果来重新绑定；resume 仅持有原绑定且原会话仍为当前代次时可用。退出和安全撤销后永远不可恢复。
- 登录、选择、恢复和准备全部限流；已有验证码策略不降低。新事务不重置账号失败次数。设备指纹是附加约束，不能替代随机绑定/CSRF。
- expiresAt/recoveryExpiresAt 输出 UTC Z；枚举、常量、注释、构造注入按仓库门禁实现，禁止本次旁支全仓重构。

## 9. 迁移与验收

部署切换见 DEPLOYMENT.md，A01–A24 见 ACCEPTANCE.md。本轮无业务 DDL。只更新本 change；current、旧 IAM 规格、历史外观记录均不提前改为新事实。
