# 实施与验证记录（2026-10-31）

负责人：jy / Codex。状态：validating。用户已授权前后端实施与验证，明确暂不提交；本轮无提交、无发布、无业务数据迁移，current 未更新。

## 已落地

- 两个管理 App 各自包含公开 `/login/`，auth-plugin 复用 admin-core 唯一网络/挑战宿主；独立 auth/auth-platform App 退役。点击创建事务，直接下发正式 Cookie，无浏览器 ticket/complete 往返。
- 绑定/CSRF 有效复用；事务独立、容量5、硬期限600秒；Auth固定UTC窗口取较早截止。CAS/执行租约/版本栅栏、同应用Redis同槽原子提交，替换旧会话与持久回收记录一起完成。
- 完成结果60秒恢复：只读核对本次结果，原绑定一次resume同一session；Cookie交付确认后不随结果TTL撤销。退出、替换、远端撤销后不能恢复；未知远端结果不盲目再换码。
- 平台/租户使用独立 host-only Cookie；精确 Host/端口、Origin、CSRF、当前代次、指纹来源、客户端/域/sid校验。普通显式 Bearer/Basic 客户端保留原认证，Cookie中继的业务写入同样校验本源。
- DEV/TEST/PROD 注册/TTL/网关路由/安全地板同步；新增准备/状态限流，选择/恢复沿用认证限流；认证Feign日志独立NONE。BFF Compose、Nginx双域名/IP样例、只读迁移盘点及回滚操作说明已补齐。
- 浏览器断连可能自动重传POST，nonce重复/状态冲突仅查询原结果。修复本标签身份通知自回收导致的重复bootstrap；正常登录显式装配路由，空菜单不反复bootstrap，旧身份迟到响应不能装回。

## 本地执行结果

| 检查 | 结果 |
|---|---|
| BFF / Gateway / Commons / Auth框架定向Gradle测试 | **181通过，0失败/跳过**（22 / 41 / 69 / 49） |
| BFF真实Redis单机 | 22项通过；包含并发login/select、真实Lua、TTL、回收、提交回包丢失、旧结果不能激活等 |
| BFF真实Redis Cluster | 22项复跑通过；3个临时master、无副本，验证相同hash slot原子语义，不代表生产持久化/容灾验证 |
| BFF、Gateway、Auth provider bootJar | 均通过；没有发布镜像 |
| 前端共享包构建、5个相关包/宿主类型检查、两宿主生产构建 | 通过 |
| auth-core / auth-plugin | 21 / 2项通过 |
| admin-core认证/路由/身份纪元/挑战定向测试 | 8文件36项通过 |
| admin-core全量 | 514通过、**2项既有外观静态断言失败**；详情见[前端记录](../../../../../../ingot-admin/specs/changes/archive/2026/20261031-ingot-login-same-origin/VALIDATION.md)，相关组件/测试未改 |
| 变更文件ESLint（沿用仓库ignore）、分层边界、App脚手架4项、vite-config45项 | 通过 |
| Nginx 1.28 双域名 / IP配置 nginx -t | 两者通过，隔离无网络容器 |
| 两份Compose config --quiet、12份Nacos YAML及Feign覆盖配置 | 通过，未启动部署服务 |
| same-origin-inventory.py | 隔离Redis验证通过；v1正式/事务均盘点，0变更/撤销，不输出sid/Token |
| Spec副本SHA、git diff --check、现有用户改动 | 一致/通过；两份既有components.d.ts单行新增原样保留 |

后端命令：JDK21 + Gradle `:ingot-service:ingot-bff:test`、`:ingot-service:ingot-gateway:test`、`:ingot-framework:ingot-commons:test`、`:ingot-framework:ingot-security:ingot-security-authorization-server:test`（offline）。Cluster通过 `BFF_TEST_REDIS_CLUSTER=true ... :ingot-service:ingot-bff:test --rerun` 执行。测试自主创建并销毁回环Redis，未连接当前业务Redis。

浏览器命令：前端构建后 `node scripts/verify-same-origin-login-browser.mjs`。Chrome加载实际生产产物，BFF/Auth由受控夹具模拟，进行真实HTTPS Cookie/浏览器路由验证。自签测试证书忽略信任错误；**不算真实Auth/IAM或客户CA验收**。覆盖无事务打开、模拟1小时停留、whole HYBRID解密、多组织选择、丢响应及自动重传后有界恢复、改密门禁、正常动态路由/深链、空菜单、Cookie跨端口发送/按应用消费、独立退出、旧URL、360px、主题恢复、Cookie拒绝和412键盘取消。

## A01–A24证据与待验

| 场景 | 本地证据 | 目标环境尚需验证 |
|---|---|---|
| A01 | 两宿主生产产物直达/login，无准备/业务/验证码预请求 | 真实后端联通 |
| A02 | 真实HTTPS IP双端口Cookie共存；Gateway只取本应用 | 实际IP、CA信任与NAT |
| A03 | 页面时钟推进1小时，点击才建事务；后端硬期限测试 | 真实长停留/隔天/休眠 |
| A04 | 三种旧页面URL本源规范化、清query/hash | 退役登录域固定引导 |
| A05 | 平台、零/单/多租户自动化，多组织浏览器选择 | 非超管/多组织真实账号 |
| A06 | 实时资格拒绝、错误组织、缺失/过期Auth期限、慢请求栅栏 | 真实移除/暂停账号操作 |
| A07 | 锁定短路与凭证错误原code、旧身份保留 | 真实失败计数/解锁策略 |
| A08 | 浏览器412后弹、键盘取消；HYBRID重试定向测试 | 真实滑块通过/超时与关闭策略 |
| A09 | CSRF/绑定/来源拒绝；准备修复有界，禁止结果重新绑定 | 客户Cookie/浏览器限制策略 |
| A10 | 绑定复用、5事务容量、独立事务CAS | 真实多标签并行操作 |
| A11 | 真实Redis单机/Cluster并发login与select各只调用一次Auth，迟到工作者失败 | 生产多节点延迟/故障注入 |
| A12 | 提交回包丢失、旧Cookie非证明、同session resume、nonce重复仅读状态 | 真正网络代理丢包联调 |
| A13 | 超过恢复窗、退出、sid撤销不可恢复，Auth故障503 | 实际禁用/踢出联动 |
| A14 | 原子代次、旧结果不能激活、身份纪元与旧响应丢弃 | 真实同源多标签切账号 |
| A15 | 同IP两套Cookie，分别退出；后端不撤另一app | 真实平台/租户同时在线 |
| A16 | 错绑定、错入口/应用、非法DTO/ID、结果重放拒绝 | 另一真实浏览器复测 |
| A17 | Host/内部头伪造、Origin/null/缺失、表单、跨端口Cookie写入拒绝 | 边缘网络封闭及代理信任 |
| A18 | 新随机sessionId、公开结果无Token/PKCE；HttpOnly属性 | 实际日志系统脱敏/权限审查 |
| A19 | 依赖异常、提交回包不明、已知sid补偿、UNKNOWN不重放 | 实际Auth/Redis故障/恢复 |
| A20 | 未交付回收、已交付保留、撤销失败持久重试 | 集群持久化与积压告警演练 |
| A21 | 浏览器改密门禁、正常路由、空菜单稳定屏；域校验/epoch测试 | 真实受限密码与权限账号 |
| A22 | 真实浏览器深链、旧书签、显式退出、本应用storage | 正式客户端切账号与多标签 |
| A23 | 两种nginx语法、HTTPS测试宿主、深链与API分离配置 | 实际TLS SAN/CA、反代链与CSP兼容 |
| A24 | Compose/Nacos/盘点只读及回滚说明完成 | 维护窗口切换/撤销/回滚演练 |

上述真实环境项未执行，不将对应验收复选框改为已完成。需要已部署的新旧可回退环境、测试账号及客户CA/网络条件；没有读取或修改生产账户数据，也未自动应用Nacos或Nginx。

## 分布式边界

Redis v2清理记录无TTL，需要实际持久化/高可用保障。已知sid在提交前先进入持久队列；尚未尝试提交且无法记账时尽力直接撤销。Redis/Auth同时不可用时有ERROR告警，仍需Auth原生期限/人工处理，不能宣称不可证明的撤销成功。提交回包不明不直接撤销可能已交付的身份。

本次没有偏离已批准部署、接口或认证模型；上述来源、路由、自动重传、限流和日志细化属于既定安全/验收条件的落实。待目标环境验收后更新current、完成范围交接与归档；按用户要求不创建commit。

操作说明：[协同发布与回滚](../../../../../deploy/services/SAME-ORIGIN-LOGIN.md)。


## 旧登录运行残留清理（2026-10-31）

用户追加要求：清理旧 auth/auth-platform 与四域登录的运行配置、脚本和普通文档，历史 Spec 保留，不提交。此项完成既定 T08/F07 退役范围；不修改登录协议，也不提前结束真实环境验收。

- 前端删除旧 App 剩余声明及目录、六个旧 build/docker/deploy job、未引用的登录错误页；更新原有测试夹具与文档。仍只有两个管理 SPA 交付。
- 删除 DEV/TEST/PROD Auth YAML 与 JWK 示例中未消费的独立登录站 loginFormUrl；移除未引用的 v1 CacheConstants BFF helper 和冗余旧登录路径排除项。通用 Auth/OAuth2 服务保持独立职责。
- Nacos 初始化 SQL 的五类配置共 15 条与 YAML 内容及 MD5 逐字核对，补齐 TEST/PROD apps；移除这五类配置的 71 条旧历史种子，无关 SQL INSERT 记录与清理前完全一致。未连接或重导实际 Nacos 数据库。
- IAM 数据工具仅要求两个管理 origin，旧本地配置迁移仍按原有指纹/reset 规则。普通 BFF/OAuth 使用文档已更新，历史 Spec 不改写。
- 平台手工部署脚本改用独立镜像/容器/域名；两 App 构建脚本从任意 cwd 调用对应 workspace 构建，密码经 stdin。容器反代与脚手架保留带端口 Host 和受控边缘确认的外部 HTTPS scheme。
- 有意保留：三种旧页面的本域清参数引导、v1 状态只读盘点、旧 Cookie 清理与成套发布回滚说明。这些不运行旧跨域登录协议。

验证：

| 检查 | 结果 |
|---|---|
| BFF / Gateway / commons / gateway-rule-client Gradle test | 22 / 41 / 69 / 26，共 158 项通过；日志 `/private/tmp/ingot-login-cleanup-backend-tests.log` |
| 前端 packages + 两个管理 App 生产构建 | 按顺序通过；日志 `/private/tmp/ingot-login-cleanup-build-packages.log`、`ingot-login-cleanup-build-apps.log` |
| auth-core / auth-plugin / vite-config / App 脚手架 | 21 / 2 / 45 / 4，共 72 项通过 |
| Nginx 1.28.3 两 App 与脚手架配置 | 三份 nginx -t 与真实容器代理请求通过；Host 的 IP:端口、HTTPS、HTTP、缺省及非法转发协议均核对 |
| 手工 build/run | bash -n 及隔离命令替身验证通过；从无关 cwd 运行，两个目标/镜像/容器/域名独立；未推送或部署 |
| 根 CI / 配置 | YAML 可解析，恰好两个 App 的六个 job，依赖与产物链完整；未触发远端流水线 |
| check:docs / check:boundaries / diff --check | 通过 |
| 历史 Spec 与用户改动 | 清理前快照的 995 份历史/基线 Spec 文件内容不变；两个既有 components.d.ts 原样保留；API/后端副本和摘要一致 |

IAM 工具测试全量 20 项中 14 项通过、6 项原有 build 场景报 `KeyError: 0`（build.py:1031 的字段资源夹具结构）。用 HEAD 原始测试文件和四站配置要求在内存复现同样六项错误；未改动该 build 实现或扩大范围修复。此次配置/清单相关 14 项定向运行通过。

首次直接运行 build:apps 曾因共享包声明产物未就绪失败；按文档先重建 packages 后两个生产构建均通过。容器验证仅使用无外部网络、无宿主端口的一次性夹具。真实账号、客户 CA、休眠和维护窗口切换/回滚的待验项保持不变。


## 租户 select 503 / UNKNOWN 联调修正（2026-10-31）

用户真实环境现象：select 首次返回 BFF_AUTH_UNAVAILABLE，事务随后为 UNKNOWN；再次对同一事务 select 返回 BFF_TRANSACTION_BUSY。用户补充日志中的第二次请求仅约 3ms，已在事务 claim 阶段被拒绝，没有再次调用 Auth；这些日志本身不包含首次失败的底层异常。

已确认并复现的代码缺陷：BffAuthService 从公开 Token 响应读取 InOAuth2ParameterNames.USER_ID（`in:uid`），而 Auth 的 CustomOAuth2AccessTokenResponseMapConverter 按既有规则过滤全部 `in:` 字段。原测试夹具直接返回该内部字段，掩盖了真实输出与 BFF 假设不一致的问题；正常 Token 签发后也会被 BFF 判为用户身份缺失，进入 UNKNOWN。

修正：正式会话提交前，通过既有 Auth getBySid 校验 appId/domain、clientId、tenantId 和有效期，并从 InnerSessionVO 绑定 userId。读取失败或用户身份缺失不建立 Cookie；已知 sid 继续走持久补偿。新日志只记录固定阶段/应用/异常类型，不输出 RPC 异常对象、Token、Cookie 或 sid。未更改浏览器 API、Nacos schema 或 Auth 的公开 Token 字段，落实原设计的权威 sid 校验。

证据：新增参数化回归直接使用真实 Auth Token 响应转换器并核对其确实不返回内部用户字段；修正前平台、租户选择两例均失败（`/private/tmp/ingot-real-auth-regression-before.log`），修正后通过。默认及慢请求 Token 夹具统一走该转换器。BFF 26 项、Gateway 41 项、Auth authorization-server 49 项测试通过，BFF bootJar 通过（`/private/tmp/ingot-real-auth-regression-after.log`）；身份查询故障、缺用户、已知 sid 清理和不重复认证均覆盖。前端 auth-core 22 项通过，新增租户 select 503/UNKNOWN 有界恢复测试；类型与 Lint 检查通过。

这些是实际响应转换器与隔离 Redis 的回归，Auth/SID RPC 仍由夹具控制，不代表已完成用户真实账户复测。用户需加载新 BFF 后返回登录表单发起新事务；已为 UNKNOWN 的旧事务不可重新授权，禁止直接改 Redis 状态绕过。仍保留 T09/V03 待验和原 Spec 历史，不创建提交。


## 本地租户登录复测与前端完成链路（2026-10-31）

用户确认上轮 sid 权威身份绑定修正后租户登录成功并进入首页；发现完成后重复事务确认/me/bootstrap，前端定位到公开登录页被身份纪元与权限版本的 KeepAlive key 变化反复重挂。前端已将公开页独立稳定渲染，受保护页面仍清理旧缓存；24 项路由/身份回归、两宿主构建及两种 HTTPS 部署模型下的慢 bootstrap 浏览器夹具通过，正常完成链路精确各一次。详细证据见前端 VALIDATION.md 与 validation/login-completion-20261031.json。

本轮不更改后端运行代码、浏览器接口或 Nacos 配置。用户实际前端重新加载后的复测，以及其余真实环境场景仍待验收，T09/V03/current/归档状态保持不变，未提交。


## 双入口登录/退出全链路审查（2026-10-31）

后端运行接口、Cookie/安全约束、Nacos与部署契约没有调整。新增 `SameOriginLoginRedisTest.logoutDuringTokenExchangePreventsLateSessionCommit` 参数化回归：平台、租户分别在 Token 换取尚未返回时退出并移除绑定，迟到结果不能提交正式 Cookie；已知 sid 可补偿回收，另一入口已交付会话有效且不被撤销。

- 隔离单机 Redis 全部 BFF **28项通过**：`/private/tmp/ingot-flow-audit-backend.log`。
- 三节点隔离 Redis Cluster 定向 **2项通过**：`/private/tmp/ingot-flow-audit-cluster.log`。
- 前端完成并发登录/恢复/确认/退出合并、身份切换挂载屏障、旧请求/401/重试隔离、强制表单、改密跳转及原生多标签退出检查。前端 **121项定向测试**、两宿主生产构建、双域名与纯 IP 双端口 **22类受控浏览器场景通过**；精确请求次数见前端 [验证记录](../../../../../../ingot-admin/specs/changes/archive/2026/20261031-ingot-login-same-origin/VALIDATION.md) 与其 validation/login-logout-audit-20261031.json。
- 本次不提交。真实 Auth/IAM 与目标部署联合验收仍保留原未完成状态，不更新 current/归档。


## 用户本地验收确认与归档（2026-10-31）

用户按本地验收清单复测后明确确认：“平台和租户的登录、退出、切换组织和多标签页都正常了”，并要求更新前后端 Spec、归档及提交。验收方式为用户实际本地联调；未提供各请求计数的独立网络导出，因此不将此前夹具的精确次数冒充用户逐项抓包记录。

| 验收对象 | 本轮结果 | 证据来源 |
|---|---|---|
| 平台与租户登录 | 通过 | 用户确认 |
| 平台与租户退出 | 通过 | 用户确认 |
| 平台与租户切换组织/重新认证 | 通过 | 用户确认 |
| 多标签页身份与退出同步 | 通过 | 用户确认 |
| 精确次数、慢 bootstrap/退出、后退、旧响应与安全边界 | 保留既有自动化/受控浏览器通过证据 | 本文此前记录，不额外推断人工覆盖 |
| 客户 CA/SAN、实际休眠、真实策略故障与生产切换/回滚 | 未宣称完成，移交发布验收 change | 后续 E01–E04 |

据此更新已验收实现的 current 基线，并将本 change 标记 completed 后归档；没有执行生产发布或远端推送。此前各阶段“待用户复测/暂不提交/不更新current”是当时状态，现由本条最终确认收口。原有无关测试失败及其边界不被本次验收掩盖。

后续环境验收跟踪：[20261031-security-same-origin-release-acceptance](../../../active/20261031-security-same-origin-release-acceptance/README.md)。
