# 公共字段访问控制

> 状态：completed
> 完成与归档：2026-10-10；人工验收：jy。
> 创建：2026-10-10；批准：用户明确要求「开始实施这个计划」，此前 Plan 已对齐范围、方案及验收。

## 目标与协作

统一业务字段可见性、编辑和筛选执行，前后端同批接入；负责人jy。关联另一仓库 change `20261010-common-field-access-control` 和本仓库既有 IAM active，旧 IAM 验收待办保持。本轮全栈实施；实施阶段未自动提交、部署或重建目标库。2026-10-10 用户确认人工验收完成，并明确要求归档及提交代码。

## 工件

- [需求](REQUIREMENTS.md)
- [设计](DESIGN.md)
- [契约](API.md)
- [任务](TASKS.md)
- [自动化证据](EVIDENCE.md)
- [业务接入说明](INTEGRATION.md)

## 来源及阶段

来源为本会话已批准最终计划。需求选择：平台/tenant/示例统一接入；当前用户级操作预览；完整可见才筛选；未绑定字段允许保存、授权前接入；保留来源范围；参数化脱敏；tenant隐私规则保留并拆操作；三个时间字段首轮只读接入。

人工验收已由用户确认完成；current 基线已整理，本 change 按本次明确归档指令完成归档。此记录不表示 Agent 执行了目标库迁移或生产部署。

2026-10-10用户追加已有库迁移SQL要求，016增量脚本及隔离MySQL10项验证已完成；仅交付脚本，未执行目标库。详见设计、任务和验收证据。

## 完成记录

2026-10-10，用户明确说明“我已经人工验收完成”，并要求归档 `20261010-framework-field-access-control` 及提交代码。两端字段控制为同一全栈变更，本仓库与关联 change 同步归档；既有 IAM change 的其他未完成验收保持原状态。

- 当前能力：[`framework/field-access-control`](../../../../current/framework/field-access-control/README.md)，有效规则见 [SPEC.md](../../../../current/framework/field-access-control/SPEC.md)。
- 关联 change：另一仓库 `specs/changes/archive/2026/20261010-common-field-access-control/`。
- 关联代码提交：后端 `416b27a6`；前端 `c650754`。归档及基线记录由本次后续 docs 提交保存。
- 最终设计差异：无未批准的业务设计偏离。用户后续追加 016 既有库迁移脚本，已同步设计、任务与隔离验证；目标库执行由用户负责。资源配置交互与标签样式优化记录在前端关联/独立 change。
