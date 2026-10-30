# 设计

- 新增 TenantWorkbenchAPI、TenantWorkbenchService 与共享响应 TenantWorkbenchStatistics。
- 服务复用 SessionService.bootstrap 的可信上下文、授权操作与可见菜单。只允许 TENANT。恢复当前组织最小名称、头像；头像经 @OssUrl 签发。
- 成员、部门统计调用既有 Repository 的新增 COUNT 方法，分别复用 tenantFilter 与 scoped；部门增加 parentId 非空过滤。组 COUNT 与现有组列表一致按可信租户过滤。
- 无对应 read action 时跳过统计查询并省略字段；合法空范围返回 0；异常传播。可用应用由 bootstrap 可用应用与非隐藏 PAGE 菜单所属应用交集去重统计。
- 新增只读端点，保持旧契约兼容，无表结构迁移，不增加缓存或扩大权限。
- 验证成员多部门不重复、状态/租户/对象范围隔离、部门根排除、无权限不查库、隐藏页面与重复应用、平台身份拒绝。
- 2026-10-30 用户要求工作台置顶：租户应用 `iam-tenant` 的根目录 `org.iam.workspace` 使用 sort_order=0，排在组织管理（1）、权限与应用（2）、安全与合规（3）之前。生成器显式覆盖此排序，保持生成遍历顺序和保留 ID 不变。已有库通过 035 增量 SQL 定向修改该根目录排序并递增菜单版本；不修改其他菜单或授权，重复执行不再增版本。前端继续消费服务端排序。回退时恢复留存的原排序并递增当前版本，再重新加载菜单。
