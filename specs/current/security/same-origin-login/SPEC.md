# 内部后台同源登录规格

## 入口与职责

平台固定为 `platform-admin / PLATFORM`，租户固定为 `tenant-admin / TENANT`，使用各自内部 OAuth 客户端。每个 origin 提供 `/login/`、管理路由及同源 `/api`。支持不同域名，或同一 IP 的两个 HTTPS 端口；两个 origin 必须不同。`require-https=false` 只允许 loopback 开发。

浏览器只部署 `apps/admin-platform` 与 `apps/admin`。旧 `/auth/start`、`/auth/complete`、`/oauth2/challenge` 页面仅在当前 origin 清理参数并引导至 `/login/`。不提供旧浏览器 ticket 兑换、四站兼容、跨域准备、对外 OAuth2/OIDC 或 SSO。BFF 内部授权码与 PKCE 保留。

Gateway 按可信 Host、scheme、端口匹配注册表；剥离外部应用身份与旧主机角色头，只注入当前 `In-Inner-Bff-App-Id`。BFF 不采纳浏览器提交的 appId/domain/clientId 或回跳 URL，未知入口及应用不匹配拒绝。

## 接口与正常流程

下表为服务端路径，浏览器增加当前 origin 的 `/api` 前缀，`{entry}` 只取 `platform` 或 `tenant`。

| 方法与路径 | 行为 |
|---|---|
| POST `/bff/auth/csrf` | 同源自举；有效绑定/CSRF复用，失效时建立匿名绑定 |
| POST `/bff/auth/{entry}/transactions` | 用户点击后创建本次 LOGIN 事务，不返回登录地址 |
| POST `/bff/auth/{entry}/login` | whole HYBRID 提交事务及凭证；平台/单组织直接完成，多组织返回候选 |
| POST `/bff/auth/tenant/select` | 在本事务候选内选择，Auth 重新校验当前资格，直接设置正式 Cookie |
| GET `/bff/auth/{entry}/transactions/{id}` | 只读阶段、期限及本次结果是否已建立；不签 Cookie、不续期 |
| POST `/bff/auth/{entry}/transactions/{id}/resume` | 仅异常恢复，重新发送同一当前有效结果的 Cookie |
| GET `/bff/auth/me` | 按当前 Cookie、代次和权威 sid 核对应用身份 |
| DELETE `/bff/auth/logout` | 撤销当前应用，清其正式/绑定 Cookie，禁止恢复；不清另一应用 |

普通访问登录页可以执行一次 me；无会话留表单，有会话进入 bootstrap。无进行中事务时，停留或恢复焦点不创建事务、轮询或心跳。正常提交为 csrf → transactions → login →（多组织 select）→ 本次事务 GET → me → IAM bootstrap，各步骤一次。事务恢复刚得到的同一完成结果可用于一次确认，仍须检查本次 Cookie 与 me。

平台不返回租户候选。租户零候选失败，单候选仍完整校验，多候选在同页选择。强制改密先进入受限流程，不能提前加载业务；无授权菜单显示稳定空态。

## 状态、期限与并发

事务使用 LOGIN、AUTHENTICATING、SELECT_TENANT、COMMITTING、COMPLETED 及失败/结果不确定状态。默认事务期限600秒，从创建开始；选择期限还受 Auth 返回的真实预授权截止约束，缺失/非法截止不猜测为有效。

默认完成恢复窗口60秒、正式会话配置期限7200秒、每绑定最多5个未终结事务。Auth 的自身期限仍生效；GET 不延长这些期限。前端不确定结果查询总预算15秒，最多15次，不盲目再次提交密码或选择；仅本次完成且 Cookie 未建立时允许一次 resume。

Redis 状态使用 `BffRedisKeys`：`in:bff:v2:{appId}:` 下的 binding、tx、session、binding-current、delivered，以及清理 Hash/到期索引。每应用单一 Cluster hash tag，保证 CAS/租约/栅栏与提交同槽，不表示跨槽均衡。Redis 不可用时不以本地缓存伪造成功。

同事务仅取得执行权的工作者调用 Auth 和提交；旧工作者、旧完成结果不能覆盖新代次。正式提交先通过 Auth getBySid 核对客户端、租户、到期和 userId；公开 Token 响应中的内部字段不是身份来源。成功存储后才签 Cookie；身份确认失败不签发。

首次合法正式 Cookie 使用幂等确认交付。恢复窗口结束不撤销已交付活跃会话；未提交/未交付的已知 sid 进入持久补偿。旧身份替换和退出立即使旧代次失效，远端撤销失败继续重试。Auth 完全丢响应且未获得 sid 时停止同操作重做，保留 UNKNOWN 和告警，不宣称能自动查回未知 sid。

## Cookie 与安全边界

HTTPS 使用 `__Host-IN_PLATFORM_SESSION/BINDING` 与 `__Host-IN_TENANT_SESSION/BINDING`；loopback HTTP 去掉 `__Host-`。统一 HttpOnly、Path=/、SameSite=Lax、无 Domain；HTTPS 使用 Secure。匿名绑定与正式 sessionId 分离。

同 IP 不同端口会收到两套 Cookie，只按当前应用名称和 appId/domain/代次/sid验证身份；不读取或清理另一应用。不同名称避免覆盖，不提供按端口的发送隔离；两个端口须属于同一可信部署。

写接口校验精确 Origin、JSON 和 CSRF；csrf 自举只豁免 CSRF 本身，不豁免同源检查。读取校验绑定/会话及同源来源。不开 credentialed CORS，不从 query/Referer 覆盖非法 Origin。指纹按配置 DEVICE 或 IPUA 固定执行，DEVICE 缺头拒绝，不逐请求降级。匿名认证路径不继承旧 Bearer。

错误区分凭证失败、403来源/绑定、410过期、409处理中/结果不可恢复、429容量和503依赖故障。503不伪装过期、不自动撤销旧身份；HTTP/业务401在前端清理后强制表单。日志与 API 不暴露密码、Token、sid、PKCE、Auth Cookie、CSRF或正式 sessionId。时间点使用带时区 ISO-8601，响应 UTC Z。

## 浏览器身份与部署

提交、选择、恢复、确认、进入、退出及导航合并在途操作。公开登录页独立渲染，不受身份/权限 KeepAlive key 重挂；受保护页面在重验/退出期间暂停，清旧菜单、缓存和表单后才进入新身份。旧请求准备、响应及重试受身份纪元约束，不能回填或退出新身份。

切换组织显式强制表单并保留安全相对深链；普通退出清深链，本地401保留安全深链。显式退出最长等待1500ms并中止请求；其他同源标签收到退出直接清理与显示表单，不重验、不重复退出。新身份通知让活动管理页重新 bootstrap；平台/租户的不同 origin 不共享通知。

注册、Gateway/BFF/Auth、两前端与反代必须协调发布；只读取 v2 状态和新 Cookie，不混跑旧协议。Nacos 配置和公开部署样例已收口两入口。生产发布、客户 CA/SAN、真实休眠与完整回滚的环境证据另由发布验收变更维护，不将本地验收宣称为生产已切换。
