# 公共字段访问控制接口契约

资源身份：ResourceKey(domain,applicationCode,resourceCode)。字段 key 是该资源内稳定逻辑键，实际 Java/JSON 属性由绑定注解映射，不按名字猜测归属。

- FieldCapability：key,label,visibilities,editable,filterable,mask（MaskSpec）；不含 sortable。能力开关是目录上限，只有对应 action 绑定后才有效。
- FieldOperations：editable,filterable；字段上下文返回 actionCode -> fieldKey -> FieldOperations。filterable 是策略获准且有效查询范围 FULL 后的实际结论。
- FieldAccess：visibility,editable；输出按实际对象计算，MASKED 可 true，HIDDEN 必 false。FieldAccess 不独立授予 action 或数据范围。
- MaskSpec：kind(PHONE/EMAIL/ALL/KEEP_EDGES/RANGE)、prefix/suffix/start/end；只文本支持，参数合法且不能因短值完整透出。
- 角色字段版本：可见性与操作定义分离，同一资源的一组逻辑字段一起冻结，缺失新增字段关闭。租户 FieldPolicyDraft 分为 visibility rules 和 operation rules。
- FieldPolicyDecision：按精确操作返回字段默认、上限、规则、operations、masks、mergeMode、expiresAt；派生结果继承配置来源最早绝对到期，缺少期限拒绝。
- FieldBindingManifest：资源、版本、绑定属性和精确 read/write/filter 操作；未接入不能授权，响应不含原值。

## 业务上下文

GET /v1/platform/members/context 更新为通用字段上下文；补齐 GET /v1/tenant/members/context、GET /v1/directory/context、GET /v1/ops/incidents/context。受原业务 read/create 准入保护，不能代替写权限。

目录继续使用既有 resource create/update 接口保存自定义字段；增加清单读取用于展示绑定状态。未绑定允许保存，能力发布必须验证实际绑定。

## 写入与筛选

PATCH：expectedVersion 保留；未提交属性不修改、显式 null 清空 nullable 属性；实际键由框架收集，非可写、未知、只读都拒绝。不能通过 UI 隐藏代替服务器校验。前端 MASKED 编辑为空且只发送实际变更，不按占位文本判断状态。

筛选：仅已有业务条件按绑定校验；不可用返回403，不静默去除后执行 SQL/count。非法参数400，版本409，授权/清单依赖不可用503。

内部授权 v2 同步新字段结构；客户端写操作始终 fresh，不接受 preview 结果。跨服务 manifest 使用固定内部端点和服务身份签名。
