# 需求与路线图

本目录存放**产品层**文档：原始需求、平台愿景与持续优化路线。面向产品负责人、架构师与开发团队，用于对齐方向与优先级。

## 文档索引

| 文档 | 用途 | 更新频率 |
|------|------|----------|
| [VISION.md](./VISION.md) | 产品 charter：背景、目标用户、核心能力、非目标 | 产品方向变化时 |
| [ROADMAP.md](./ROADMAP.md) | 平台优化路线：阶段、优先级、状态与关联链接 | 双周或每个里程碑 |
| [themes/](./themes/) | 跨域能力主题（安全、多租户、性能等） | 按需扩展 |

## 与 `specs/` 的分工

| 层级 | 位置 | 内容粒度 | 典型读者 |
|------|------|----------|----------|
| 产品意图 | `docs/requirements/` | 能力级、阶段级 | 全员 |
| 变更规格 | `specs/changes/active/` | 单次可验收变更 | 开发、测试 |
| 系统基线 | `specs/current/` | 已上线行为事实 | 开发、Agent |
| 实现说明 | `docs/modules/` | 架构、用法、FAQ | 开发、运维 |

**规则**：

1. ROADMAP 条目进入开发时，必须在 `specs/changes/active/` 创建 change，并在 ROADMAP 中链接 change ID。
2. 不在本目录写接口字段、SQL、任务 checkbox——那些属于 specs。
3. 功能验收完成后，实现细节写入 `docs/modules/`，行为事实写入 `specs/current/`。

## 维护约定

- **新增路线项**：在 ROADMAP 分配 ID（`R-YYYY-NNN`），填写动机、阶段与状态。
- **路线项开工**：状态改为 `in-spec` 或 `implementing`，链接 active change。
- **路线项完成**：状态改为 `done`，链接 `specs/current/` 或 `docs/modules/` 文档。
- **取消路线项**：保留条目，状态改为 `cancelled` 并注明原因。
