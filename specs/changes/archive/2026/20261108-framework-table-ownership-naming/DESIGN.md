# 设计

以table-renames.json为确定映射，仅替换完整表标识，不改字段、JSON键、HTTP路径和配置键。生产实体、注解SQL、XML、JDBC及相应测试同步。密码模型已有MyBatis compileOnly依赖，补显式@TableName，保持Mapper泛型、服务返回类型与公开字段不变，不为改表名引入新的运行依赖。

每个拥有直接SQL存储的模块以src/main/resources/sql/<module>/schema.sql为唯一DDL来源（模块命名空间避免多个JAR的classpath资源重名）。原账号保护和密码脚本迁到统一路径；事件、Leaf和上传DDL从IAM005原样迁出，表名统一，移除运行时AUTO_INCREMENT水位。表注释注明所属模块。IAM005仅保留三张IAM辅助表；manifest的frameworkSchemas引用5份脚本，最终仍59表。

databases/framework-tables.json登记模块、脚本、表与服务投影。生成器只替换Member/Security的共享表建表段并删除这些表的运行数据INSERT，不改其他服务种子。依赖需要但服务脚本缺失的共享表补入；Auth的OAuth注册客户端由其宿主自有仓储负责，authorization-server的授权/consent使用Redis，无需捏造框架SQL。

所有框架脚本为可重复CREATE IF NOT EXISTS，不含USE、DROP、数据库用户或默认数据。首次部署仍执行完整ingot_iam.sql，其他消费方可将模块资源脚本应用于自己的DataSource对应库；不能一律建到IAM或凭证安全中心配置库。

历史SQL/冻结夹具原样保护；旧工具固定的026/027/028目标不随新表名改变，使用版本固定参考结构和显式历史夹具，不将当前新名DDL冒充旧库。当前验证工具可按其真实目标使用新名。现行文档、数据字典和结构快照重新生成，current待实际验收再更新。

单独脚本在无网络一次性MySQL运行，检查重放、纯DDL、精确表集和其他库保留；IAM完整导入和多实例HTTP、框架单测覆盖真实SQL。对照改名前的有效结构，将新名还原后比较，避免改名顺带改变字段/索引/约束。已有实际库及其他服务实际数据均不操作，失败恢复采用原版本和原结构或首次部署重建。
