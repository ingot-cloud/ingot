# ingot-security-event-store-mysql


## 数据库建表

本模块的数据库结构由[src/main/resources/sql/ingot-security-event-store-mysql/schema.sql](src/main/resources/sql/ingot-security-event-store-mysql/schema.sql)维护，表为`fw_security_event`。在消费服务DataSource对应的库执行，脚本不选库、不删表、不写入运行数据；完整服务初始化已包含时无需重复执行。依赖与服务落库关系见仓库根[databases/README.md](../../../databases/README.md)。
