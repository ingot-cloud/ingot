# 接口：内置帮助中心（契约版本 1）

## 来源与通用约定

- 权威来源：`ingot/specs/changes/active/20261103-iam-help-center/API.md`；2026-11-03 首版。
- 服务：`ingot-iam-provider`；服务端前缀 `/v1/tenant/help`、`/v1/platform/help`。下文 `{entry}` 仅表示两套固定控制器，不是客户端可选择的授权域。
- 浏览器在当前 origin 请求 `/api/iam/v1/tenant/help` 或 `/api/iam/v1/platform/help`，经既有 Gateway/BFF；不得拼另一端 origin。
- 鉴权：当前有效已登录成员、固定管理域、现有强制改密限制；无新增阅读权限码。业务功能跳转仍遵循原有操作、菜单、应用开通及数据范围。
- 所有响应含 `Cache-Control: private, no-store`、`X-Content-Type-Options: nosniff`，包括错误响应；不提供公开下载重定向。
- JSON 包装 `R<T>`：`code` / `message` / `data`。本项目实际成功码为 **`S0200`**，不是字符串 `0`。错误使用 HTTP 非 2xx 与 R 错误码，data 不携带敏感细节。
- 首期语言固定简体中文，响应 `schemaVersion=1`，不提供 locale/domain/tenantId/accountId/memberId/appId/audience 筛选。
- 参数先经当前身份校验再读取内容；未声明 query 参数（包括伪造域参数）返回 400。所有 ID 为1～96字符，格式 `^[a-z0-9]+(?:-[a-z0-9]+)*$`；无斜杠或点段。
- `contentVersion` 为该域规范内容集的64位小写十六进制 SHA-256。产品版本 `productVersion` 是部署发布标识，不推导成权限。所有 `updatedAt` 是带 UTC Z 的 ISO-8601 时间点。

## 接口清单

| 方法 | 服务路径 | 响应 data | 说明 |
| --- | --- | --- | --- |
| GET | `/v1/{entry}/help/catalog` | HelpCatalog | 当前域分类树、文章摘要和首页编排；无参数 |
| GET | `/v1/{entry}/help/search` | HelpSearchPage | 当前域、指定内容版本中检索 |
| GET | `/v1/{entry}/help/articles/{articleId}` | HelpArticle | 当前域文章与阅读元数据 |
| GET | `/v1/{entry}/help/assets/{assetId}` | HelpAsset | 当前域已发布文章引用的原图或缩略图，JSON Base64 |

每个端点必须由服务器明确绑定 TENANT 或 PLATFORM，不能将 URL 文本原样转换为可信域。

## 1. GET catalog

无 query。返回完整的**目录树和摘要**，不包含全文、搜索索引、图片字节、另一端分类或正文源路径。树是有界结构：最多200分类、1000文章、5层分类；超限在构建时失败，不截断目录。

### HelpCatalog

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| schemaVersion | integer | 固定1 |
| domain | AuthorizationDomain | 固定为当前端的 TENANT / PLATFORM |
| contentVersion | string | 当前域内容版本 |
| productVersion | string | 功能与文档对应的产品发布版本 |
| categories | HelpCategory[] | 有序分类树；空分类不发布 |
| quickStartArticleIds | string[] | 同域快速入门，1～3篇，无重复 |
| faqArticleIds | string[] | 同域常见问题，最多6篇，无重复 |
| recentArticleIds | string[] | 同域按更新时间倒序，最多6篇，非热度排行 |
| pageBindings | HelpPageBinding[] | 已发布业务页与主说明文章的映射，域内 viewPath 唯一 |

### HelpCategory / HelpArticleSummary / HelpPageBinding

| 类型 | 字段 | 类型与语义 |
| --- | --- | --- |
| HelpCategory | id / title / description / icon | string；稳定分类ID、名称、简述、已离线打包的 Iconify 图标键 |
| HelpCategory | children | HelpCategory[]；有序子节点 |
| HelpCategory | articles | HelpArticleSummary[]；该节点直属文章，按 order 再 id 排序 |
| HelpArticleSummary | id / title / summary / categoryId | string；summary 纯文本，最多160个 Unicode 码点 |
| HelpArticleSummary | updatedAt / readMinutes | UTC ISO string / positive integer |
| HelpPageBinding | viewPath / articleId | string；canonical viewPath 与当前域主说明ID；不是任意 URL |

readMinutes 为构建期去除标记后的非空白可见字符数除以400并向上取整，最少1分钟。categories 中每篇文章只出现一次；所有首页ID和映射必须指向树中实际文章。

## 2. GET search

| query | 类型 | 必填 | 约束 |
| --- | --- | --- | --- |
| version | string | 是 | 当前 catalog.contentVersion；不匹配409 |
| q | string | 是 | 归一化后1～100个 Unicode 码点；全空白400；无正则表达式或通配语法 |
| categoryId | string | 否 | 当前域分类ID，包含其子树；本域不存在或其他域ID均404 |
| page | integer | 否 | 默认1，最小1；越界页 items=[]，total 保持真实总数 |
| pageSize | integer | 否 | 默认20，范围1～50；超出400，不静默改写 |

### HelpSearchPage

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| domain / contentVersion | AuthorizationDomain / string | 与请求上下文和 version 相同 |
| items | HelpSearchHit[] | 当前页有序命中 |
| total / page / pageSize | integer | 当前域、分类与关键词条件下的真实总数和分页；total 为 JSON 数字 |

HelpSearchHit 包含 `article: HelpArticleSummary`、`titleSegments: HelpTextSegment[]`、`snippetSegments: HelpTextSegment[]`、`headingId?: string`。HelpTextSegment 为 `{ text: string, matched: boolean }`；顺序拼接为标题/摘要，字段本身没有 HTML。headingId 仅能是该文已发布章节。前端必须用文本节点渲染，不使用 v-html 高亮。

检索归一化、匹配、权重和稳定排序见 [DESIGN.md](./DESIGN.md) 第5节。合法查询无结果返回200及 items=[]/total=0；无权内容不得影响总数或摘要。

## 3. GET articles/{articleId}

唯一 query：`version`（必填，格式同上）。articleId 位于当前域已发布集合中，否则404。原始 front matter、内部校验字段及文件路径不返回。

### HelpArticle

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| domain / contentVersion | AuthorizationDomain / string | 当前域及请求版本 |
| id / title / summary / categoryId | string | 与目录摘要相同 |
| audienceDescription | string | 例如“具有成员创建权限的组织管理员”；说明文字不构成授权 |
| updatedAt / readMinutes | UTC ISO string / integer | 与目录一致 |
| markdown | string | 已去 front matter 的受限 Markdown，最多256 KiB UTF-8 |
| toc | HelpHeading[] | 正文 h2～h4 顺序列表，字段 id/title/level；id 稳定且唯一 |
| assets | HelpArticleAsset[] | 正文直接引用的图片描述，不含字节 |
| relatedArticles | HelpArticleSummary[] | 同域相关文章，最多6篇，不含当前篇 |
| previousArticle / nextArticle | HelpArticleSummary? | 当前分类直属文章排序的前后篇，边界时省略 |
| relatedViewPaths | string[] | 同域正式 canonical viewPath；由前端与已授权菜单匹配后生成“前往功能” |

HelpArticleAsset 字段：`id`、`thumbnailAssetId`、`alt`、`caption`（string），`width`、`height`（原图 positive integer）。thumbnailAssetId 为同组缩略图ID；thumbnail 与 original 均为当前文章可读资产。正文图片用 `![替代文字](help-asset:asset-id)` 引用原图ID，预览初次加载 thumbnail，放大时再加载原图。

帮助文章链接语法为 `[文字](help:article-id#heading-id)`，章节可省略；页内章节使用 `#heading-id`。章节标题为 `## 标题 {#heading-id}`，前端去除标记后用 toc.id 渲染锚点。所有引用在构建时校验同域及目标章节。外部 HTTPS 参考链接必须显式审查，不能指向另一端内部帮助 URL，外部图片一律禁止。

## 4. GET assets/{assetId}

唯一 query：`version`（必填）。assetId 只能从当前域快照中的发布文章及其缩略图引用闭包解析；无引用的文件不可读。先校验身份、域、版本再读字节；不可用与404不回退公开地址。

### HelpAsset

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| domain / contentVersion | AuthorizationDomain / string | 当前域及版本 |
| id | string | 本次请求资产ID |
| contentType | string | image/png、image/jpeg、image/webp 之一，按实际文件签名验证 |
| byteLength / width / height | integer | 解码后的字节数及像素尺寸 |
| contentBase64 | string | 无 data: 前缀的规范 Base64；不含 URL |

原图不超过2 MiB、最大边4096px；缩略图不超过512 KiB、宽度不超过1200px，缩小保持比例且不放大原图。SVG 概念图须在构建前导出 PNG/WebP，运行接口不传 SVG/HTML/PDF。首期无其他附件类型。

前端通过统一 request 读取 JSON，验证类型/长度并转换 Blob URL；每次文章卸载、身份变化或版本变化时撤销 URL。禁止 img.src 直接调用受保护接口、把 Base64 存 localStorage、持久保存 Blob URL或使用公共外链。

## 错误与恢复

| HTTP | R.code | 语义与前端行为 |
| --- | --- | --- |
| 400 | InvalidArgument | 参数不合法、额外参数、空查询；不重试相同参数 |
| 401 | IdentityInvalid 或既有 BFF 401 | 当前身份无效/过期，进入统一会话清理；保留安全的当前端相对深链 |
| 403 | ActionDenied | 当前身份与端点固定域不匹配，不切换域重试 |
| 403 | PasswordChangeRequired | 继续既有强制改密流程，不加载帮助正文或资产 |
| 404 | ObjectNotFound | 本域不存在或不可见文章、分类、资产；相同默认 message，不说明资源实际属于哪个域 |
| 409 | HelpContentVersionChanged | 目录版本已变更；清帮助缓存及图片，重新取目录后自动重试至多一次，仍失败显示刷新提示 |
| 503 | AuthorizationUnavailable | 有效身份核对依赖不可用，沿用统一错误，不能当作退出或合法空 |
| 503 | HelpContentUnavailable | 内容包缺失、破损或未就绪；显示帮助暂不可用及重试/返回，不请求另一端 |

新增后两种帮助专用业务码中的 `HelpContentVersionChanged`、`HelpContentUnavailable` 加入 IamReasonCode；其他错误码复用。异常响应只包含 code/message，data 省略或 null。后端升级前框架404应识别为能力尚不可用，不能当成某篇文档被删除。

## 隔离验证矩阵

| 用例 | 必须结果 |
| --- | --- |
| 租户会话请求 platform 端点，反向同理 | 403，无正文、标题或资产 |
| 租户端点请求 platform 专属ID，反向同理 | 404，与随机不存在ID同错误形态 |
| 同账号在两端分别登录 | 各自只得到自身域＋common；ID或账号相同不扩权 |
| 搜索只在另一域出现的唯一词 | 200、total=0，无联想、摘要或相关链接 |
| 请求其他租户/域参数或路径穿越资产 | 400/被路由拒绝，无文件系统路径回显 |
| 正文已加载后撤销身份，再请求缩略图/原图 | 401或当前身份限制的403，无图片字节 |
| 两个版本节点交替响应 | 409恢复上限生效，禁止混版或无限自动重试 |
