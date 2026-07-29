# Requirements

## 用户场景

### S1 账号登录成功 / 失败事件上报中心

- 使用者：任意 ADMIN（PMS）或 Member（APP）用户。
- 触发条件：登录成功或失败，`ingot.security.event.mode=remote` 且 `enabled=true`，安全中心服务可达。
- 期望结果：
  - 业务库 `account_security_event` 仍写入对应记录（双写）。
  - `ingot_security.security_event` 出现 `event_type=LOGIN_SUCCESS` 或 `LOGIN_FAILURE`，`event_category=AUTH`。
  - 字段至少包含：`user_id`、`user_type`（`ADMIN`/`APP`）、`client_ip`、`source_module`（如 `ingot-pms` / `ingot-member`）、`event_time`。

### S2 账号锁定 / 解锁 / 凭证变更事件上报中心

- 使用者：被锁定/解锁的用户，或执行改密/重置/强制改密的对象。
- 触发条件：对应 UseCase 调用 `SecurityEventPort.publishEvent`，remote 模式开启。
- 期望结果：
  - 本地 `account_security_event` 写入 ACCOUNT / CREDENTIAL 类事件。
  - 中心库同步出现：`ACCOUNT_LOCKED`、`ACCOUNT_UNLOCKED`、`ACCOUNT_CREATED`、`ACCOUNT_ENABLED`、`ACCOUNT_DISABLED`、`ACCOUNT_DELETED`、`PASSWORD_CHANGED`、`PASSWORD_RESET`、`FORCE_CHANGE_PASSWORD`（按实际触发类型）。

### S3 网关限流违规封禁事件上报中心

- 使用者：触发 Sentinel 违规升级并写入临时封禁的客户端。
- 触发条件：`SentinelBlockHandler` 达阈值封禁，`ingot.security.event.categories.access=true`。
- 期望结果：
  - `ingot_security.security_event` 写入 ACCESS 类事件（`BLACKLIST_BLOCK` 或 `RATE_LIMIT_VIOLATION`，见 DESIGN）。
  - `extension` 含 `ruleCode`、`countInWindow`、`ttlSec`、`keyType`、`keyValue` 等网关审计字段。
  - **不再**向 `gateway_blacklist_event` 插入新记录。

### S4 未部署安全中心或 remote 不可用

- 使用者：仅部署 PMS/Member/Gateway、未注册 `ingot-security` 的环境。
- 触发条件：`ingot.security.event.mode=local`，或 Feign `RemoteSecurityEventService` 不可用。
- 期望结果：
  - 登录、锁定、封禁等主链路行为与 L2 一致。
  - 账号域本地表正常写入（adapter 存在时）；无 RPC 异常抛出到业务线程。
  - 网关封禁逻辑不受影响；仅中心上报静默跳过（debug/warn 日志）。

### S5 记录开关与类别降级

- 使用者：运维 / 安全管理员。
- 触发条件：Nacos 设置 `ingot.security.event.enabled=false`，或关闭某类别（如 `categories.credential=false`）。
- 期望结果：
  - 被关闭的路径不上报中心库。
  - 本地 `account_security_event` 写入行为**不变**（账号域本地审计不受中心开关影响）。
  - 主链路不受影响。

### S6 配置热刷新

- 使用者：运维 / 安全管理员。
- 触发条件：运行中修改 Nacos 的 `enabled` / `mode` / `categories.*`，不重启服务。
- 期望结果：下一次事件触发时，上报行为按新配置生效（例如关闭 `enabled` 后不再写入中心库）。

## 业务规则

### P0 事件类型（本期必须支持）

**AUTH（2 种，已发布）**

| event_type | 说明 |
|------------|------|
| `LOGIN_SUCCESS` | 登录成功 |
| `LOGIN_FAILURE` | 登录失败 |

**ACCOUNT（6 种，已发布）**

| event_type | 说明 |
|------------|------|
| `ACCOUNT_CREATED` | 账号创建 |
| `ACCOUNT_ENABLED` | 账号启用 |
| `ACCOUNT_DISABLED` | 账号禁用 |
| `ACCOUNT_LOCKED` | 账号锁定 |
| `ACCOUNT_UNLOCKED` | 账号解锁 |
| `ACCOUNT_DELETED` | 账号删除 |

**CREDENTIAL（3 种，已发布）**

| event_type | 说明 |
|------------|------|
| `PASSWORD_CHANGED` | 密码修改 |
| `PASSWORD_RESET` | 密码重置 |
| `FORCE_CHANGE_PASSWORD` | 强制修改密码 |

**ACCESS（2 种，网关新增）**

| event_type | 说明 |
|------------|------|
| `BLACKLIST_BLOCK` | 封禁写入（含自动/手工，动作 B/U/R 映射至 extension） |
| `RATE_LIMIT_VIOLATION` | 限流违规达升级阈值（尚未封禁仅记录时可选，封禁场景与 BLOCK 二选一映射，见 DESIGN） |

### 延期类型（enum 预留，本期不强制 producer 发布）

`LOGOUT`、`TOKEN_REFRESH`、`PASSWORD_EXPIRED`、`SESSION_*`、`POLICY_*`、`MFA_*` 及需求第九章其余访问/会话/策略类事件。

### 优先级

- P0：中心入库 API、账号域双写+转发、网关 ACCESS 上报、S4–S6 降级与热刷新。
- P1：批量上报接口（`publishBatch` 映射）、`extension` 字段规范化。
- P2：单元测试覆盖映射与 Composite Port 分支。

### 与其他模块关系

- **安全事件 vs 安全审计**：本期事件为「系统运行安全事实」；管理员改策略、导出日志等属审计域，不在 L3。
- **安全事件 vs 风险命中**：风险命中解释「为何判为风险」，L3 不实现。
- **L4 访问防护**：依赖 L3 ACCESS 类事件上报能力；L3 归档后再实施 L4。

## 边界与非目标

- **异常场景**：中心库写入失败不 retry 到阻塞业务；可记录 warn，本地表已成功即可。
- **兼容**：
  - 不改变 L2 账号保护判定逻辑与 `account_security_event` 表结构。
  - `gateway_blacklist_event` 历史只读；旧 Platform GET `/events` 仍可查历史，新事件仅入 `security_event`。
  - `user_type` 规范为 `UserTypeEnum.name()`（`ADMIN`/`APP`）；不迁移历史 `'0'/'1'` 注释歧义数据。
- **非目标**：
  - Platform 查询 / 导出 API。
  - 安全概览、告警、风险规则、MQ、历史回填、全量第九章事件类型。

## 验收标准

- [ ] S1：`mode=remote` 下 ADMIN/Member 登录成功/失败后，本地表与 `ingot_security.security_event` 均有 AUTH 记录（DB 直查）。
- [ ] S2：锁定/解锁/改密等操作后，中心库出现对应 ACCOUNT/CREDENTIAL 事件，字段与本地语义一致。
- [ ] S3：网关触发封禁后，中心库 ACCESS 事件含 ruleCode/ip/path/traceId；`gateway_blacklist_event` 无新 INSERT。
- [ ] S4：`mode=local` 或 security 未部署时，主链路正常，无 Feign 异常泄漏。
- [ ] S5：`enabled=false` 或类别关闭时，中心库无对应类别新记录，本地表仍写入。
- [ ] S6：改 Nacos 开关不重启，下一次事件行为符合新配置。
- [ ] P0 共 13 种 event_type 均可通过对应操作触发并入库（按实际 producer 覆盖 11+2）。
- [ ] 相关模块编译通过；migration `010` 在 `ingot_security` 可执行且可回滚。
