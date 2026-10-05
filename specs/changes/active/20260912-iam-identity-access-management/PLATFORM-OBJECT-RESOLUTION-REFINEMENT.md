# 平台范围对象资源识别收紧

> 状态：validating（OR01–OR03 已完成；OR04 人工验收待执行）
> 批准记录：2026-10-05，用户确认先提交既有代码，再实施基于真实应用、资源关联的对象识别收紧。

## 需求与设计

范围候选按操作实际关联的应用和资源识别对象类型，不截取操作编码，不以当前参数关联操作的首条记录决定对象类型。内置操作与自定义操作只要属于同一已接入资源，使用相同对象候选适配器；完整操作码继续用于权限校验，其契约不变。

当前参数引用的全部操作必须存在、启用，且属于同一平台应用和资源。元数据查询同时核对资源所属应用与操作所属应用一致，返回可信应用编码和资源编码。对象适配器由这些关联编码确定，验证全部关联操作一致；核心平台应用及已注册资源之外继续明确不支持，不能通过伪造操作编码接入其他资源。

分配、委派、诊断、记录已选回显以及提交时的对象存在性校验共用此识别逻辑。委派对象候选仍与所有关联操作的上限逐条取交集；空交集保持空，不扩大权限。诊断仍使用资源读取权限过滤。跨资源参数、停用/缺失操作、未注册资源及不一致关联按既有错误或不支持契约拒绝。

## 兼容与效率

不新增接口、DTO、数据库字段或索引，不改变已绑定对象、固定版本、权限操作码及租户逻辑。内部操作投影增加已关联表的编码列，不增加逐操作查询。历史操作编码即使不符合三段格式，也不再影响其所属已接入资源的候选识别；不修改历史编码或其权限校验含义。

## 任务与验收

- [x] OR01（开发完成）：扩展可信操作投影；统一关联资源识别，移除对象路径的首条操作码截取；保持全部委派上限交集。
- [x] OR02（自动化完成）：验证自定义/内置操作、编码与关联不一致、操作顺序交换、全部上限交集、未知资源/应用、跨资源与不一致关联；覆盖候选、回显、诊断与提交校验及实际 SQL 投影。
- [x] OR03（完成）：两端 active change 与后端来源副本/哈希同步，公开请求响应结构保持原样。
- [ ] OR04（人工）：在平台应用资源创建自定义操作 aaaa，只给该操作指定对象时即能查询应用列表；加入或移除创建应用不改变对象类型。验证新建/编辑分配和委派、已选回显、诊断及越界提交。

开发完成与人工验收分开标记，不更新 current。本次先提交的旧代码与本增量分开，本增量不自动提交。

## 2026-10-05 实施与验证记录

生产元数据投影补充 application_code/resource_code，并通过 resource.application_id=action.application_id 校验关联一致。PlatformAuthorizationEditor 的对象路径统一使用完整关联操作集合验证资源适配，不再使用 actions.getFirst 或解析操作码。所有委派上限交集、诊断读取范围及原有分页/已选关联边界保留。不新增逐行查询、公开 DTO 或 DDL。

定向回归合计 **52 项通过，0 失败，0 跳过**：

- PlatformObjectResourceResolutionTest 12、PlatformAssignmentSelectionTest 3、PlatformDiagnoseCandidateTest 3、PlatformCandidateSqlTest 8：验证首条操作不会掩盖未知资源，顺序不影响结果，伪造操作码不能改变实际对象类型；H2 使用生产 Mapper SQL，验证真实编码投影及跨应用资源关联被过滤。
- RoleWorkspaceMySqlHttpTest 7（隔离 MySQL 8.4 / 真实 HTTP）、PlatformAssignmentBoundaryTest 7、AssignmentServiceTest 10、PlatformDelegationRoleCandidateTest 2：新增历史末段操作码 aaaa 的实际对象回显与写入验证，原有权限边界与整批事务回归通过。测试容器由既有用例自动清理。

命令（在后端仓库执行）：

```sh
./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --offline --tests '*PlatformObjectResourceResolutionTest' --tests '*PlatformAssignmentSelectionTest' --tests '*PlatformDiagnoseCandidateTest' --tests '*PlatformCandidateSqlTest'
env IAM_MYSQL_HTTP_TEST=true ./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --offline --quiet --tests '*RoleWorkspaceMySqlHttpTest' --tests '*PlatformAssignmentBoundaryTest' --tests '*AssignmentServiceTest' --tests '*PlatformDelegationRoleCandidateTest'
```

前端仅同步 active change 与来源副本，无前端代码变更；公开接口结构未变，无需重发 OpenAPI/schema。OR04 人工验收保持未完成。已有代码提交为后端 f1eae023/9fe71389、前端 84a9239/abbc2e7；2026-10-05 用户明确要求提交本增量，后端代码已提交 d390b096；人工验收仍待 OR04。

### 人工步骤

1. 更新并重启 IAM，在平台应用的应用资源下确认 aaaa 的真实所属资源；角色新版本只将 aaaa 设置为指定对象，分配时选择该固定版本，应能搜索/分页选择应用。
2. 将创建应用也改为指定对象后选择新版本，共用对象参数只配置一次；再移除该操作的指定对象范围，aaaa 的候选类型仍为应用。内置操作有无或排序不影响结果。
3. 保存并编辑分配，核对已选对象回显；创建/编辑委派和权限诊断选择 aaaa 时也使用应用对象，诊断仍受资源读取范围限制。
4. 受限管理员使用委派来源，核对候选是所有相关操作上限的交集；无交集时为空。直接提交越界对象、跨资源参数、未接入资源均不能借用已有操作扩大权限。
