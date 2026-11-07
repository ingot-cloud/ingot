# 验证记录

2026-11-07：附件报告countGroupsOfMember在groupIds为null时生成WHERE 1=1，被Druid1.2.27拒绝。真实Mapper生成BoundSql并交由MySqlWallProvider检查，修复前全范围计数检查复现该错误。改用MyBatis where标签包裹现有组范围片段，实际无条件时省略WHERE，有范围时使用参数化IN。

## 回归

`./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*TenantMemberGroupSqlWallTest' --tests '*GroupRepositoryTest' --tests '*GroupServiceTest' -I /tmp/iam-cleanup-tests.gradle --offline --console=plain`：BUILD SUCCESSFUL（/tmp/iam-member-groups-tests.log）。

- SQL防护3项通过：全范围与受限范围的真实计数/列表SQL通过Druid检查；原恒真条件仍被相同防护拒绝，未放宽配置。
- GroupRepository8项通过：原有效成员展开回归，以及新增成员所属组分页、直接与部门来源去重、祖先部门是否连带下级、可见组范围、跨租户隔离、无关联与空范围返回空页。
- GroupService9项通过，合计20项，0失败、0跳过。查询语义在隔离H2内存库验证，未连接用户实际数据库。
- `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:bootJar --offline --console=plain`通过（/tmp/iam-member-groups-build.log），修复已打入IAM可执行JAR。
- git diff --check通过；原历史清理change的153项数据库/离线工具基线哈希均未变化。没有修改Druid/Nacos配置、生产表结构、数据或数据库升级脚本。

用户实际环境复测仍待反馈；重启更新后的IAM服务后请求原成员所属组接口。不提前更新current或归档。

## IAM 扩展 SQL 检查

2026-11-07 用户要求继续检查 IAM 其他 SQL，并修复同类防护拦截。

### 范围与复现

- 清点 59 个 Mapper 接口、5 个 XML（实际身份查询位于 IamIdentityMapper.xml）、全部 SQL Provider 与持久化层手写 Wrapper 片段。自动检查 187 条注册自定义语句，覆盖 778 个参数场景；测试结束检查注册语句与覆盖集合，避免新增语句静默漏检。
- 使用真实 MyBatis BoundSql 和 Druid 1.2.27，配置对齐 nacos 三个环境的 in-database.yml。恒真、SELECT INTO 和 DDL 拒绝对照仍生效；未放宽配置。
- 复现 Druid 2113（part alway false condition not allow）：空范围产生 AND 1=0 时，授权候选、角色树、角色工作区、负责人、租户角色/分配/委派列表及计数被拒绝。扫描中有 149 个失败参数场景，另有 4 个既有真实数据库查询测试在增加 Wall 检查后复现。证据见 [sql-wall-failure-reproductions.json](sql-wall-failure-reproductions.json)。
- 最初失败运行还发现并修正了测试参数生成器缺少 record/LocalDateTime 样本的问题；这些夹具错误不计入上述 SQL 失败数量。

### 修复与语义

11 个生产文件将空范围改为目标列 IN (NULL)，对应 Wrapper 使用固定 NULL 集合；空委派来源不会因此匹配 delegation_grant_id 为 NULL 的直接分配。负责人范围直接组合实际条件，移除 1=1 占位。参数化 IN、EXISTS、可信租户、域、状态、有效期、排序和分页保持原样。

IAM MyBatis 测试夹具在最终 StatementHandler.prepare 前增加 Wall 检查，结合已有结果断言覆盖 Wrapper、分页总数、列表及 DML。新增结果回归验证：空条款、空任职集合、根不可见选择器、NULL 来源、空范围与合法 OR 分支组合、跨租户、页与汇总均保持拒绝或原有可见结果。

MemberPickerBranch.all() 仍会在已有可信租户等条件后追加 AND 1=1；实际生成 SQL 在全部相关场景均通过当前防护，不存在原接口单独 WHERE 1=1 的问题，因此未对该正常分支做无关修改。

### 结果

1. `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test -I /tmp/iam-cleanup-tests.gradle --offline --console=plain`：BUILD SUCCESSFUL（/tmp/iam-sql-wall-full.log）。1,480 项中 1,451 项通过、0 失败、29 项默认关闭的环境测试跳过；其中 SQL 兼容性类 779 项（778 个 SQL 场景及 1 项防护拒绝对照）。完整结果见 [extended-test-results.json](extended-test-results.json)。
2. 补跑 `IAM_MYSQL_HTTP_TEST=true` 的 RoleWorkspaceMySqlHttpTest：23 项全部通过（/tmp/iam-sql-wall-mysql.log）。补跑 `IAM_TENANT_LIVE_MYSQL_HTTP_TEST=true` 的 TenantLiveMySqlHttpTest：3 项通过，性能/浏览器 2 项未启用（/tmp/iam-sql-wall-tenant-mysql.log）。两者使用本机已有 mysql:8.4 镜像、临时独立 fixture 库和 HTTP 服务，未连接用户业务库。证据见 [mysql-test-results.json](mysql-test-results.json)。
3. 合并两轮证据，1,477 个测试已通过；剩余 3 个可选环境测试为租户大规模性能、实际浏览器组件和 MinIO 上传。它们不构成本次已执行的 SQL/HTTP 回归证据。
4. IAM bootJar 重新打包成功（/tmp/iam-sql-wall-build.log）。`python3 -m unittest tools.iam.test_package_boundaries` 7 项通过；git diff --check 通过。
5. 153 项数据库/离线工具基线哈希全部未变化；未修改数据库表、数据、升级脚本和 Nacos/Druid 配置。

实际环境复测建议：重启 IAM 后确认原成员所属组接口，以及租户角色/分配/委派列表、负责人选择、角色工作区在全范围、受限范围、无可见结果时正常返回。自动化集成测试中的可信身份由测试边界替身提供，不能替代用户实际登录会话验收。
