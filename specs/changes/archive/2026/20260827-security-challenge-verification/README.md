# 挑战验证（L6）

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260827-security-challenge-verification` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-08-27 |
| 目标发布日期 | TBD |
| Roadmap | [安全中心分阶段落地 Roadmap](../../../../../docs/requirements/themes/security-center-roadmap.md) · L6 |
| 需求来源 | [安全中心服务需求 §六 6.1](../../../../../docs/requirements/themes/security-center-service.md) |
| 平台路线 | `R-2026-029`（见 [ROADMAP.md](../../../../../docs/requirements/ROADMAP.md)） |

## 目标

把「图形 / 滑块验证码」从 Nacos-only 的 `ingot.vc.verifyUrls` 随请求带码，收口为可验收的挑战验证闭环：

1. **触发策略可降级**：`ingot.security.challenge` 与限流 / 黑名单同一套 `local | remote`。remote 走安全中心 `security_challenge_policy` + 共享快照；local / 地板走 Nacos。
2. **执行模型统一**：命中策略返回 HTTP 412 `CHALLENGE_REQUIRED`；客户端完成滑块后拿到 PassToken（Header `In-Vc-Pass-Token`），重试业务请求。登录入口 `POST /bff/auth/login` 与敏感接口共用此模型。
3. **补齐已写一半的闭环**：网关验码成功后签发 PassToken（当前签发逻辑被注释）；打开 L4 未启用的挑战 SDK 执行面。
4. **验证码引擎与 OTP 解耦**：anji 滑块不再走短信/邮箱那套 `checkOnly` / `VCRepository`；短信/邮箱留给 MFA / 二次确认。
5. **修既有执行面 bug**：`ON_RATE_LIMIT` 签发的 PassToken 在重试时 scope 对不上，无法跳过 Sentinel（见 D10）。

**核心原则**（承接 Roadmap 横切约定）：

| 原则 | 含义 |
|---|---|
| 执行面 vs 中心面 | 网关拦截请求、签发/消费 PassToken、跑滑块引擎；安全中心管策略 CRUD 与快照下发 |
| 策略 vs 引擎分离 | 可 Nacos 降级的是**触发策略**（路径 / trigger / 类型 / PassToken）；滑块底图、误差、anji 频率限制只在 `ingot.vc.image.*` |
| 依赖单向 | 网关读共享快照，不反向写安全中心；BFF 登录不内嵌验码，只处理 412 之后带 PassToken 的重试 |
| 主链路 fail-closed | 命中 ALWAYS 且无有效 PassToken → 412；Redis 不可用时不签发、不放行 |

## 依赖

| 前置 | 说明 |
|---|---|
| L4 访问防护 | 挑战 SDK、`security_challenge_policy`、Platform `/challenges`、共享快照 `challengePolicies` 已存在；L4 **未启用**执行面 |
| L5 会话安全 | 登录对外路径已是 `POST /bff/auth/login`；OAuth2 `/oauth2/**` 由 BFF Feign 直连、不经网关 |
| `ingot-cache` | remote 挑战策略继续用共享快照 + `VersionedDerivedCache`，不新建缓存域 |
| 配置治理 | 地板在 `in-security-gateway.yml`，开关 / mode 在 `in-service-gateway.yml` |

## 范围

**包含：**

- 打开 `ingot.security.challenge.enabled`；DEV `mode=local`，TEST/PROD `mode=remote`。
- 412 → `GET/POST /vc/image` → Header PassToken → 重试业务 的闭环。
- DB 种子：`ALWAYS` + `SLIDER` 覆盖 `POST /bff/auth/login`（复用 L4 分组 `login-auth`，scope=`login`）。
- Nacos 地板同等策略；敏感接口不写死，由策略配置。
- `ingot-verification-code` 内拆分 captcha 引擎与 OTP；废弃 `verifyUrls` 作为安全触发面。
- Platform 挑战 API 从「L6 前只读」改为可运营（字段校验）。
- 客户端 412 契约与 E2E（含登录全链路）。
- 修复 `ChallengeFilter` 消费 PassToken 只认 ALWAYS/`default` scope 的缺陷，使限流后挑战闭环可用。
- 验收后新建 `specs/current/security/challenge-verification/`，并更新 access-protection / config-governance 已知限制。

**不包含（非目标）：**

- §6.2 MFA、§6.3 二次确认、§6.4 挑战记录查询。
- 短信 / 邮箱作为挑战类型的发送与校验（策略若配置则跳过并 warn）。
- `challengeFailureLimit` / 验码失败拉黑（管理面字段保留，执行面不实现）。
- 每次 412 写 DURABLE 安全事件（登录高频；记录留阶段二）。
- 本仓库外的前端实现（只给契约）。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [Platform / 客户端契约](./PLATFORM-API.md)

## 待审阅决策（D1–D14）

2026-08-27 负责人确认开始实施，D1–D10 按推荐决议闭合。D11–D14 于实施中补齐（路径绑定、Header、不做 IP 绑定、验码去 captcha）。

| ID | 议题 | 决议 |
|---|---|---|
| D1 | 登录 / 敏感接口执行模型 | **412 + PassToken**（已确认 2026-08-27） |
| D2 | 策略来源 | 复用 `ingot.security.challenge` + 共享快照，不新建 Feign/缓存域 |
| D3 | 引擎配置 | `ingot.vc.image.*` 仅 Nacos，不上安全中心 |
| D4 | 验证码模块 | 模块内拆分 captcha 引擎；OTP 原样保留 |
| D5 | L6 挑战类型 | 仅 `IMAGE` / `SLIDER`；`SMS` / `EMAIL` 跳过 |
| D6 | 登录保护路径 | 只保护网关上的 `POST /bff/auth/login` |
| D7 | `verifyUrls` | 废弃为安全触发；三环境 Nacos 清空 |
| D8 | Redis 不可用 | 不签发、不放行（fail-closed） |
| D9 | 白名单 | 继续跳过挑战 |
| D10 | PassToken 消费 scope | **以请求 Header `In-Vc-Scope` 为准**（与签发一致） |
| D11 | PassToken 路径绑定 | **消费时** `matchByScope`；不匹配不 consume |
| D12 | 传输位置 | **Header** `In-Vc-Scope` / `In-Vc-Pass-Token`；不保留 query |
| D13 | IP 绑定 | **本轮不做** |
| D14 | check 成功体 `captcha` | **不返回** |

审阅已通过，按 TASKS 实施。

## 完成记录

- 完成日期：2026-08-28
- 关联提交或 PR：
- 更新的 current capability：`specs/current/security/challenge-verification/`；同步 `access-protection`、`config-governance`、`gateway/header-conventions`
- 与原设计的差异：D11 消费路径绑定；D12 Header 定为 `In-Vc-Scope` / `In-Vc-Pass-Token`（曾计划 `X-Vc-*`，后按平台 `In-` 约定改；禁止 `In-Inner-*`）；D13 明确不做 IP 绑定；D14 check 成功体去掉 `captcha`；412 瘦身不含 `ttlSec`/`remaining`。Platform pattern `method` 必须是 `ANY` 或 HTTP 动词，不认 `*`。挑战 `groupCode` 只解析挑战域 groups，不能引用限流 yaml 的 `groupCode`。
- 取消原因：
