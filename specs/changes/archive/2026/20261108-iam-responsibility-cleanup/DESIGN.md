# 设计

## 成员与策略

PolicyWriteRepository 迁入 infrastructure.persistence，命名 FieldPolicyRepository。其 Mapper 和读写方法原样保留。

MemberFacade 只对外聚合成员用例；MemberQueryService 保留读取/投影，MemberProfileCommandService 承担创建、资料和角色变更及草稿预览。写入返回投影复用查询装配方法，不调用需要额外读权限的公开 get。原事务边界与锁顺序不变。

MemberRepository 聚合持久化接口；MemberQueryRepository 仅普通读取，MemberWriteRepository 承担 DML、行锁和关系变更。新增成员复制联系方式复用查询 Repository，写实现不反向依赖应用层。测试专用构造在 fixture 适配，不新增生产兼容构造器。

## 目录与字段策略

CatalogRepository 改为纯委托；按应用、资源、操作、菜单、套餐分别持久化，purgeApplication 的跨表操作保留原顺序并独立成清理实现，事务仍由原应用用例承担。

字段策略保持 FieldAccessEvaluator 入口及公共载荷，先提取默认引用/配置快照加载，再分离纯字段投影与操作约束。避免在缓存 loader 中回读热缓存，不改变请求内复用和 fresh 校验。拆分只转移原方法体，必要的共享规则用单一协作者表达。

## 初始化边界

当前 manifest 已生成 ingot_iam.sql，并通过007预置 platform、BCrypt(11111111)、must_change_password=TRUE。此代码变更只检查该事实与改密链路；是否从新项目表清单移出历史迁移专用表等待用户选择。现有库、历史升级与迁移工具不变。

统一验证入口：ingot-iam-provider 的 Gradle check 依赖 Python3 标准库执行架构和契约检查；Java SQL 防护继续由 test 覆盖。保留已有 MemberCommandService 的生命周期职责，资料/角色变更独立命名 MemberProfileCommandService。

事务装配：MemberProfileCommandService 使用显式构造器从原 PlatformTransactionManager 创建 TransactionTemplate，保留同一事务管理器语义；不要求使用方新增事务模板 Bean。已有生命周期命令不变。
