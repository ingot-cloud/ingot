# 安全配置落点治理

> 状态：completed（已验收，已更新 current，已归档）

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260825-security-config-governance` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-08-25 |
| 目标发布日期 | TBD |

## 目标

把 `ingot.security.*` 按「共享策略 / 执行面开关 / 投递运行时 / 密钥」落到固定 Nacos dataId，共享文件只给真正的消费者 import。账号锁定相关前缀收口到 `ingot.security.account.*`。查找靠配置地图，不靠每个服务挂大全。

## 范围

**包含：**

- 前缀一次性改键：`account-lock-signal` / `account-lock-bff` / `account-lock-gateway` → `account.signal` / `account.bff` / `account.gateway`
- `account.signal` 与 PMS/Member/Security 的 `event.delivery` / `mysql` 收到 `in-security-policy.yml`
- 新建 `in-security-gateway.yml` 承接网关限流/黑名单/违规升级地板（不含各域 `enabled` / `mode`）
- 按消费关系补 BFF → policy、Gateway → gateway-policy；Auth 不挂 policy
- 验收后新增 current `security/config-governance`，并回写相关 SPEC 的 dataId 描述

**不包含：**

- 不改 lockout / 限流 / 凭证等策略数值
- 不改 `mode=remote` 生产默认
- 不把所有安全配置塞进单一文件，也不让每个服务 import 同一份大全
- 不拆 `account-core` → credential 的传递依赖
- 不把 `account.bff` / `account.gateway` 并入 `AccountDomainProperties`

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-08-25
- 关联提交或 PR：
- 更新的 current capability：`specs/current/security/config-governance/`
- 与原设计的差异：无
- 取消原因：
