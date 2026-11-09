# 需求与验收

- IAM维护的社交配置、账号社交绑定、字典分别为iam_social_config、iam_account_social_binding、iam_dictionary；运行依赖继续保留。
- 共享表采用已确认的fw_security_*、fw_id_leaf_alloc、fw_oss_asset_upload命名，相关库代码及服务脚本一致；iam_download_task仍由IAM实现存储接口并保留原名。
- 账号保护、凭证数据、MySQL事件存储、发号、OSS公共模块自带纯建表schema.sql：在调用方选定库执行，IF NOT EXISTS、UTC，不建/删/切换数据库、不带样例业务或运行数据。
- 根databases/README.md说明直接及传递依赖、启用条件、目标数据库、脚本路径、IAM完整单文件已包含的内容。无关系库持久化的library明确不需要建表；OAuth客户端等宿主实现按服务DDL说明。
- 保持IAM单文件59表、默认目录、platform/11111111及强制改密；Member与Security使用共享框架DDL的投影，移除框架表的旧运行快照数据，保留服务自有初始化内容。
- 旧core库、历史迁移、冻结夹具和current基线不改。新旧表名不双写、不自动建兼容视图；实际部署需同步库结构和新版本服务，已有库数据保留另案处理。
- 验收：旧名无运行SQL消费者，5份独立脚本空库/重放/无关库隔离验证，59表与共享DDL一致，MyBatis隐式密码表映射正确，安全事件/锁定/发号/上传/字典/社交回归及服务编译通过；历史保护摘要不变。
