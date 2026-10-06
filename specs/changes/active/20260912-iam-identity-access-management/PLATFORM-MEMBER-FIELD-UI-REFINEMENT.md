# 平台成员字段展示与对象编辑边界

> 状态：validating（approved → implementing → validating），2026-10-06；用户逐项确认后明确要求“实施刚刚说的内容”。开发与相关自动化完成，人工 MF04 待执行；不代表整个 IAM change 已验收。

## 已确认需求与设计

仅补齐平台成员列表、详情、创建与编辑的字段闭环，保留其他未提交修改，不修改 current、不提交、不变更真实数据库。角色字段模型、对象范围与租户行为保持既有设计。

- HIDDEN 不显示列、标签、头像或表单控件；列设置不能恢复隐藏字段。读视图 MASKED 只显示后端脱敏值，编辑草稿不填入脱敏字符串；只读字段保持只读，空的禁用控件不得产生清空提交。FULL+editable 且对应对象允许写入时才开放编辑。
- 平台列表默认增加联系邮箱。列可见性来自服务端整份有效读策略概览，不能用当前页样本决定。对象范围交叉时仍逐行按 ResourceDetail.fieldAccess 投影、控制展示，不借列概览扩大授权。
- 新增 GET /v1/platform/members/context，允许成员查看或创建操作访问；返回 listFieldVisibility（潜在列可见性，仅供布局）、createFieldAccess（新对象上创建操作的字段结果）和 canSearchDisplayName（服务端原值搜索资格）。仅按服务端有效身份/授权计算，不返回原值；列表/写入仍重新校验。
- 创建成员的账号查找用途 MEMBER_CREATE 在平台域额外校验成员创建准入及新对象范围，联系方式按创建字段权限在服务器投影，返回 fieldAccess；不改变全局账号普通查询及租户用途。创建仅提交可写显示名/头像；不可写显示名使用现有系统默认，不强行回传登录名，初始联系资料仍由服务器一次复制。
- 详情编辑入口使用服务端逐成员 update capability；字段编辑再按 fieldAccess，状态变更使用独立 status capability。没有可执行变更时隐藏编辑入口。只提交用户实际修改的可写字段；未编辑的空值不清空原值，不回传隐藏、脱敏或只读字段。
- 平台编辑在事务内锁定后重验。不存在或不可查看目标保持 ObjectNotFound；可查看而不在编辑范围返回 DataScopeDenied，无编辑操作返回 ActionDenied。不改变租户错误语义，不暴露不可查看对象是否存在。
- 不新增搜索/排序能力、成员导出、数据库结构或独立字段策略。

## 契约、验证与验收

同步 Java 契约、OpenAPI、夹具、前端类型/API、副本及来源哈希。验证读/创建字段分离、空列表与跨页列概览、隐藏/脱敏/只读/完整编辑、仅改头像不回传其他字段、平台账号查找原值投影、对象范围403与不可查看404、创建默认值、状态独立权限及迟到响应。执行相关后端服务/HTTP回归、前端组件/逻辑测试、类型检查、只读lint、边界和管理台构建。

- [x] MF01 后端上下文、创建查找投影及平台成员编辑错误边界
- [x] MF02 前端字段显隐、默认邮箱、逐对象入口、草稿与最小提交
- [x] MF03 契约、副本、自动化验证与结果记录
- [ ] MF04 人工使用普通角色验证：phone MASKED/readonly、email HIDDEN、displayName FULL/readonly、avatar FULL/editable；update 指定对象内外；创建不可写默认、混合对象范围、超管FULL、列表/详情/编辑/列设置一致


## 实施与自动化证据（2026-10-06）

后端提供平台成员上下文、MEMBER_CREATE 专用字段投影及事务内编辑边界重验；前端成员页/组成员页复用字段辅助逻辑，默认邮箱、列设置、详情和创建同步显隐，编辑仅提交实际改变的可写字段。公共 InDetailIdentity 增加默认 true 的 showAvatar，平台可隐藏头像及占位，租户默认行为不变。新对象的 ScopeTarget 无对象 ID，范围匹配显式排除空 ID 的指定对象匹配，修复不可变列表 contains(null) 异常；ALL 新建对象匹配保持正常。

- 后端相关服务回归通过：PlatformMemberFieldContextTest、PlatformMemberUpdateAccessTest（实际成员 SQL）、AccountServiceTest、PlatformMemberContactsTest、MemberCreationAssignmentTest；SDK RoleFieldAuthorizationTest 5 项通过。
- 隔离 MySQL/真实 HTTP：RoleWorkspaceMySqlHttpTest 12 项通过，新增成员上下文接口验证，使用实际角色字段元数据/版本查询和 Controller，可信身份/权限边界替身固定，不替代部署后真实登录验证。测试容器结束自动清理。
- Java 公共契约 IamAuthorizationContractTest 14 项通过；新增响应夹具按实际 S0200 成功码/只读 success 契约验证。OpenAPI --check 确认130路径/197操作，Python契约7项通过；夹具和来源副本同步。
- 前端人员相关6个测试文件16项通过；InDetailIdentity 3项通过。公共包及平台插件类型检查、所改文件只读 ESLint、依赖边界和 docs 检查通过；公共包及平台/组织两管理台构建通过。
- 测试使用 Node22.17.0/pnpm10.12.4；后端大夹具用临时 Gradle init 设置 Test.maxHeapSize=1g/maxParallelForks=1，未修改项目构建配置。初次新测试的路由模拟/响应夹具问题已修正重跑；构建仍提示现有图标扫描、动态导入和Gradle弃用警告，无本轮新增编译或测试错误。

复核命令（后端根目录，MySQL需可用Docker；不操作真实库）：

```sh
cat > /tmp/iam-member-test-memory.gradle <<'GRADLE'
allprojects { tasks.withType(Test).configureEach { maxHeapSize = '1g'; maxParallelForks = 1 } }
GRADLE
IAM_MYSQL_HTTP_TEST=true ./gradlew -I /tmp/iam-member-test-memory.gradle :ingot-service:ingot-iam:ingot-iam-provider:test :ingot-framework:ingot-authorization:test --offline --tests '*PlatformMemberFieldContextTest' --tests '*PlatformMemberUpdateAccessTest' --tests '*AccountServiceTest' --tests '*PlatformMemberContactsTest' --tests '*MemberCreationAssignmentTest' --tests '*RoleFieldAuthorizationTest' --tests '*RoleWorkspaceMySqlHttpTest'
./gradlew :ingot-framework:ingot-commons:test --offline --tests '*IamAuthorizationContractTest'
python3 tools/iam/build_contract.py --check
python3 tools/iam/test_contract.py
```

前端在 ingot-admin 使用项目 Node 环境执行：

```sh
pnpm --filter @ingot/platform-plugin exec vitest run src/pages/iam/personnel/memberFieldAccess.test.ts src/pages/iam/personnel/useMemberFieldContext.test.ts src/pages/iam/personnel/components/MemberFieldUi.test.ts src/pages/iam/personnel/components/MemberCreateFields.test.ts src/pages/iam/personnel/components/MemberDrawers.test.ts src/pages/iam/personnel/components/GroupWorkspace.test.ts
pnpm --filter @ingot/admin-core exec vitest run src/components/detail/InDetailIdentity.test.ts
pnpm --filter @ingot/admin-core --filter @ingot/admin-common --filter @ingot/platform-plugin type-check
pnpm check:boundaries
pnpm check:docs
pnpm --filter @ingot/admin-core --filter @ingot/admin-common build
pnpm --filter @ingot/admin-platform-app --filter @ingot/admin-app build
```

所改TS/Vue文件另以 pnpm exec eslint 只读检查。人工步骤见 [资源扩展验收](RESOURCE-EXTENSION-VERIFICATION.md#平台成员字段展示与对象编辑mf04本增量全部待人工)，全部未勾选。本次没有新增SQL，无需因本次增量重建库；部署最新IAM与前端，既有结构准备仍依前序计划。
