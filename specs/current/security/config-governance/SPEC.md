# 安全配置落点 SPEC

> 记录当前已验收并在线生效的系统事实。

## 1. 分层

| 层 | 放什么 | 改的时候 |
|---|---|---|
| L0 密钥 | 私钥、主密钥 | 轮换密钥 |
| L1 共享策略 | 各消费者必须读到同一份的阈值 / 规则 / TTL | 改安全策略 |
| L2 执行面 | `enabled` / `mode` / 路径排除 / oauth2 URL | 开关某个进程的能力 |
| L3 投递运行时 | `event.target`、`source-module`、`categories`、`retention` | 改事件落点和保留期 |

`mode=local|remote`：local 读 L1 地板；remote 读安全中心，L1 仅作末级地板。

**import 规则**：classpath 会执行该能力，才 import 对应 dataId。不要为了「好找」给 Auth 挂凭证文件。

## 2. dataId 与消费者

`spring.config.import` 顺序：common → database（若有） → crypto（若消费） → policy 或 gateway-policy（若消费） → `${spring.application.name}.yml`（最后覆盖）。

| dataId | 内容 | import |
|---|---|---|
| `in-security-crypto.yml` | `ingot.security.crypto` | PMS、Member、BFF、Security |
| `in-security-policy.yml` | `replay`、`credential`、`account.signal`、`event.delivery`/`mysql`、`ignoreTenantValidateRoleCodeList` | PMS、Member、Security、BFF |
| `in-security-gateway.yml` | 限流 / 黑名单 / 违规升级 / 挑战**地板**（groups/rules/items，**不写**各域 `enabled`/`mode`） | 仅 Gateway |
| `in-service-pms.yml` | `account.mode`/`lockout`、event L3 | PMS |
| `in-service-member.yml` | `account.mode`/`lockout`、event L3 | Member |
| `in-service-auth.yml` | `oauth2.auth`、`jwk`、`access`、`session`、event L3 | Auth（**不**挂 policy / crypto / gateway-policy） |
| `in-service-gateway.yml` | 各域 `enabled`/`mode`、`policy.client`、`account.gateway`、event L3 | Gateway |
| `in-service-bff.yml` | `oauth2.resource`、`account.bff` | BFF（policy 中会多带未执行的 `credential` 段，不在 BFF 上改） |
| `in-service-security.yml` | event L3（中心库 retention） | Security |

常量：`NacosConstants.IN_SECURITY_POLICY`、`IN_SECURITY_GATEWAY`、`IN_SECURITY_CRYPTO`。

## 3. 前缀地图

| 前缀 | 层 | dataId | 消费者 |
|---|---|---|---|
| `ingot.security.credential` | L1 | `in-security-policy.yml` | PMS、Member、Security |
| `ingot.security.replay` | L1 | `in-security-policy.yml` | PMS、Member、BFF、Security |
| `ingot.security.account.signal` | L1 | `in-security-policy.yml` | PMS、Member 写；BFF（及 Auth 代码默认 TTL）读 |
| `ingot.security.event.delivery` / `mysql` | L1 | `in-security-policy.yml` | PMS、Member、Security |
| `ingot.security.crypto` | L0 | `in-security-crypto.yml` | PMS、Member、BFF、Security |
| `ingot.security.account.lockout` + `mode` | L1+L2 | `in-service-pms.yml` / `in-service-member.yml` | 各用户体系一份（B/C 可不同） |
| `ingot.security.account.bff` | L2 | `in-service-bff.yml` | BFF |
| `ingot.security.account.gateway` | L2 | `in-service-gateway.yml` | Gateway |
| `ingot.security.access` | L2+地板 | `in-service-auth.yml` | Auth（登录失败四维，不是 Gateway） |
| `ingot.security.session` | L2 | `in-service-auth.yml` | Auth |
| `ingot.security.oauth2` / `jwk` | L2/L0 | `in-service-auth.yml` 或 `in-service-bff.yml` | Auth / BFF |
| `ingot.security.ratelimit` `enabled`/`mode` | L2 | `in-service-gateway.yml` | Gateway |
| `ingot.security.ratelimit.policy.groups/rules` | L1 | `in-security-gateway.yml` | Gateway |
| `ingot.security.blacklist` / `violation-escalation` | 同限流拆分 | 同上 | Gateway |
| `ingot.security.challenge` `enabled`/`mode` | L2 | `in-service-gateway.yml` | Gateway |
| `ingot.security.challenge.policy.groups/policies` | L1 | `in-security-gateway.yml` | Gateway |
| `ingot.security.policy.client` | L2 缓存调参 | `in-service-gateway.yml` | Gateway |
| `ingot.security.event` L3 | L3 | 各 `in-service-*.yml` | 对应进程 |

旧前缀 `ingot.security.account-lock-signal` / `account-lock-bff` / `account-lock-gateway` 已废弃，代码不再读取。

## 4. 已知限制

1. BFF import `in-security-policy.yml` 会在配置源中看到未执行的 `credential` 段；不要在 BFF 上改那一段。
2. Auth / BFF 经 `account-core` 传递依赖 `ingot-security-credential`；Auth 不 import policy yaml，避免把生产凭证策略绑到不执行该能力的进程。彻底拆掉该 `api` 依赖是后续可选 change。
3. Member `lockout.lockDurationMinutes` 现网仍可能为 30，与 account-protection SPEC 推荐的 15 分钟漂移，本能力不改数值。
4. `databases/ingot_nacos_config.sql` 是历史 dump，不以它为 Nacos 真源。
