# 双入口部署、切换与回滚

> 状态：completed；配置与样例已落地并完成本地检查，目标环境发布/回滚尚未执行。

## 1. 部署矩阵

| 应用 | 域名生产 origin | 纯 IP 生产 origin | 本机 DEV origin |
|---|---|---|---|
| platform-admin / PLATFORM | https://platform.example.com | https://192.168.1.100:8443 | http://platform.localhost:5799 |
| tenant-admin / TENANT | https://admin.example.com | https://192.168.1.100:9443 | http://tenant.localhost:5798 |

每一行的 /login/ 是公共登录页、/ 是管理首页、/api 是同源 Gateway 代理。
只有两个 SPA 管理宿主，均挂载登录插件；不再需要 1798/1799 独立登录服务。开发时也从相应管理宿主的 /login/ 进入。
不支持四域名登录、跨域准备或外部 Origin 动态回跳。本地两个 origin 仍按可信注册匹配。

## 2. 目标注册配置

现有 ingot.bff.apps 保留；一行仅一个 origin。示意：

    ingot:
      bff:
        require-https: true
        transaction-ttl: 600
        session-ttl: 7200
        completion-recovery-ttl: 60
        max-active-transactions: 5
        apps:
          - app-id: platform-admin
            domain: PLATFORM
            origin: https://192.168.1.100:8443
            oauth-client-id: in-bff-platform
            oauth-scope: system
            oauth-redirect-uri: https://gateway.example.com/bff/auth/platform/callback
            default-return-to: /
          - app-id: tenant-admin
            domain: TENANT
            origin: https://192.168.1.100:9443
            oauth-client-id: in-bff-tenant
            oauth-scope: system
            oauth-redirect-uri: https://gateway.example.com/bff/auth/tenant/callback
            default-return-to: /

示例 oauth-redirect-uri 是 Auth 注册的内部协议值，浏览器不访问；部署时填写实际且与 RegisteredClient 精确一致的值。不能复制示例域名当生产可用配置，也不能换成用户传入 URL。
apps/require-https 仍只由 Gateway/BFF 加载 in-bff-apps.yml；TTL/容量由 BFF 的 in-service-bff.yml 消费，Gateway 不复制事务配置。
原 admin-origin/login-origin、登录域/start/completion 编排字段在新 schema 删除；固定 login-path 由应用路由提供，不接受任意回跳参数。
切换 DEV/TEST/PROD 三环境样例和部署覆盖。origin 规范化后唯一，HTTPS IP 合法，无“必须同主域”限制；require-https=false 仅 loopback DEV。

## 3. Cookie

| 场景 | 平台正式 / 绑定 | 租户正式 / 绑定 |
|---|---|---|
| HTTPS | __Host-IN_PLATFORM_SESSION / __Host-IN_PLATFORM_BINDING | __Host-IN_TENANT_SESSION / __Host-IN_TENANT_BINDING |
| loopback HTTP DEV | IN_PLATFORM_SESSION / IN_PLATFORM_BINDING | IN_TENANT_SESSION / IN_TENANT_BINDING |

共同属性：HttpOnly、Path=/、SameSite=Lax、无 Domain；HTTPS 加 Secure，名称只跟 require-https 和固定应用命名空间，不按不可信请求 scheme 自行降级。
正式会话 max-age 使用剩余 session-ttl；绑定续期不缩短已有有效期限。名称、默认值、Header 与状态值均用命名常量/枚举，不在调用点裸写。
同一 IP 的不同端口会收到两套 Cookie。只解析当前应用名称，校验 Redis appId/domain 及会话代次；忽略其他应用 Cookie，不查其 Redis、不清除它们。
不同名称提供覆盖/误用防护，不提供 Cookie 发送隔离。所有同 IP 端口必须由同一可信部署控制。若客户要求浏览器不向另一应用发送 Cookie，使用不同主机名或不同 IP；不能用端口或 Path 冒充该保证。
平台/租户因 origin 不同，浏览器存储、BroadcastChannel 等按 origin 隔离；应用数据键仍带 appId 前缀，避免配置误用。

## 4. Nginx、TLS 与静态站点

- 一台服务器可用两个 server_name 或两个 HTTPS listen 端口，分别托管 admin-platform/admin 两份产物；每份均由本 SPA 处理 /login/ 与管理路由。
- /api 优先反代 Gateway，保留包含非默认端口的原 Host；HTTP Header 由可信边缘规范化，Gateway 校验注册表，拒绝绕过代理的内部身份伪造。TLS 终止与可信转发 scheme 配置一致。
- 所有 SPA 深链刷新回本应用 index.html；/api 错误不能回落为 index.html。静态资源 base 与产物路由一致，不串到另一应用。
- 登录 HTML/认证响应不缓存；带 hash 静态资源可缓存。切换时清理旧 Service Worker/HTML 缓存，不能将密码或 API 响应置于离线缓存。
- 生产全程 HTTPS。域名证书覆盖实际 hostname；私有 IP 证书包含对应 iPAddress SAN，优先客户 CA，没有时使用项目私有 CA并在客户端安装根信任。不能要求长期忽略证书错误。
- BFF 不直接向用户网络暴露；Gateway/边缘封闭内部头、Host、转发信任。不因内网而放开 credentialed CORS。
- 登录页与管理台同源，统一 CSP、frame-ancestors/点击劫持防护与第三方脚本治理；Referrer-Policy 不泄露敏感参数。

## 5. 切换步骤

M01：备份前后端及Auth版本、Nacos 注册、Nginx、TLS、Auth RegisteredClient 协议回调以及原 Cookie/Redis schema说明；确认应用 appId/clientId 对应，不动 JWK。
M02：在隔离环境完整部署提供预授权有效期字段的Auth接入、新 BFF/Gateway、两份管理宿主与新注册配置，执行 A01–A24；前后端不混跑。
M03：生产维护窗口停止旧登录/选择/complete 写入。等待在途或明确取消旧事务；清理/撤销已知未交付 sid，不能只删除含 Token 的事务键。
M04：同时切换两份前端、Auth接入、BFF、Gateway、Nacos 与反代。新 Redis 使用 v2 命名空间，只读取新 Cookie；不 fallback 到旧 IN_SESSION/__Host-IN_SESSION，避免身份混用。
M05：当前新 host 清除已知旧公共 Cookie（同 Path/Domain 属性）；退役独立登录 host 若保留 DNS，仅配置固定映射到对应新 /login/ 的静态引导，不能用 query 控制目标，不提供四站 BFF 兼容。
M06：明确通知一次重新登录。旧 BFF 会话先在受控旧环境按该内部客户端范围撤销/清理，禁止误撤 Member、外部客户端或另一无关应用；提供 dry-run、数量和备份记录。
M07：验证 /login/ 直达、登录/退出、Cookie 隔离、bootstrap与强制改密，观察 403/503、结果恢复、补偿积压；完成生产验收后再更新 current。
本轮无业务 DDL，但配置/Redis schema/接口为破坏性部署变更。不能只改 Nginx 或单独发布新前端。

## 6. 回滚

停止新认证写入，撤销/清理新 v2 的已知未交付和活跃内部会话，记录范围；回退完整前后端/Auth/Gateway版本及成套旧 Nacos/Nginx/TLS配置。
清新 Cookie，恢复旧版本要求的四站环境后重新登录；不把 v2 会话转换成旧会话，不承诺恢复已撤销 sid。
若旧四站地址不可恢复，则在切换前把“修复发布而非版本回退”的运维策略写入发布记录；没有可用回退环境不得执行 M03。
保留两代 Spec、发布与清理证据，不删除未完成 change；新旧 current 只按实际验收结果维护。

## 7. 运行残留清理（2026-10-31）

用户明确要求清理旧 App、配置、脚本与使用文档，保留历史 Spec，不创建提交。本项落实既定双入口退役范围，不改变登录协议或安全模型。

- 只保留两个管理宿主的构建/启动/部署链，旧登录 App 与 CI job 删除。
- 相应 Group 同步 `in-bff-apps.yml`、`in-service-bff.yml`、`in-service-gateway.yml`、`in-security-gateway.yml`、`in-service-auth.yml`；最后一项仅移除未消费的独立登录站 `loginFormUrl`。
- 初始化 SQL 同步上述三组共 15 条当前配置及 MD5；补齐 TEST/PROD 的应用注册，不再导入这五类配置的旧登录域历史种子。已有环境不重导初始化 SQL。
- IAM 独立测试工具只要求 tenantAdmin/platformAdmin 两个 origin，登录位于各自 /login/；迁移旧本地配置后按已有指纹漂移/reset 规则重建清单。
- 容器反代在受控边缘后保留外部 HTTPS scheme 和带端口 Host；平台手工脚本使用独立镜像、容器与域名。
- 三种旧页面仅保留本域无参数引导；旧状态只读盘点和成套回滚说明按第 5/6 节保留。历史 Spec 与 current 不在本项清理中改写。

## 8. 依据

- Cookie 端口边界：[RFC 6265 §8.5](https://www.rfc-editor.org/rfc/rfc6265.html#section-8.5)。
- 同源定义：[MDN](https://developer.mozilla.org/en-US/docs/Web/Security/Defenses/Same-origin_policy#definition_of_an_origin)。
- TLS IP 身份：[RFC 9525 §6.4](https://www.rfc-editor.org/rfc/rfc9525.html#section-6.4)。
