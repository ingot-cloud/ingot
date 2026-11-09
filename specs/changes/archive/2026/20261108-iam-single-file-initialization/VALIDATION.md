# 验证记录

2026-11-08，状态validating。仅使用一次性MySQL容器验证交付文件，没有连接或修改实际业务库，未创建commit。机器可读结果见test-results.json。

- 交付文件固定重建ingot_iam库；无需预建或选择数据库，无SOURCE或外部文件依赖。
- 保留59张运行表及原默认数据：5应用、44资源、181权限操作、41菜单、80菜单关联、2个治理角色及版本、默认字段策略、发号起点和唯一平台管理员分配。platform/11111111及must_change_password=TRUE不变。
- 来源清单6项、字典/历史预检9项、种子生成7项，共22项离线回归通过。generate_database.py --check及generate_database_review.py --check通过。
- test_bootstrap_seed.py完整24项通过：无库直接导入、固定数据库及字符集、全部表定义与分片一致、默认目录和平台管理员、部分初始化后重建、真实组织/所有者循环外键、外键检查0/1状态恢复、无权限扩张的菜单入口等。
- 历史清理回归在容器中创建6张可选历史表、016备份表及额外旧表，并写入历史模拟数据；执行完整文件后精确剩余59张运行表，没有历史或额外表。其他数据库中的sentinel记录在重复重建后保留。
- IAM_TENANT_LIVE_MYSQL_HTTP_TEST=true的TenantLiveMySqlHttpTest和AccountIdentityServiceTest定向执行成功，合计14项通过、2项可选性能/真实浏览器场景跳过。Java夹具执行原始交付SQL后以ingot_iam建立连接池，身份强制改密仅签发初始化权限的既有回归通过。未重复无关的全套Java测试。
- protected-files.json内104个文件摘要均未变化，覆盖运行DDL、框架DDL、006/007种子、manifest/结构快照、历史升级及冻结夹具。git diff --check通过。

日志：/tmp/iam-single-file-*.log。执行SQL的账号需要ingot_iam的CREATE/DROP及建表/写入权限；SQL不创建MySQL用户或调整全局授权。其他服务数据库不在该入口范围。

待用户停止使用该库的服务后实际导入，并使用platform/11111111验证首次改密和平台访问。验收后再更新current与归档；本次没有提前修改系统基线。
