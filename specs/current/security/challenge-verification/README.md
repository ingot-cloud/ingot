# 挑战验证（412 + PassToken）

> 能力域：`security` / `challenge-verification`

## 摘要

L6 已验收闭环：图形 / 滑块验证码从 `ingot.vc.verifyUrls` 随业务请求带码，收口为网关 **HTTP 412 `CHALLENGE_REQUIRED` + PassToken**。触发策略与限流 / 黑名单同一套 `local | remote`；滑块引擎参数只在 `ingot.vc.image.*`。

1. **ALWAYS**：匹配路径且无有效通行证 → 412，请求不到下游。
2. **ON_RATE_LIMIT**：Sentinel 拦住后若匹配策略则 412，否则 429。
3. **PassToken**：验码成功签发；消费时 scope 取请求头，且路径必须被该 scope 策略覆盖。
4. **登录种子**：`POST /bff/auth/login` 走 `ALWAYS` + `SLIDER`（`scope=login`）。

## 边界

- **含**：挑战 SDK 执行面、412 契约、PassToken Redis、captcha 与 OTP 拆分、废弃 `verifyUrls` 作为安全触发、Platform 写入校验、登录种子与 Nacos 地板。
- **不含**：MFA、二次确认、挑战记录查询、验码失败拉黑（`challengeFailureLimit` / `blockTtlSec` 管理面保留、执行面不读）、PassToken 绑客户端 IP、短信 / 邮箱作为挑战类型。

## 所有者

- 策略 SDK：`ingot-framework/ingot-gateway-rule-client`
- 网关执行：`ingot-service/ingot-gateway`（`ChallengeFilter`、`PassTokenStore`、`CaptchaVCProcessor`）
- 滑块引擎：`ingot-framework/ingot-verification-code`
- 管理面：`ingot-service/ingot-security`

## 关联模块

| 职责 | 路径 |
|---|---|
| 412 组装 | `ChallengeResponses` |
| 策略匹配 | `ChallengePolicyService` / `CompiledChallengePolicy` |
| 前端契约（归档） | `specs/changes/archive/2026/20260827-security-challenge-verification/PLATFORM-API.md` |
| 运维 | `docs/modules/security-center/GATEWAY-RATE-LIMIT.md` |
| E2E | `test-case/security-policy-e2e.md` |

## 文档索引

- [SPEC](./SPEC.md)：执行模型、配置、Redis、契约、已知限制
- 配置落点：[config-governance](../config-governance/SPEC.md)
- 来源变更：`specs/changes/archive/2026/20260827-security-challenge-verification/`
