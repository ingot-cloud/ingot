# 同源双入口发布与回滚

本次为协议/配置破坏性发布，没有业务 DDL。镜像先构建并验证，生产切换在维护窗口成套完成，不混跑新旧 Auth/BFF/Gateway/前端。

## 配置与发布清单

- 两份前端 dist：`admin-platform` 和 `admin`，各自 `/login/`；反代样例见配套 [前端部署目录](../../../ingot-admin/deploy/same-origin/README.md)。
- BFF 已加入单机与 Swarm Compose，5400/6400 只在私网使用，不发布用户端口。环境模板新增 BFF_VERSION/BFF_MEMORY/BFF_CPUS/BFF_REPLICAS。填写本轮已验证且可回滚的版本，保留旧镜像，勿覆盖旧版本标签。
- Auth 框架必须提供固定 UTC `authenticationExpiresAt` 并在上下文序列化/租户选择时保留期限。BFF、Gateway 两应用注册表保持一致。
- DEV/TEST/PROD 同步 `in-bff-apps.yml`、`in-service-bff.yml`、`in-service-gateway.yml`、`in-security-gateway.yml`、`in-service-auth.yml`（删除已废弃且不被消费的独立登录站 `loginFormUrl`）。BFF 路由必须 PreserveHostHeader；所有边缘代理使用含端口的原 Host，并覆盖不可信转发头。
- 生产 require-https=true。纯 IP 仅替换两个 origin 为实际 HTTPS IP:8443 / IP:9443；OAuth 回调作为内部协议值与 Auth RegisteredClient 精确一致，无浏览器跳转或域名解析依赖。客户端 CA 信任和 SAN 在目标环境验证。

初始化种子 `databases/ingot_nacos_config.sql` 的上述五份配置已同步到三个 Group，并移除这些配置的旧登录域历史种子；无关记录保持原样。已有环境通过 Nacos 发布 YAML，不重新导入初始化 SQL。历史 Spec 保留。

## 维护窗口前的只读盘点

`python3 deploy/services/same-origin-inventory.py --host <redis-private-host> --port <port> --db <db>`

使用最小只读 Redis ACL；密码通过 REDISCLI_AUTH 传给 redis-cli，不放命令行/日志。可通过 --platform-client/--tenant-client 传实际注册客户端。Cluster 按主节点分别盘点；脚本不会扫描所有槽或代替集群备份。

仅扫描明确的 v1 BFF key 与两个 v2 命名空间，输出脱敏分类数量，无任何删除/撤销功能。数量是记录数，不是唯一 sid 数；Hash/ZSet 只计索引/队列，不展开敏感载荷。未知结构、客户端不符的记录保持原样，需人工核实，不能扩大为全库清理。

## 协同切换

1. 备份旧镜像、前端、Nacos、Nginx、TLS、RegisteredClient 和 Redis 持久化快照。先在隔离环境验证两套新入口并确认旧环境可恢复。
2. 停止旧登录/选择/complete 写入，等待在途或明确取消旧流程。按 v1 BFF 记录确定候选 sid；通过受控 Auth `RemoteAuthSessionService.getBySid` 再核对实际 clientId 与应用范围，仅对两个内部客户端调用 revokeBySid。禁止直接删 Auth Redis/OAuth 键，避免 Refresh Token 残留；无 sid 的未知结果按 Auth 安全记录人工核实并等待原生期限，不能声称已撤销。
3. 同批切换 Auth、新 BFF/Gateway、两前端、Nacos 与反代。v2 不读 v1 Cookie/Redis，不转换旧身份；用户一次重新登录。新域自举只清实际携带的已知 v1 host-only Cookie，退役登录域仅保留固定映射到新 /login/ 的静态引导。
4. 观察 prepared/authentication/failure/commit/resume 与 cleanup.backlog 指标。BFF 的两个认证 Feign context 单独设置 loggerLevel=NONE，避免 URL/头/Body 中的 sid、Cookie、PKCE 泄漏；故障只保留低基数指标与脱敏关联。清理队列在 Redis 中无TTL；远端撤销失败保留重试，不能仅等过期通知。Redis 本身须有生产级持久化/高可用和备份；每应用一个 hash slot，三主无副本的本地测试只验证同槽语义，不验证生产容量或容灾。
5. UNKNOWN 不自动换码重试；有 sid 的提交前故障先记持久任务，Redis 无法记账且尚未尝试提交时尽力直接撤销；Redis/Auth 同时不可用时有 ERROR 告警与 Auth TTL/人工处理边界。提交回包不明时先查询持久结果，不能撤销可能已交付的会话。

## 回滚

停止新认证写入，盘点并按上述 Auth 接口撤销属于新 v2 的内部会话，待清理任务成功后再清精确范围的 BFF v2 键。恢复整套旧镜像/前端/Nacos/Nginx/RegisteredClient，清新 Cookie，再登录旧环境。被撤销的 sid 不恢复，不将 v2 转成 v1。

若旧四站环境无法恢复，应在发布前明确采用修复发布策略；无有效回退方案不开始生产维护窗口。真实账号、CA信任、长停留/休眠、生产切换与回滚均在联合验收记录签认，不能用本地 Mock 或配置语法检查替代。

网关安全地板新增准备/状态读取分组（每 IP 1 秒统计窗口请求预算 30、突发 45），选择/恢复加入既有登录限流分组；未将准备/选择/恢复加入 ALWAYS 挑战。若实际部署使用 remote 策略，需先在远端策略源加入同样分组，再切换发布；按 NAT 规模压测调参，不能仅依赖地板而宣称远端策略已更新。Cookie 中继的业务写请求也校验精确 Origin；显式 Bearer/Basic 等非 Cookie 客户端保持原认证方式。
