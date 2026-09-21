# IAM 独立测试数据工具

对应 change `20260912-iam-identity-access-management` 的 **测试数据 D01/D02**。正式冷启动仍走权威 DDL 与 `006_bootstrap.sql`；本目录只编排已登记的独立测试环境，不写入生产种子。

联调点击步骤与功能说明见 [VERIFICATION-GUIDE](../../../specs/changes/active/20260912-iam-identity-access-management/VERIFICATION-GUIDE.md)。场景卡见 [TEST-DATA](../../../specs/changes/active/20260912-iam-identity-access-management/TEST-DATA.md)。

## 命令

```bash
python3 tools/iam/test-data/iam_test_data.py help
python3 tools/iam/test-data/iam_test_data.py prepare --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py build   --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py verify  --config tools/iam/test-data/config.local.json --run-id demo
python3 tools/iam/test-data/iam_test_data.py reset   --config tools/iam/test-data/config.local.json --run-id demo --confirm-reset
```

## 导入前

1. 目标必须是**已登记的独立空库**。DDL 顺序：`001`–`005` → `007_member_export.sql` → 框架 `account_lock_state.sql` / `add_password_history.sql` → `006_bootstrap.sql`。详见 `databases/iam/README.md`。
2. **不要**再执行 `seed-manual-verification.sql`（那是历史 Bruno 的 `platform`/`owner` + `password`，与 `iam-test-*` 不是一套数据）。
3. IAM 打开 `ingot.iam.bootstrap.enabled=true` 启动一次，用 WARN 日志中的初始口令登录 `platform`（不传 `org`），立刻改密。把改密后的口令放到环境变量 `IAM_TEST_PASSWORD_PLATFORM_GOVERNOR`。
4. 复制 `config.example.json` 为未入库的 `config.local.json`：`registered`/`independent` 必须为 true；填独立库、Redis 命名空间和四站地址。配置缺失会失败，不会猜测开发库。
5. **不要**预先 export `IAM_TEST_PASSWORD_OWNER_A` / `OWNER_B`。未设置时 `build` 生成一次性口令，写入同目录 `runs/<environmentId>/<run-id>.secrets.json`（已 gitignore）。报告不打印密码、token。
6. 启动 Auth / Gateway / IAM。`build` 用 Auth password grant 登录平台账号，再调 `/iam/v1`。

组织所有者登录 `S0400`：先确认 OWNER 环境变量未设置；删除对应 `*.secrets.json`（可保留 inventory）后重跑 `build`，让工具重新 resetPassword。不要用 SQL dump 代替本工具。

## 行为

- `prepare`：校验配置，写入 runId、UTC t0、指纹和对象映射。同一 runId 复用清单，不覆盖已有 `objects`。
- `build`：通过真实 `/iam/v1` 创建账号、A/B 组织、部门、成员、组、共享角色和一条分配。先 `lookup` 已有登录名再创建；组织按名称复用，所有者成员取自租户详情的 `ownerMemberId`，平台/租户成员按显示名复用。共享角色使用租户域 `iam-tenant` 的成员读写操作，不引用平台域应用。重置口令后回写账号版本；停用账号/暂停成员前提取当前版本，409 则重读再提交。中途失败会写入已成功对象。不覆盖 250+ 成员（D03）和动态期限。`reset` 只清清单，不 DROP 库。
- 配置或 DDL 指纹漂移：拒绝静默改回，需 `reset` 后重建。
- `verify`：核对清单与当前配置、DDL 文件存在；列出 TD 场景编号。不是业务验收。
- `reset`：必须 `--confirm-reset`，只清除该环境该 runId 的清单，不对任意开发库执行 DROP。

登录名：`iam-test-<身份键>`。租户登录 Auth 表单字段为 `org`（组织 ID，见 inventory）。平台治理账号仍是 bootstrap 登录名，不是 `iam-test-platform-governor`。

## 测试

```bash
python3 tools/iam/test-data/test_iam_test_data.py
```
