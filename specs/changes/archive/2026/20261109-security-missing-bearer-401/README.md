# 受保护接口的无令牌响应

> 状态：completed

用户于 2026-11-09 明确要求修复无令牌请求的 `500 invalid_token`，期望响应为 `401`。本机 IAM 直连和经网关均复现了问题。此次独立修复共享资源服务器的会话过滤器。

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [实施与验收任务](./TASKS.md)
- [问题复现](../20261109-iam-versioned-shared-catalog-cache/TARGET-ACCEPTANCE.md)
- [会话安全基线](../../../../current/security/session-safety/SPEC.md)

已在共享过滤器复用现有 Bearer 认证响应入口。22 项定向测试通过，IAM 打包、HTTP 契约和包边界检查通过。本机 IAM 直连及经网关复测通过。

共享安全模块全量运行 67 项，66 项通过，`InUserIdentityTest.callerCannotMutateBoundDepartmentList` 失败。该用例直接构造 `InUser` 并校验部门列表的不可变性，相关实现和测试文件与 HEAD 一致。

## 完成记录

- 2026-11-09 在本机分别直连 IAM 与经网关请求 `GET /v1/me/bootstrap`。无令牌与格式错误的 Bearer 四个请求均返回 HTTP 401、`invalid_token` 和 Bearer challenge，IAM 库 SELECT 增量为 0。
- 使用平台与租户测试会话，通过 IAM 直连及网关读取 `bootstrap` 均返回 HTTP 200。
- 关联实现提交为 `07d98953`。实现与设计一致。已更新[会话安全基线](../../../../current/security/session-safety/SPEC.md#3-resource-server-校验)。
