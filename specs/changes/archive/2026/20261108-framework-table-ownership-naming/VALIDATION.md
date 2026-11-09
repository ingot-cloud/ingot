# 验证记录

2026-11-08。实际数据库未连接或修改，代码未提交，current未提前更新。

## 实现结果

- 3张IAM自有辅助表及6张共享框架表按table-renames.json统一命名，字段、索引和约束逐表对照保持不变；IAM仍59表501列，默认账号及强制改密标记保留。
- 5个拥有SQL存储的library提供src/main/resources/sql/<module>/schema.sql，模块命名空间避免多JAR同路径冲突；DDL只有UTC设置和CREATE IF NOT EXISTS，不选库、不删表、不写运行数据。
- manifest引用5份框架SQL，005只保留IAM自有3表；Member/Security共享表定义从library投影，不再携带共享表的旧运行数据。依赖、脚本、表、落库和完整初始化覆盖关系见databases/README.md及framework-tables.json。
- 密码模型补显式@TableName，沿用现有MyBatis compileOnly依赖，Mapper泛型、服务返回类型与字段不变；无需引入新依赖或修改API。
- 历史工具在隔离夹具中使用旧表名和固定SQL，026/027/028门禁不套用新表名；94个历史文件、冻结夹具和可选历史DDL摘要不变。

## 已完成验证

- IAM完整check与bootJar、账号保护适配器、凭证核心、凭证数据、MySQL事件存储和OSS公共库测试通过。JVM共1557项，1554通过、3项可选环境场景跳过，0失败/错误；完整逐suite结果见jvm-full-results.json。发号模块没有现成单测，通过IAM发号初始化及消费端验证。
- IAM、Member、Auth和Security相关源码编译通过。资源目录加模块命名空间后，密码历史实际映射测试、Gradle脚本归属门禁和bootJar再次通过；未重复无关全套测试。
- 5个library的实际JAR均包含正确的命名空间SQL，内容与权威源一致，无根sql/schema.sql冲突，记录见packaged-resources.json。
- 独立library及Member/Security脚本2项MySQL测试通过：5模块空库建表、重复执行、空运行数据、精确表集、其他库sentinel保留、外键设置不被关闭。
- IAM完整初始化24项、身份结构28项MySQL回归通过，包含新库、重复重建、角色与菜单、平台账号、强制改密标记、历史表清除及跨域约束。
- 固定026注释升级3项、027/028主键与索引升级9项MySQL回归通过，重复、漂移阻断、回滚和发号高水位语义保留。
- 模块归属6项、来源清单6项、字典/历史预检9项、种子生成7项、离线租户迁移9项，以及测试数据工具本次受影响的manifest用例1项通过；Gradle原架构10项、发布契约8项通过，新增iamFrameworkSchemaCheck执行6项通过。
- 59表结构对比和94项历史摘要保护结果见structure-validation.json。根及模块说明中的SQL链接、生成器一致性和git diff --check已核对。

## 历史迁移补跑

历史租户迁移整组初次执行3项通过、2项因宿主OSError 12（无法为docker创建进程，内存不足）中断，未出现数据库断言失败。为Python隔离容器设置64MiB缓冲池、关闭performance schema后，顺序补跑两项中断用例均通过；开发准备脚本3项整组通过。初次运行与补跑证据均写入test-results.json，没有把它们描述为同一次整组运行。

本轮有效通过证据：相关离线38项、隔离MySQL 74个用例、JVM 1554项；另3项可选场景跳过。测试数据构建工具本轮只执行受manifest文件数变化影响的1项，其余既有问题仍由原change记录，不作为本轮通过证据。

## 验收与部署

本轮按首次部署处理，新版代码与新版表结构必须同步。用户上一次执行的SQL仍是改名前结构，需在停止服务后重新执行最新ingot_iam.sql；该入口整库重建。Member/Security的实际库按各自新脚本和匹配版本部署，不能只替换IAM文件后启动旧表名数据库。library脚本不会替已有表自动改名或迁移数据。

实际导入、首次改密、字典、社交配置、锁定/事件及相关业务功能验收后，才更新current并归档。历史SQL/工具不用于升级当前新名数据库；本轮没有执行实际业务库的初始化、迁移或删表。
