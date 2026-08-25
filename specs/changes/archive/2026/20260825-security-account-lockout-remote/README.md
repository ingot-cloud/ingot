# 账号锁定策略 remote 模式

> 状态：completed

## 元数据

- Change ID：`20260825-security-account-lockout-remote`
- 领域：`security`
- 负责人：jy
- 创建日期：`2026-08-25`
- 目标发布日期：TBD

## 目标

补齐 `ingot.security.account.mode=remote`：安全中心按用户类型维护账号锁定策略，PMS / Member 经统一分层缓存链读取。去掉当前「remote 回退 local + WARN」占位。

## 范围

包含：

- 安全中心 `account_lockout_policy_config` 表、Platform / Inner API、Feign、失效域 `ACCOUNT_LOCKOUT`
- account-core seam 按 `userType` 取策略；local 仅在 `mode=local` 装配
- account-adapter：`L1 → L2 → Feign → LKG → Nacos 地板`
- 前端对接文档 `PLATFORM-API.md`

不包含：

- 安全中心前端页面实现
- 改锁定状态 / 失败计数落库语义
- 把 DEV/TEST/PROD 默认 `mode` 切成 `remote`（灰度另做）

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [前端 Platform API](./PLATFORM-API.md)

## 完成记录

- 完成日期：2026-08-25
- 关联提交或 PR：
- 更新的 current capability：`specs/current/security/account-protection/`、`specs/current/framework/layered-cache/`
- 与原设计的差异：无实质性偏离。`LockoutPolicy` 从 Lombok `@Value` 改为 record 以便 L2 Jackson 反序列化。
- 取消原因：
