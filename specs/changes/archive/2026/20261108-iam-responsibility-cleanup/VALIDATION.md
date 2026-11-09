# 验证记录

2026-11-08，状态 validating。验证执行时尚未提交，此代码阶段未修改业务库和数据库脚本；后续获准的新项目初始化变更由20261108-iam-clean-initialization独立记录。用户已于同日要求提交，实际环境验收状态保持。

## 实现

- PolicyWriteRepository 迁入 infrastructure.persistence，改名 FieldPolicyRepository；现有字段策略、选择器与默认版本读写保持。
- MemberFacade 聚合读取与资料/角色变更，实际实现为 MemberQueryService 与 MemberProfileCommandService。原 MemberCommandService 保留资格生命周期职责。创建、写入返回安全投影通过内部装配复用，不新增读权限要求。
- MemberRepository 只委托 MemberQueryRepository/MemberWriteRepository；普通读取与 DML、行锁分开，任职及联系方式语义保持。
- CatalogRepository 只委托应用、资源、操作、菜单、套餐和跨表清理仓储；跨表清理顺序不变，菜单与套餐共用重复关系标识校验。
- FieldAccessEvaluator 保留求值入口及嵌套载荷类型，快照加载进入 FieldPolicySnapshotLoader，成员投影进入 MemberFieldProjector。fresh/default/cache 读取路径及缓存载荷不变。
- 构造变化在测试 fixture 中适配，不为生产增加历史构造器。生产仍使用 final 字段和构造注入；资料命令通过显式构造器使用原事务管理器创建模板，并有无 TransactionTemplate Bean 的 Spring 装配回归。

## 阶段与完整回归

- P1：Member/Field 定向回归通过（/tmp/iam-followup-p1-tests.log）。
- P2：Catalog/Menu/Plan/RelationWrite 定向回归通过（/tmp/iam-followup-p2-tests.log）。
- P3：Field/TenantPolicy/Resource 定向回归通过（/tmp/iam-followup-p3-tests.log）。
- P4：`IAM_MYSQL_HTTP_TEST=true IAM_TENANT_LIVE_MYSQL_HTTP_TEST=true ./gradlew :ingot-service:ingot-iam:ingot-iam-provider:check :ingot-service:ingot-iam:ingot-iam-provider:bootJar -I /tmp/iam-cleanup-tests.gradle --offline --console=plain` 通过（/tmp/iam-followup-final-check.log）。
- JVM 合计1,481项：1,478通过、0失败、3跳过。包含已启用的26项隔离MySQL/HTTP及Druid最终SQL防护回归。跳过项为租户大规模性能、实际浏览器、MinIO，未作为本次证据。
- Gradle check 实际执行架构门禁10项、发布契约8项，均通过。新增门禁约束@Repository落点、成员Query只读、聚合仓储无Mapper/SQL依赖。check需要Python3标准库。
- 额外的 MemberProfileCommandWiringTest 通过：仅提供 PlatformTransactionManager、不提供 TransactionTemplate Bean 时，生产服务可正常装配。
- git diff --check通过。153项数据库与离线工具保护基线哈希全部未变化。前端工作区未修改。

## 新项目初始化核对

- `python3 tools/iam/generate_database.py --check`通过，此代码阶段核对时的65表完整SQL与权威来源同步；后续精简后的59表证据见独立初始化变更。
- 007种子的BCrypt哈希用密码验证函数核对，匹配`11111111`。账号为platform，数据库must_change_password=TRUE。
- 现有AccountIdentityServiceTest覆盖强制改密时平台/租户均只返回in:init_pwd，不混入管理员操作；改密与账号标记回归纳入完整测试。
- `python3 databases/iam/test_bootstrap_seed.py BootstrapSeedTest.test_full_initialization_creates_one_platform_admin_and_no_tenant`：独立MySQL、无外部网络、1项通过（/tmp/iam-followup-initialization-test.log），验证完整SQL建立唯一平台账号、成员和系统管理员分配，没有租户，单独重跑007不覆盖已改密码及改密标记。
- 以上不连接实际业务库，不代表用户实际登录页面验收完成。当前初始化事实与待确认表精简边界见INITIALIZATION-ASSESSMENT.md。

## 待办

用户实际环境复测成员资料与资格、组/角色关联、目录与字段策略后，才能更新current与归档。用户已确认的新项目表清单精简由独立变更实施，不得混同已有库升级。
