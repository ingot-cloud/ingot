# 数据库初始化与框架建表脚本

首次部署IAM只执行[ingot_iam.sql](ingot_iam.sql)，已包含IAM业务表、所依赖的6张框架表及默认应用、资源、权限、角色、菜单和平台账号。该文件会删除并重建ingot_iam库。默认platform/11111111，首次登录强制改密。

框架表以`fw_<能力域>_`标识所属能力，IAM自有表以`iam_`标识。建表定义由实际执行SQL的library维护。数据库位置由消费服务的DataSource决定：同一框架表可以分别存在于IAM和Member自己的库中，不代表有一个统一的framework数据库。

## 依赖库与脚本对应关系

| 引用的library | 建表脚本 | 表 | 部署到哪里 |
|---|---|---|---|
| ingot-security-account-adapter | [schema.sql](../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/ingot-security-account-adapter/schema.sql) | fw_security_account_lock_state | 使用MySQL账号锁定适配器的服务库；当前IAM、Member |
| ingot-security-credential-data | [schema.sql](../ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/ingot-security-credential-data/schema.sql) | fw_security_password_history、fw_security_password_expiration | 使用数据库凭证历史/有效期实现的服务库；当前IAM |
| ingot-security-event-store-mysql | [schema.sql](../ingot-framework/ingot-security/ingot-security-event-store-mysql/src/main/resources/sql/ingot-security-event-store-mysql/schema.sql) | fw_security_event | 装配MySQL事件Store的服务库；当前IAM、Member、Security |
| ingot-id | [schema.sql](../ingot-framework/ingot-id/src/main/resources/sql/ingot-id/schema.sql) | fw_id_leaf_alloc | 存在DataSource并自动装配BizGenerator的服务库；当前IAM、Member |
| ingot-oss-common | [schema.sql](../ingot-framework/ingot-oss/ingot-oss-common/src/main/resources/sql/ingot-oss-common/schema.sql) | fw_oss_asset_upload | 使用StagedImageUploads的业务库；当前IAM、Member |

以上脚本随library打包在各自的`classpath:sql/<module>/schema.sql`（例如`sql/ingot-security-account-adapter/schema.sql`），源码路径也可直接执行。它们只按`IF NOT EXISTS`建表并设置UTC，不创建/删除数据库、不执行USE、不删除旧表、不写入样例账号或运行记录。已经存在的表不会被自动改名或升级。

独立接入某个library时，在消费服务的数据库执行对应脚本，例如：

```sh
mysql --host=127.0.0.1 --user=root --password --database=your_service_db < ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/ingot-security-account-adapter/schema.sql
```

不只检查直接依赖，也要检查传递依赖和实际装配：account-adapter会传递引入MySQL事件Store，OSS具体实现会引入oss-common。密码策略/凭证核心库本身不等于credential-data，只有引用数据实现才需要其两张表。ingot-id在存在DataSource且未替换BizGenerator时会自动初始化Leaf，即使主要使用雪花ID，也需要号段表。OSS上传暂存表对应StagedImageUploads能力，不应仅因classpath包含oss-common就推断所有宿主都启用了该存储。

## 已有服务初始化入口

| 服务 | 文件 | 已包含的框架表 |
|---|---|---|
| IAM | [ingot_iam.sql](ingot_iam.sql) | 账号锁定、密码历史/过期、安全事件、Leaf、上传暂存全部6表 |
| Member | [ingot_member.sql](ingot_member.sql) | 账号锁定、安全事件、Leaf、上传暂存4表 |
| Security | [ingot_security.sql](ingot_security.sql) | MySQL安全事件表 |
| Auth | [ingot_auth.sql](ingot_auth.sql) | OAuth注册客户端由Auth自有JDBC仓储维护，属于宿主表，不是上述框架共享表 |

IAM文件自带建库与选库。Member、Security、Auth文件仍在各自明确选定的数据库使用；不要把所有服务的表放进IAM库。使用这些完整入口后，不需要再逐份执行已包含的library脚本。

框架DDL唯一来源和服务投影记录在[framework-tables.json](framework-tables.json)。维护命令：

```sh
python3 tools/iam/generate_database.py
python3 tools/iam/generate_framework_databases.py --check
python3 tools/iam/generate_database.py --check
```

共享表定义不在服务导出文件中独立维护；生成器从library同步。Member/Security自有表及其正式种子沿现有文件维护，共享表不携带旧运行数据。

## 不需要额外关系库表的依赖

- security-account-core、security-credential、security-recording提供核心用例/契约；数据库实现见上表，不能仅因引用核心库就自动安装全部共享表。
- event-store-log写日志；event-transport-feign上报远端；Redis相关实现使用Redis，不需要在本地创建fw_security_event。
- ingot-download要求宿主实现DownloadTaskStore。IAM的适配器使用iam_download_task，随IAM初始化；其他宿主按自己的存储实现建表，框架不硬编码统一表名。
- ingot-social-common/wechat通过SocialDetailsProvider读取宿主配置。IAM的iam_social_config和iam_account_social_binding属于IAM自有存储；Member的member_social_details和member_user_social属于Member。
- ingot-dict-client通过字典查询端口/远端API消费字典，不自带字典数据库。IAM的iam_dictionary支持平台、租户和应用作用域。
- authorization-server的当前授权及consent实现使用Redis；OAuth客户端表由Auth的RegisteredClientRepository宿主实现负责，不额外创建oauth2_authorization或consent表。
- XXL Job、Nacos等中间件有自己的数据库和安装脚本，属于中间件部署，不属于消费业务库的library表。

## 历史脚本与升级边界

[ingot_core.sql](ingot_core.sql)、[migrations](migrations)、[iam/migrations](iam/migrations)及[可选历史DDL](iam/legacy/README.md)保留其历史名称和对应版本，不作为当前首次部署清单，不应逐个遍历执行。表名统一后不能用旧历史脚本升级当前新名结构；保留现有数据的迁移须单独设计。

本轮不创建兼容旧表名的视图或双写。首次部署需同步使用匹配的新版本应用和初始化文件；已有旧名表不会因为library脚本的IF NOT EXISTS而自动转换。
