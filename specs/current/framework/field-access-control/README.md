# 公共字段访问控制

> 能力域：`framework` / `field-access-control`
> 验收：2026-10-10，负责人 jy 已确认人工验收完成。

资源目录集中定义逻辑字段，业务 DTO 和接口通过注解绑定实际属性与操作。公共执行层负责逐对象隐藏、脱敏及编辑/筛选门禁，IAM 提供策略和角色权限求值。

关联模块：`ingot-commons`、`ingot-access-control`、`ingot-iam`；首轮接入平台/租户成员、通讯录、关联返回、导出与 `examples/iam-ops`。字段控制继续受业务动作权限、对象归属和数据范围约束。

- [当前规则](./SPEC.md)
- [业务接入说明](../../../changes/archive/2026/20261010-framework-field-access-control/INTEGRATION.md)
- [接口契约摘要](../../../changes/archive/2026/20261010-framework-field-access-control/API.md)
- [来源与验收记录](../../../changes/archive/2026/20261010-framework-field-access-control/README.md)
- [统一分层缓存](../layered-cache/SPEC.md)

前端对应基线位于 `ingot-admin/specs/current/common/field-access-control/`。
