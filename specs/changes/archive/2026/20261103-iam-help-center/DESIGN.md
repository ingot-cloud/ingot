# 设计：受保护的版本化帮助内容

## 1. 决策摘要

| 决策 | 采用方案 |
| --- | --- |
| 服务归属 | 现有 `ingot-iam-provider` 的独立 `help` 模块，复用当前身份；不建新微服务或文档数据库。 |
| 内容源 | 后端仓库 `docs/help/zh-CN/`，common/tenant/platform 分开维护；Git 是内容事实来源。 |
| 发布 | 构建生成域隔离内容包与内存检索数据，打入 IAM 受保护 classpath；不自动暴露为 Spring 静态资源。 |
| 授权 | tenant/platform 两组固定域端点，调用 `CurrentIdentityService.requireDomain`；不从请求推断域，不新增阅读操作码。 |
| 传输 | JSON `R<T>`；图片也用 `R<HelpAsset>` 返回受限大小的 Base64，沿用统一客户端与指纹/BFF 链。 |
| 解析与呈现 | 后端构建校验 Markdown 并生成纯文本检索及章节元数据；前端禁用原始 HTML、净化渲染，仅接受显式帮助链接和受保护图片。 |
| 搜索 | 当前域不可变快照中本地检索；不引入 ES、Redis 或远端加载/降级逻辑。 |

## 2. 模块与数据

实现位置建议固定为：

- `com.ingot.cloud.iam.help`：HelpContentRepository、HelpContentService、HelpSearchService 及常量/校验。
- `web.v1.tenant.TenantHelpAPI`、`web.v1.platform.PlatformHelpAPI`：固定域控制器，不接受域选择参数。
- `ingot-iam-api` 下独立 help DTO 包：仅 IAM 服务内对外使用，不为方便而扩散到框架公共 IAM 模型。已有 `AuthorizationDomain`、`PageResponse`、`IamReasonCode` 复用。
- `tools/help/`：构建与校验工具；Gradle `verifyHelpContent`、`buildHelpContent` 接入 provider 的资源构建与检查。

源文件、元数据与功能映射见 [CONTENT.md](./CONTENT.md)；公开字段及请求约束只在 [API.md](./API.md) 定义。新增枚举按 in-framework 统一形态实现，状态、媒体类型、限制值和路径使用命名常量；对外类型、方法、字段写 JavaDoc，依赖使用构造注入。

内容包包含 `schemaVersion=1`、产品版本与各域独立 `contentVersion`。每域内容版本由规范化后的元数据、Markdown、分类/入口映射、图片及缩略图字节共同计算 SHA-256（小写十六进制）；同输入可重复生成。common 内容进入两域各自快照，修改会使两域版本分别变化。API 不暴露作者机器路径、原始 front matter、另一域清单或文件系统地址。

## 3. 认证与信息隔离

```mermaid
flowchart LR
  A[当前端同源请求] --> B[Gateway/BFF 已认证会话]
  B --> C[固定域 API 与有效身份校验]
  C --> D[只选当前域不可变内容快照]
  D --> E[目录 / 搜索 / 正文 / 图片]
```

1. 继续使用现有不同 origin 的 tenant-admin/TENANT、platform-admin/PLATFORM 注册。没有新增 Cookie、Bearer 通道、CORS 或匿名静态例外。
2. API 首先检查有效当前成员、固定域及既有强制改密约束，随后检查版本、参数、文章和资产归属；身份依赖故障按既有授权不可用规则拒绝。
3. 帮助阅读不绑定业务页 read 操作，否则缺少业务权限的成员无法阅读申请权限的方法。文章只描述如何使用，不能返回真实业务对象、配置值或权限外资料。
4. Repository 以 `(domain, contentVersion)` 选出独立快照，服务不向控制器暴露未过滤的全局索引。目录、首页编排、搜索排序前的候选、总数、相关文章、前后篇均从该快照产生。
5. 图片通过快照内 assetId 索引读取，必须是该域已发布文章所引用资源。common 资源须通过两域引用闭包校验。任何用户输入均不得拼成磁盘路径；拒绝斜杠、点段、编码穿越与任意文件访问。
6. 响应统一 `Cache-Control: private, no-store`、`X-Content-Type-Options: nosniff`；反代/CDN不缓存帮助响应。原图/缩略图只在 jar 私有目录，不创建绕过鉴权的二进制直链、静态映射、预签名 OSS 链接或 source map。
7. 帮助图片为构建期本地资源，不是 OSS 入库字段，本期不新增对象字段。今后接入 OSS 必须另审保护机制并遵循 bucket/objectName 与 @OssUrl，不能把短时链接当成严格身份隔离的替代。

## 4. 内容构建与运行

`verifyHelpContent` 校验文件命名、元数据、正文结构、章节标识、链接、图片、域闭包、功能覆盖与人工证据字段；`buildHelpContent` 用同一解析结果生成正文、摘要树、章节、图片描述及纯文本检索快照。

生成资源放在构建输出的 `help-content/`，不放在 `static/`、`public/`、`META-INF/resources/`。不把 `docs/` 或 `specs/` 整目录打包。启动时完整校验包版本、摘要及引用后原子安装两域快照；任一包破损使帮助模块处于不可用状态并记录不含正文的诊断，所有帮助数据接口返回 `HelpContentUnavailable`，不装入半包或 fallback 到其他内容。原有 IAM 业务服务继续工作。

只读内存快照由应用版本管理，不按用户存储，不是跨身份响应缓存。每次请求仍校验身份。无写接口、后台同步、LKG 或热更新。

## 5. 检索

- 输入 q 做 NFKC 归一化、首尾去空白及大小写折叠，以空白分词；连续中文保留为子串，不引入分词服务。每个词必须命中至少一个字段，作者通过 keywords 覆盖“人员/成员”等常见说法。
- 每词在标题、关键词、章节标题、正文分别计 12/8/4/1 分，每字段只计一次；完整查询与标题相等额外 20 分，标题包含完整查询额外 10 分。总分降序，再按 updatedAt 降序、articleId 升序稳定排序。
- categoryId 限定该节点及子树；先选域与分类再匹配、计数、分页。每页默认 20，上限 50；空白 q 是 400，不返回全站索引。
- 返回纯文本分段高亮，禁止返回可执行 HTML；摘要最多 160 个 Unicode 码点，优先正文首次命中附近，末尾可用省略号。
- 章节命中时可给出最佳匹配的已验证 headingId。只在标题/关键词命中时定位文章顶部。不得返回另一域的联想、搜索纠错或总数。
- 不记录原始 q、正文、Base64 或客户资料；不新增搜索统计。

## 6. 版本与错误恢复

目录接口不带版本，返回当前 `contentVersion`；搜索、文章及图片请求必须带该版本。不匹配时先返回 409 `HelpContentVersionChanged`，不返回不匹配的内容。前端清当前帮助状态、重新读目录并仅自动重试一次；持续混版显示刷新提示，禁止无限轮询。前端检查响应域与版本和当前请求一致才渲染。

API 错误采用现有 IAM HTTP/R 映射：InvalidArgument/IdentityInvalid/ActionDenied/PasswordChangeRequired/ObjectNotFound/AuthorizationUnavailable。向 `IamReasonCode` 增加 `HelpContentVersionChanged`（409）与 `HelpContentUnavailable`（503），按同一枚举门禁实现。普通未找到资源与跨域 ID 使用相同默认 message，不回显输入所对应的标题。

图片沿用 JSON 文件响应，前端通过统一 request 获取，再解码为 Blob URL。这样请求可以携带必要设备头，收到 401 时也能进入统一身份清理；不直接给 `<img>` 设置 API URL。每张原图最多 2 MiB、最大边 4096px，缩略图最多 512 KiB、最大宽1200px，允许 PNG/JPEG/WebP；运行内容不接受原始 SVG/HTML 或远程图片。

## 7. 兼容、发布与回滚

- 新增只读接口，无 DDL、数据迁移、Nacos 键、业务角色或菜单种子变更。旧前端不调用，保持兼容。
- 先部署含已验证内容包的 IAM，再部署前端帮助插件；前端不兼容 `schemaVersion` 或后端未升级时展示帮助暂不可用并提供返回管理台，不能把错误伪装为空内容。
- 两前端、内容包与功能版本同一发布清单固定制品摘要；帮助首发上线前必须完成全部已交付功能的内容覆盖、截图和实操验证。
- 滚动发布的 409 恢复可能短暂不可用，发布时应完成后端滚动再切新前端，不提供历史版本 pin 接口。前端恢复不改变登录状态，401 按既有流程处理。
- 回滚先回退前端，再回退 IAM/内容制品；重新核对文档与对应功能版本，数据库无需恢复。出现串域立即停止新版帮助访问并回滚制品，不用关闭认证来排障。

## 8. 测试策略

### 2026-11-03 用户确认的截图延期

用户说明本地暂时无法登录，明确要求真实操作截图后补。按此授权，先完成程序、文字教程、概念示意图及隔离/交互的自动化和受控浏览器验证；真实Gateway/BFF账号联调与操作截图实操证据保留待验收，不伪造证据或截图。

构建工具增加显式 `--preview`，Gradle仅在 `-PhelpContentPreview=true` 时使用：仍严格校验域、版本、链接、文件/媒体和已有图片引用，仅暂缓人工实操证据及必需截图的完整性检查。默认构建、`verifyHelpContent` 和正式发布继续完整校验，不允许预览制品作为正式发布通过。没有截图的文章先呈现完整文字步骤；已有概念图可正常检验受保护图片与预览效果。

- 构建级：断链、章节重复、跨域关系和资产、伪造媒体、体积、元数据、可重复摘要、功能覆盖与缺图失败用例。
- 服务级：三类内容闭包、排序/分页/高亮、两个版本、不完整包、内容不可用；图片与文章同版本。
- HTTP/身份：租户/平台各自允许，双向域不匹配、同账号双身份、匿名、暂停/移除、会话失效、强制改密、身份服务故障、伪造参数和路径穿越拒绝；核对 HTTP 与 R 错误码和 no-store。
- 联调：经真实 Gateway/BFF/设备头访问正文和图片，身份撤销后再次获取拒绝，静态包及反代路径无旁路。已获取并显示的内容无法远程撤回，但应用在收到身份失效事件时须清除 DOM、缓存及 Blob URL。
- 人工：内容实操与截图一致；视觉与交互由前端 UX 验收记录共同签收。
