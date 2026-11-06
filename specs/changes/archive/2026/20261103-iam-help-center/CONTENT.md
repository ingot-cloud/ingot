# 帮助内容、配图与功能覆盖规范

> 权威来源：`ingot/specs/changes/active/20261103-iam-help-center/CONTENT.md`；2026-11-03 首版。前端同名文件是同版副本。本文件规定待实现内容，不代表文章或截图已完成。

## 1. 内容目录与维护

实现阶段在后端仓库维护：

```text
docs/help/zh-CN/
├── release.yml                 # schemaVersion、productVersion
├── common/                     # 只包含两端均适用的说明
│   ├── catalog.yml
│   ├── articles/*.md
│   └── assets/*
├── tenant/
│   ├── catalog.yml
│   ├── articles/*.md
│   └── assets/*
├── platform/
│   ├── catalog.yml
│   ├── articles/*.md
│   └── assets/*
└── coverage.yml                # 功能、文章、配图及实操证据映射
```

每次功能变更一起修改对应文章、截图、关键词和 coverage；研发提交 Git 评审。禁止从 specs/current 或 active 直接整篇复制对外文档；内部规格只作为核实线索。无需运营在线编辑或租户定制。

## 2. 元数据与正文契约

文章文件名等于稳定 articleId；分类、文章、资产、章节ID采用 API 的 kebab-case 约束。文章ID在所有源目录中全局唯一，避免 common 合入后冲突；已发布ID不随标题修改，删除/更名需要同批更新全部引用，旧ID统一404，本期不建重定向服务。

| front matter | 必需 | 规则 |
| --- | --- | --- |
| id / categoryId / title / summary | 是 | summary 纯文本且不超过160码点；title 非空 |
| audience | 是 | COMMON / TENANT / PLATFORM，必须与目录一致；运行API只返回实际域 |
| audienceDescription | 是 | 用户语言说明谁能操作，不硬编码授予实际权限 |
| order | 是 | 非负整数，同序按id排序 |
| keywords | 是 | 非空字符串数组，含真实功能名称与常见同义表达 |
| updatedAt | 是 | 带时区的 ISO-8601，构建规范成 UTC Z；无时区拒绝 |
| relatedArticleIds | 是 | 数组，可空，同域最多6篇且不引用自己 |
| relatedViewPaths | 是 | 数组，可空，只允许本域发布清单内正式 viewPath |
| images | 是 | 数组，可空；各项含 id、file、thumbnailId、thumbnailFile、alt、caption、sourceFeatureKey、capturedProductVersion；路径必须位于所属内容目录资产根内 |

分类源每项提供 id/title/description/icon/order/children，首页提供 quickStartArticleIds 与 faqArticleIds；空分类不发布。common 分类树分别合入两域，分类ID也全局唯一。正式 viewPath 与主文章映射从 coverage 中生成，不维护第二份独立映射。

正文按以下结构编写：用途、适用对象/前置条件、操作入口、按顺序的步骤、成功结果、失败与常见问题、相关说明。正文不再写 h1；h2～h4 必须有显式稳定 `{#heading-id}`。图片放在相关步骤后，图注说明用户应观察什么，不堆在文章结尾。

只使用受限 CommonMark/GFM：段落、h2～h4、有序/无序列表、强调、表格、代码段、引用及三类提示块（说明、注意、风险）。提示块由受控扩展渲染。禁止原始 HTML、脚本、iframe、表单、动态 Vue/MDX、内联事件、内联 style、远程图片、data URL。帮助链接/图片语法见 [API.md](./API.md)。

概念图与截图不嵌入真实账号、手机号、邮箱、租户ID、密钥、Token、用户业务数据或内部网络信息。演示数据优先在截图前准备；密钥类字段展示为演示占位，不能拍摄真实值后仅用可移除覆盖层遮挡。

## 3. 首批文档清单

下表是必须核实并覆盖的功能范围。实施时根据正式菜单清单、当前规格及实际页面补全操作级 featureKey；现有源码中未发布的历史页不能仅因文件存在就写成可用功能。

| 域/分类 | 计划文章ID与内容 | 必须配图的内容 |
| --- | --- | --- |
| COMMON / 快速入门 | common-navigation：顶栏、应用切换、功能搜索；common-account-access：登录、退出及身份失效；common-password：修改密码与强制改密；common-personal-settings：个人及界面设置 | 仅使用两端实际一致且不含端专属菜单的局部截图；不同界面拆为专属文章 |
| TENANT / 快速入门 | tenant-getting-started：登录组织后的入门路径；tenant-workbench：已发布工作台和快捷入口 | 入门步骤、工作台实际可用状态 |
| TENANT / 组织管理 | tenant-members：添加、编辑、暂停、恢复、移出、重新加入；tenant-member-departments：批量变更部门 | 添加成员、重新加入提示、两层部门变更确认 |
| TENANT / 组织管理 | tenant-departments：树、直属人数、创建、编辑、删除；tenant-department-leaders：主负责人及兼管关系 | 部门树、编辑负责人、受限/不可用状态说明 |
| TENANT / 组织管理 | tenant-groups：组维护及有效成员；tenant-organization-settings：组织信息与实际发布的拥有者相关操作 | 组选择、成员规则、组织设置关键步骤；拥有者流程仅按已交付事实编写 |
| TENANT / 权限与应用 | tenant-roles：角色类型、版本与发布；tenant-assignments：直接授权、对象范围、有效期；tenant-delegations：委派、接收与撤销；tenant-authorization-diagnostics：实际开放的诊断 | 权限选择、角色/版本关系示意、范围选择、授权/委派步骤 |
| TENANT / 权限与应用 | tenant-applications：应用查看、可见性与访问问题 | 应用列表及“看不到应用”的判断流程 |
| TENANT / 安全与合规 | tenant-member-policies：正式成员访问策略的可见范围/字段规则；tenant-authorization-audit：筛选与可读事件 | 实际已发布策略Tab、字段规则示例、审计详情 |
| TENANT / 数据工具 | tenant-member-workbook：导入创建/更新、模板、结果与错误报告、初始密码；tenant-department-workbook：部门导入与更新；tenant-downloads：导出任务、状态、下载与过期 | 两种导入页、范围选择、结果、我的下载；初始密码图使用虚构数据 |
| PLATFORM / 快速入门 | platform-getting-started：平台职责、导航与基础配置顺序 | 平台导航与入门流程 |
| PLATFORM / 组织与租户 | platform-tenants：创建、启停、详情及实际开放治理操作；platform-entitlements：租户应用开通与适用人群 | 租户详情、开通配置、范围与状态 |
| PLATFORM / 应用与配置 | platform-applications：应用、资源、操作及菜单关联；platform-plans：套餐及关联；platform-shared-roles：共享角色与版本 | 应用配置链路、菜单/操作关系示意、套餐、共享角色版本 |
| PLATFORM / 平台管理 | platform-accounts：全局账号及凭证管理；platform-personnel：平台成员与组；platform-roles：平台角色；platform-assignments：授权及委派 | 账号与成员区别示意、人员创建、角色与授权流程 |
| PLATFORM / 系统与审计 | platform-dictionaries：字典项、启停及排序；platform-authorization-audit：平台审计 | 字典维护与审计筛选/详情 |
| PLATFORM / 安全中心 | platform-credential-policies：凭证策略；platform-account-protection：账号保护；platform-access-protection：访问防护与登录失败策略；platform-sessions：在线会话、撤销及并发策略 | 每个页面及Tab的关键配置；策略效果和失败提示，不能展示真实安全配置 |
| PLATFORM / 开发者平台 | platform-clients：客户端创建与密钥操作；platform-social：社交配置；platform-id-allocation：业务ID；platform-qrcode：二维码 | 实际正式菜单下的操作入口/表单；使用虚构凭据与地址 |

这份清单可拆成更细教程，不能合并后丢失操作说明。尚未实装/发布的事项必须在 coverage 标记 NOT_SHIPPED 并提供依据；已发布功能不能以“后续补文档”退出首期验收。通讯录浏览移除、字段策略与工作台等并行变更按目标发布清单核实，禁止写失效旧入口。

## 4. 功能覆盖账本

coverage.yml 每项包括：`featureKey`、`domain`、`applicationCode`、`viewPath`（非菜单流程可空）、`operation`、`articleIds`、`primaryArticleId`（页面入口时必填）、`requiresImages`、`imageIds`、`status`、`evidence`、`reviewedBy`、`reviewedAt`、`reason`（NOT_SHIPPED时必填）。

- featureKey 是稳定的用户任务标识，如 tenant.member.import-create；不是按文件数量计算覆盖率。
- status 只允许 DOCUMENTED / NOT_SHIPPED。编写中不得伪填 DOCUMENTED；未完成条目放开发中的账本工作区，发布校验拒绝无证据或缺少目标发布功能的账本。
- DOCUMENTED 的 articleIds 必须存在且同域；requiresImages=true 时 imageIds 非空、图片在文章中被实际引用，evidence 记录目标版本实操及截图核对结果。
- 正式页面源以 IAM 的目录/菜单生成源和前端正式路由注册共同核实；业务页内动作、隐藏导入导出页、修改密码、切换组织、下载等非菜单流程人工扩展账本。
- 构建核对目标发布功能清单与账本，所有已交付功能有 DOCUMENTED 项。以有文档的已交付功能数/全部已交付功能数计算，必须100%。NOT_SHIPPED不算交付，也不能自动把没有文档的功能变成未交付。
- 证据存于内部验收记录，不随公共帮助API返回；人工必须核实文义与操作，结构校验不能证明截图真实或步骤正确。

2026-11-03用户确认真实操作截图及账号实操后补：开发预览工作区中的DOCUMENTED仅表示正文及映射已经编写，不表示通过本节正式发布验收。另记 `captureStatus`：NOT_REQUIRED（不需配图）、PENDING（待采集）、CONCEPT_ONLY（仅有概念图）、VERIFIED（操作配图及目标版本已经人工核实）。显式preview构建可以暂缓必需图和人工证据的完整性检查，仍验证已有文章、资产及账本引用的同域闭包；正常构建必须满足全部规则，需图条目还必须为VERIFIED。

## 5. 配图制作和阅读

入口不明显、多个步骤/状态、复杂表单、授权关系等说明必须配图。简单定义和纯文字提示可不强行配图。

1. 在目标版本测试环境准备演示数据，采集本系统实际界面；截图记录来源功能及 capturedProductVersion，页面变化同步更新。
2. 截取足够的导航/弹窗上下文，保留用户定位所需信息；不能只给脱离场景的小按钮。注释用少量箭头、框选、①②③及对应图注，不改变界面文字或状态来制造流程。
3. 原图清晰可读，不拉伸；普通操作优先单步单图，长图按步骤拆分。缩略图从原图生成，原图与缩略图形成同一引用组。
4. 业务教程用截图，角色/版本/委派等关系可补原创示意图。概念图需导出受支持的 PNG/WebP，原始设计文件仅留研发源，不开放资源接口读取。
5. 所有图片有中文 alt 与 caption，先保留比例占位再懒加载；失败显示图注、重试及明确状态。点击进入预览，支持适应窗口、原始比例、50%～400%缩放，同篇多图前后切换和 Esc/关闭按钮。
6. 只有点击预览才取原图，不预加载所有全文或所有原图；关闭后恢复焦点和文章滚动位置。身份/版本变化立即关闭并销毁 Blob URL。

资源限制与接口保护见 [API.md](./API.md)。tenant 不能引用 platform 图片，platform 不能引用 tenant；common 图片不得携带任一专属域信息。跨域原图、缩略图、图注及引用ID均不得进入另一端快照。

## 6. 发布门禁

自动检查：元数据/域/ID/时间、正文结构和限额、稳定章节、图像文件签名与尺寸、原图/缩略图关系、完整引用闭包、死链及跨域链接、目录限额、正式viewPath、功能覆盖、缺少必需配图、重复/空文章、TODO/占位正文。PNG/JPEG/WebP不得保留不必要的EXIF等元数据。

人工检查：演示数据及无敏感内容、飞书式图文排版、截图与目标产品版本一致、角色/权限/限制说明正确、关键路径逐步可执行、错误/受限状态准确、所有已发布功能均已纳入账本。飞书仅作布局与交互参考，不复制其正文、品牌或产品截图到本系统教程。
