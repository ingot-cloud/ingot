# Tasks

## 规格审阅

- [ ] T0：四件套 review，决策点 D1–D6 敲定
  - 依赖：无
  - 验收：README 状态由 `draft` 转为 `approved`；TASKS 中 `[待确认]` 项已闭合

## 实施任务

- [ ] T1：API 契约（`ingot-security-api`）
  - 依赖：T0
  - 内容：`SecurityEventReportDTO`、`SecurityEventType`、`SecurityEventCategory`、`RemoteSecurityEventService`（report + reportBatch）
  - 验收：模块编译通过；enum 覆盖 P0 共 13 种类型

- [ ] T2：中心存储与 Inner API（`ingot-security-provider`）
  - 依赖：T1
  - 内容：`010_unified_security_event.sql` + rollback + `ingot_security.sql` 基线；Entity/Mapper/Service；`InnerSecurityEventAPI`
  - 验收：POST `/inner/security/event/report` 入库成功；非法 payload 返回 4xx

- [ ] T3：账号域映射与 Composite Port（`ingot-account-adapter`）
  - 依赖：T1
  - 内容：`SecurityEventProperties`；`AccountSecurityEventMapper`；`RemoteSecurityEventPortAdapter`；`CompositeSecurityEventPort`；自动配置替换默认 `SecurityEventPort` Bean
  - 验收：`mode=local` 仅本地 INSERT；`mode=remote` 本地 + 异步 Feign；Feign 失败不抛到 UseCase

- [ ] T4：PMS / Member 配置与 source-module
  - 依赖：T3
  - 内容：`in-service-pms.yml` / `in-service-member.yml` 增加 `ingot.security.event.*`；`source-module` 分别为 `ingot-pms` / `ingot-member`
  - 验收：两服务引入 adapter 后 Composite Port 生效

- [ ] T5：网关 Reporter 改造（`ingot-gateway`）
  - 依赖：T1, T2
  - 内容：`SecurityEventReporter`（或改造 `BlacklistEventReporter`）；`BlacklistReportDTO` 映射；`ingot-security-api` 依赖；停止 `gateway_blacklist_event` 新写入（`reportBlacklist` 转调或废弃）
  - 验收：Sentinel 封禁后 `security_event` 有 ACCESS 记录；`gateway_blacklist_event` 无新 INSERT

- [ ] T6：网关配置
  - 依赖：T5
  - 内容：`in-service-gateway.yml` 样例与 `ingot.security.event.*`
  - 验收：Gateway 启动无循环依赖；Reporter 懒解析 Feign

- [ ] T7：account-domain enum 映射（可选最小）
  - 依赖：T1, T3
  - 内容：`SecurityEventTypeMapping` 或 adapter 内 code 直传（code 一致则薄封装）
  - 验收：11 种账号事件类型映射正确

## 验证任务

- [ ] V1：S1–S2 账号域双写（remote 模式）
  - ADMIN/Member 登录成功/失败、锁定/解锁、改密
  - DB：`account_security_event` + `security_event` 均有对应记录

- [ ] V2：S3 网关 ACCESS 上报
  - 触发限流违规封禁
  - DB：`security_event` 含 extension；旧表无新行

- [ ] V3：S4 降级（local / security 不可用）
  - 主链路正常；无 Feign 异常泄漏

- [ ] V4：S5–S6 开关与热刷新
  - `enabled=false`、类别关闭、Nacos 改配置不重启

- [ ] V5：migration
  - `010` 执行成功；`rollback_010` 可回滚

- [ ] V6：编译与回归
  - 相关模块 `./gradlew` 编译通过；L2 账号保护行为无回归

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] 更新 `specs/current/security/security-event-center/`（README + SPEC）
- [ ] 更新 [security-center-roadmap.md](../../../../docs/requirements/themes/security-center-roadmap.md) L3 状态为 `done`
- [ ] 更新 [ROADMAP.md](../../../../docs/requirements/ROADMAP.md) R-2026-025 为 `done`
- [ ] Change 已记录完成信息并归档至 `specs/changes/archive/2026/20260729-security-event-center/`

## 任务依赖图

```text
T0 → T1 → T2 → T5 → T6
         ↘ T3 → T4
         ↘ T7
T2,T6 → V1–V6 → 归档
```

## 决策跟踪（T0 关闭）

| ID | 决策 | 状态 |
|----|------|------|
| D1 | enum SoT 在 `ingot-security-api` | 待确认 |
| D2 | 停止 `gateway_blacklist_event` 新写入 | 待确认 |
| D3 | remote 模式始终双写本地 | 待确认 |
| D4 | migration 编号 `010` | 待确认 |
| D5 | security 不可用等同 local | 待确认 |
| D6 | `reportBlacklist` 转调统一入库 | 待确认 |
