# Tasks

## 决策（已确认）

- D1 按 user_type 分行，禁止删除
- D2 seam 增加 UserTypeEnum 参数
- D3 remote 落 adapter + LayeredCacheBuilder
- D4 配置前缀不改
- D5 空列表 = 不可用
- D6 交付 PLATFORM-API.md，不做前端页

## 实施任务

- [x] T1：创建本 change 工件（含 PLATFORM-API.md）
  - 依赖：无
  - 验收：active change 状态 approved
- [x] T2：DDL / 回滚 / 基线 + 权限种子
  - 依赖：T1
  - 验收：018/019 与 ingot_security.sql / ingot_core.sql 一致
- [x] T3：安全中心 Entity/Mapper/Admin/Platform/Inner/Feign/Domain/i18n
  - 依赖：T2
  - 验收：APP 永久锁拒绝；upsert 发 ACCOUNT_LOCKOUT；OpenAPI 注解齐全
- [x] T4：core Properties、LockoutPolicy、loader 签名、local 按 mode 装配
  - 依赖：T1
  - 验收：去掉 remote WARN；local 仅 mode=local
- [x] T5：adapter 分层缓存链、协调器、Actuator、RedisKey
  - 依赖：T3、T4
  - 验收：remote 建链；empty 抛 unavailable
- [x] T6：Nacos 补充 policy.cache / fallback 注释；单元测试
  - 依赖：T5
  - 验收：相关模块测试通过
- [x] T7：更新 current 并完成检查
  - 依赖：T6
  - 验收：account-protection 与 layered-cache SPEC 已更新

## 验证任务

- [x] V1：account-core / account-adapter / security-provider 相关测试通过

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
