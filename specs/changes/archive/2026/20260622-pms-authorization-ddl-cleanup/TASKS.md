# Tasks

> 状态：completed（DDL 已在目标库执行并验证）

## 归档核对（2026-06-22）

- 破坏性 DDL 已在目标数据库执行并验证（负责人确认）。
- 仓库迁移脚本（`008`/`rollback_008`）与 `databases/ingot_core.sql` 初始化脚本同步**另行处理**，不在本次归档提交范围。

## 实施任务

- [x] T1：前置闸门确认（承接原 T6.0）
- [x] T2：执行破坏性 DDL（DROP 旧列、SET NOT NULL、唯一/索引）——已在目标库执行；仓库脚本 `008`/`rollback_008` 另行补齐
- [~] T3：同步 `databases/ingot_core.sql` 初始化脚本与种子数据（另行处理）
- [~] T4：同步运维与 API 文档（另行处理）

## 验证任务

- [x] V1：目标库执行 DDL 并校验结构变更
- [x] V2：全链路回归——平台/租户用户、多角色、应用启停、通配授权
- [x] V3：安全测试——越权提交、平台权限删除、审计完整性

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准满足（仓库脚本同步另行处理）
- [x] `specs/current/pms/application-authorization/` 已根据最终物理模型更新
- [x] Change 已记录完成信息并归档
