# 平台超级管理员与应用导航

状态：implementing（代码已完成，待用户人工验收）。2026-10-06 用户在讨论中确认仅平台域、允许超级管理员受控直接分配，随后明确要求实施。主 change 保持 implementing。

## 已确认规则

- 初始化平台 SYSTEM 角色改名“超级管理员”，编码统一使用 RoleConstants.ROLE_ADMIN_CODE（role_admin）。不按账号名、显示名称或客户端提交的角色判断身份。
- 有效、直接、非委派的平台成员系统角色分配提供全部平台操作、ALL 对象范围和注册业务字段 FULL；写入仍受真实字段可写能力约束，未注册的内部凭证字段不公开。新增平台资源和操作自动覆盖；租户身份和接口不继承平台超管资格。
- 普通平台角色继续执行固定版本、字段权限、对象范围和委派限制。系统角色不可编辑，自定义角色禁止占用超管保留编码。只允许现有超级管理员直接授予或撤销超管；不能向组分配或经委派分配。
- 在同一授权锁及事务内重验超管授予、撤销、期限和人员/账号的行政停用、移除、锁定；不得使最后一个长期有效、可用超级管理员失去资格。自动安全锁定仍按安全框架处理。
- 平台管理接口采用 AdminOrHasAnyAuthority 的准入表达式，表达式对 IAM 身份使用在线可信有效授权，不能靠过期 JWT 中的 role_admin 放行。保留 IamAccess、对象范围、业务校验、审计和写入事务中的重验；受限委派入口保持可用。
- 授权求值、候选、对象能力、SDK、诊断及导航使用同一超管事实。撤销、到期、成员和账号状态变化失效相关缓存。依赖失败继续拒绝。

## 导航与页面

- 复用 bootstrap 的应用、图标、排序和菜单 applicationId；平台前端顶栏最多直出两个应用，当前应用优先，其他进入更多，窄屏继续收纳。侧栏显示当前应用菜单，刷新、深链接、搜索和路由切换同步应用；无可见菜单的应用不作为空导航入口。
- 应用图标支持 Iconify 和已签发图片地址，缺少或加载失败采用统一默认图标。菜单本身仍由后端授权过滤；切换应用不删除其他已授权路由。
- 二维码移除重复配置标题，配置区宽度与分栏一致，统一表单间距和选择器宽度，保持生成及两种下载行为。
- 全局账号列表及详情启用/锁定保持两项独立语义，使用框架状态组件及主题变量。

## 接口、初始化和规格

保持业务提交结构；按需要补充当前身份的可信角色摘要，并同步正式 OpenAPI、夹具及前端来源。内置超管特权只由服务器求值产生，诊断展示真实系统角色分配来源。角色字段旧验收中“治理账号默认脱敏”的预期标记为本增量替代，普通角色的字段验收保留。

测试阶段直接使用最新正式初始化，不保留 platform-governance 的兼容超管判定。初始化生成器读取/校验 Java 保留编码，内置字段快照改为 FULL。已有测试库由用户停止全部 IAM 节点后重建、选择一种身份初始化方式再启动；本次不操作实际库。

## 任务与验收

- [x] SN01 记录两端需求、设计、接口和任务并启动实施
- [x] SN02 可信超管求值、范围、字段、SDK与在线注解准入
- [x] SN03 超管分配门禁、保留编码和最后可用超管保护
- [x] SN04 初始化、契约、框架接入说明和人工验收同步
- [x] SN05 应用导航与页面修正
- [x] SN06 本增量相关自动化已执行通过（全量仓库既有失败单列，不标记全量通过）：普通/超管/伪造角色/租户身份、动态新增操作、FULL读写、撤权到期、委派拒绝、最后超管和并发原子性；真实HTTP/MySQL、SDK、前端导航与组件、类型、只读lint、依赖边界和两管理台构建
- [ ] SN07 用户人工：初始化登录、应用图标/更多/侧栏/深链接、三类身份、最后超管门禁、二维码与状态、主题和窄屏；人工项独立于开发和自动化，不更新current、不归档

授权缓存载荷新增服务器超管事实，继续使用原分层框架并更新载荷命名空间，旧 Redis 快照不用于新模型。平台账号行政变更也在提交后广播失效。接口注解对 IAM 会话使用精确在线集合，租户上下文拒绝平台超管标记；无在线端口或依赖失败不退回 JWT。


## 实施结果与证据（2026-10-06）

- 超管事实由定义、版本、有效直接分配、成员、账号及锁定态共同确认；非法平台SYSTEM组/委派或旧系统记录不按普通角色继续贡献业务和字段权限，诊断同样排除；启用平台操作批量查询，SDK继续接收同一逐操作ALL/FULL条款。接口注解只读本次服务器事实，业务码和JWT中的role_admin文本不能提升资格。内部快照新增布尔platformAdministrator，租户恒false。
- 超管只在现有超管的直接分配树/普通候选中可选，委派候选和提交禁止；历史只读回显沿用详情记录边界。修改/撤销、人员快捷替换、成员状态和账号行政状态保护最后长期可用超管；共同锁、事务内重验、失败回滚、提交失效均已接入。
- 初始化生成器从RoleConstants导出编码；正式SQL中平台SYSTEM为超级管理员、平台成员字段FULL，内置应用图标补齐。测试阶段没有旧platform-governance识别、迁移开关或兼容扩权；没有执行用户实际库。
- 应用导航复用bootstrap，保留所有授权路由，只按当前应用过滤侧栏，最多直出2项，支持图标和失败回退；二维码配置/预览及全局账号独立状态标签完成。人工步骤已同步RESOURCE-EXTENSION-VERIFICATION.md，普通角色与系统超管预期分开。

通过的检查：IAM全量362项（含隔离MySQL/HTTP；RoleWorkspaceMySqlHttpTest为11项，新增实际系统来源/账号失效和并发最后超管回滚），最后账号事务重验/快照收紧再通过13项，来源保护完成后全量362项复跑通过；SDK17项；在线注解3项；数据范围模块12项（含远程快照3项）；隔离MySQL初始化15项；契约/数据库来源/种子生成器15项。公开OpenAPI保持129路径/196操作，内部为3个端点，生成一致性通过。前端平台插件57文件/116项通过，最后导航目录10文件/32项通过；全工作区类型、相关只读lint、依赖边界、公共包/主题和两管理台构建通过。

额外全量检查的既有失败（本次未修改对应实现）：

1. security-common的InUserIdentityTest.callerCannotMutateBoundDepartmentList：getDeptIds返回可变副本，原测试要求不可变；新增在线注解测试通过。
2. admin-core全量397项中396通过，defaultTokens“公开Token名与浅色CSS声明一致”失败，既有--in-permission-panel-bg出现在公开名称中但缺少对应浅色声明；本次未改Token定义。增量导航与图标测试通过。
3. tools/iam全量服务命名检查的2项依赖缺失.gitlab-ci.yml而失败；相关契约/生成器/数据库来源15项独立通过。

复核命令：

```sh
IAM_MYSQL_HTTP_TEST=true ./gradlew -I /tmp/iam-role-field-test.init.gradle :ingot-service:ingot-iam:ingot-iam-provider:test
./gradlew :ingot-framework:ingot-authorization:test :ingot-framework:ingot-data:ingot-data-mybatis-scope:test
./gradlew :ingot-framework:ingot-security:ingot-security-common:test --tests '*InSecurityExpressionTest'
python3 databases/iam/test_bootstrap_seed.py
PYTHONPATH=tools/iam python3 -m unittest test_contract test_database_sources test_generate_bootstrap
python3 tools/iam/build_contract.py --check
python3 tools/iam/build_internal_contract.py --check
```

前端执行pnpm type-check、pnpm check:boundaries、pnpm --filter @ingot/platform-plugin test:unit、pnpm --filter @ingot/admin-core exec vitest run src/layouts/widgets/header、pnpm build:packages、pnpm build:themes及两管理台build。只读lint仅对本次TS/Vue变更运行；不要用默认带--fix的lint替代检查。测试临时提高Gradle堆到1536m并单worker分批，配置文件示例见人工验收文档。

SN07全部未执行：用户目标测试库重建、同版本节点部署、实际登录、页面窄屏/主题与业务联调。开发和相关自动化不代替人工验收；active保留implementing，不更新current、不归档、不提交。
