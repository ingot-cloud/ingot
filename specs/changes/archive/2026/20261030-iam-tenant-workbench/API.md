# 租户工作台统计接口

`GET /v1/tenant/workbench/statistics`，前端经既有 IAM 前缀调用。响应 `R<TenantWorkbenchStatistics>`，无请求参数。身份从服务端当前会话恢复，仅租户域可调用；不接受客户端指定 tenantId。

| 字段 | 类型 | 语义 |
| --- | --- | --- |
| tenantId | string | 当前组织 ID |
| tenantName | string | 当前组织名称 |
| tenantAvatar | string? | 当前组织头像，OSS 时效链接；空时省略 |
| memberCount | integer? | 当前成员 read 范围内未移出成员，含暂停；无操作资格时省略 |
| departmentCount | integer? | 当前部门 read 范围内普通部门，排除根部门；无操作资格时省略 |
| groupCount | integer? | 当前租户用户组；沿用组列表 ALL 读取语义，无读取资格时省略 |
| applicationCount | integer | 当前可用应用中具有可见、非隐藏页面入口的应用去重数量 |

成员、部门的 COUNT SQL 分别复用现有列表的类型化对象范围谓词，不读取全部行计数，不累计部门人数。可用应用复用 bootstrap 的开通、人群、页面操作过滤结果，不统计不可进入的应用。

当前成员可读取自己所在组织的最小名称和头像，不返回拥有者联系资料。无权限与合法零严格区分；依赖或数据库失败传播，不伪装为零。错误沿用身份无效、ActionDenied、AuthorizationUnavailable 等现有契约。统计接口无新增数据库表、配置、权限码或表结构迁移；工作台导航排序的数据调整见 DESIGN.md。

待办不新增接口，不发送请求，不填充示例业务或未接入时的零计数。
