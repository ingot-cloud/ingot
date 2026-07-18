# 安全中心分阶段落地 Roadmap

本文件是 [安全中心服务需求](./security-center-service.md) 的实施路线图，把庞大的安全能力拆成可独立验收的「闭环」，按 SDD 工作流一个一个推进。它只负责**排序、跟踪与横切原则约定**，不替代各闭环的 `specs/changes/active/` 需求与设计。

- **状态**：in-progress
- **动机**：需求文档覆盖 12 个安全域，无法一次交付；需要按当前代码成熟度与依赖关系排序，逐个闭环落地。
- **涉及模块**：
  - [ingot-service/ingot-security](../../../ingot-service/ingot-security)（安全中心服务）
  - [ingot-security-credential(-data)](../../../ingot-framework/ingot-security/ingot-security-credential)（框架密码引擎）
  - [ingot-account-domain](../../../ingot-framework/ingot-account-domain)（账号锁定、安全事件）
  - [ingot-auth](../../../ingot-service/ingot-auth)、[ingot-gateway](../../../ingot-service/ingot-gateway)（执行面）
  - [ingot-verification-code](../../../ingot-framework/ingot-verification-code)（挑战验证）
- **ROADMAP 关联**：[R-2026-007 网关限流与安全策略](../ROADMAP.md)（已完成执行面）、R-2026-022 凭证安全策略扩展

---

## 一、现状基线（截至 2026-07-17）

已具备、可直接复用：

- 安全中心服务：凭证策略 CRUD、网关策略中心（限流 / 黑白名单 / 路径分组 / 挑战策略 / 违规升级 / 封禁审计）、内网快照下发（`RemoteSecurityPolicyService`）、凭证策略读取（`RemoteCredentialService`）、跨节点缓存失效（`InvalidationBus`）。
- 框架密码引擎：强度 / 历史 / 过期三类策略 + 持久化（`password_history`、`password_expiration`）完整，支持 `local` / `remote` 双模式与多级缓存。
- 账号保护：`account_lock_state`（失败计数 / 锁定）、`account_security_event`（安全事件表）已存在，**仅 ADMIN 闭环，Member 未接入**。
- 会话 / Token：Redis 在线 Token、强制下线 API 已有。
- 验证码：能力独立，**未接入登录链路**。

主要缺口：安全概览、统一安全事件中心、通用安全审计、告警、风险控制、处置管理、MFA / 二次确认、登录设备、并发会话策略、异常登录、策略视图；以及若干「已实现未接入」点（`decrementGraceLogin` 无调用方、初始密码无独立策略、`password_expiration.force_change` 未映射、Member 侧凭证策略未对齐）。

---

## 二、横切架构原则：中心化 + Nacos 降级

需求文档每个模块都写明了「降级方式」。统一为一套双模式架构，**每个可降级能力都必须同时具备两条路径**：

| 模式 | 触发条件 | 数据来源 | 生效机制 |
|------|----------|----------|----------|
| 中心化 | 部署安全中心 | 安全中心服务集中管理 | Feign 快照下发 + `InvalidationBus` 跨节点失效 |
| 降级 | 不部署安全中心 | Nacos 配置 | `@ConfigurationProperties` + `?refreshEnabled=true` **动态刷新** |

**统一开关**：复用凭证模块已有的 `ingot.security.credential.policy.mode = local | remote` 范式，后续各能力沿用同一 `mode` 思路（`local` 走 Nacos，`remote` 走安全中心）。

**Nacos 动态刷新落地约定**：

1. 降级配置类使用 `@ConfigurationProperties`，在 `spring.config.import` 以 `?refreshEnabled=true` 引入对应 Nacos dataId。
2. 配置变更后运行时对象必须即时感知：优先 `@RefreshScope`，或监听 `RefreshEvent` / Nacos 变更事件重建策略对象，避免策略在启动时固化导致刷新不生效。
3. 每个闭环的 `DESIGN.md` 必须显式给出：可 Nacos 降级的字段、对应 dataId、动态刷新验证方式（改配置 → 无重启 → 行为变化）。
4. 不可降级的能力（依赖 DB / Redis 聚合，如安全大盘、审计查询、风险评分）在文档中标注「不可降级」，不部署时该能力不可用，但不影响主链路。

---

## 三、闭环序列与状态跟踪

每个闭环 = 一个独立 SDD active change，验收后更新 `specs/current/` 再归档。**严格串行**：一个闭环归档后再开下一个。

### 阶段一：基础能力闭环

| 闭环 | 名称 | 对应需求章节 | 关键内容 | 可降级 | 状态 | Change |
|------|------|--------------|----------|--------|------|--------|
| L1 | 凭证安全收口 | 二（2.2 / 2.4） | 初始密码、宽限期扣减、force_change 对齐、Member 对齐 | 是（策略字段） | done | `specs/changes/archive/2026/20260717-security-credential-closure`；current `security/credential-security` |
| L2 | 账号保护全用户闭环 | 三 | 失败计数 / 锁定 / 安全事件从 ADMIN 扩到 Member | 是（阈值 / 时长） | planned | 待建 |
| L3 | 统一安全事件中心 | 九 | 通用安全事件模型 + 上报 / 存储（安全中心侧） | 部分（记录开关可降级，聚合不可降级） | planned | 待建 |
| L4 | 访问防护补全 | 四 | 现有网关策略中心的防爆破 / 执行面收口 | 是（阈值 / 名单） | planned | 待建 |
| L5 | 会话安全 | 五 | 在线会话 / 并发会话 / 强制下线统一管理面 | 部分（并发策略可降级，统一管理不可降级） | planned | 待建 |
| L6 | 挑战验证 | 六（6.1） | 图形验证码接入登录 / 敏感接口 | 是（触发策略） | planned | 待建 |

### 阶段二：运营与展示

安全概览大盘、策略后台、登录设备、挑战记录、安全审计、策略视图。

### 阶段三：风险与处置

风险规则 / 等级 / 决策 / 命中、动作模板 / 执行记录、MFA、二次确认、安全告警。

### 阶段四：高级能力

复杂动作编排、风险画像、设备 / 地域风险、高级告警、合规审计、多租户 / 应用 / Client 精细策略。

---

## 四、SDD 工作流（每个闭环都遵循）

1. **步骤 A（先出文件供审阅）**：从 `specs/templates/change/` 创建 `specs/changes/active/<YYYYMMDD>-security-<feature>/`，生成 `README.md`（状态 `draft`）、`REQUIREMENTS.md`、`DESIGN.md`（含 Nacos 降级与动态刷新设计）、`TASKS.md`。生成后**暂停交负责人阅读确认**。
2. **步骤 B（审阅通过后实施）**：确认无误 → 状态转 `approved` → 按 `TASKS.md` 逐项实现 → `validating` → 更新 `specs/current/` → 归档。

---

## 五、状态维护

- 每完成一个闭环，更新本文件第三节对应行的「状态」与「Change」链接。
- 完成后在 [ROADMAP.md](../ROADMAP.md) 追加或更新对应 `R-YYYY-NNN` 条目。
- 状态取值：`planned` / `in-spec` / `implementing` / `done` / `cancelled`。
