# Requirements

## 用户场景

### S1 改跨服务必须一致的策略只改一处

- 触发条件：调整永久锁 Redis TTL、防重放窗口或凭证强度。
- 期望结果：只改 `in-security-policy.yml`；PMS / Member / Security（及 BFF 的 replay/signal）下次 rebinder 后读到同一份值，不必进各 `in-service-*.yml` 翻副本。

### S2 改本进程开关不误伤共享策略

- 触发条件：关闭 Gateway 账号锁定 Filter，或把 Auth 会话并发 `mode` 从 local 切 remote。
- 期望结果：只改对应 `in-service-gateway.yml` / `in-service-auth.yml`；凭证、replay、signal 不受影响。

### S3 Auth 不消费凭证配置

- 触发条件：Nacos 变更 `in-security-policy.yml` 中的 `credential.*`。
- 期望结果：Auth 进程不 import 该 dataId，不因凭证变更触发无意义刷新；Auth 的 `access` / `session` / `event` L3 仍只在 `in-service-auth.yml`。

### S4 账号前缀可按域查找

- 触发条件：运维查找锁定相关配置。
- 期望结果：全部挂在 `ingot.security.account.*` 下（`lockout` / `signal` / `bff` / `gateway`）；旧前缀 `account-lock-*` 不再被代码读取。

## 业务规则

1. **import 按消费**：classpath 会执行该能力才 import 对应 dataId。未执行的前缀不应出现在该服务配置源里（BFF import policy 会多带未执行的 `credential` 段，作为「少拆 dataId」的已知噪音，不再把同一文件挂到 Auth）。（P0）
2. **L1 共享策略**（阈值、规则、TTL）放共享 dataId；**L2 执行面**（`enabled` / `mode` / 路径排除）和 **L3 投递运行时**（`event.target` / `source-module` / `categories` / `retention`）放各 `in-service-*.yml`。（P0）
3. **账号 lockout** 继续分 PMS / Member 服务 yml（B/C 必须允许不同）；`account.signal` 必须全局同一份，只出现在 `in-security-policy.yml`。（P0）
4. **前缀一次性改键、不双写**。（P0）
5. 网关限流/黑名单/违规升级的**地板数据**进 `in-security-gateway.yml`，**不声明**各域 `enabled` / `policy.mode`。（P0）
6. 本次不改任何策略数值，只搬家与改键。（P1）

## 边界与非目标

- 已知漂移：现网 Member `lockDurationMinutes: 30` 与 account-protection SPEC 推荐 15 分钟不一致，本次不改值。
- 不部署安全中心、不切 remote。
- 不把 Gateway 规则写入 PMS 也 import 的 `in-security-policy.yml`。
- `databases/ingot_nacos_config.sql` 为历史 dump，不以它为 Nacos 真源；真源为仓库 `nacos/` 目录。

## 验收标准

- [x] 全代码库无 `ingot.security.account-lock-signal` / `account-lock-bff` / `account-lock-gateway` 读取
- [x] `account.signal` 只出现在三环境 `in-security-policy.yml`；PMS/BFF 服务 yml 无副本
- [x] PMS/Member 服务 yml 的 `event` 段不含 `delivery` / `mysql`；对应键在 `in-security-policy.yml`
- [x] 三环境存在 `in-security-gateway.yml` 地板；`in-service-gateway.yml` 只留各域 `enabled`/`mode`、`policy.client`、`account.gateway`、event L3
- [x] BFF `spring.config.import` 含 `in-security-policy.yml`；Gateway 含 `in-security-gateway.yml`；Auth 不含二者
- [x] DEV 不改阈值的前提下，搬家前后绑定数值一致
