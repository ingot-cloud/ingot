# 平台角色分配多角色与范围配置增量

- 状态：validating
- 状态流转：approved → implementing → validating（2026-10-05，开发与定向自动化完成，人工验收待执行）
- 批准：2026-10-05 用户“开始实施计划”；实施前完成 Plan 对齐。
- 所属 change 保留原阶段；本增量处于 validating，不提前更新 current。

## 需求与设计

新建分配选择多个不同角色，每个角色一个固定版本；所有接收成员与组共用有效期，各角色范围参数独立。沿用固定版本的范围类型，展示全部操作，对指定对象选择实际内容。编辑固定接收主体、版本和来源，仅调整对象与期限。本增量替代 ROLE-PICKER-REFINEMENT 中新建单版本限制，保留历史记录。

第二步复用角色→版本多选双栏弹窗，根和版本分别分页20，名称搜索，不默认最新版；同角色换版本替换原版本。确认才改草稿，取消保留。保持选中节点的角色身份、版本与完整参数/操作资料；保留未变化版本参数，移除或替换时清理对应参数与旧预览。

第三步按角色→应用→资源→操作展示固定范围，使用 --in-permission-panel-bg、InLoading 和统一表单间距；派生操作本地搜索分页20。共用参数只配置一次，不同版本同名参数隔离。对象使用原候选权限边界，层级对象树形展示；缺少参数、不可用资源或资料不完整不能当作全部范围。统一有效期并校验委派期限，服务端提交重验。

创建 items 按主体×角色展开，各项只有一个固定版本，整批共用来源与时间；任一失败整批回滚。预览响应 items 与请求 items 同序；界面以该次请求快照逐条对应主体、版本、范围，旧草稿或迟到响应不能提交。

## 接口与兼容

保留 AssignmentInput、AssignmentBatchInput 和数据库结构。新增 GET /v1/platform/assignments/{id}/selected-candidates，kind 为 ROLE_REVISION 或 OBJECT；OBJECT 必须 parameterKey，page/pageSize 默认1/20。响应 AuthorizationCandidatePage。读取沿用分配详情可见边界；ROLE_REVISION 仅返回记录固定版本及完整参数/操作资料，OBJECT 以持久化绑定关系在 SQL 中分页并与当前对象候选边界交集。来源失效时对象返回明确不支持说明，不回退更宽列表。

角色选择新增内部快照类型，保存树节点身份和完整版本资料。新建树/详情查询自动携带唯一委派依据；对象查询携带对应版本与参数。已选草稿名称独立于候选分页。编辑通过记录关联接口回显，不循环 ids 拼已绑定全集。

HTTP 提交契约保持；新增查询同步 OpenAPI、示例与前端来源副本。只改平台新建/编辑分配；租户分配和委派既有多版本行为保持兼容。保留上一轮排序未提交改动，不创建提交。

## 任务与验收

- [x] MA01（开发完成）：后端记录关联回显、SQL 分页与权限边界；OpenAPI/夹具同步。
- [x] MA02（开发完成）：共享角色多选支持每角色一个版本，分配草稿主体×版本与参数隔离。
- [x] MA03（开发完成）：范围步骤、编辑回显、有效期校验与逐项预览。
- [x] MA04（自动化完成）：组件/后端边界及 HTTP/MySQL 回归、类型检查、只读 lint、依赖边界和两管理台构建。
- [ ] MA05（人工验收）：多主体多角色、版本替换、跨页回显、树对象、范围与有效期限制、预览失效、窄屏及主题；由用户执行。

自动化覆盖多个角色同名参数隔离、组与成员的记录数量、缺少或越界对象、委派限制、失效来源和整批回滚；开发完成与人工验收分别记录。


## 2026-10-05 自动化验证记录

本增量开发完成，进入 validating；MA05 人工验收未完成，主 change 和 current 不提前结项。本记录仅覆盖本轮改动。

- IAM provider 定向回归 **33 项通过，0 跳过**：PlatformAssignmentSelectionTest、PlatformAssignmentBoundaryTest、PlatformCandidateSqlTest、AssignmentServiceTest、RoleWorkspaceMySqlHttpTest。最后一组包含 6 项隔离 MySQL 8.4 / 真实 HTTP 用例，验证固定版本资料、带点参数键的真实关联分页、当前来源交集、受限详情边界、成员和组 × 两角色的 4 条记录，以及中途失败整批回滚。
- IamAuthorizationContractTest **12 项通过**：新增多角色批量及已选对象夹具，检查嵌套验证、字符串 ID、各角色独立参数，重新从 Java 公共类型导出 schema。
- 前端相关 **7 个文件 / 21 项通过**：platformAssignment、BizIamAssignmentScopeStep、BizIamDelegationRolePicker、BizIamPlatformAssignmentDrawer、BizIamAssignmentDrawer.scope、BizIamDelegationCandidatePicker、BizIamTreeCandidateDialog。覆盖同角色版本替换、确认／取消、跨页快照、角色参数隔离、委派期限、4 项预览与草稿失效、编辑固定版本、已选首屏请求复用和不截断真实 ID。
- admin-common 与 platform-plugin 类型检查、修改文件只读 ESLint、`pnpm check:boundaries`、公共包及两管理台构建通过。先重建 workspace 声明解决旧 dist 导致的 TS6305；Vite 保留现有图标／动态导入提示。
- 路由生成、OpenAPI `--check`、7 项 `test_contract.py` 检查通过，当前 **124 路径 / 191 操作**；正式初始化生成仍为 2 应用、34 资源、130 操作、24 菜单、44 菜单操作、130 授权，无新增权限或 SQL 变更。
- 全量 `tools/iam` Python 检查为 13 通过、2 错误：test_service_naming 的两项依赖根目录现有缺失的 `.gitlab-ci.yml`。该限制未通过修改无关 CI 文件规避；本增量契约检查均通过。

复跑后端：

```sh
env IAM_MYSQL_HTTP_TEST=true ./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --offline --tests '*RoleWorkspaceMySqlHttpTest' --tests '*PlatformAssignmentSelectionTest' --tests '*PlatformAssignmentBoundaryTest' --tests '*PlatformCandidateSqlTest' --tests '*AssignmentServiceTest'
./gradlew :ingot-framework:ingot-commons:test --offline --tests '*IamAuthorizationContractTest'
python3 tools/iam/build_contract.py --check
python3 tools/iam/test_contract.py
```

复跑前端（ingot-admin 根目录，Node 22.17）：

```sh
pnpm --filter @ingot/admin-common exec vitest run src/models/iam/platformAssignment.test.ts src/components/BizIamAssignmentScopeStep.test.ts src/components/BizIamDelegationRolePicker.test.ts src/components/BizIamPlatformAssignmentDrawer.test.ts src/components/BizIamAssignmentDrawer.scope.test.ts src/components/BizIamDelegationCandidatePicker.test.ts src/components/BizIamTreeCandidateDialog.test.ts
pnpm --filter @ingot/admin-common type-check
pnpm --filter @ingot/platform-plugin type-check
pnpm check:boundaries
pnpm --filter @ingot/admin-platform-app --filter @ingot/admin-app build
```

## MA05 人工验收步骤（待执行）

先重启 IAM 服务并刷新平台管理台，以治理管理员、纯受限授权管理员分别验证：

1. 选择一个成员和一个非空用户组，再选两个不同角色的固定版本。预览显示 4 项，各项主体、角色及版本准确；保存后列表产生 4 条记录。
2. 同角色选择另一版本时替换原版本；另一个角色与其范围保留。取消选择弹窗不改草稿；移除／替换版本使旧预览及保存入口失效。
3. 第三步展示角色→应用→资源→操作及固定范围；ALL／SELF 无对象输入，OBJECT_SET 必须选择真实对象。相同版本共用参数只填写一次，不同角色同名参数互不覆盖。
4. 对象弹窗搜索、跨页选择和已选回显；菜单等层级对象按树展示。编辑大于 20 个已选对象的记录，第一页名称不齐时仍保留全部 ID，右侧继续分页，不通过普通人员／对象接口补回无权内容。
5. 完整治理资格可留空有效期；有限来源必须设置符合单次最长时长及来源起止限制的期限；UNLIMITED 可长期分配但仍依赖来源有效。越界对象／角色／来源提交由服务端拒绝。
6. 编辑记录固定主体、角色版本与授权依据，仅范围参数／时间可调整；资料不完整显示重试或说明，不降为全部。只读记录展示固定范围，不能修改。修改、权限刷新及迟到预览不能保留可用的旧保存入口。
7. 检查窄屏、明暗主题、表单间距、InLoading 和权限展示背景；确认租户分配、委派多版本选择交互正常。


## 2026-10-05 分配范围配置与步骤调整（approved → implementing → validating）

批准依据：用户在对齐分析后要求“开始按照我们说的进行修改”。以下增量替代上文第三步同时设置有效期及操作分页混排配置的交互；公共 HTTP DTO、数据库及授权规则不变。

- 第一步授权依据与接收对象；第二步选择角色与设置有效期，所选角色共用时间，时间校验在进入第三步前执行；第三步仅设置范围；第四步预览与保存。
- 新建和有调整资格的编辑均能增加、移除或替换指定对象；主体、固定版本、来源继续保持编辑不可变。范围类型固定，候选沿用当前来源上限和真实资源边界，提交后端重验。
- 第三步提供默认“范围配置”与“全部权限”视图。范围配置只列需要填写的独立参数，按角色→应用→资源显示关联操作及已配置/待配置状态；全部权限按角色→应用→资源→操作只读核对，保留本地搜索、每页20条。
- 独立配置单元以固定版本+参数键标识；同版本共用参数只计一次，不同角色同名参数分别统计。配置卡每页20项，顺序按角色及声明参数保持稳定；对象更新不重排卡片。全局需配置/已配置/待配置计数不受视图、搜索或页码影响。
- 可勾选“仅看未配置”，通过显式点击刷新筛选快照。正在填写的卡片保留当前位置，不因完成配置自动消失。提供“查看待配置”以及可搜索分页的未完成清单；点击具体项清除搜索、切回配置视图、跳到该项页并聚焦选择入口。
- 下一步会按所有版本/所有配置项校验。未完成时保留在第三步并显示可定位的问题清单；不能因搜索或翻页漏掉校验。元数据缺失/不兼容仍明确提示并阻止预览，不降为全部。无配置项显示“所选角色无需配置具体对象”。
- 修改时间或对象均使旧预览失效。编辑的已有对象由原关联接口回显、可正常调整；只读记录不能打开对象选择器。统一 InLoading、实体弹窗、权限背景 Token 和步骤抽屉样式。

### 本次任务

- [x] SF01（开发及自动化完成）：第二步移动有效期、时间门禁及说明，第三步保留范围门禁。
- [x] SF02（开发及自动化完成）：配置单元派生、全局进度、稳定分页、仅看未配置与跨页问题定位；全部权限独立只读视图。
- [x] SF03（开发及自动化完成）：新建与编辑对象调整、共用参数计数、同名隔离、完成不消失、跨页遗漏、无参数及预览失效的组件回归；类型、只读 lint、依赖边界及两管理台构建。
- [ ] SF04（人工验收）：新建/编辑/只读身份、第二步时间限制、超过20配置项的进度及定位、搜索分页、主题与窄屏；用户执行，独立于自动化。


### SF01–SF03 实施与验证记录

2026-10-05：开发及定向自动化完成，进入 validating；SF04 页面人工验收待执行。本轮仅调整平台分配前端流程与范围配置展示，沿用上一轮后端关联接口、原子写入和权限重验，不改公共请求/响应 DTO 或数据库。

- 第二步展示共用有效期并检查起止时间；第三步只配置范围，未完成的全量配置会阻止进入预览，展开可定位清单。
- 范围按固定版本+参数标识统计与分页；默认配置视图，全部权限独立核对。已填卡片保持原页和原位置；“仅看未配置”按显式操作记录筛选集合，“查看待配置”刷新集合。清单定位先完成搜索页码重置，再跳到目标页并聚焦，避免第三页目标被重置回第一页。
- 编辑关联对象名称只加载可见配置页，每项复用首屏关系查询；隐藏页及全部权限视图不额外查询名称。保留未加载的全部绑定 ID；分页或确认不会截断。对象修改进入原 update DTO，固定主体与版本保持，旧预览失效。
- 7 个文件 **29 项前端测试通过**：platformAssignment、BizIamAssignmentScopeStep、BizIamPlatformAssignmentDrawer、BizIamAssignmentDrawer.scope、BizIamDelegationRolePicker、BizIamDelegationCandidatePicker、BizIamTreeCandidateDialog。重点覆盖共用参数与同名隔离、45 项配置的全局统计/第三页定位、搜索不隐藏待办、编辑对象实际提交、已填筛选卡片保持、无参数提示、只读回显、第二步时间门禁以及未配置禁止预览。
- admin-common 和 platform-plugin 类型检查、8 个修改源文件/测试的只读 ESLint、`pnpm check:boundaries`、公共包及两管理台 build 通过；Vite 保留现有图标/动态导入提示。本轮后端业务代码未变，不重复执行上一轮 33 项 provider / 12 项 Java 契约回归。

复跑前端仍使用上文 7 个文件的 Vitest 命令；只读 lint 用 `pnpm exec eslint` 加修改的源文件/测试，不使用仓库会写文件的 lint 脚本。

### SF04 人工验收步骤（待执行）

1. 新建角色分配，第二步选择多个角色并设置共用有效期，第三步不再显示日期输入。反向时间或有限委派超期无法继续；直接资格及允许长期的来源按原规则处理。
2. 角色仅含全部/本人时，默认配置视图明确显示无需配置具体对象；切到全部权限核对所有操作及固定范围。范围类型不能在分配时改成更宽类型。
3. 用超过20个独立范围配置项验证：只填第一页时顶部仍显示其他页的待配置数量；下一步展开全量待办，点击第三页项目清除搜索、跳到对应页并定位。搜索和翻页不改变全局进度或绕过校验。
4. 同版本多个操作共用一个对象参数时只显示一个配置项；不同角色的同名参数分别配置。选择器确认才更新，取消保留；树对象、搜索和分页与原实体弹窗一致。
5. 编辑已有分配，在第二步调整有效期，在第三步增加、移除或替换指定对象。主体、角色版本及依据固定；修改后必须重新预览，保存后重新打开确认对象和时间正确。
6. 仅看未配置后完成一个配置，该卡片保持原位置且状态更新；查看待配置刷新清单。检查第一页/第三页之间切换、已选大集合回显，以及隐藏页不会提前请求对象名称。
7. 只读记录能核对范围与有效期，不能打开对象选择；检查窄屏、明暗主题、表单对齐、背景 Token 及 InLoading。人工验收与历史 MA05 等任务分别记录，不提前更新 current。
