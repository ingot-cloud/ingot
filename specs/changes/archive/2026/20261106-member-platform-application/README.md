# App 用户管理平台应用

> 状态：completed

2026-11-06 用户确认方案并要求先对齐两仓规格再实施。平台应用编码为 `platform:member`。配套前端变更为 ingot-admin 的 `20261106-member-platform-application`，其 API.md 记录现有接口和操作码映射。

- [需求与验收](./REQUIREMENTS.md)
- [技术设计](./DESIGN.md)
- [实施任务](./TASKS.md)

## 验证记录

- Member 与 IAM 编译通过。Member 的 3 项权限回归覆盖授权写入、只读与租户拒绝、范围及依赖异常。
- 生成器检查和 7 项目录单测通过。隔离 MySQL 8.0.44 通过 4 项冷启动及 036 增量相关用例，包含预演、重复执行、路径冲突和目录回滚。
- 扩展运行的数据库来源检查中，`test_bundle_resets_each_manifest_table_before_creating_and_checks_seed_foreign_keys` 预期 66 张表。变更前后的完整初始化文件均为 64 张，本次 036 只新增目录记录。
- 用户在平台角色、权限管理页面反馈了操作列和详情抽屉的布局，修复后已通过浏览器回归。2026-11-06 用户要求归档、提交并推送。

## 完成记录

- 完成日期为 2026-11-06。
- 实现提交为后端 `14638bd16c3cca9ba99a4aad3e805cf01972f57c`、前端 `70fc3e03476288df8cf4fa26f2019e247bd7463d`。
- 系统基线更新见 [平台能力与页面归属](../../../../current/iam/platform-capabilities/README.md)。
- 最终实现沿用批准的 IAM 目录和执行权限设计。联调补充了显式空集合清除绑定、旧关系表非空字段的存储兼容以及 036 的选库说明。
- [前端配套归档](../../../../../../ingot-admin/specs/changes/archive/2026/20261106-member-platform-application/README.md)
- 真实平台会话授权、撤权和目标库回滚的剩余证据交由 [目标环境发布验收](../../../active/20261106-member-platform-release-acceptance/README.md) 跟踪。
