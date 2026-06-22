# 应用中心化授权改造任务

> 状态：implementing（整体实施中，待统一验收）

## 实施策略

整体连续交付，完成后一次性执行下方统一验收清单。P6 旧字段清理不在本次实施范围。

### 已完成（代码）

- [x] P0：审计、快照、shadow 框架、基线单元测试
- [x] P1：`007_application_authorization.sql`、领域模型新字段
- [x] P2–P5：新鉴权引擎、菜单树生成、租户应用授权、`role_source` 双写、菜单/应用写入双写
- [x] 默认 `authorization.model=application`

### 待统一验收

- [ ] 测试库执行 `databases/migrations/007_application_authorization.sql`
- [ ] 应用中心化 API 联调（见 [API.md](./API.md)）
- [ ] 前端应用列表 → 详情（基本信息/菜单/权限）冒烟
- [ ] Dev API 审计/快照 + 登录菜单权限回归
- [ ] 更新 current 并归档 change

## 统一验收清单

- [ ] REQUIREMENTS 验收标准全部满足
- [ ] 迁移脚本在测试/预发布库成功
- [ ] 单元测试全部通过
- [ ] 应用中心化 API + API.md 交付，SpringDoc 可浏览
- [ ] 平台/租户用户菜单、角色、应用启停回归
- [ ] 与设计一致（P6 除外）

## 阶段任务明细（参考）

### P0：基线与迁移护栏

详见 [phases/00-baseline.md](./phases/00-baseline.md)。

- [x] T0.1：实现只读数据审计任务
  - 依赖：无
  - 验收：`AuthorizationDataAuditService` + `GET /v1/platform/dev/authorization/audit`
- [x] T0.2：为核心授权路径补充单元/集成测试（首批）
  - 依赖：无
  - 验收：`BizMenuUtilsTest`、`ShadowComparisonServiceTest`；更多场景待补集成测试
- [x] T0.3：接入 `authorization.model` 配置与 shadow 差异日志
  - 依赖：T0.2
  - 验收：`ingot.pms.authorization.model` 支持 legacy/shadow/application；默认 application
- [x] T0.4：建立迁移对照样本与逻辑快照脚本
  - 依赖：T0.1
  - 验收：`GET /v1/platform/dev/authorization/snapshot`；样本用户通过配置项指定
- [ ] P0 阶段验收：测试库审计运行、基线测试连续三次一致、样本用户配置完成

### P1：兼容性数据库扩展

详见 [phases/01-schema.md](./phases/01-schema.md)。

- [x] T1.1：编写 DDL 迁移脚本
- [x] T1.2：SQL 回填（code/app_id/access_mode/role_source/租户授权）
- [x] T1.3：菜单/应用/角色关联双写
- [ ] T1.4：测试库执行迁移并校验

### P2：应用中心化资源管理

详见 [phases/02-application-resource.md](./phases/02-application-resource.md)。

- [x] T2.1：实现统一应用资源领域服务
  - 依赖：P1 退出
  - 验收：应用归属校验、编码规则、跨应用拦截、事务一致性、缓存失效事件
- [x] T2.2：实现应用 CRUD 与 `app_code:*` 根权限自动创建
  - 依赖：T2.1
  - 验收：创建/删除/禁用规则符合 DESIGN；有绑定时不允许物理删除
- [x] T2.3：实现应用下菜单 CRUD 与托管 NAVIGATION 权限生命周期
  - 依赖：T2.1
  - 验收：菜单与权限无单边成功；OPEN/PERMISSION 切换不丢角色绑定
- [x] T2.4：实现应用下操作权限 CRUD
  - 依赖：T2.1
  - 验收：托管权限只读；GROUP/ACTION 编码规则 enforced
- [x] T2.5：新增应用中心化 API，旧接口兼容转发
  - 依赖：T2.2、T2.3、T2.4
  - 验收：新接口可用；旧接口兼容期内不产生缺失 `app_id` 的数据
- [ ] T2.6：补充资源写入自动化事务测试
  - 依赖：T2.5
  - 验收：阶段 2 全部测试场景通过

### P3：权限树与鉴权引擎

详见 [phases/03-permission-engine.md](./phases/03-permission-engine.md)。

- [ ] T3.1：实现精确权限与 SUBTREE（`:*`）匹配器
  - 依赖：P2 退出
  - 验收：匹配规则测试全通过；不支持中间通配和隐式父级包含
- [ ] T3.2：实现 `EffectiveAuthorization` 计算服务
  - 依赖：T3.1
  - 验收：处理顺序符合 DESIGN；过滤禁用/未授权应用
- [ ] T3.3：实现统一鉴权 API（`hasPermission` / `hasAny` / `hasAll` / `getEffectiveAuthorization`）
  - 依赖：T3.2
  - 验收：菜单、按钮、后端 API 共用同一匹配器
- [ ] T3.4：实现授权缓存与事务后失效
  - 依赖：T3.2
  - 验收：缓存键含租户+用户上下文；写操作提交后失效
- [ ] T3.5：历史父级授权迁移与人工确认清单
  - 依赖：T3.1
  - 验收：每条历史绑定有 EXACT/SUBTREE/人工 结果；无批量无脑扩权
- [ ] T3.6：启用 shadow 模式并运行差异观测
  - 依赖：T3.3、T0.3
  - 验收：线上仍返回 legacy 结果；非预期差异可定位到用户/租户/角色/权限

### P4：租户应用与角色授权

详见 [phases/04-tenant-role.md](./phases/04-tenant-role.md)。

- [ ] T4.1：实现租户应用授权管理（开通/禁用/来源/有效期）
  - 依赖：P3 退出
  - 验收：仅 `TENANT` 应用可授权；未授权默认不可用
- [ ] T4.2：迁移现有租户应用继承逻辑为显式授权记录
  - 依赖：T4.1
  - 验收：活跃租户有效应用数量不意外减少；输出迁移统计
- [ ] T4.3：实现预设角色平台默认 + 租户追加权限模型
  - 依赖：T4.1
  - 验收：默认权限锁定；追加权限可增删；超范围默认权限暂不生效但可见
- [ ] T4.4：实现租户自定义角色权限范围校验
  - 依赖：T4.1、T3.2
  - 验收：不可提交未授权应用权限
- [ ] T4.5：正式启用 `role_source`，修复角色查询路径
  - 依赖：T4.3
  - 验收：查询使用 `role_source + role_id`；相同 ID 不同来源不会错误关联
- [ ] T4.6：实现租户管理员动态全应用通配策略
  - 依赖：T4.1
  - 验收：仅受保护系统管理员角色生效；新应用/新权限自动覆盖
- [ ] T4.7：按租户灰度切换并验证
  - 依赖：T4.2–T4.6
  - 验收：灰度期间无非预期越权或权限缺失

### P5：菜单交付与正式切换

详见 [phases/05-menu-cutover.md](./phases/05-menu-cutover.md)。

- [ ] T5.1：实现新菜单树生成器（应用排序 + 递归子节点排序）
  - 依赖：P4 退出
  - 验收：根菜单按 app.sort → menu.sort → menu.id；子菜单在父节点下排序
- [ ] T5.2：菜单返回协议增加 `appId`、`appCode`（向后兼容）
  - 依赖：T5.1
  - 验收：不增加应用层菜单节点；旧客户端可忽略新字段
- [ ] T5.3：分开关切换写路径、权限读路径、菜单读路径、登录快照
  - 依赖：T5.1、T3.3
  - 验收：各开关独立；禁止一次性全切
- [ ] T5.4：性能验收与 N+1 查询排查
  - 依赖：T5.3
  - 验收：登录/菜单/鉴权性能不低于基线；菜单生成无 per-item DB 查询
- [ ] T5.5：`application` 模式稳定运行一个发布周期
  - 依赖：T5.3
  - 验收：差异观测无非预期差异；核心接口无越权回归

### P6：旧模型清理

详见 [phases/06-cleanup.md](./phases/06-cleanup.md)。决策 D1–D4 见该文档 §9。

> 拆为两次发布：发布 A（T6.1–T6.5，纯代码、字段共存、可回滚）；发布 B（T6.6，破坏性 DDL，独立上线）。

#### 发布 A（代码收尾）

- [ ] T6.0：确认前置条件（稳定周期、全租户切换、零差异、无旧实例）+ 备份与回滚演练
- [x] T6.1：审计/旧 `/base` 删除守卫去除 `menu_id/permission_id` 读取（按 `app_id+source_type+pid` 列无关识别根）
  - 验收：审计与 `/base` 服务不再调用 `getMenuId()`；编译 + 单测通过
- [x] T6.2：`access_mode` 单一来源，移除 `enable_permission` 读写分支（APPLICATION 模式已验证）
  - 已完成：`ApplicationMenuTreeBuilder`/`BizMenuUtils`/`ApplicationResourceServiceImpl` 仅读 `access_mode`，移除派生与写回；`BizPlatformMenuServiceImpl` 移除写回（保留旧 `/base` 入参译源）；`MenuTreeNodeVO.accessMode` 经 `menu.*` 已填充
  - 补充（§0.4）：已进一步删除 `PlatformMenu`/`MenuTreeNodeVO.enablePermission` Java 字段及 `/base` 入参译源，`accessMode` 为唯一表示；物理列 `enable_permission` 待 T6.6 删除
  - 验收：菜单可见性/权限开关与基线一致；`BizMenuUtilsTest` 改 `accessMode` 驱动；编译 + 单测通过
- [x] T6.3：删除 `AppTypeEnum` 统一为 `OrgTypeEnum`；应用内菜单/权限 `orgType` 由应用派生（D1）
  - 验收：编译 + 单测通过；DB 值兼容
- [x] T6.4：保留 `getPlatformRole()` 作为唯一来源；删除冗余 `roleSource`/`RoleSourceEnum`（实体字段 + 双写 + 未发布 `role_source` 列/种子）（D2 修订，§0.4）
- [x] T6.5：移除 `authorization.model` 与 shadow/legacy 双轨（D4）；保留旧 `/base`（D3）（APPLICATION 模式已验证）
  - 已完成：删除 `LegacyAuthorizationResolver`/`AuthorizationObservationService`/`ShadowComparisonService`(+Test)、snapshot/diff 服务与 VO、`AuthorizationProperties`/`AuthorizationConfiguration`/`AuthorizationModelEnum` 与 `application.yml` 配置块；`BizAuthServiceImpl`/`UsernameIdentityResolver`/`SocialIdentityResolver` 直调应用模型
  - 验收：登录/菜单走应用模型；dev 工具仅保留只读 audit；编译 + `authorization.*`/`core.*` 单测通过

#### 发布 B（破坏性 DDL）

- [ ] T6.6：`008_application_authorization_cleanup.sql` + `rollback_008` + 初始化脚本/文档
  - 依赖：发布 A 稳定
  - 验收：DROP 旧字段（保留 `platform_role`）、NOT NULL、唯一/索引符合 §6；全新库可启动、可升级可回滚

## 验证任务

- [ ] V1：每阶段完成后执行对应 phases 文档中的验收标准和退出条件检查
- [ ] V2：全链路回归——平台用户、租户用户、多角色、应用启停、通配授权场景
- [ ] V3：安全测试——越权提交、平台权限删除、审计完整性
- [ ] V4：迁移回归——测试库/预发布库全量回填 + 影子对比零非预期差异

## 完成检查

- [ ] 实现与 [DESIGN](./DESIGN.md) 一致
- [ ] [REQUIREMENTS](./REQUIREMENTS.md) 验收标准全部满足
- [ ] 全部 P0–P6 任务完成
- [ ] `specs/current/pms/application-authorization/` 已根据最终实现更新
- [ ] README 完成信息已填写，change 已移入 `specs/changes/archive/2026/`
