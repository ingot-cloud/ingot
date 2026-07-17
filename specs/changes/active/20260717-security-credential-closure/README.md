# 凭证安全收口（L1）

> 状态：implemented（代码完成，待运行时/集成验收后归档）

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260717-security-credential-closure` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-07-17 |
| 目标发布日期 | TBD |

## 目标

把框架层已成熟的密码引擎（强度 / 历史 / 过期）补齐为登录 / 改密的**完整闭环**，消除四个「已实现未接入」缺口：

1. 密码过期宽限期次数 `decrementGraceLogin` 无调用方，登录不扣减。
2. 初始密码缺少独立策略（随机生成 / 统一默认、短期有效、用后失效）。
3. `password_expiration.force_change` 列已存在但领域模型未映射，语义与 `mustChangePwd` 割裂。
4. 软过期宽限期内未限制访问范围（应仅允许改密相关接口）。

并确认凭证闭环覆盖全部用户类型（ADMIN / Member），且降级模式（`local`）下策略可经 Nacos 动态刷新。

## 范围

**包含：**

- 宽限期扣减接入登录成功链路。
- 初始密码策略能力（生成方式、有效期、用后失效、首登强制改密）。
- `force_change` 字段与领域模型对齐，统一 `mustChangePwd` 语义。
- 软过期宽限期内访问范围限制（仅改密相关接口）。
- Member 侧凭证策略装配与登录判定对齐。
- 凭证策略 `local` 模式 Nacos 动态刷新校验与补齐。

**不包含：**

- 凭证风险（需求 2.5，弱密码识别 / 泄露库）——归后续风险控制闭环。
- MFA / 二次确认（需求六）。
- 账号登录失败锁定的 Member 扩展——归 L2 账号保护闭环。
- 统一安全事件中心（需求九）——归 L3。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：代码完成 2026-07-17（待验收）
- 关联提交或 PR：TBD
- 更新的 current capability：待验收后更新 `specs/current` 凭证安全基线
- 与原设计的差异：
  - D2 强制改密拦截由「网关拦截」改为「方案 B 受限 scope」，且确认现网已由 `IdentityUtil` + `INIT_PASSWORD` scope 实现，无需新增代码。
  - 「无 DDL 变更」结论修正：`ingot_core.password_expiration` 实际缺 `force_change` 列，已补基线 + 迁移 008 + 回滚。
  - 初始密码 `validHours` 硬超期登录拦截拆为后续增强。
  - Member 完整凭证持久化（credential-data + `ingot_member` DDL）拆为后续独立 change，本闭环仅域级对齐。
- 取消原因：—

## 后续跟踪（拆出项）

1. 初始密码 `validHours` 登录硬超期拦截（需登录期读取 `mustChangePwd + passwordChangedAt`）。
2. Member 完整凭证持久化：Member provider 增 `ingot-security-credential-data` 依赖 + `ingot_member` 建 `password_history`/`password_expiration`（DDL + 回滚）。
