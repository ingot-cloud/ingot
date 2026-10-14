# 验证与发布记录

2026-10-14，用户明确确认本 Spec 已验收完成，要求归档并提交。current 已按最终实现固化，状态 completed；验收结论以本次用户确认为准，本任务不重复执行部署或目标业务库升级。

下述 2026-10-12 的自动化、待部署及联调记录保留为历史实施证据，不再代表当前未完成状态。

## 已通过的验证

| 范围 | 结果与覆盖 |
|---|---|
| 生成一致性 | generate_database.py --check；4 应用、42 资源、154 操作、36 菜单、68 菜单操作关联；安全中心6资源16操作；5项生成器测试 |
| 隔离 MySQL | 最终补丁全量28项种子/升级用例；空库、已有部分目录、保留ID与人工配置、已有固定版本/授权/分配/委派上限/审计引用、重复升级、占用ID、dry-run、路径/资源/自定义关联冲突回滚及旧目录自定义子项 |
| 后端模块 | Security 36项；IAM 428项普通测试；授权SDK 30项；ALL与窄范围拒绝、过期/撤权/跨域/依赖故障，真实HTTP只读GET允许而PUT拒绝、SDK写fresh及读取缓存限制 |
| IAM MySQL HTTP | 显式启用 IAM_MYSQL_HTTP_TEST=true，既有23项可选集成测试通过，测试自行创建并清理隔离MySQL；不连接既有库 |
| 前端单元 | admin-common 133项、platform 179项、org 35项、security 27项，共374项；共享审计、固定域API及伪造域过滤、精确能力、撤权后Tab切换和非激活面板不请求 |
| 工程 | 三插件类型检查、admin-core/admin-common构建及类型检查、两宿主构建、只读eslint零错误零警告、分层边界及git diff --check |
| 真实页面 | 两宿主生产预览共13项Playwright测试通过，业务API均为夹具；八页深链接/刷新/搜索、平台治理与安全应用切换、独立字段策略Tab、仅并发/登录失败读取权限、只读无写按钮、审计筛选/详情/空结果/错误/拒绝、480px窄屏与深色主题 |

IAM全量测试在本次运行使用临时init脚本将测试JVM堆设为2GiB，不修改项目构建配置。浏览器使用已缓存Chromium，截图等待加载遮罩退出后检查实际页面。

复现入口：后端 tools/iam/generate_database.py --check、tools/iam/test_generate_bootstrap.py、databases/iam/test_capability_migration.py；Gradle运行Security/IAM provider及ingot-access-control的test任务，IAM MySQL HTTP用 IAM_MYSQL_HTTP_TEST=true 与 --tests '*RoleWorkspaceMySqlHttpTest'。前端按仓库Node/pnpm版本运行四包test:unit、check:boundaries及两管理台build；在apps/admin-platform运行 pnpm exec playwright test --config playwright.capabilities.config.ts。可用 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH 指定本机缓存浏览器。

## 部署与人工验收

1. 完整备份目标IAM库并停止相关写入；在同一目标库以 @iam_capability_dry_run=TRUE 执行020，检查应用、六资源、十六操作及八页面清单。任何冲突先处理，禁止mysql --force。
2. 执行020；同批发布IAM、Security和两管理台，失效授权/路由缓存，至少等待SDK最长30秒热窗口，重新bootstrap。
3. 普通角色经既有发布/分配流程配置：安全只读、策略维护、会话处置、字典只读/维护、各域审计、成员策略分别读取/更新/预览。现存普通角色不会自动补权，既有固定版本和分配不改写。
4. 验证无权限平台人员、租户身份无法执行安全接口；直接调用写API只读角色被拒绝。验证撤权、到期、账号/成员停用、应用/操作停用以及实际IAM依赖故障。读取按SDK缓存窗口验收，写入立即fresh拒绝。
5. 检查实际菜单中的新URL、注册键、路由名、父目录和人工配置；旧URL不作为功能入口。租户宿主仅org即可使用成员访问策略与组织审计。
6. 回滚停新版写入，按完整备份和输出ID恢复目录与关联并回退应用；不重建IAM库。验收确认且线上生效后更新两端current并归档。

菜单访问模式保留人工配置；仅访问防护、会话管理、成员访问策略三个多读取能力入口按批准规则调整为ANY。正式新菜单为ACTION/ANY。无法保留的跨应用自定义操作关联明确中止，避免静默丢失。

## 内部授权联调缺陷修复（2026-10-12）

用户提供凭证策略 list 请求。本地 Security 日志 12:24:24 显示 snapshot RPC 得到 S0403 / AuthorizationDenied，在线改密门禁随后返回 AuthorizationUnavailable；故障发生在业务权限检查之前。通用 INNER 链没有 Bearer 认证，快照缺失成员上下文；菜单由浏览器直接访问 IAM，使用不同认证链，不能证明内部 RPC 已认证。系统超级管理员按已有可信系统直接分配动态拥有所有启用平台操作，本次无需补充角色授权。

已增加仅匹配三个 POST 内部授权端点的优先认证链，保留内部调用头检查，恢复 JWT 与在线会话身份。真实生产通用 INNER 链与专用链共同装配的新增8项测试通过：快照、evaluate/preview、普通角色、无 Token/伪造账号头、无效签名/撤销会话、缺少内部调用头、请求体替代身份、强制改密快照。JWT decoder 和数据依赖为夹具，未注入固定 SecurityContext。

关联回归命令为 Gradle IAM provider、IAM API、Security provider、授权 SDK 的 test（offline，2GiB测试堆）。IAM provider 436项执行通过、23项 MySQL opt-in 本次跳过；IAM API 3项、Security 36项通过，SDK 30项结果有效且任务 UP-TO-DATE。此前已执行的 MySQL 验证记录保持不变。git diff --check 通过。

2026-10-12 联调修复时，Agent 未重启运行中的 IAM 进程，部署及实际会话验证当时待人工完成。修复不改数据库、菜单、角色或业务 API 契约；2026-10-14 用户确认完整验收完成，本次据此结项，不把人工确认表述为 Agent 新增压测或部署证据。

## 结项检查（2026-10-14）

- 用户确认人工验收完成；T7 已完成。
- 固化八页入口、六安全资源十六操作、固定域审计、成员策略、SDK 执行、内部授权身份恢复与保留数据升级规则。
- 修正会话管理基线的旧操作码、网关前缀及权限种子说明。
- 主 IAM 与前端关联链接指向归档；其余 active change 保持原状态。
- 本次为文档变更，检查归档状态、任务、相对链接及 git diff --check；沿用上文已通过的功能、升级和工程测试，不新增部署或重跑业务测试。
