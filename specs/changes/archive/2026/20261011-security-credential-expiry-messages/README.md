# 密码过期场景化提示与独立 Nacos 文案配置

> 状态：completed

## 元数据

- Change ID：`20261011-security-credential-expiry-messages`
- 领域：security
- 负责人：jy
- 创建日期：2026-10-11
- 验收与归档日期：2026-10-12
- 批准依据：用户在 Plan 模式确认独立配置文件方案，并于本轮明确要求实施完整计划。

## 目标与范围

初始／重置密码过期时说明原因并引导管理员重置；三类密码过期提示通过独立 Nacos 文件自定义和动态刷新。仅改变提示，不改变有效期、宽限、准入、强制改密、HTTP 状态或外部错误码。平台与租户共用 Auth 配置。无 DDL、前端管理页或其他登录失败文案扩展。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 状态记录

- 2026-10-11：review → approved；Plan 对齐已完成，用户明确要求实施；TASKS 决策完整。
- 2026-10-11：approved → implementing；开始实施。
- 2026-10-11：implementing → validating；实现及 62 个定向回归通过，IAM/Member/Auth/BFF 编译通过；等待实际 Nacos 与平台／租户人工验收。
- 2026-10-12：validating → completed；用户确认“测试成功，归档相关spec，并且提交代码”，结合自动化结果完成验收；更新 current 后归档。

## 验证结果

自动化验证及命令见 [任务记录](./TASKS.md)。三环境文案模板、配置键、Nacos 常量及 optional 刷新导入一致；已从本项目使用的 Nacos 依赖源码核实 `refreshEnabled` 参数与 optional 加载语义。JWT 定制器没有读取该内部 meta 字段。

2026-10-11 的实施轮未连接实际 Nacos 或执行真实账号登录。2026-10-12 用户确认测试成功并要求归档，作为 V3 验收完成的依据；用户未提供 Group、发布版本、账号或逐项日志，本记录不补造这些细节。

## 完成记录

- 完成日期：2026-10-12
- 关联提交或 PR：与本次实现和归档同一提交，标题为 `feat(security): 支持按密码过期场景自定义登录提示`；无 PR。
- 更新的 current capability：[security/credential-security](../../../../current/security/credential-security/README.md)、[security/config-governance](../../../../current/security/config-governance/README.md)
- 与原设计的差异：无
