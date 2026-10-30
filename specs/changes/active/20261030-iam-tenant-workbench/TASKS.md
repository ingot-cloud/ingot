# Tasks

- [x] B1：新增响应和只读端点，说明授权与统计口径
- [x] B2：实现带范围 COUNT 与可用应用去重统计
- [x] B3：补充权限、租户隔离、状态与 SQL 范围测试
- [x] B4：执行后端目标测试与编译
- [x] B5：工作台根菜单置顶，同步初始化与增量数据，并核对实际导航

- [x] 实现与设计一致
- [ ] 用户产品验收
- [ ] 验收后更新 current 并归档

## 验证

- `:ingot-service:ingot-iam:ingot-iam-provider:compileJava --offline` 通过。
- `:ingot-service:ingot-iam:ingot-iam-provider:test --tests 'com.ingot.cloud.iam.tenant.TenantWorkbench*' --tests 'com.ingot.cloud.iam.support.IamOssJacksonConfigurationTest' --offline` 通过。
- 覆盖读取资格、省略与合法零、租户隔离、成员状态、部门根节点、交集范围、去重、错误传播和头像签名。
- 2026-10-30 用户选择自行测试；真实登录已进入测试组织 1，接口联调与产品验收由用户继续。
- 导航修正：生成器 `--check` 通过，初始化生成差异仅工作台排序由4变0，保留ID及其他目录。035 已应用到本地开发库，浏览器刷新后实际导航顺序为工作台、组织管理、权限与应用、安全与合规。
