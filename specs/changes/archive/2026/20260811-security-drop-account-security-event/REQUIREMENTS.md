# Requirements

## 用户场景

### US-1 库表已下线

- **触发**：在 `ingot_core`、`ingot_member` 执行 migration `013`。
- **期望**：`account_security_event` 表不存在；`account_lock_state`、canonical `security_event` 不受影响。

### US-2 账号安全事件仍可用

- **触发**：ADMIN / APP 登录成功或失败、锁定/解锁等。
- **期望**：新事件仅写入对应库或中心链路下的 `security_event`；业务与 recording 无因缺旧表而报错。

### US-3 仓库无误导残留

- **触发**：检索仓库基线、模块 SQL、对账脚本与账号域注释。
- **期望**：无「仍写入 / 仍依赖 `account_security_event` 表」的现行说明；历史 migration `009` 可保留原文作档案。

## 业务规则

### R1 双库对称

`ingot_core` 与 `ingot_member` 均 DROP；不触及 `ingot_security`（该库本无此表）。

### R2 数据销毁

`DROP TABLE` 销毁旧表全部行。本 change **不**提供在线迁移到 `security_event`。需要留存时，上线前离线备份。

### R3 领域模型保留

账号用例继续使用领域对象 `AccountSecurityEvent`，经 Port → Publisher 写入 `security_event`。禁止借本 change 删除或强制改名该类型。

### R4 历史脚本不变

已执行的 `009_member_account_protection.sql` / `rollback_009.sql` 正文不改写；前向清理仅通过 `013`。

## 边界与非目标

- 异常：若某环境仍有作业查询旧表，DROP 后失败——上线前须确认无外部依赖。
- 非目标：ES/Kafka、Platform 读侧 API、`AccountSecurityEvent` 重命名、`account_lock_state` 变更、recording 优先级/队列改造。

## 验收标准

- [ ] E1–E5 见 [FUNCTIONAL-TEST-CHECKLIST.md](./FUNCTIONAL-TEST-CHECKLIST.md) 全部通过
- [ ] `ingot_core` / `ingot_member` 无 `account_security_event` 表
- [ ] 登录/锁定新事件仅出现在 `security_event`
- [ ] 代码库无 `AccountSecurityEventEntity` / 旧 Mapper / 模块旁路 DDL
- [ ] `specs/current` 已去掉「历史只读 / 尚未物理下线」表述，本 change 已归档
