# Tasks

本 change 可在一个发布单元内完成，不拆 `phases/`。**draft / review 期间禁止改业务代码。**

## Phase 0 — 规格审阅（当前）

- [x] T0-1：负责人审阅 README / REQUIREMENTS / DESIGN / TASKS / PLATFORM-API；闭合 D1–D10 后将 change 转为 `approved`（D11–D14 实施中补齐）
  - 依赖：无
  - 验收：DESIGN 决策无待确认项；状态 `approved`（2026-08-27 开始实施，转 `implementing`）

- [x] T0-2：更新 [security-center-roadmap.md](../../../../../docs/requirements/themes/security-center-roadmap.md) L6 为 `in-spec` 并链接本目录
  - 依赖：本草稿落盘
  - 验收：L6 行 Change 指向 `specs/changes/active/20260827-security-challenge-verification/`

- [x] T0-3：在 [ROADMAP.md](../../../../../docs/requirements/ROADMAP.md) Next 增加 `R-2026-029` 挑战验证，状态 `in-spec`
  - 依赖：T0-2
  - 验收：Next 表存在 R-2026-029 且链接本 change

**暂停点**：T0-1 已通过，进入下列编码任务。

---

## 实施任务（approved 之后）

### A. 验证码引擎与 PassToken

- [x] T1：captcha 与 OTP 拆分（D4）
  - `ingot-verification-code`：image 的 get/check 不依赖 `AbstractVCProcessor` / `VCRepository`；SMS/Email 行为不变
  - 魔法值：`IMAGE`/`SLIDER`、路由 `image` 使用命名常量 / 枚举（D5）
  - 对外类型与 public 方法按 [Javadoc 规范](../../../../../docs/standards/Javadoc.md) 补注释
  - 依赖：T0-1
  - 验收：image 拉码/验码单测通过；OTP 既有路径可编译

- [x] T2：网关 check 签发 PassToken（D1 / D8）
  - 恢复并修正 `CaptchaVCProcessor.check`：anji 成功 + scope → `findByScope` → `PassTokenStore.issue`；响应含 PassToken
  - Redis 不可用或 scope 无策略 → 错误，禁止成功空 token
  - 无 scope Header → 不签发
  - 删除 `/auth/oauth2/token`、`pre_authorize` 的 `checkOnly` 特例（D6）
  - 修正 `ChallengeResponses` 示例与实现一致（`vcType=image`）
  - 依赖：T1
  - 验收：A2、A3、A10、A13 单测或组件测试

- [x] T2b：修复 PassToken 消费 scope（D10 / A17）
  - `ChallengeFilter`：`consume` 使用请求 scope，与签发 Redis key 一致
  - 删除「ALWAYS.scope 否则 default」分支；缺 scope 视为无效 token
  - ALWAYS 无效 token → 412；无 ALWAYS → 放行至 Sentinel（不打 `ATTR_PASS_TOKEN_OK`）
  - 单测：`scope=e2e-anon` 的 ON_RATE_LIMIT 消费成功并跳过 Sentinel；错 scope / 缺 scope 不跳过
  - 依赖：T2
  - 验收：A17；E2E TC-SP-012 重试须带 scope

- [x] T2c：路径绑定 + Header + 验码去 captcha（D11 / D12 / D14）
  - `CompiledChallengePolicy.matchByScope`；Local / Remote 委托编译索引
  - `ChallengeFilter` / check 只读 Header `In-Vc-Scope` / `In-Vc-Pass-Token`；路径不匹配不 consume
  - 412 `scopeParam` / `passTokenParam` 改为头名；check 成功体去掉 `captcha`
  - DESIGN D13：本轮不做 IP 绑定
  - 依赖：T2b
  - 验收：login token 打非覆盖路径不跳过限流；只带 query 视为无 token；A2 成功体无 `captcha`

- [x] T3：执行面拒绝 SMS/EMAIL 路由（D5）
  - `ChallengeTypes.toVcType` 或匹配层：非 IMAGE/SLIDER 不作为可执行挑战
  - 网关对 `/vc/**` 请求不做 ALWAYS 挑战（防御误配）
  - 依赖：T1
  - 验收：A11

### B. 策略启用与配置

- [x] T4：Nacos 三环境
  - `in-service-gateway.yml`：`challenge.enabled=true`；DEV `mode=local`，TEST/PROD `mode=remote`；`ingot.vc.verifyUrls` 清空（D7）
  - `in-security-gateway.yml`：写入 DESIGN 中的 login-always 地板（不写 enabled/mode）
  - Gateway `spring.config.import` 为 `in-security-gateway.yml` 与 `in-service-gateway.yml` 增加 `?refreshEnabled=true`（若该环境尚未带）
  - 依赖：T0-1
  - 验收：配置地图符合 config-governance；A7 配置侧完成

- [x] T5：DB 种子 `login-always`
  - migration（建议 `020_challenge_login_always_seed.sql`）+ rollback；同步 `databases/ingot_security.sql` 基线
  - `group_code=login-auth`，`scope=login`，`trigger=always`，`challenge_type=SLIDER`
  - 依赖：T0-1
  - 验收：新库与已有 011 分组的库插入成功；回滚可删

- [x] T6：local 动态刷新（A5 / A16）
  - 确认 `LocalPolicyEnvironmentRefreshListener` 对 `ingot.security.challenge.` 在 refresh 后 `evictAll`（`ChallengeAutoConfiguration` 已注册）
  - 验证：单测改策略 `enabled` 后 `evictAll`，登录路径 ALWAYS 匹配出现/消失；运行时改地板 `login-always-floor.enabled` → 无重启 → `POST /bff/auth/login` 412 出现/消失
  - 依赖：T4
  - 验收：A5、A16

### C. 管理面与文档

- [x] T7：Platform 校验
  - `validateChallengePolicy`：scope、类型、PassToken 数值、分组或路径、拒绝 `/vc/**`
  - L6 写入 `challengeType` 仅 SLIDER/IMAGE
  - 依赖：T0-1
  - 验收：非法 body 被拒绝；合法写入发 `CHALLENGE_POLICY` 失效；A4 可测

- [x] T8：文档与 E2E
  - [PLATFORM-API.md](./PLATFORM-API.md) 与实现对齐（T7 后回写差异）
  - 更新 `GATEWAY-RATE-LIMIT.md` PassToken 签发事实
  - 扩展 `test-case/security-policy-e2e.md`：登录 A1/A2；TC-SP-012 补 Header scope；A17
  - 清理 BFF `vcCode` 过时注释
  - 依赖：T2、T2b、T7
  - 验收：A12、A15、A17；注释与代码一致

### D. 收口

- [x] T9：验收后更新 current
  - 新建 `specs/current/security/challenge-verification/`
  - 改 access-protection「挑战未启用」；config-governance 前缀地图
  - roadmap L6 / R-2026-029 → `done`；本 change 归档
  - 依赖：V1
  - 验收：current 只含已上线事实

## 验证任务

- [x] V1：A1–A17
  - 依赖：T1–T8
  - 验收：REQUIREMENTS 验收标准全部勾选；至少覆盖登录 412 全链路、**非 default scope 的 ON_RATE_LIMIT 跳过限流**、local 刷新、remote 失效、Redis 失败、verifyUrls 已空

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
