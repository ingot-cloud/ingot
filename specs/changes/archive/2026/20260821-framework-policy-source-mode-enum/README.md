# 用枚举统一策略来源与事件投递配置

> 状态：completed（已验收；YAML 契约未变，未改 current；已归档）

## 元数据

- Change ID：`20260821-framework-policy-source-mode-enum`
- 领域：`framework`（commons、security 策略装配、security-event-recording）
- 负责人：jy
- 创建日期：2026-08-21
- 目标发布日期：TBD

## 目标

消除各安全属性里 `mode` / `target` 的字符串硬编码与重复内部枚举：策略来源统一为 commons `PolicySourceMode`（`local` / `remote`），事件投递属性改用已有 `RecordingTarget`（`local` / `center`）。YAML 字面量与运行语义不变。

## 范围

### 包含

- 在 `ingot-commons` 新增 `PolicySourceMode`，供 access / session / credential / account / gateway 四域共用
- 删除 gateway 四域与 account 的内部 `Mode` / `PolicyMode`，`String mode` 改为枚举
- `@ConditionalOnProperty(havingValue)` 改引用 `PolicySourceMode.VALUE_*`
- `SecurityEventProperties.target` / `shadowTargets` 改为 `RecordingTarget`；保留 YAML `remote` → `CENTER` 别名

### 不包含

- 修改 Nacos YAML 用词或默认值
- 统一 `remote` 与 `center` 为同一 YAML 词汇
- 字典客户端 `AUTO/LOCAL/REMOTE/NONE`、发号器 `redis/machine` 等无关 mode
- 更新 `specs/current/` 业务语义（YAML 字面量描述保持原样）

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-08-21
- 关联提交或 PR：工作区实施（随代码一并提交）
- 更新的 current capability：不更新（配置 Java 类型收敛，YAML 契约仍为 `local|remote` / `local|center`）
- 与原设计的差异：无
- 取消原因：—
