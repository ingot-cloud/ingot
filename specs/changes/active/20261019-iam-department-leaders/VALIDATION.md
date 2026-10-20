# 验证与验收

验证日期：2026-10-19（Asia/Shanghai）。实现完成，状态为 validating，待用户产品验收。未执行业务环境迁移、发布或 Git 提交；current 尚未更新。

## 首轮自动验证证据（历史记录，本轮结果见下文）

| 范围 | 结果 |
| --- | --- |
| commons 全量测试 | 70 项通过 |
| IAM provider 全量测试 | 509 项中 481 项通过、28 项按环境条件跳过；使用仓库既有进程隔离配置，排除既有启动环境类 UnitTest |
| 最终定向回归 | commons 公共契约类 17 项、provider 负责人/部门类 19 项全部通过；包含新增 15 项负责人 SQL、事务、权限与隐私测试及 2 项部门删除/改根边界测试 |
| 真实 MySQL 8.4 | 28 项结构测试通过；一次性隔离容器，含复合租户外键、主负责人唯一约束、移出保留、任职/角色分配不变及 024 重复执行 |
| 前端 org 插件 | 28 个文件、77 项测试通过；含 10 项负责人状态/组件测试 |
| 前端公共包定向回归 | audit、IAM helpers、roleFields 共 21 项通过 |
| 前端静态与构建 | org vue-tsc、改动文件 ESLint、公共包构建及 admin 应用生产构建通过；保留现有动态导入/体积警告 |
| 生成工件 | 新库初始化 bundle、公共 OpenAPI（152 路径/223 操作）、内部契约（5 操作）一致性检查通过 |
| 数据库生成源测试 | test_database_sources 的 4 项测试通过，清单包含 63 张表 |

全量后端首次运行遇到既有 MyBatis 测试夹具在单进程累积造成的堆内存不足；使用已有 `tools/iam/tenant_live/verification.gradle` 隔离进程后通过，未更改业务构建配置。

工具目录 unittest 共 24 项，其中 22 项通过，2 项既有 service_naming 测试因仓库缺少 `.gitlab-ci.yml` 读取失败（HEAD 中也不存在该文件）。本次没有添加 CI 配置或跳过断言以掩盖此问题。

验证日志位于本机 `/tmp/ingot-department-leaders-*.log`，Gradle/Vitest 报告可由下列命令复现。

## 复现命令

后端仓库：

```sh
./gradlew -I tools/iam/tenant_live/verification.gradle :ingot-framework:ingot-commons:test :ingot-service:ingot-iam:ingot-iam-provider:test --offline --console=plain
./gradlew -I tools/iam/tenant_live/verification.gradle :ingot-framework:ingot-commons:test --tests '*IamAuthorizationContractTest' :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*DepartmentLeader*Test' --tests '*DepartmentServiceTest' --offline --console=plain
python3 databases/iam/test_identity_schema.py
python3 tools/iam/generate_database.py --check
python3 tools/iam/build_contract.py --check
python3 tools/iam/build_internal_contract.py --check
python3 -m unittest discover -s tools/iam -p 'test_*.py'
```

前端仓库使用 Node 22.17.0 / pnpm 10.12.4：

```sh
pnpm build:packages
pnpm --filter @ingot/org-plugin test:unit
pnpm --filter @ingot/org-plugin type-check
pnpm --filter @ingot/admin-common exec vitest run src/components/authorization-audit/useOps.test.ts src/models/iam/helpers.test.ts src/models/iam/roleFields.test.ts
pnpm --filter @ingot/admin-app build
```

## 部署兼容

既有 IAM 数据库先按迁移流程执行 `databases/iam/migrations/024_department_leaders.sql`，再部署新后端与前端。该脚本只增加关系表与 ACTION 目录，不回填负责人、不补普通角色、不更新授权分配。新库使用已同步的权威 DDL 与初始化清单。

新操作固定保留号独立于历史操作标识；重复初始化不会改动旧目录。公共模型生成同时与已存在的真实部门树、按手机号添加成员代码对齐，保留既有行为。schema 生成器统一 JSON 格式，避免 Swagger 格式差异产生整份文件变动。

回退应用时保留关系表和审计数据；旧应用不使用新增关系。不可在带真实数据的环境重建库来代替增量迁移。

## 人工验收清单（待执行）

- [ ] 给测试管理角色配置 department:read、member:read；无 department:leaders 时可看不可改，单独有 department:update 也不可维护负责人。
- [ ] 显式授予 department:leaders 及一个普通部门范围：添加两名本租户人员、跨部门兼管、指定唯一主负责人并保存。成员任职部门保持原样；根部门和导航骨架无负责人维护入口。
- [ ] 对照任命前后的已有角色分配、数据范围与 token：任免不产生额外权限；独立分配给当事人的角色在撤任后仍保留。
- [ ] 收窄 member:read 范围：隐藏关系只显示受限提示，分页总数只计可见记录。姓名隐藏/脱敏仍遵循成员管理字段策略；没有原文检索资格时姓名搜索被拒绝，接口不返回联系方式。
- [ ] 首次非空保存必须选主负责人；删除主负责人后仍非空必须明确选新主；全部移除后允许为空。跨租户、不可见成员及不可用新增/新主负责人均被拒绝。
- [ ] 暂停/移出负责人后关系仍在且显示不可用，不自动换主；恢复暂停后重新可用。仍有负责人关系（含不可用）时禁止删部门或改为根部门。
- [ ] 准备超过 20 条关系和受限关系，跨页编辑确认只保存差量，未加载或不可见关系保留。候选分页只含当前允许的有效未绑定成员。
- [ ] 两个编辑者并发保存：后提交返回 409 并保留草稿，不自动重发；重新加载后核对。切换部门/授权上下文后旧响应不能覆盖当前界面。

用户验收完成后，更新前后端 current 系统基线，再将两个 change 移入 archive；此前保持 active/validating。


## 统一编辑与组织成员选择修订验证（2026-10-19）

用户批准修订后，已更新本 change 的需求、设计、接口和任务，并更新前端 `in-coding-standards/tenant-member-picker.md` 专项规范及索引。上一版负责人独立 Tab 已替换，不存在两个保存入口。

| 范围 | 本轮结果 |
| --- | --- |
| 后端全量 | commons 70 项通过；IAM provider 523 项中 495 项通过、28 项按既有环境条件跳过，继续使用进程隔离配置排除启动环境类 UnitTest |
| 原子编辑 | DepartmentEditServiceTest 最终 7 项通过：分别仅有一种写权限、两部分一次版本、权限/审计失败整体回滚、过期版本及不可用新增不产生部分保存、目标不可见时先拒绝而不披露版本冲突 |
| SQL 数据边界 | OrganizationMemberRepositoryTest 最终 5 项通过；覆盖有效候选/已选回显、租户/账号/成员范围、分支分页计数、全局搜索、可见部门片段、组关系分页，以及分配候选的部门与委派接收人交集（排除本人、非接收人和跨租户） |
| 公共组件 | admin-common 34 文件 150 项通过；含组织成员对话框 8 项及委派候选新增 3 项：部门禁选、分支、搜索恢复、跨分支去重、关联分页差量、重新打开、单选清空、大量旧ID只补少量姓名、身份纪元/权限版本变化时失效旧请求 |
| 组织插件与抽屉 | org 28 文件 73 项、InDetailDrawer 6 项通过；包括无 Tab、按实际差量一次提交、409 保留草稿 |
| 静态与构建 | 公共包构建、org vue-tsc、改动源码 ESLint/Prettier、工作区分层边界检查，以及 admin/admin-platform 生产构建通过；现有动态导入/体积提示保留 |
| 公共契约 | 155 路径 / 227 操作、内部 5 操作、数据库初始化 bundle 一致性检查通过；本轮无新增迁移 |
| 规范 | skill quick_validate 通过（使用已有 Anaconda Python 的 PyYAML 依赖） |
| 浏览器组件验收 | 隔离临时页面使用真实公共组件和虚构数据，确认无 Tab 抽屉、统一编辑入口、部门禁选/展开、多选右栏、确认只回填姓名草稿；未调用业务保存接口 |

本轮日志：`/tmp/ingot-organization-picker-*.log`；组件截图：`/tmp/ingot-organization-member-picker.png`。临时预览文件及服务已清理。真实租户页面当前停在认证入口，本轮浏览器检查为隔离组件验收，未冒充业务环境端到端验收。首轮记录的 2 项工具测试缺少 `.gitlab-ci.yml` 问题仍是既有环境限制。

复现本轮补充验证：

```sh
# 后端
./gradlew -I tools/iam/tenant_live/verification.gradle :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*DepartmentEditServiceTest' --tests '*OrganizationMemberRepositoryTest' --offline --console=plain
# 前端（先 pnpm build:packages，再类型检查，避免并发重建声明文件）
pnpm --filter @ingot/admin-common test:unit
pnpm --filter @ingot/org-plugin test:unit
pnpm --filter @ingot/admin-core exec vitest run src/components/drawer/InDetailDrawer.test.ts
pnpm --filter @ingot/org-plugin type-check
pnpm check:boundaries
pnpm --filter @ingot/admin-app build
pnpm --filter @ingot/admin-platform-app build
```

### 本轮人工验收补充（待用户执行）

- [ ] 部门详情没有 Tab；基本信息和负责人在同一编辑会话，取消均不落库，保存整体成功/失败，版本只前进一步。
- [ ] 只授予 department:update 或 department:leaders 的测试角色分别只编辑有权字段，单独修改基本信息不需要成员/负责人维护权。
- [ ] 逐一检查租户用户组、应用人群、成员策略、角色分配/委派、诊断、所有者转交的选人入口；平台及非人员对象交互保持原语义。
- [ ] 部门只能展开，单选单栏、多选双栏；搜索跨当前候选边界而非扩大范围，清空搜索回到原分支，已选跨部门/分页保留。
- [ ] 收窄部门/成员/委派来源权限后，导航、成员及计数不泄露；没有 department:read 时仅原候选入口可用；切换身份/租户后旧弹窗关闭，迟到响应不回填。
- [ ] 超过一页的旧负责人/组成员，重复打开、增删、清空、改主及 409 后继续编辑时，不丢失未加载或不可见关系；候选不出现新的停用/移出成员。

实现完成后状态进入 validating；仍不提前更新 current 或归档，保留用户人工验收项。


## 飞书对照精修验证（2026-10-19）

本轮用户通过两张截图明确授权细化视觉与主负责人交互；实页复核了部门面包屑、48px人员行、32px头像、已选任职、首次设主与切换/移除行为。只观察/操作飞书弹窗草稿，没有保存飞书部门表单。实现前已将本轮S01-S04及兼容/权限边界写入Spec和选人skill。

- 公共组件34文件154项通过，组织插件28文件73项通过；新增覆盖首次自动设主、明确切换、移除后草稿设主、保留既有不可用主负责人、直接复用候选部门快照、面包屑祖先返回、姓名延迟搜索与关闭取消。
- commons公共契约17项、负责人服务10项、统一编辑7项通过；组织SQL最终6项、任职投影3项通过。任职投影同时要求member:read和department:read，并分别收窄到成员/部门范围；原候选准入、SQL分页/count及Token无变化。
- org类型检查、改动源码ESLint/Prettier、公共包构建、租户/平台生产构建、分层边界检查及skill校验通过。
- 公共OpenAPI155路径/227操作、内部5操作及数据库初始化bundle一致性通过；本轮不新增DDL或迁移。
- 隔离真实组件浏览器验收：冷启动下对话框宽840px，双栏内容792×420px（窄屏按视口收缩），行48px，头像32px，方形复选框。空选择右栏留白，多选有提示、人数、清空及部门主负责人标签；确认切换只回填外层草稿。浅色与深色均检查，深色头像/组织图标前景实测rgb(255,255,255)。主题主色、提示/标签浅底、边框和悬停沿框架Token。
- 图片为虚构成员的隔离组件验收，不代替业务租户端到端验收；临时页面、文件和服务均已清理。日志`/tmp/ingot-picker-polish-*.log`，截图`/tmp/ingot-picker-polished-light.png`及`/tmp/ingot-picker-polished-dark.png`。

人工验收补充：确认当前部门路径和多任职摘要符合可见范围；从空集合新增第一位自动设主，可切换及移除，打开既有受限/不可用主负责人时不自动换人。搜索仅声明当前已支持的姓名字段；“全选”仍只处理当前加载页，不选择部门或拉取组织全集。


## 选择器专用浏览与勾选修正验证（2026-10-19）

用户“开始优化这部分内容”授权本轮实施。U01-U04已完成，状态为validating，仍待真实业务环境产品验收；未提交、发布或新增数据库迁移，current未提前变更。

- 新增 `GET /v1/tenant/member-picker/browse`，按 MEMBER / DEPARTMENT_LEADER / ASSIGNMENT 复用原候选资格；部门和成员分别分页。生产租户入口接入统一浏览；少量ID回显、已绑定关系分页和平台入口保持各自原契约。
- 根条件为显式根绑定 OR 本租户真实零关系；普通部门仅直属。条件在SQL计数及分页前应用，双绑定不重复，跨租户关系不影响当前租户判断，隐藏任职不被误当作未分配。搜索使用完整场景池但不放宽来源/字段/对象范围。
- 任职元数据按成员完整SQL范围和可读部门交集批量投影；含“指定成员ID AND 多组部门条件”，避免单看ID命中。历史移出负责人仍可保留可见任职投影；默认成员查询仍排除移出。implicitRoot是单独显示投影，真实departments为空，不回填关系。
- 后端定向回归：commons契约17项；provider的组织浏览服务8、组织查询9、任职投影3、负责人服务10、负责人SQL6、统一编辑7、既有成员Repository9项均通过。共覆盖SQL真实执行、分页计数、委派接收人/排除本人、跨场景参数拒绝、无部门读取回退、隐藏根入口、搜索分支、关系兼容及权限不扩张。
- 前端公共包34文件159项、组织插件29文件75项通过。新增验证一次请求返回导航/成员、独立分页、跨分支勾选去重、搜索清空缓存恢复、关闭后的迟到响应、服务端收回部门权限时使用“可选成员”，以及负责人目标与委派来源透传、任职/虚拟根快照复用。
- 公共包构建、org类型检查、改动文件ESLint/Prettier、分层边界、租户与平台生产构建、skill校验通过。构建保留既有动态导入、体积及插件耗时提示。
- 公共OpenAPI为156路径/228操作，内部5操作和数据库初始化bundle一致性通过；bootstrap/数据库生成源10项工具测试通过。
- 隔离真实组件浏览器验证使用虚构成员：根首屏仅1次浏览，展开部门后累计2次；16px复选框的勾号保留原生50%居中，半选横线使用50%与单次translateY。浅/深主题检查勾选、半选及禁用部门；进入“测试组织 › 产品部”后兼任成员勾选保留，右栏显示真实任职摘要。临时文件、预览服务及标签均清理，截图不代替真实租户端到端验收。

日志：`/tmp/ingot-picker-browse-*.log`。截图：`/tmp/ingot-picker-browse-checked-light.png`、`/tmp/ingot-picker-browse-checked-dark.png`、`/tmp/ingot-picker-browse-department-light.png`。

本轮人工验收补充（待用户执行）：

- [ ] 在同一租户准备零任职、仅普通部门、仅根、根+普通部门、仅隐藏部门人员，核对根/分支显示及搜索；部门列表和直属人数保持原规则。
- [ ] 负责人场景仍排除已绑定人员；委派场景仍仅本人有效来源的接收对象。收窄成员/部门/字段资格后不补回隐藏资料。
- [ ] 根与部门双绑定人员跨分支勾选同步，已选按成员ID去重；大量旧负责人分页增删不丢关系；取消不保存。
- [ ] 浅深主题检查勾号/半选居中；操作权限、数据范围和token不因部门负责人或虚拟根投影改变。


## 姓名搜索ActionDenied修复（2026-10-19）

根因：新选择器按姓名搜索使用requireOriginalLookup，但租户member.displayName的运行时注册、目录filterable及HTTP FILTER接入清单仍未支持姓名。三个能力取交集后为false；这不是部门负责人应变为角色或token缺scope的问题。

修复内容：

- 补租户成员显示名筛选能力；四个实际关键词入口将OrganizationMemberNameFilter.keyword映射到displayName，精确操作为member:read。平台/通讯录能力原样保留。
- 默认平台操作上限未显式配置时允许已接入姓名筛选；显式false仍保留。普通用户默认/历史字段操作不自动打开、无新角色/分配/token授予；查询范围FULL与filterable门禁仍在SQL/count之前。拥有者也不能越过显式平台上限。
- 025增量只按完整资源和字段key更新目录能力，实际变化递增版本；不依赖数组位置，可重复执行。新库006及完整bundle同步，历史默认策略及租户规则保持原值。

验证：最终IAM provider定向46项通过（FieldAccessEvaluator11、ResourceFieldMetadata2、DefaultPolicyDefinitions3、OrganizationMemberPickerService8、OrganizationMemberRepository9、OrganizationMemberDepartments3、DepartmentLeaderService10）。使用真实字段求值覆盖普通用户无筛选许可拒绝、授权+完整可见允许、隐藏/脱敏拒绝、真实拥有者允许及平台显式禁筛仍拒绝；元数据测试使用真实FieldBindingRegistry和实际控制器注解，验证缺清单/缺目录能力均拒绝，三个能力齐全才支持。

隔离MySQL8.4两项通过：冷启动成员目录支持姓名筛选但默认筛选授权仍为false；025对反序字段数组按key更新、重复执行不重复推进版本，并核对其他资源、角色固定字段和历史默认策略没有变化。bootstrap/数据库生成源工具10项及公共156路径/228操作、内部5操作和初始化bundle一致性通过。本轮无前端行为代码变更。

日志：/tmp/ingot-picker-name-fix-{final,mysql,tools}.log。先前缺口的测试使用了字段/清单替身，本轮补充了真实求值及真实执行清单交集覆盖。

已有运行库尚未更新：需要025_member_picker_name_filter.sql与对应后端同批部署，失效既有元数据缓存/等待缓存窗口后，再在真实租户验证姓名搜索。若普通用户字段策略明确禁筛，仍应配置其真实资格，不能通过移除校验或给负责人默认授予角色解决。current仍待用户验收后更新。

## 已选快照复用与追加加载验证（2026-10-19）

本轮仅修改前端交互及同步Spec/skill；服务端路由、分页参数、权限/字段/范围判定和Token均沿用现有契约。

- 负责人字段将外层已加载的records/total/current/size传入initialBoundPage；完整已选集合不额外请求，部分集合从下一页加载。确认回传连续原始关系快照供重开复用，同时同步最新total；取消不回写父表单。
- 部门、候选成员、部分已选均支持滚动追加与居中加载更多兜底；保留选中和主负责人草稿。全选仅作用于当前分支/搜索已加载且可选的成员；平台仍使用页码。
- 以原始已加载数、服务端页位置及上一页是否为空共同判定是否还有数据；移除/去重不触发无限加载。全部展示时不渲染按钮和留白容器。失败保留已有行，重试失败页；加载中禁止重复；搜索/部门/会话/权限上下文变化后丢弃迟到响应。
- 身份/权限版本/部门变化时立即清空旧快照、人数、主负责人及差量，重新加载当前上下文；受限或未加载关系仍由差量保存保留。

自动验证：

- admin-common：34文件170项通过。覆盖外部0/1/20条完整快照、候选0/20/21条、已选20+5条、末页隐藏/无占位、滚动单请求、空页停止、失败重试同页、原始快照不受移除影响、重开复用、搜索迟到响应及平台页码兼容。
- org-plugin：30文件77项通过；负责人专项追加验证2项通过。覆盖首屏传递、后续快照回填及总数变化、增删与设主、切部门/权限版本失效，以及新旧请求竞态。
- 公共包及组织插件类型检查、公共包构建、定向ESLint/Prettier、分层边界、skill校验及两仓库diff空白检查通过。构建只有已有auth模块静态/动态导入提示。
- 日志：/tmp/ingot-picker-pages-common.log、/tmp/ingot-picker-pages-org.log、/tmp/ingot-picker-pages-leader-final.log、/tmp/ingot-picker-pages-build.log、/tmp/ingot-picker-pages-common-types.log、/tmp/ingot-picker-pages-org-types.log。

真实浏览器隔离验证（真实Vue/ElementPlus/框架主题，虚拟数据，未写入业务环境）：

- 25名候选、21个部门、25名已选；首屏候选请求1次，已选追加请求0次。候选/部门各追加1次，已选只请求下一页1次；末页三个区域的加载更多DOM均为0。
- 0条与恰好20条均无加载更多；右侧不发请求。验证滚动触发、手动兜底按钮、加载禁用及浅深主题。
- 三类加载容器均为44px高、margin 8px 8px 4px、padding 6px 0，按钮中心与容器中心偏差0px。加载状态文字使用框架次级文字色并移除按钮遮罩，保证深色可读。
- 900×640视口弹窗边界为(30,10)-(870,630)，无横向溢出；双栏独立滚动，底部确定/取消可见。已恢复浏览器主题/视口，删除临时预览文件并停止服务。
- 截图：[浅色末页（无加载更多）](/tmp/ingot-picker-pages-light.png)、[深色加载反馈](/tmp/ingot-picker-pages-loading-dark.png)、[较小视口](/tmp/ingot-picker-pages-compact.png)。

P01-P04完成，状态恢复validating；A01仍待用户在业务环境验收。不修改current，不归档，不提交、不发布。


## 部门详情聚合与上级单选验证（2026-10-19）

H01-H04实现完成，状态validating。详情保留无Tab及基本信息/负责人统一保存；本轮同步实体选择skill。新增数据仅在既有读取资格/范围内返回，不新增默认授权、角色/Token字段或数据库迁移。

- GET详情、PATCH统一编辑及具备当前部门读取资格的旧PUT，添加record.parentDepartment与record.leaderPage；上级名称按同租户department:read SQL范围读取。无权父对象不补名称，前端显示“上级部门信息受限”；原parentId仅作为稳定引用。
- leaderPage复用原关联查询、20条首屏、摘要人数和原始字段投影。缺少成员读取资格不查询人员、不附首屏；隐藏关系仅保留restricted语义。树/通讯录/列表不装配详情聚合，保留独立/leaders后续分页。旧PUT缺少read或read范围不覆盖目标仍可执行原更新，省略新增聚合。
- 负责人组件直接初始化聚合首屏；进入/退出编辑不再重查首屏，保存返回新版本聚合直接回显。仅确定未设置时显示ASCII横杠；受限/不可用/未加载关系继续原规则。
- 顶部56×56部门图标与名称，图标底使用框架主题色，前景用框架实色标签文字Token，避免深色模式将in-white当作纯白。InDetailIdentity新增可选avatar槽，不改变默认头像/上传行为。
- MemberDepartmentField扩展multiple=false，共用树、搜索、加载/重试、名称快照和上下文失效；单栏radio，排除自身、子树和navigationOnly。当前及子节点仍可通过箭头展开。多选默认、主部门顺序和拖曳保留。录入框支持Enter/Space；确认才回填、取消不改；同上下文进行中的树请求合并。

验证：

- 后端commons公共模型17项，provider部门详情/负责人/统一编辑/关联仓储/父名称真实SQL32项，全部通过。覆盖父对象读取范围/跨租户、无成员读取时不查人员、姓名字段投影、首屏20及复用计数、旧PUT读取范围兼容、版本/原子保存与权限边界。
- 前端组织插件31文件84项、公共包34文件170项、核心身份区/抽屉定向2文件10项通过（合计264项）。覆盖聚合传递、空/受限文案、单选禁选和子树展开、取消、确认回填ID、旧多选、权限变更迟到响应、进行中请求合并和原负责人追加加载。
- 核心包、公共包构建/类型检查，组织插件类型检查，admin应用完整构建及最终CSS重构建、定向ESLint/Prettier、分层边界及skill校验通过。保留既有分包体积/静态动态导入提示。
- 契约生成与复现检查通过（156路径、228操作），tools契约8项及数据库源一致性4项通过。补齐既有场景browse的契约断言：只有明确的两条租户路由允许无单一ACTION；browse必须声明必填scene及三个封闭值。
- 核心包全量114文件474项中473通过，既有顶栏视觉断言失败：visual-contract.test.ts期待InAppBar包含旧margin字面量；另有bootstrap.integration.spec.ts在环境销毁后的useGlobalLoading定时器window引用错误。这些文件与HEAD一致，本轮未修改；相关核心组件10项独立回归通过。未将这次全量运行记为成功。

真实浏览器隔离验证（真实Vue/ElementPlus/框架主题与请求客户端，虚拟数据）：

- 首屏只有GET /departments/11，不调用/leaders，也不为上级名称请求完整树。打开部门单选才请求部门树；HMR后的同一组件重开复用缓存。外层保存一次PATCH，响应回显上级“产品部”，没有追加详情或负责人首屏查询。
- 单选当前上级正确回显；研发部及研发一组禁选，箭头可展开；选择产品部后取消仍为原上级，再次选择确认回填产品部。保存后负责人空态为“-”。
- 部门图标实测56×56，深色前景rgb(255,255,255)，对话框边框使用框架Token；单选圆点与图标间距、禁用/当前状态及浅深主题均核对。临时预览、标签和服务器已清理，主题已恢复。
- 截图：[浅色详情](/tmp/ingot-dept-detail-light.png)、[浅色上级单选](/tmp/ingot-dept-parent-picker-light.png)、[深色详情](/tmp/ingot-dept-detail-dark.png)、[深色上级单选](/tmp/ingot-dept-parent-picker-dark.png)。

日志：/tmp/ingot-dept-detail-backend-final.log、/tmp/ingot-dept-detail-org.log、/tmp/ingot-dept-detail-common.log、/tmp/ingot-dept-detail-core-target.log、/tmp/ingot-dept-detail-core.log、/tmp/ingot-dept-detail-app-build.log、/tmp/ingot-dept-detail-app-final-build.log、/tmp/ingot-dept-detail-contract-tools.log。

A01仍待业务环境人工验收；未提前更新current/归档，未提交、迁移或发布。本轮前后端需配套部署以消费新增详情字段。

## 组织选人InvalidArgument参数分类修复（2026-10-19）

根因：此前姓名搜索FILTER声明只包含keyword，但FieldFilterInterceptor将整个query交给FieldInputCollector分类。browse合法的scene、targetDepartmentId、page/pageSize、departmentPage/departmentPageSize因未声明而在业务查询前被拒绝；同一声明还影响成员picker、负责人绑定分页与旧候选接口。此错误不来自拥有者权限或Token。

修复：四个入口使用与各自query参数匹配的FILTER DTO，keyword继续映射member.displayName；分页、场景和引用参数显式PublicField分类。保留框架未知键拒绝、业务场景/引用/分页校验及所有原操作、对象、字段权限。没有新路由、公共参数、默认授权、DDL或Token变更。

验证采用实际控制器注解、FieldBindingRegistry、FieldFilterInterceptor及框架StringToEnumConverterFactory的MockMvc请求；业务服务使用替身以定位HTTP接入边界，既有服务/SQL/字段求值测试另行真实执行。修复前7项HTTP测试中5项失败：用户完整请求及其他合法请求期望200却得到400，业务ActionDenied也被前置400遮蔽；修复后7项全部通过，覆盖三场景、默认页、独立页码、导航/姓名搜索、三个兼容入口、逐接口未知参数拒绝、仅keyword受控映射及ActionDenied仍为403。

最终provider定向51项通过：TenantMemberPickerAPITest 7、OrganizationMemberPickerServiceTest 8、DepartmentLeaderServiceTest 11、OrganizationMemberRepositoryTest 9、OrganizationMemberDepartmentsTest 3、ResourceFieldMetadataTest 2、FieldAccessEvaluatorTest 11。公共契约检查156路径/228操作不变，tools契约8项通过，git diff --check通过。

复现：

```sh
./gradlew -I tools/iam/tenant_live/verification.gradle :ingot-service:ingot-iam:ingot-iam-provider:test --tests '*TenantMemberPickerAPITest' --tests '*OrganizationMemberPickerServiceTest' --tests '*DepartmentLeaderServiceTest' --tests '*OrganizationMemberRepositoryTest' --tests '*OrganizationMemberDepartmentsTest' --tests '*ResourceFieldMetadataTest' --tests '*FieldAccessEvaluatorTest' --offline --console=plain
python3 tools/iam/build_contract.py --check
python3 -m unittest discover -s tools/iam -p 'test_contract.py'
```

日志：/tmp/ingot-picker-http-red.log（修复前失败复现）、/tmp/ingot-picker-http-green.log（修复后通过）。需要重新构建并重启当前IAM实例，使commons分类DTO及provider控制器声明共同生效；本轮未重启用户服务或执行业务数据库迁移，真实业务环境仍待A01验收。current不提前更新，状态validating，不归档、不提交。

## 负责人头像投影（2026-10-20）

负责人关联页/候选、主负责人摘要及普通成员候选SQL在原范围内读取成员avatar；摘要用主负责人条件聚合，不从其他负责人取头像。姓名与头像经同一MemberRecord字段投影分别受控后装配，不附带联系方式。DepartmentLeaderRecord和OrganizationMemberPickerOption新增可选avatar，保留旧构造器，IAM ReadAvatar mixin通过@OssUrl签发响应；库存路径不变。browse在MEMBER/DEPARTMENT_LEADER及implicitRoot重装配时保留已投影头像，ASSIGNMENT保持原投影。无DDL、Token、角色、字段上限或候选范围变化。

验证：commons公共契约17项、provider定向49项全部通过（DepartmentLeaderService11、DepartmentLeaderRepository6、OrganizationMemberRepository9、OrganizationMemberPickerService8、IamOssJacksonConfiguration3、FieldAccessEvaluator12）。SQL覆盖绑定页及主负责人头像一致、不用非主负责人的头像；真实字段投影覆盖FULL保留/隐藏省略且原对象不变；服务覆盖头像进入投影、姓名隐藏时头像仍可独立授权、隐藏头像不透出；OSS验证两个新DTO把库存路径签发为链接；组织浏览重装配保留头像及原分支/资格。

公共契约156路径/228操作，两个DTO仅增加非必填avatar；生成及复现检查、工具契约8项、git diff --check通过。日志：/tmp/ingot-leader-avatar-backend.log、/tmp/ingot-leader-avatar-service-final.log。配套前端的浅深主题隔离组件核验见其VALIDATION.md，未访问或修改真实业务数据。

需要重新构建/重启IAM并配套更新前端以展示真实头像，本轮未重启用户服务、未部署、迁移或提交。V01-V03完成，状态validating；真实上传头像及租户权限切换验收仍归A01，current不提前更新。
