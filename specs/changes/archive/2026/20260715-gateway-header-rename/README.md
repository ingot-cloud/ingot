# 网关自定义 Header 命名统一

> 状态：completed

## 元数据

- Change ID：`20260715-gateway-header-rename`
- 领域：`gateway` / `security`
- 负责人：jy
- 创建日期：2026-07-15
- 目标发布日期：2026-07-15

## 目标

将 Ingot 平台自定义 HTTP Header 统一为 `In-*` 命名体系，与现有 `In-Inner-From` 对齐；同步重命名 Java 常量字段，消除文档与代码间 `X-Ca-Sig` / `X-In-Ca-Sig` 不一致。

## 范围

**包含：**

- `HeaderConstants` 三个 Header 值与两个常量名重命名
- 全仓库 Java 引用与 JavaDoc 更新
- 网关限流、BFF 指纹、安全策略相关文档更新

**不包含：**

- 旧 Header 向后兼容回退
- `authorization-view` 前端源码改造（由文档驱动）
- `specs/current/` 基线更新（验收归档时处理）

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-07-15
- 关联提交或 PR：工作区本地实施（待提交）
- 更新的 current capability：`specs/current/gateway/header-conventions/`
- 与原设计的差异：无；按 DESIGN 完整实施，未添加旧 Header 向后兼容
- 取消原因：—
