# Tasks

> 状态：`completed`

## 已定决策

| 决策 | 结论 | 依据 |
|------|------|------|
| 清理范围 | 仅 DROP `account_security_event`（双库） | 用户确认；plan |
| 历史数据 | 销毁；无在线回填；运维可选 dump | REQUIREMENTS R2 |
| 领域模型 | 保留 `AccountSecurityEvent` 等 | DESIGN D5 |
| 历史 migration `009` | 不改写正文 | DESIGN D1 |
| shadow 对账脚本 | 删除 | DESIGN D6 |

## Phase 0 · SDD 与评审

- [x] T0-1：SDD 四工件 + 验收清单
- [x] T0-2：评审 → 开始实施

## Phase 1 · DDL 与基线

- [x] T1-1：`013` + `rollback_013`
- [x] T1-2：基线 SQL
- [x] T1-3：删除旁路 DDL
- [x] T1-4：删除 shadow 对账脚本

## Phase 2 · 注释与文档

- [x] T2-1：UseCase / Port 注释
- [x] T2-2：roadmap

## Phase 3 · 验收与归档

- [x] T3-1：功能验收（用户确认归档）
- [x] T3-2：更新 `specs/current`
- [x] T3-3：移入 archive，更新 `specs/README.md` §7

## 验证任务

- [x] V1：现行基线 / 旁路 DDL / 对账脚本已清除
- [x] V2：用户确认环境验收通过

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
