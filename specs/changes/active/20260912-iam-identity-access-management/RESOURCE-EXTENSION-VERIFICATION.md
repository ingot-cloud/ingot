# 资源扩展增量验收

开发验证与人工验收分开记录。只在隔离库验证；本轮未更新 current、未提交代码。

## 部署准备

框架尚未投产，平台字段模型直接替换。本次源码调整不操作已有测试库。测试前由用户确认目标测试库，使用最新 `databases/ingot_iam.sql` 重建，再导入测试身份；正式种子会创建显式字段快照，phone/email 默认脱敏。不要在旧测试库只改配置开关，旧版表和快照结构已不再支持。

2026-10-06 初始化调整：完整文件已包含56张清单表的 `DROP TABLE IF EXISTS`，无需先手工删除这些表；重复导入会清空账号、授权和业务数据。先停止服务、确认库名并备份，再执行完整文件和可选人工身份种子（每次重建后一次），随后启动同一版本服务、重新登录。目录菜单默认使用 `layout.main`；单独执行006仍只补缺，不能修复旧表结构或重置旧版本。

部署同一版本的 IAM 全部节点、SDK 消费服务和前端，无需 `ingot.iam.role-fields.enabled`。平台独立成员/组策略接口、菜单、专用表及012/013迁移已删除。共享角色、租户字段策略和通讯录保持原行为。v2只接受资源和操作，每个精确操作返回字段结论，不再有请求能力标志或响应顶层兼容字段；缺失快照/字段结论明确拒绝。

治理账号不自动获得完整字段。已有角色版本、分配不被种子扩权；治理账号想看 FULL 也要显式创建、发布并分配含实际成员读操作及字段 FULL 的平台自定义角色。角色字段配置复用 role:create/publish/preview 等治理权限，旧平台字段策略菜单、快捷入口和专门配置权限退出新流程。版本升级仍需 assignment:upgrade 或本人允许该固定版本的委派。

远程资源按 `examples/iam-ops` 注册完整 ResourceKey 和真实字段/默认能力。密钥至少32字节，保持可信原身份转发。启用后回退必须先恢复等价字段限制，不得让旧 IAM 节点直接处理新字段角色。

## 人工验收（待执行）

### 原步骤替代说明

原“按成员／组独立配置平台字段策略，多匹配 HIDDEN 优先”步骤被 [角色字段改造](ROLE-FIELD-AUTHORIZATION-REFINEMENT.md) 替代，不能继续按旧流程验收。历史独立策略及其审计保留作迁移依据。新模型在同一操作、同一目标上取 FULL > MASKED > HIDDEN 的授权并集。以下所有人工项均尚未执行。

### 平台角色字段闭环

- [ ] 资源能力：应用目录 → 平台管理 → 成员，核对 phone/email 支持的隐藏、脱敏、完整和编辑选项。这里只有能力声明，无适用成员/组配置；收窄后选项与后端结果同步受限。原值 phone/email 搜索和排序仍不支持，不因目录勾选而开放。保存能力截图及目录响应。
- [ ] 安全默认：治理账号不增补字段角色，查看平台人员列表、详情的 phone/email 均脱敏。创建自定义角色仅含成员 read，字段步骤默认 phone/email 脱敏；发布 v1 并分配给测试成员 A，A 登录验证同样结果。记录角色版本、分配 ID、登录主体及脱敏响应。
- [ ] 隐藏与完整：创建成员 read 的 HIDDEN 和 FULL 字段角色，分别分配给 B、C（固定版本）。B 列表/详情没有 phone/email 原值；C 同一接口完整可见。用接口响应检查，不能只看输入框。治理账号显式分配 FULL 后才完整可见。
- [ ] 多角色同目标：给 A 同时分配 HIDDEN/MASKED/FULL，对相同成员目标完整优先；撤销 FULL 后脱敏，撤销 MASKED 后隐藏。委派没有增加业务字段权限；记录各次字段访问结论和来源。
- [ ] 范围交叉：FULL 角色的成员 read 设指定对象 X，MASKED 角色 read 设全部。A 查 X 完整，查 Y 脱敏；HIDDEN/FULL 在不相交对象上不能拼出全域完整。对仅 FULL-X 的账号查 Y 被对象范围拒绝或不在列表；保存对象绑定及 X/Y 请求。
- [ ] 用户组：FULL 角色分配给非空组 G，A 加入 G 后匹配；移除 A 后重新登录/等待失效传播，FULL 不残留。直接分配撤销、截止到期、成员停用、委派撤销/到期同样检查；如有其他有效 FULL 来源，仍完整并显示真实来源。
- [ ] 字段写入：对应成员 update 的固定版本必须对真实目标提供 FULL+editable 才能提交。只读 read 的 FULL 叠加 update 的 MASKED 仍拒绝；显式 null、未知字段、`***`、隐藏/脱敏字段不能绕过校验。平台人员列表/详情显示的可编辑能力也必须来源于 update。保存实际 PATCH、响应及数据库无非法变化证据。创建 displayName/avatar 同样由 create 操作校验。
- [ ] 版本固定：v1 phone MASKED，发布 v2 FULL 后原分配仍用 v1。版本历史、升级预览显示字段差异；显式升级后才 FULL。已有分配 ID/主体/期限/来源保留；版本新字段未声明时 HIDDEN；并发角色/分配变化按409和最新能力重验。
- [ ] 目录与执行：收窄字段目录能力后输出不能超上限；未知字段、非角色操作资源、未注册执行能力、MASKED+editable 发布被拒绝。角色预览只读，提交重新校验。缺失角色字段快照或精确操作字段结论时明确503拒绝；共享/租户及通讯录流程保持原行为。
- [ ] iam-ops 导出：注册 title/contact 后，角色配置 read/update/export 及资源字段权限，按 UUID 指定对象分配。列表/count/详情/export 使用各自操作和真实对象范围，隐藏原值省略、脱敏在服务端完成。READ 的 FULL 不能补 UPDATE/EXPORT 不同来源的字段权限。平台人员本轮没有导出接口，不在其页面验收导出。
- [ ] 页面：字段按应用/资源配置一次，默认值来自目录；搜索、20条分页、跨页保留、全局进度和错误定位准确。移除权限资源清理字段草稿；任何范围/字段/信息变更使预览失效。固定版本在分配、委派只读展示，没有成员/组覆盖入口。检查 InLoading、窄屏及深浅主题并保存截图。

### 保留的资源、升级和委派验收

- [ ] 初始化菜单：治理账号登录，在平台治理应用的“平台管理”看到全局账号；独立开发者应用显示生成二维码、客户端管理、社交管理和业务ID管理，目录布局为layout.main。逐页检查客户端查询/详情/创建/修改/删除/重置密钥以及社交、业务ID操作的原接口权限。没有开发者操作的普通平台成员不出现对应ACTION页，租户账号没有平台开发者授权。保存应用归属、菜单响应和接口证据。
- [ ] 自定义应用：iam-ops/incident 注册后，角色 OBJECT_SET 候选为 UUID 工单；同名其他应用不会混用；列表/count/详情/导出范围一致。停止资源服务，候选显示503且不能保存越界绑定；停止 IAM，写鉴权不使用旧热缓存。
- [ ] 同一角色 v1→v2：单条/批量入口默认最新可用固定版本；预览显示新增/删除操作及对象绑定变化，新增参数必须配置；主体、分配ID、授权时间、期限、来源保持不变。更晚发布v3不改变本次v2目标。
- [ ] 原子性与字段版本：选择两条记录，另一窗口修改其中一条；整批升级409且任何记录都未升级。撤销/到期记录不能恢复，未来生效记录保留原生效时间。
- [ ] 纯受限管理员：自己的委派先只含v1，v2候选不可见；治理管理员显式加入v2及上限后可升级。不能升级直接记录、他人委派、其他角色、越界对象、越界组或期限；来源撤销后保存拒绝。
- [ ] 兼具治理与委派资格：完整资格独立生效，不被小范围委派收窄；业务操作权限不因委派自动增加。
- [ ] 页面：选择器搜索/分页/跨页回显、确认取消、加载样式、迟到响应、窄屏和深浅主题；草稿改变必须重新预览，提交防重复。

验收通过后才能更新 current 并归档本增量。直接替换前的自动化证据保留作历史；直接替换后的证据记录在 ROLE-FIELD-AUTHORIZATION-REFINEMENT.md；原资源扩展证据保留在 RESOURCE-EXTENSION-REFINEMENT.md。


## 自动化复核命令

后端在仓库根目录运行：

```sh
cat > /tmp/iam-role-field-test.init.gradle <<'GRADLE'
allprojects { tasks.withType(Test).configureEach { maxHeapSize = '1536m'; forkEvery = 40 } }
GRADLE
IAM_MYSQL_HTTP_TEST=true ./gradlew -I /tmp/iam-role-field-test.init.gradle :ingot-framework:ingot-authorization:test :ingot-framework:ingot-commons:test :ingot-service:ingot-iam:ingot-iam-provider:test :examples:iam-ops:compileJava
python3 databases/iam/test_identity_schema.py
python3 databases/iam/test_bootstrap_seed.py
python3 tools/iam/build_contract.py --check
python3 tools/iam/build_internal_contract.py --check
python3 tools/iam/test_contract.py
```

MySQL验证需要可用Docker，脚本使用隔离库/容器。本轮测试使用临时Gradle init设置 `tasks.withType(Test).configureEach { maxHeapSize = "1536m"; forkEvery = 40 }`；大夹具共用默认512m可能耗尽堆，建议 CI 同样分批启动测试进程。

管理台仓库使用项目Node环境运行：

```sh
pnpm --filter @ingot/admin-common --filter @ingot/platform-plugin test:unit
pnpm --filter @ingot/admin-common --filter @ingot/platform-plugin type-check
pnpm check:boundaries
pnpm build:admin-platform
pnpm --filter @ingot/admin-app build
```

组织管理台构建前复用已编译公共包/主题。只读lint使用 `pnpm exec eslint` 加本次修改的TS/Vue文件，不能用带 `--fix` 的默认lint脚本代替检查。

## 部署后联调和性能（RE09待执行）

- [ ] 使用真实登录身份，分别检验治理、纯委派和兼具资格的页面与请求，覆盖新权限显式发布前后；未持新精确操作不能创建/发布平台字段角色或访问完整升级入口。
- [ ] 独立iam-ops部署后，用相同完整ResourceKey比较本地/远程求值与对象查询；跨应用同名资源、身份/租户替换均拒绝。授权到期、组变化、多节点失效、IAM或对象服务停止时无过期放行。
- [ ] 保存前发生角色发布、撤权、委派收窄、组扩大及记录版本变化，复核冲突和整批回滚；接口不能用预览结果绕过新检查。
- [ ] 在生产相近数据量下记录候选、角色字段元数据和执行快照读取、成员分页及100条升级的冷/热耗时、SQL/RPC次数与P95；确认列表不逐行字段RPC，分页计数与实际范围一致。当前未给出真实服务性能结论。
- [ ] 保存验收截图及HTTP/数据库证据，完成后再更新current与归档，不把本轮隔离测试当作部署成功。

## 本轮状态记录

- 开发与自动化：以 ROLE-FIELD-AUTHORIZATION-REFINEMENT.md 的“直接替换后最终证据”（DC01–DC04）为准。
- 部署、用户人工、真实账号联调及真实性能：全部待执行，RF08 未完成；本文不提前勾选任何人工项。
