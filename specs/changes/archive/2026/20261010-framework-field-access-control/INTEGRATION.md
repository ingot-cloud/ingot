# 业务服务接入

本说明对应此次实施，验收完成前与 active change 一起维护。可编译的完整样例见 `examples/iam-ops`。

## 资源与字段目录

1. 注册业务资源及精确操作，身份必须包含 domain、applicationCode、resourceCode。资源提供者继续负责对象存在性、归属及既有查询范围，不由字段框架生成 SQL。
2. 在资源目录集中配置稳定逻辑字段、可见性、编辑/筛选上限及脱敏规则。未绑定字段可以保存，但有效能力关闭；角色固定版本缺失的新字段保持 HIDDEN/无操作能力。
3. 注解绑定实际 DTO，不把每个接口的实体名称当成另一套字段目录。多个 DTO、列表与详情可以共同绑定一个逻辑字段。

```java
public record Detail(
        @PublicField String id,
        @FieldBinding(key = Fields.PHONE) @JsonProperty("contactPhone") String aphone) { }
```

`FieldBinding` 可指定完整资源，也可继承 `FieldControl` 的资源。READ 为默认用途；写入、筛选分别声明 WRITE/FILTER。固定公开元数据使用 `PublicField`。未分类属性、冲突、重复输入绑定、受控原始类型、嵌套循环在启动时失败；可隐藏的数值必须使用可空包装类型。

## HTTP 与直接调用

在 Controller 的实际接入方法声明 `FieldControl`，使用编译期常量填写资源与精确 action，`valueType` 指明 DTO，`use` 指明用途。同一方法可以重复声明 READ 与 FILTER。Spring 路由初始化后自动编译，纯清单记录实际 JSON 属性与逻辑字段映射。

READ 先完成业务动作准入与对象范围，再批量加载可信对象事实，调用 `FieldPolicyProcessor.forAction`、`access` 生成行级快照，使用 `FieldProjectionEngine.projectRecord/project/projectBatch` 输出安全新实例或 JSON。原对象不修改；请求外任务必须显式传入不可变上下文。序列化和批次投影函数不能再查询数据库或 RPC。普通 bean 使用 JSON 投影；不可变 record 使用新实例投影；集合、嵌套和分页由业务取出其已加载内容后统一投影。

WRITE 普通 DTO 的真实 JSON 在反序列化之前检查；自定义反序列化器必须保留实际键，使用 `submitted` 收集，或直接对原始 JsonNode 使用 `FieldInputCollector`。嵌套单对象递归验证；批量修改命令逐对象收集并执行最终门禁，不将不同目标合并成单次授权。

写业务必须按以下顺序执行：

1. 开启业务事务，加载/锁定真实目标，核对版本及对象归属。
2. 收集本次实际提交的逻辑字段，包含显式 null。
3. 调用 `FieldWriteExecutor.require(resource, action, actualTarget, submitted)`。执行器内部重新获取当前事实，不接收浏览器预览决策。
4. 在同一事务执行业务校验、按实际键更新及审计。缺键不更新；null 仅清空业务允许的 nullable 字段。

注解负责绑定和分类。服务开发者仍需把上述读投影、SQL 范围、最终写入和筛选门禁接入实际业务路径，不能仅加注解就省略执行器。

FILTER 按实际输入键收集，在查询和 count 之前验证操作能力以及整份有效查询范围的 FULL 可见性。禁止条件返回403，未知参数400；不能静默忽略。对范围敏感的业务服务使用带 `queryScope` 的 `requireOriginalLookup`。固定排序继续由业务 SQL 决定。

## 前端

只读视图消费安全响应与行级 `fieldAccess`。HIDDEN 隐藏标签和输入；MASKED 显示服务端掩码，允许编辑时草稿为空。使用共享 `fieldTextDraft/fieldTextPatch`，由实际输入事件标记 dirty；未触碰不发送，明确清空 nullable 字段发送 null。不得通过 `***` 等文本猜测脱敏状态。

筛选使用全局 `fieldOperations[action][field]`，不从当前页推断。身份、权限版本、场景变化后先关闭旧上下文，剔除禁止条件并重置页码/查询键；后端仍执行同样的门禁。新增字段界面只能配置能力，不生成业务输入控件或 SQL。

## 清单和缓存

跨服务调用固定 `/inner/iam/field-bindings/manifest`。服务发现名来自 IAM 服务端白名单；独立 manifest 密钥至少32字节，两端分别配置 `remote-registration[].manifest-secret` 和 `ingot.iam.field-manifest.secret`。签名携带固定 purpose、允许的调用服务及完整资源，不需要伪造用户登录，也不传原值或用户 URL。

配置接入统一 LayeredCacheBuilder，授权读取无 LKG/地板放行。配置来源携带绝对期限，派生策略和远端决策继承最早期限，叠加缓存不重置30秒窗口。写入重新读取当前身份/分配/资源上限/租户策略，固定角色版本可缓存。提交后发布方先清本地缓存再广播；回滚不失效。

启动仅编译本服务注解并预热本节点资源和公共默认引用；不遍历所有用户、租户和对象。预热失败记录告警，受控请求拒绝，后续可重试。部署 IAM、SDK 消费服务和前端应使用同一字段契约；本次未执行目标数据库初始化或部署。
