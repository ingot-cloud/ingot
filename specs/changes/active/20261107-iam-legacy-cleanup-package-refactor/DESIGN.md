# Design

## 目标分层

provider 使用 `interfaces.http/inner`、`application.<业务域>`、`domain.<业务域>`、`infrastructure.persistence/cache/security/social/storage/serialization` 和 `configuration`。

启动类 `com.ingot.cloud.iam.InIamApplication` 保留在模块根包，作为整体装配入口，不迁入 application 业务层。根包必须且仅有该启动类；默认组件扫描覆盖 IAM 所有业务和装配子包，构建产物的 Start-Class 保持这一全限定名。

依赖为 interfaces→application；application→domain/infrastructure/api；infrastructure→domain/api/基础框架；domain→必要基础契约；configuration 负责装配。domain 不读 SecurityContext、数据库或 Redis。基础模型独立后消除 infrastructure→application；不新增可选必需依赖或用 Lazy 掩盖循环。

## 类型处置

API 现状127文件：保留21、迁移9、删除97，替代类型新增另计。保留七RPC、三消费端装配、四授权快照模型、LoginRecordDTO/InnerUserDTO、DictQueryDTO/DictItemVO及三个字典契约枚举。快照 roleBindings 的传递依赖不能删除。

四有效实体迁入持久化层并命名为 IdAllocationEntity、DictionaryEntity、SocialConfigEntity、AccountSocialBindingEntity，表映射不变。DictConvert、DictSortDTO、DictTreeNodeVO 进入字典用例；两注册模型进入 Member provider 并合并其已有重复定义。

SysUser 先替换为内部 AccountSocialPrincipal；SysTenant RPC 使用独立 TenantDetailsDTO，保留原字段和空值语义。SysSocialDetails RPC 使用独立 SocialDetailsDTO，保留内部密钥边界。字典、发号、社交管理入口使用独立命令/响应，不暴露实体。

旧20实体及对应20 Mapper、18组Service/Impl、现存同名XML退出；旧BO、转换器、用户/角色/部门DTO/VO及专用枚举按清单退出。Java历史迁移分析器、其模型和旧PermissionMatcher及专用测试删除，处置依据保留在本change；离线迁移工具不改。

commons 的当前共享生产消费者及字段闭包保留60类型；IAM内部260类型迁入provider，孤立MigrationBatchStatus/MigrationDisposition两个Java枚举退出（不影响对应历史表）。精确映射和命名以CLASS-INVENTORY及其JSON清单记录，后续新增类型不由旧数量约束。

## 命名与职责

Account/Member/Identity按业务主体区分；Application表示目录应用；RoleVersionApplication表示固定版本应用。平台授权Editor拆成候选查询和绑定校验；TenantLive*改为TenantAuthorizationEvaluator及TenantAssignment/TenantDelegation查询/命令服务；IamDetails→ResourceDetailFactory；IamCapabilities→ActionCapabilitySnapshot；IamAdmission→AuthorizationAdmission；BizIdGen有效部分→ApplicationIdGenerator；ResourceAccess→ResourceScopeAccessService。

RoleApplication系列统一RoleVersionApplication命名，业务端口为RoleVersionApplicationPort、平台实现为PlatformRoleVersionApplicationAdapter。CatalogService拆应用/资源/操作/菜单/套餐服务，ApplicationBundleService保留整包事务；RoleService拆查询/定义/版本，AssignmentService拆查询/写入/版本调整。先做机械迁移并验证，再拆职责。

角色版本应用进入application.roleapplication，消除role与assignment的反向编排。范围资源枚举、身份、范围、事件和缓存载荷进入独立domain模型。租户分配/委派共用角色解析、角色锁、绑定校验，不互相调用完整写服务。

## 兼容、缓存与回滚

正式HTTP/RPC数据契约、配置键、Bean限定、队列/事件协议、事务锁顺序及数据库映射保持。源代码引用、XML namespace/type、配置class名、测试和生成工具同步迁移；历史Spec不做全局替换。

RedisTemplate的类型元数据和Spring Cache的JDK序列化包含Java类名。对受更名影响的持久缓存逐项验证，无法读取旧类型的缓存使用受限命名空间版本，避免旧新节点互相写入；不清空整个Redis、不改变会话及LKG事实。分层缓存JSON可兼容时保持协议。新旧载荷、TTL及广播另测。

部署使用配套重建制品；首次统一切换。回退切回配套制品和对应缓存命名空间，数据库不需回退。实际发布、缓存删除和真实库操作不在本轮执行。

## 关联模块与测试

修改IAM、commons、Member、Security、social-common/wechat、dict-client及必要契约工具。Auth/BFF/access-control/mybatis-scope/examples编译及装配回归，仅调整必要引用。前端仅清理已退役PMS调用子图和专用类型/组件，正式页面及共享组件保留。

模型契约测试随模型迁移；共享测试不反向依赖provider。工具更新Schema输出和控制器源定位，不修改数据库升级脚本。测试覆盖类边界、公共字段依赖、启动/Mapper扫描、RPC、社交与有效辅助功能，保留已有授权及事务测试。数据库文件哈希与diff用于只读边界验证。

实施依赖核对：公开快照AuthorizationResourceRuleDTO字段引用框架DataScopeTypeEnum，故API保留data-mybatis-common依赖作为现有字段契约来源；不为减少依赖数量而拆分基础框架或改变该字段类型。API源码仍不保留实体/Mapper/MapStruct。

## 实施细化

CatalogFacade、PlatformRoleFacade、PlatformAssignmentFacade、TenantAssignmentFacade、TenantDelegationFacade 仅聚合跨用例调用；实际方法主体位于各查询/命令/版本等服务，共用 Operations 只提供兄弟服务使用的协作者和规则，不成为公共 API。租户公共规则使用 TenantRoleDefinitionResolver、TenantRoleLockService、TenantScopeBindingService。平台提交使用 PlatformScopeBindingValidator，与候选查询共用 PlatformScopeObjectResolver。

RoleVersionApplication 更名覆盖 HTTP 控制器、持久化实体/Mapper/Repository/投影和发布输入；表名、路由、operationId、事件 TYPE、配置键及指标名保持原协议。原校验消息键保留，因为其属于既有模板，不是 Java 类型引用。

关联前端退出150个专用文件，包含 platform 旧配置/用户/组织和 org 旧 contacts 调用子图，以及无消费者的公共 PMS 查询；变更记录在 ingot-admin 的 20261107-base-iam-legacy-cleanup。
