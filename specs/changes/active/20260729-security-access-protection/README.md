# 访问防护补全（L4）

> 状态：approved（implementing）

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260729-security-access-protection` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-07-29 |
| 目标发布日期 | TBD |
| Roadmap | [安全中心分阶段落地 Roadmap](../../../../docs/requirements/themes/security-center-roadmap.md) · L4 |
| 需求来源 | [安全中心服务需求 §四、§3.1](../../../../docs/requirements/themes/security-center-service.md) |

## 目标

本 change 有两层目标，在一次 SDD 变更内分 Phase 交付：

**目标一（Closure — 执行面收口）**：把 R-2026-007 已交付的网关策略 SDK 与 Filter 链从「代码存在、默认未启用、DB 无种子」推进到**可验收的生产闭环**：

1. Nacos 三环境启用 SDK（`ratelimit` / `blacklist` / `violation-escalation`，`policy.mode=remote`）。
2. 安全中心 DB 种子规则（登录路径 endpoint group + IP 限流基线）。
3. 统一限流执行面收敛到 **Sentinel + SDK**；旧版路由级 `RequestRateLimiter` 迁移后移除（见 DESIGN D9）。
4. E2E 验收：`test-case/security-policy-e2e.md` 阶段一 + 阶段二 + 登录路径补充用例。

**目标二（Resilience — 网关策略弹性）**：镜像 L1 凭证 `20260717-security-credential-resilience`，为 `ingot-gateway-rule-client` 的 remote 快照拉取引入 **`remote → LKG → Nacos 地板`** 降级阶梯，消除 `RemoteSnapshotFetcher` 失败时 fail-open（无限流/无名单）的安全风险。

**目标三（3.1 四维度登录失败保护 + 安全中心 remote 策略）**：补齐需求 §3.1 中 L2 延后的多维度失败统计与封禁；**登录失败保护策略 P0 即实现完整 remote**（Platform CRUD + Inner Feign + Auth 侧 Resilient 降级），不仿 L2 account lockout「仅土台、remote 后续」节奏。

| 维度 | 机制 | 达阈值动作 |
|------|------|-----------|
| IP | Redis 滑动窗口 | 网关 `TempBlockStore` 临时封禁 |
| 设备 | Redis 滑动窗口（`In-Ca-Sig`） | 同上 |
| Client | Redis 滑动窗口（OAuth2 `client_id`） | 同上 |
| 账号+IP | Redis 复合键滑动窗口 | 同上（不叠加账号 lockout） |

并修复 L2 已知缺口：`attemptWindowMinutes` 账号失败计数滑动窗口重置。

**核心原则**（承接 Roadmap 横切约定）：

- **策略 vs 运行时分离**：可 remote/LKG/Nacos 降级的是**策略参数**（阈值/窗口/TTL）；Redis 失败计数与临时封禁状态不可「降级为空」。
- **主链路不阻塞**：登录失败计数与封禁判定异步或轻量 Redis，Feign/事件上报失败不阻断认证响应。
- **与 L2 lockout 独立**：账号 lockout remote 弹性与安全中心 lockout 策略表**不在本 change**（见范围「不包含」）。

## 依赖

| 前置闭环 | 说明 |
|----------|------|
| R-2026-007 网关限流与安全策略 | Filter 链、SDK 四域、Platform CRUD、As-Built 见 [GATEWAY-RATE-LIMIT.md](../../../../docs/modules/security-center/GATEWAY-RATE-LIMIT.md) |
| L2 账号保护 | 账号维度 lockout 已闭环；本 change 补 IP/设备/Client/账号+IP |
| L3 统一安全事件中心 | ACCESS 类事件上报；本 change 扩展登录失败相关 event_type |

## 范围

**包含：**

- `ingot-gateway-rule-client`：`ResilientSnapshotFetcher`、LKG、Nacos 地板、`PolicySourceHolder` + Actuator。
- Nacos 三环境 `in-service-gateway.yml`：SDK 域开关 + remote + 事件上报。
- migration `011`：登录路径 endpoint group + 限流种子规则；PMS/Member/Security 路由 SDK 等价规则。
- 移除旧版 `RequestRateLimiter` 路由 filter（迁移验证通过后）。
- 新建 `ingot-security-access`（core + adapter）：四维度登录失败保护 Service、Redis 计数、`LoginFailurePolicyLoader`（local + **remote 完整实现**）。
- **安全中心登录失败策略**：DB 表 `login_failure_protection_policy`、Platform CRUD API、Inner Feign、`SecurityPolicyDomain.LOGIN_FAILURE_PROTECTION` 失效广播。
- **Auth 侧策略弹性**：`ResilientLoginFailurePolicyLoader`（`remote → LKG → Nacos 地板`），镜像凭证 resilience，**非占位 stub**。
- 扩展 `AuthFailureDTO`（`clientId` / `deviceId`）与 `DefaultAuthenticationFailureHandler`。
- Auth `LoginEventListener` 或等价 listener 接线失败保护。
- 扩展 `ClientIdentity` + `RateLimitDimension.CLIENT` + `IpKeyType.CLIENT`（网关通用限流/名单对齐）。
- `RecordLoginUseCaseService`：`attemptWindowMinutes` 滑动窗口实现。
- 新 ACCESS 事件类型 + `RemoteSecurityEventService` 上报。
- 新建 `specs/current/security/access-protection/`（验收后）。

**不包含（非目标）：**

- **账号 lockout remote 弹性 + 安全中心 lockout 策略表**（L2 后续独立 change，见 [20260724-security-account-protection 后续跟踪](../../archive/2026/20260724-security-account-protection/README.md)）。
- **L6 挑战验证**：`ingot.security.challenge` SDK 启用与 `ingot.vc` 策略合并。
- **4.3 行为型防爆破**：同 IP 试多个不同账号的模式识别（阶段三风险规则）。
- **4.4 高级网络控制**：地域/VPN/IDC/代理识别。
- Platform 读侧 / 安全概览、历史回填、告警联动、MQ 总线。
- `dry_run` 影子模式、HTTP Method 参与 Sentinel 编译（已知限制，后续 change）。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [Platform API — 前端对接文档](./PLATFORM-API.md)

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
