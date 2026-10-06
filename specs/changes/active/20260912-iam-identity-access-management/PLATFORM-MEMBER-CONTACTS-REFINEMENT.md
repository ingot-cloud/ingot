# 平台成员联系资料独立存储

- 状态：validating（approved → implementing → validating，2026-10-06；人工验收待执行）
- 批准：用户明确要求平台成员增加独立联系方式，仅修改相关前后端接口及 SQL，并提供迁移脚本。
- 本增量替代 API 中平台成员联系方式写回全局账号的约定，不提前更新 current；用户后续已明确要求提交代码，人工验收仍待执行。

## 需求与设计

`iam_account.phone/email` 保留账号登录及账号级资料语义；`iam_platform_member.phone/email` 为可空的联系手机号/邮箱，长度分别32/128，保持租户成员联系资料不变。平台成员列表、详情及用户组成员响应从成员表取联系字段；账号关联仅用于登录名。编辑按现有成员版本、对象范围和字段可写权限修改成员表，不写全局账号或租户成员，不同步联系资料到登录信息。

保持现有 POST/PATCH 的字段和结构，不新增接口或创建参数。新建平台成员（含受控首账号初始化）一次复制关联账号联系方式作为初值；创建后两者独立，成员联系字段为空时不回退到账号。创建界面说明初值来源；详情及列表明确联系资料含义，使用原字段策略和提交流程。

## SQL、兼容与回退

权威001 DDL增加两列，由现有生成器同步完整`databases/ingot_iam.sql`。已有库新增`databases/iam/migrations/014_platform_member_contacts.sql`：在停止IAM写入并备份后，一次ALTER增加两列，再从未删除的关联账号回填已有成员；不更新账号、版本或租户资料。仅缺少这两列的库执行一次，重复执行应拒绝，避免再次同步覆盖独立资料；新建库不执行历史迁移。

发布顺序为迁移→后端→前端。中断于DDL后时保持服务停止，确认列已新增后单独完成该脚本的回填语句。应用回退前冻结平台联系资料编辑，保留新增列与备份；独立资料不得自动写回账号。重新发布新版后恢复编辑；不通过完整初始化SQL升级已有库。

## 任务与验收

- [x] PC01：成员实体/持久化/读写来源调整，创建仅一次复制，账号和租户资料保持独立，字段/版本检查保留。
- [x] PC02：权威DDL、完整SQL与一次性迁移，独立MySQL验证迁移回填、重复拒绝和后续隔离。
- [x] PC03：前端联系字段文案、创建初值说明，公共类型/API/OpenAPI及来源同步，接口形状不变。
- [x] PC04：定向持久化/服务/初始化及前端回归，类型检查、只读lint和相关构建。
- [ ] PC05：人工执行目标库迁移，核对平台联系修改/清空不改变登录及租户资料，账号修改不覆盖平台资料，隐藏/脱敏/不可编辑及冲突行为正确。

## 验证记录（2026-10-06）

后端仓库执行：

- `./gradlew :ingot-framework:ingot-commons:test --offline --tests '*IamAuthorizationContractTest'`：13项通过，Java契约已导出。
- `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --offline --tests '*MemberQueryRepositoryTest' --tests '*PlatformMemberContactsTest' --tests '*PlatformBootstrapServiceTest' --tests '*MemberCreationAssignmentTest' --tests '*AccountServiceTest'`：27项通过，包含账号/本人账号回归。持久化及服务回归覆盖一次复制、账号隔离、清空不回退、字段写入门禁、版本冲突和审计失败回滚。
- `python3 -B -m unittest databases.iam.test_identity_schema.IdentitySchemaTest.test_platform_member_contacts_migrate_once_and_remain_independent`：隔离MySQL通过，覆盖回填、已删除账号不回填、两域隔离及重复执行拒绝。未连接实际数据库。
- `python3 tools/iam/build_contract.py --check`、`python3 tools/iam/test_contract.py`（7项）、`python3 tools/iam/generate_database.py --check`、`python3 -B tools/iam/test_database_sources.py`（4项）：通过。

前端仓库：人员抽屉/用户组4项回归、admin-common/platform插件类型检查、4个修改源文件的只读ESLint、平台管理台构建、依赖边界和文档检查通过。构建已有图标扫描、crypto外置及重复CSS等警告，本次不扩展修改。

人工验收独立保留：已有库先停写备份并仅执行014迁移，再发布后端与前端；核对成员列表、组成员及详情同源，新建初始复制、随后编辑/清空、账号修改和租户联系方式互不覆盖。字段隐藏/脱敏/只读与并发冲突也需在实际身份下验证。
