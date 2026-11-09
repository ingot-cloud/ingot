# IAM 与框架表命名及建表脚本归属

> 状态：completed
> 日期：2026-11-08

用户确认上一轮命名计划，并要求每个需要数据库表的library自带建表SQL，根databases目录说明依赖与脚本对应关系。按首次部署维护，不连接或修改实际库，不自动提交；验收后更新current并归档。

范围：3张IAM表及6张共享表改名，5个框架模块提供schema.sql，IAM/Member/Security初始化及实际消费者同步，根README和可验证的脚本清单。表字段、业务接口、认证与缓存行为不变。具体映射见table-renames.json。

- [需求](REQUIREMENTS.md)
- [设计](DESIGN.md)
- [任务](TASKS.md)
- [验证记录](VALIDATION.md)

P1–P4已完成。JVM 1554项通过、3项可选场景跳过；相关离线38项、隔离MySQL 74个用例具备通过证据，包含宿主内存不足后顺序补跑的2项历史用例。5个JAR的SQL资源和94个历史文件摘要核对通过。

## 完成记录

- 2026-11-09 用户确认本轮验证成功并要求归档、提交和推送。
- 关联实现提交为 `4ed2d60c`。
- 验收后的基线见[数据库持久化](../../../../current/iam/database-persistence/SPEC.md)、[框架事件存储](../../../../current/framework/security-event-recording/SPEC.md)、[账号保护](../../../../current/security/account-protection/SPEC.md)及[凭证安全](../../../../current/security/credential-security/SPEC.md)。
- 按批准的 9 张表命名交付。框架 SQL 使用模块命名空间，5 个模块的定义随 JAR 发布。
- 自动化过程和原阶段的待办描述保留在[验证记录](VALIDATION.md)，用于追溯当时的验证范围。
