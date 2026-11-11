# IAM 独立测试数据工具

对应 IAM 主 change 的测试数据 D01/D02。首次部署使用正式单文件 `databases/ingot_iam.sql`，本工具通过现行 API 准备独立环境的联调数据。

联调点击步骤与功能说明见 [VERIFICATION-GUIDE](../../../specs/changes/archive/2026/20260912-iam-identity-access-management/VERIFICATION-GUIDE.md)。场景卡见 [TEST-DATA](../../../specs/changes/archive/2026/20260912-iam-identity-access-management/TEST-DATA.md)。

## 命令

```bash
python3 tools/iam/test-data/iam_test_data.py help
python3 tools/iam/test-data/iam_test_data.py prepare --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py build   --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py verify  --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py reset   --config tools/iam/test-data/config.local.json --run-id demo --confirm-reset
```

## 导入前

1. 在已登记的独立 MySQL 实例执行 [ingot_iam.sql](../../../databases/ingot_iam.sql)。脚本会重建固定名称的 `ingot_iam`。IAM 与联调配置须指向同一独立实例。
2. 启动 Auth、BFF、Gateway、IAM 和两个管理台。用默认平台账号完成首次改密，将改密后的口令通过 `IAM_TEST_PASSWORD_PLATFORM_GOVERNOR` 提供给工具。
3. 从 `config.example.json` 创建本地配置，填写独立服务地址、数据库、Redis 命名空间与两个管理台 origin。`registered` 与 `independent` 都须为 true。浏览器登录页位于各自 origin 的 `/login/`。
4. 安装 Python `cryptography`。工具使用独立环境的 Auth password grant，走 Gateway 获取 HYBRID 公钥并调用 `/iam/v1`。浏览器另行验证同源登录。工具创建的所有者首次改密后，将新口令留在本地忽略的 secrets 文件。其他身份的一次性口令供后续验收。

## 行为

- `prepare`：校验配置，写入 runId、UTC t0、指纹和对象映射。同一 runId 复用清单，不覆盖已有 `objects`。
- `build` 创建或复用账号、A/B 组织、部门、成员、组、共享角色及分配。平台成员用账号 ID，租户成员按夹具手机号关联账号。部门从正式部门树读取，租户分配引用当前 `roleId`，字段策略按成员资源读取。规模、并发和动态期限见 D03。
- 指纹覆盖环境配置和单文件初始化内容。旧清单或内容漂移时，可用新的 runId 或显式重置原清单。重置仅删除运行清单。
- `verify`：核对清单与当前配置、DDL 文件存在；列出 TD 场景编号。不是业务验收。
- `reset`：必须 `--confirm-reset`，只清除该环境该 runId 的清单，不对任意开发库执行 DROP。

登录名：`iam-test-<身份键>`。租户登录 Auth 表单字段为 `org`（组织 ID，见 inventory）。平台治理账号仍是 bootstrap 登录名，不是 `iam-test-platform-governor`。

## 测试

```bash
python3 tools/iam/test-data/test_iam_test_data.py
python3 tools/iam/test-data/operation_matrix.py --check
```

更新公开或内部 IAM 契约后，用 `operation_matrix.py` 重新生成 TD 与页面对应表。真实 HTTP 与浏览器结果分别登记在主变更验收矩阵。

## 平台角色分配增量

在已按正式单文件初始化的独立环境运行。治理账号改密后的口令来自环境变量或本地忽略的 secrets 文件。

```bash
python3 tools/iam/test-data/iam_test_data.py refinement-build --config tools/iam/test-data/config.local.json --run-id platform-refinement-20260928
python3 tools/iam/test-data/iam_test_data.py refinement-verify --config tools/iam/test-data/config.local.json --run-id platform-refinement-20260928
```

`refinement-build` 先验证新上下文接口与直接创建/撤销资格，再通过正式接口创建 9 个平台身份：治理管理员、纯受限管理员、兼具两类资格的管理员、两个接收成员、名单外成员，以及业务权限与委派范围完全重合/部分重合/不重合的管理员。创建固定角色版本、允许组与越界组、有效委派和真实派生分配；已有清单对象复用。过期清单需要新 run-id，并按验收步骤重新设置有效期，不能将历史数据的过期当成逻辑失败。

`refinement-verify` 检查独立资格、入口及业务范围、候选边界、来源与授权时间、伪造来源/成员/组/对象/期限、多来源拼接、委派收窄拒绝及无部分保存、他人分配不可读/撤销、人员快捷角色门禁和真实诊断来源。HTTP 报告通过只代表列出的子集；浏览器、多时区、并发锁竞争、委派撤销/到期及组变化仍须执行 PR-A01–PR-A07。

基础构建器已在一次性新库经真实 IAM、Gateway、Auth 和 BFF 验证，包含首次构建、重复构建和清单核对。平台 `refinement-build` 会为执行断言的身份完成首次改密并把新口令保留在本地 secrets 文件。真实新库的重复构建及 `refinement-verify` 27 项 HTTP 断言通过。数据验收与页面证据见[收尾实施记录](../../../specs/changes/archive/2026/20260912-iam-identity-access-management/TASKS.md#本地验收边界)。
