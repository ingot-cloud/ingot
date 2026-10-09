# 菜单高级配置验证记录

> 日期：2026-10-09
> 状态：validating；实现与限定自动化完成，真实服务联调及上线验收待执行。
> 范围：平台应用/组织应用的菜单配置、应用创建草稿、Bootstrap 路由与页面缓存。

## 已执行结果

| 检查 | 结果 |
|---|---|
| commons Java 测试及契约导出 | 64 项通过；新增旧 JSON 兼容、参数声明校验 |
| IAM provider 全量测试 | 398 项，0 失败/错误，15 项跳过（383 项实际执行）；菜单往返、遗漏更新保留、显式关闭、冲突和整包回滚通过 |
| OpenAPI 一致性及 Python 契约 | 生成 --check 通过，7 项检查通过；132 路径/200 操作 |
| 数据库来源/种子与生成检查 | 4+4 项 Python 检查及 generate_database.py --check 通过；种子生成器保留既有 UTC 会话设置 |
| 隔离 MySQL 8.0.44 | HEAD 初始化后执行 015，30 条菜单原字段及 54 条操作关联完全一致；旧记录新增开关 false、参数列 null；新初始化通过 |
| admin-common 全量单元测试 | 25 文件/96 项通过 |
| platform 插件全量单元测试 | 65 文件/161 项通过，包含真实组件挂载的菜单交互回归 |
| admin-core 全量单元测试 | 109 文件通过，429 项通过；另有 1 项既有主题 Token 失败，见下文 |
| 全工作区类型检查 | pnpm type-check 通过；菜单组件最终补充类型检查、平台 e2e DOM 类型检查通过 |
| 只读 lint | 本次变更的 34 个源文件检查通过；最终测试文件检查通过；全工作区存在既有错误，见下文 |
| 依赖边界 | pnpm check:boundaries 通过 |
| 管理台构建 | pnpm build:admin 以及共享包构建后的平台管理台 build 通过 |
| Chromium 真实组件浏览器检查 | 1 个场景通过，含 1440×1000/390×844 两种视口、截图及窄屏无横向溢出断言；接口全部使用夹具 |

浏览器场景覆盖：详情高级配置及备注 → 直接进入对应编辑步骤 → 修改取消并确认丢弃 → 返回原分组 → 窄屏修改缓存开关 → 保存触发 Bootstrap 路由变化 → 详情抽屉保留 → 外层应用创建向导保留草稿/步骤，菜单完成不发出 POST。代码中的存量应用菜单创建/更新/删除成功均主动刷新 Bootstrap。

自动化行为覆盖：隐藏页面仍注册授权路由但排除侧栏/入口并清理空目录；实际路径参数 props、面包屑地址和导航高亮；空名称稳定生成；动态注册移除和替换；同名组件、不同参数及查询/hash 缓存行为；嵌套/跨目录返回；LRU20；配置失效立即清失活实例、活跃编辑页离开时销毁；身份/授权版本立即清缓存；相同 Bootstrap 保留状态；旧身份的延迟 Bootstrap 不恢复权限，迟到改密错误不清除新身份状态。

后端服务测试使用隔离 H2/MyBatis 及真实事务；数据库验证使用独立 Docker MySQL（无网络）并已删除容器。前端浏览器使用临时本机 Vite 和 API 夹具，服务器已关闭。上述验证期间没有执行业务库迁移、部署或提交。

## 执行方式

- 后端：`:ingot-framework:ingot-commons:test` 和 `:ingot-service:ingot-iam:ingot-iam-provider:test`。provider 全量一次 JVM 内存不足，最终通过临时 Gradle init script 设置 Test.maxHeapSize=2g/forkEvery=20；仓库测试配置未修改。
- 契约：`python3 tools/iam/build_contract.py --check`、`test_contract.py`、`generate_database.py --check`、`test_database_sources.py`、`test_generate_bootstrap.py`。
- 前端：Node 22.17.0/pnpm 10.12.4；各共享包/插件 `test:unit`、`pnpm type-check`、`eslint`（无 --fix）、`check:boundaries` 及两管理台构建。
- 浏览器：永久回归在 `apps/admin-platform/e2e/menu-configuration.spec.ts`；本次通过临时 Playwright 配置使用本机现有 Chromium 1187 和 5189 端口，输出 `menu-wide.png`/`menu-narrow.png`。未下载浏览器，也未访问真实登录或业务接口。

## 全量质量门禁中的既有失败

1. `admin-core/src/theme/defaultTokens.test.ts`：CSS 包含 `--in-permission-panel-bg`，`IN_THEME_TOKEN_NAMES` 缺少对应项。两项来源及测试文件与 HEAD 相同，本轮未修改。
2. 工作区 `pnpm lint:check`：8 个错误来自既有 dev-portal（Layout 组件名，以及 7 个 mjs 文件的 tsconfigRootDir 解析），另有既有警告。本轮变更源文件 0 lint 错误；不标记全量 lint 已通过。

这些失败不归因于菜单增量，也不在菜单任务中扩展修复。15 项 Java 跳过用例未作为通过证据。

## 待真实环境验收

MA06-B：使用真实平台及组织身份，在升级后的 IAM 服务与两个管理台验证配置往返、业务跳转的参数 props、隐藏菜单授权边界、会话刷新和整包失败回滚；实际业务页面的筛选状态、缓存淘汰和身份/授权变化清理需要结合真实页面验收。当前浏览器夹具及服务测试不冒充端到端联调。

发布顺序为 015 增量 DDL → IAM 节点 → 两管理台；旧库不得执行完整初始化文件。实际库迁移和部署不属于本次已执行操作。上线验收后再更新 current 与归档已完成增量；主 IAM change 的其他未完成事项继续保留。依据 `specs/README.md`，current 只允许已上线并验收事实，本轮不提前写入。

## 2026-10-09 代码提交

用户要求提交后，后端代码提交为 `48c36735`，前端代码提交为 `f8b91b5`（含后续抽屉、参数布局及主题色验收修正）。MA06-B 真实服务联调及上线验收仍待执行，增量保持 validating。
