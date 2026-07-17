# Tasks

## 决策任务（实施前必须敲定，对应 DESIGN 待审阅决策点）

- [ ] D1：宽限扣减调用位置 → 方案 A / B / C（默认建议 A）
- [ ] D2：强制改密拦截位置 → 网关 / 资源服务（默认建议 网关）
- [ ] D3：初始密码能力归属 → 账号域 / 凭证模块（默认建议 账号域）
- [ ] D4：Member 凭证装配现状确认结论（是否需补 Bean）

## 实施任务

- [ ] T1：`PasswordExpiration` 领域模型补 `force_change` 字段映射，`initExpiration` 修正（`nextWarningAt` / `force_change` 初始化）
  - 依赖：无
  - 验收：读写 `force_change` 与 DB 一致，`initExpiration` 不再遗漏字段

- [ ] T2：force_change / mustChangePwd 语义对齐
  - 依赖：T1
  - 验收：改密成功同时清零两者；重置 / 管理员创建同时置位两者；登录判定读取二者并集

- [ ] T3：宽限期扣减接入登录成功链路（按 D1）
  - 依赖：T1、D1
  - 验收：软过期成功登录扣减一次，remaining=0 幂等，硬过期阻断不变

- [ ] T4：初始密码策略（按 D3）
  - 依赖：D3
  - 验收：支持 RANDOM/FIXED 生成、`validHours` 过期、`oneTime` 用后失效、首登强制改密；默认值兼容现状

- [ ] T5：强制改密访问限制拦截（按 D2）
  - 依赖：T2、D2
  - 验收：强制改密态下仅白名单接口可访问，其余拒绝并引导改密

- [ ] T6：Member 侧凭证策略装配与登录判定对齐（按 D4）
  - 依赖：D4
  - 验收：Member 启用过期 / 强制改密策略时行为与 ADMIN 一致

- [ ] T7：新增初始密码配置项 `ingot.security.credential.policy.initial-password.*` 并纳入 `local` Nacos 动态刷新
  - 依赖：T4
  - 验收：`mode=local` 修改 Nacos 配置无需重启即时生效

- [ ] T8：校验 `local` 策略加载是否随 Nacos 刷新重建，不生效则补 `@RefreshScope` / `RefreshEvent` 处理
  - 依赖：无
  - 验收：改 `strength.minLength` / `expiration.maxDays` 不重启即生效

## 验证任务

- [ ] V1：单元测试（宽限扣减边界、force_change 对齐、初始密码过期 / 生成）
- [ ] V2：集成测试（ADMIN 与 Member「创建→首登强制改密→改密→再登录」全链路）
- [ ] V3：降级验证（`mode=local` Nacos 动态刷新）
- [ ] V4：回归（现有 PMS 登录 / 改密、硬过期阻断不受影响）；相关模块编译通过

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新（`specs/current/security/credential-security/`）
- [ ] roadmap 状态表 L1 更新为 done，ROADMAP.md 关联更新
- [ ] Change 已记录完成信息并归档
