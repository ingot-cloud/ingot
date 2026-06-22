# Spec-Driven Development

本仓库使用 SDD（Spec-Driven Development，规格驱动开发）管理需求、设计、实施、验收和系统基线。

`specs` 是公司内部规格与变更管理的事实来源，结合以下思想：

- OpenSpec：区分当前系统事实与待实施变更，并保留变更历史。
- GitHub Spec Kit：每个变更包含需求、设计、任务和验收工件。

## 1. 目录结构

```text
specs/
├── README.md
├── current/
│   └── <domain>/
│       └── <capability>/
│           ├── README.md
│           └── SPEC.md
├── changes/
│   ├── active/
│   │   └── <YYYYMMDD>-<domain>-<feature>/
│   │       ├── README.md
│   │       ├── REQUIREMENTS.md
│   │       ├── DESIGN.md
│   │       ├── TASKS.md
│   │       └── phases/
│   └── archive/
│       └── <year>/
│           └── <change-id>/
└── templates/
    └── change/
```

## 2. 区域职责

### `current/`

描述已经验收并在线生效的系统事实，包括：

- 当前能力与业务规则。
- 当前数据模型和公共接口。
- 当前兼容、安全和运维约束。

不得包含讨论过程、被否决方案、临时迁移步骤、历史任务清单或已经失效的过渡逻辑。

### `changes/active/`

保存处于规划、评审、实施或验证阶段的变更。目录命名：

```text
<YYYYMMDD>-<domain>-<feature>
```

例如：

```text
20260612-pms-application-authorization
```

### `changes/archive/`

保存已完成、取消或被替代的变更。按归档年份组织，保留完整上下文和最终结论。

### `templates/`

保存新 change 的最小工件模板。模板提供结构，不替代需求和设计对齐。

## 3. 变更工件

| 工件 | 职责 |
|---|---|
| `README.md` | 状态、目标、范围、负责人、时间和文档索引 |
| `REQUIREMENTS.md` | 用户场景、业务规则、边界、非目标和验收标准 |
| `DESIGN.md` | 架构、数据模型、接口、数据流、迁移、兼容和关键决策 |
| `TASKS.md` | 可执行任务、依赖顺序、状态和任务级验收条件 |
| `phases/` | 大型变更的阶段实现、验收、退出条件和回滚方案 |

所有变更必须具备前四个工件。只有无法在一个可控发布单元内完成的大型变更才需要 `phases/`。

## 4. 状态机

```text
draft → review → approved → implementing → validating → completed
                                                ↘ cancelled
```

| 状态 | 含义 |
|---|---|
| `draft` | 需求或设计仍在形成，不允许实施 |
| `review` | 等待相关方确认，不允许实施 |
| `approved` | 需求和设计已确认，可以开始实施 |
| `implementing` | 正在实施 |
| `validating` | 实施完成，正在测试、迁移或验收 |
| `completed` | 已验收、已更新 current，等待或已经归档 |
| `cancelled` | 已取消，记录原因后归档 |

状态必须写在 change 的 `README.md` 中。

## 5. 新功能工作流

### 5.1 Plan 模式

Agent 或开发者必须：

1. 阅读根 `AGENTS.md`。
2. 检索相关 `specs/current/`，理解线上事实。
3. 检索 `specs/changes/active/`，识别并行或冲突变更。
4. 阅读相关代码、数据库、接口和测试。
5. 对齐需求、范围、兼容性、迁移、测试和验收标准。

需求未稳定时，不创建正式 change，也不修改业务代码。

### 5.2 创建 Active Change

方案确认后，从 `specs/templates/change/` 创建 active change，初始状态设为 `draft` 或 `review`。所有实现决策完成并获得确认后改为 `approved`。

小型变更可以不创建 `phases/`，但不能缺少 `REQUIREMENTS.md`、`DESIGN.md` 或 `TASKS.md`。

### 5.3 实施

实施前必须满足：

- 状态为 `approved`。
- `TASKS.md` 已决策完整。
- 当前任务具备明确验收标准。
- 数据迁移和破坏性操作具备回滚方案。

开始实施时将状态改为 `implementing`。实施期间：

- 行为变化同步回写 `DESIGN.md`。
- 完成任务后立即更新 `TASKS.md`。
- 不提前修改 `current/`。
- 偏离设计时先更新 Spec 并重新确认。
- 大型变更必须逐阶段验收。

### 5.4 验证与完成

实现完成后将状态改为 `validating`。完成条件：

- 所有任务完成。
- 自动化测试和阶段验收通过。
- 数据迁移验证通过。
- 实际接口、数据模型和设计一致。
- 未完成项已经拆分为新的 active change。

### 5.5 更新 Current

验收后，根据最终实现创建或更新：

```text
specs/current/<domain>/<capability>/
```

Current 必须根据最终代码整理，不得直接复制 change。一个 change 可以更新多个 capability，多个 change 也可以持续更新同一 capability。

### 5.6 Archive

更新 current 后，将状态改为 `completed`，并在 change `README.md` 记录：

- 完成日期。
- 关联提交或 PR。
- 更新的 current capability。
- 最终实现与原设计的差异。

然后移动到：

```text
specs/changes/archive/<year>/<change-id>/
```

取消的变更标记 `cancelled`，记录取消原因后同样归档。

## 6. Current 更新规则

Current capability 推荐结构：

```text
current/<domain>/<capability>/
├── README.md
└── SPEC.md
```

- `README.md`：能力摘要、边界、所有者、关联模块和文档索引。
- `SPEC.md`：当前有效的数据模型、接口、规则、约束和运行特性。

只有已上线并验收的事实可以进入 current。

## 7. 当前活动变更

- 暂无进行中的变更。

已归档（2026）：

- [PMS 应用中心化授权改造（发布 A）](./changes/archive/2026/20260612-pms-application-authorization/README.md)
- [PMS 应用授权旧字段破坏性清理（发布 B）](./changes/archive/2026/20260622-pms-authorization-ddl-cleanup/README.md)

对应 current 能力：[pms/application-authorization](./current/pms/application-authorization/README.md)
