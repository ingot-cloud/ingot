# IAM 多租户身份与访问管理重构

> 状态：implementing
> 2026-09-16 双入口 BFF 增量：implementing（规格已确认 Host→appId / Nacos 回跳 / 前端不传 URL）。B01–B05 已落地，B06 四站浏览器与全量 IAM 证据未做；主 change 既有进展与证据保留。

## 元数据

- Change ID：20260912-iam-identity-access-management
- 领域：IAM / PMS / 授权框架 / 后台交互契约
- 负责人：jy
- 创建日期：2026-09-12
- 目标发布日期：TBD（新系统冷启动、后端综合验收及前端联调通过后确定）
- 当前阶段：2026-09-19 已核对并在 TASKS 补标开发子项；T16 后端综合验收进行中。第一批与第二批人工 HTTP 已确认通过（含 A01b、A02、A04、A26）。未勾选 T16。下一阶段优先 A21 空库冷启动、A18.1 镜像，以及 A08 / 升级 / 多实例 / F 系列。
- 批准记录：2026-09-13，jy 明确要求开始实施本 change；据此由 review 经 approved 转入 implementing。实施遵循已确认的 REQUIREMENTS/DESIGN/API；T01 发现的契约缺口单独记录并确认，不视为授权 Agent 自行改变业务语义。
- 本轮范围确认：2026-09-15，jy 要求按全新系统建设、保留必要既有功能、清理被替代实现；限定权限角色及关联接入，复用成熟框架能力；平台登录不返回允许访问的租户。本轮仅更新 Spec，不实施代码或前端。
- 发布方式：全新系统初始化后启用，不依赖旧库升级、双读、双写或兼容接口；内部按 TASKS 推进。

## 目标

把 ingot-pms 重构并更名为 ingot-iam，建立平台、租户隔离的身份与访问管理体系。共享角色采用固定版本加租户差异，避免每个组织复制平台定义。按全新系统建设，复用现有认证与安全框架，提供前端 Agent 可独立读取的登录、页面与接口契约。

## 范围

包含账号与成员分离、组织部门、静态用户组、应用开通、操作权限、角色版本、差异、授权记录、受限委派、数据范围执行、通讯录可见性、成员字段策略、诊断、审计、必要服务更名与调用方适配、全新系统初始化。历史迁移工具退出本次必需交付范围，保留处置记录。

本仓库实施后端及文档。前端业务代码在独立仓库由后续 Agent 根据本 change 开发；本 change 不修改相邻前端仓库。后端完成不等于整个产品可以切流，正式启用以新系统初始化、后端与前端联调验收为门禁。

不包含动态条件组、多级委派、通用 ABAC 脚本、文档共享 ReBAC、平台临时进入租户业务域、会员服务合并、额外业务模块开发。关于“小业务可同 JVM 部署”的讨论不改变本次服务拆分或增加 ingot-server。

## 工件与阅读顺序

1. [需求](./REQUIREMENTS.md)：范围、业务规则及结果要求。
2. [设计](./DESIGN.md)：模型、授权计算、执行及边界。
3. [接口契约](./API.md)：管理接口、公共类型、预览与错误。
4. [前端交互](./FRONTEND.md)：导航、页面、操作流程及异常状态。
5. [历史迁移方案](./MIGRATION.md)：已退出本次交付范围，保留原方案与取消原因。
6. [验收矩阵](./ACCEPTANCE.md)：后端、冷启动、框架复用与后续前端联调验收。
7. [任务](./TASKS.md)：实施顺序、依赖和完成门禁。
8. [实施核对记录](./IMPLEMENTATION.md)：T01 发现、待确认决策与实施证据。
9. [人工认证清单](./MANUAL-VERIFICATION.md)：需在隔离环境由人执行的步骤、预期与证据。
10. [旧入口目标映射](./endpoint-mapping.json)：163 个入口的目标操作、域、执行约束或移除说明。
11. [阶段契约快照](./contracts/README.md)：历史结构验证，未覆盖本轮修订。
12. [整改与职责边界](./REMEDIATION.md)：评估发现、框架归属、修正目标及任务验收映射。
13. [独立测试数据](./TEST-DATA.md)：可重建环境、TD01–TD18 场景卡与测试数据 D01–D05；与 DESIGN D01（平台主体决策）不是同一编号。
14. [联调操作手册](./VERIFICATION-GUIDE.md)：功能测试导读、数据导入步骤与按页操作；执行证据不自动勾选验收。

前端 Agent 至少读取 REQUIREMENTS、DESIGN、API、FRONTEND、ACCEPTANCE、TEST-DATA、VERIFICATION-GUIDE；不依赖聊天记录推测行为。API 与实现导出的 OpenAPI 必须一致，实施若改变契约先回写并重新确认。

## 现状与相关变更

- 当前基线：[应用授权](../../../current/pms/application-authorization/SPEC.md)、[数据授权](../../../current/pms/data-authorization/SPEC.md)。
- [SQL 规模优化 draft](../20260912-mybatis-data-scope-predicate-scale/README.md) 以旧快照及规则模型为前提。本 change 不直接实施其闭包表或 UNION 改写，不修改其状态；后续须按新模型重新评估。
- 当前实现的平台默认层与租户追加层只有并集语义；本方案新增移除与范围替换，不能照搬旧合并算法。
- 静态审查发现权限与范围跨角色合并风险、PMS 业务范围接入缺失、全局账号/成员混用、初始化操作目录不完整；这些是重构验收输入，不代表已执行线上漏洞验证。
- 本次不等待实际旧库快照；以空库初始化及必要既有功能的完整链路验收。历史迁移方案不得作为恢复运行时旧表依赖的理由。

## 完成记录

- 完成日期：未完成
- 关联提交或 PR：未创建
- 更新的 current capability：验收后建立 IAM 基线并处理被替代 PMS 基线及交叉引用
- 与原设计的差异：2026-09-15 调整为全新系统交付，移出历史数据迁移门禁，明确框架复用和平台登录隔离；代码评估缺陷及待办见 REMEDIATION。文档更新不代表缺陷已修复。
- 取消原因：不适用

## 双入口 BFF 与前端全量对齐增量（2026-09-16）

根据已讨论计划生成 [BFF-LOGIN](./BFF-LOGIN.md)，同时修订 API/FRONTEND/DESIGN/REQUIREMENTS/ACCEPTANCE/TASKS。固定两域入口；一份 admin 源码两部署；两个登录应用；四子域、独立会话、事务与安全交接；单租户自动进入、多租户选择；包含完整 IAM 前端对齐。取消前缀返回白名单、父域共享会话及 App 跨引用方案。

规格已按确认意见补齐：回跳只由注入 appId 读 Nacos 注册表；Origin 仅一致性校验；浏览器不传重定向 URL；401 只进本站 `/auth/start`。B01–B05 已落地；B06 与 L01–L12 在有浏览器证据前不勾选。

## 2026-09-19 状态校准与测试数据交付约定

本轮按用户确认只更新两端change，保留implementing。多数后端核心研发已落地，任务父项同时包含验收，不能由未勾选推断为尚未开发；以 [TASKS开发完成标记](./TASKS.md) 与 [IMPLEMENTATION](./IMPLEMENTATION.md) 区分开发、历史自动化和真实HTTP证据。

新增并续补 [TEST-DATA](./TEST-DATA.md)：测试数据 D01–D05 后续在可重建的独立环境落地；TD01–TD18 为可执行场景卡，关联后端 A/F/L 与前端 U/P24–P26。DESIGN D01 仍是 2026-09-13 平台主体决策。现有人工种子不足以代表完整测试数据已交付。前端须适配本次范围内全部已实现管理能力，列表/封装不算完整页面；视觉与交互约束见 [FRONTEND](./FRONTEND.md) 第5节。

本轮未改变API/DTO/授权规则，未运行测试或准备数据，未更新current、未归档。缺口依赖和状态继续由原change维护，不另建重复变更。


## 2026-09-28 平台角色分配增量

已获用户明确实施批准；规格与契约见 [AUTHORIZATION-REFINEMENT](./AUTHORIZATION-REFINEMENT.md)。本轮先完成平台两端，主 change 保留 implementing；真实验收单列记录。

2026-09-28 增量开发与验证证据见 [AUTHORIZATION-REFINEMENT-STATUS](./AUTHORIZATION-REFINEMENT-STATUS.md)，PR07 真实环境验收保持未完成。


2026-09-29：用户已批准实施 [角色单选树增量](./ROLE-PICKER-REFINEMENT.md)，状态 approved→implementing。测试由用户执行，本轮不运行测试/构建/类型检查/lint，不更新 current 或提交。

2026-09-30：用户要求优化平台角色分配表格列宽与接收对象筛选。增量 API、交互、查询边界及 AL01–AL03 任务记录在本 active change；主状态保持 implementing，人工验收仍由用户执行。

2026-09-30：平台委派列表新增可选管理员显示名称筛选和名称回显，分页/计数由数据库执行；DL01 开发完成，DL02 真实 HTTP 与页面验收待完成。租户委派接口保持兼容。


2026-10-03 用户批准实施 [平台角色工作区与委派优化](./ROLE-WORKSPACE-DELEGATION-REFINEMENT.md)，含独立关联分页、期限模式、自我授权收紧与统一选择器/样式。

2026-10-03：平台角色工作区与委派优化开发及自动化完成，RW07 人工验收待执行；主 change 仍 implementing。

2026-10-03：用户要求修复登录记录时间解析异常，按既有 JSON 时间契约补充 JT01–JT03 增量；批准后实施，主 change 保留 implementing。修复框架基础时间模块被 IAM OSS 扩展错误抑制的问题，不改变登录回调字段和账号保护规则。

登录时间修复代码提交：`c5263023`；9 项定向回归通过，JT03 真实登录人工验收待完成。

2026-10-03：用户要求增加平台角色分配状态筛选，AS01–AS02 开发及定向自动化完成；AS03 人工验收待完成，主 change 保留 implementing。本轮未创建提交或更新 current。

2026-10-04：平台分配状态筛选性能修正 ASP01–ASP02 开发与定向回归完成。隔离复现确认复杂计算状态触发分页插件自动 COUNT 改写为主要瓶颈，改为按状态拆分谓词及同边界显式计数；耗时证据见 DESIGN。ASP03 真实接口人工复测待执行，主 change 保留 implementing，未更新 current 或创建提交。

2026-10-04：用户要求提交，状态筛选与性能修正代码已提交为 `400d0a46`；AS03/ASP03 人工验收仍待执行，提交不代表验收通过，未更新 current。

2026-10-04：用户要求两个平台授权主列表默认按 ID 倒序，SO01–SO02 开发及 7 项现有查询回归完成，API 与前端来源同步；SO03 人工验收待执行，主 change 保留 implementing，未新增索引、更新 current 或创建本轮提交。


## 2026-10-05 平台多角色分配与范围配置

已获用户批准，需求、接口、兼容和任务见 [ASSIGNMENT-MULTI-ROLE-REFINEMENT](./ASSIGNMENT-MULTI-ROLE-REFINEMENT.md)。新建支持多个角色（每个角色一个固定版本），范围沿用角色定义、对象参数独立，统一有效期；编辑仍固定版本。开发与人工验收分别记录，保留已有未提交改动。


2026-10-05：用户批准分配范围步骤调整，第二步选择角色与有效期，第三步独立范围配置/全部权限视图、全局进度及跨页遗漏定位；新建和编辑均可调整指定对象。详见 [分配范围配置增量](./ASSIGNMENT-MULTI-ROLE-REFINEMENT.md)，HTTP DTO 与后端范围校验契约保持。


## 2026-10-05 平台对象资源识别收紧

用户已明确批准实施，需求、兼容与任务见 [PLATFORM-OBJECT-RESOLUTION-REFINEMENT](./PLATFORM-OBJECT-RESOLUTION-REFINEMENT.md)。根据真实应用和资源关联识别对象候选，覆盖分配、委派、诊断、回显及提交校验；主 change 保持 implementing。


## 2026-10-05 通用资源扩展与分配升级

用户已批准实施 [资源扩展增量](./RESOURCE-EXTENSION-REFINEMENT.md)，包含跨服务授权、平台独立字段策略及显式分配版本升级。以增量契约为准；不提前更新 current。

本增量状态：validating，RE01–RE08 开发及自动化完成，RE09 人工验收待执行。主 change 保持 implementing，历史待办保留。部署与验收见 [RESOURCE-EXTENSION-VERIFICATION](./RESOURCE-EXTENSION-VERIFICATION.md)。


## 2026-10-05 IAM SQL 整理

用户已授权整理脚本，保持既有结构与授权语义；权威DDL合并、历史补丁归类及完整初始化生成见 [DATABASE-SCRIPT-REFINEMENT](./DATABASE-SCRIPT-REFINEMENT.md)。

SQL01–SQL05开发及自动化验证完成，增量validating；实际环境导入未执行。001–006为新建库来源，原007–012归入migrations；完整文件生成57张表及最新正式目录，不含开发快照数据。


## 2026-10-05 平台角色字段权限（用户已批准）

本轮替代平台独立字段策略，角色版本固化字段权限，按操作与来源范围正向合并。共享/租户保持原行为；实施与独立人工任务见 [ROLE-FIELD-AUTHORIZATION-REFINEMENT](./ROLE-FIELD-AUTHORIZATION-REFINEMENT.md)。


2026-10-05：角色字段增量 RF01–RF07 开发、自动化及来源同步完成，增量 validating；RF08 用户人工/部署/性能待执行，主 change 仍 implementing。按 [新版资源验收清单](RESOURCE-EXTENSION-VERIFICATION.md) 执行，不能再用旧独立成员/组字段策略与 HIDDEN 优先步骤。新模型需先部署013/全部IAM/SDK/前端，并明确完成旧策略迁移后启用。

2026-10-05：平台字段模型直接替换DC01–DC04开发/自动化完成（后端57+17+345，前端92+116，隔离数据库24+12），增量validating；人工RF08未执行，主change仍implementing。无需启用开关，已移除平台独立策略及012/013。


## 2026-10-06 目录默认布局与重复初始化（用户已批准）

正式种子 DIRECTORY 的 `view_path` 统一为 `layout.main`。完整 `ingot_iam.sql` 在创建前删除manifest内的目标表，仅删除阶段关闭外键检查，建表/种子阶段开启并在完成后恢复原会话设置。重复执行清空目标表，不含账号或业务授权，不是存量升级；原无DROP决定被替代。决策、回退和独立人工项见 [DATABASE-SCRIPT-REFINEMENT](./DATABASE-SCRIPT-REFINEMENT.md#2026-10-06-目录布局与重复初始化已批准)。


## 2026-10-06 初始化菜单补齐（用户已批准）

全局账号加入平台治理的“平台管理”；开发者平台恢复独立应用 `platform:develop`，包含生成二维码、客户端管理、社交管理、业务ID管理。页面注册键与现有接口权限码保持，应用/资源归属显式声明，平台新建治理版本覆盖本域全部正式应用，租户不继承。实施及人工项见 [DATABASE-SCRIPT-REFINEMENT](./DATABASE-SCRIPT-REFINEMENT.md#2026-10-06-全局账号与开发者平台初始化补齐已批准)。
