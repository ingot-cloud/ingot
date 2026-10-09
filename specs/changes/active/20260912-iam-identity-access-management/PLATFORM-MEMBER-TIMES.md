# 平台成员详情只读时间（2026-10-08）

> 状态：validating（开发及定向自动化完成；真实页面/身份验收待执行）
> 批准：用户在前一轮分析中确认三项时间、只读展示和账号级登录口径，并明确要求修改。沿用既有 approved → implementing IAM change，增量决策完整。

## 需求与验收标准

平台人员详情“其他”按用户组、加入平台时间、账号最后登录时间、成员更新时间排列。全部纯文本只读，不进入编辑草稿或提交。登录时间提示“包含平台及组织身份登录”，无记录显示“暂无登录记录”；其他缺失时间显示“-”。按浏览器时区显示到秒，识别失败回退 Asia/Shanghai。

## 设计与兼容

- MemberRecord 新增可选 Instant joinedAt、lastLoginAt、updatedAt；仅平台详情及平台编辑成功响应填充，成员列表、组成员列表、租户响应仍省略。保留无时间的构造器以复用既有成员投影。
- joinedAt 取 iam_platform_member.created_at，表示首次创建成员资格，暂停/恢复不重置；updatedAt 取该成员的 updated_at，含资料/状态等变更，不等于最后活跃；lastLoginAt 取 iam_account.last_login_at，为全局账号最近成功登录，不区分平台/租户。
- 现有存储 LocalDateTime 明确为 UTC，读取转 Instant，API 输出 UTC Z。无新表、DDL、配置或登录事件修改。
- 详情先执行 iam-platform:member:read 及真实成员对象范围校验；编辑成功响应沿用 member:update 的目标范围校验。通过当前操作及成员范围后，以账号 ID 精确读取白名单 username/lastLoginAt；不返回账号实体、凭证或其他身份。不要求全局账号管理权限，不新增浏览器账号查询。三项时间是固定只读元数据，与已有 username/status 一样由当前成员 read/update 操作及目标范围控制，不进入可配置资料字段或可写字段白名单；资料隐藏/脱敏策略保持。
- 平台 PATCH/preview 的未知字段门禁继续拒绝时间字段，包括显式 null；列表字段、筛选、排序、角色、用户组、租户行为保持。
- 新字段可选、旧字段和 HTTP 路径不变；同步公共 Java 契约、OpenAPI、前端类型和权威来源副本。

## 任务

- [x] MT01 后端：详情白名单查询、UTC 时间投影、字段策略保留只读元数据。
- [x] MT02 前端：其他分组展示、共享格式化、空值文案、登录口径说明。
- [x] MT03 契约与定向验证：真实持久化来源/UTC、投影隐藏/脱敏与元数据、无记录、写入拒绝及详情对象范围；前端类型、相关组件回归、只读 lint、构建、边界与文档检查。
- [ ] MT04 人工验收：真实人员详情及其他 Tab、未登录人员、资料保存后更新时间、暂停/恢复保留加入时间、受限成员范围、两时区显示；实际环境验收后才更新 current 并归档。

## 回滚与发布

IAM 与前端一起发布。回滚两端版本即可；没有数据写入迁移。自动化通过不替代真实页面/身份验收，主 change 保持 implementing，不提前更新 current 或归档。

## 自动化证据（2026-10-08）

- 后端 provider 30 项定向测试通过：PlatformMemberContactsTest 10、FieldAccessEvaluatorTest 8、PlatformMemberFieldContextTest 3、PlatformMemberUpdateAccessTest 3、MemberQueryRepositoryTest 6。验证真实 MyBatis/H2 时间来源、UTC Z 输出、空记录/账号删除、范围拒绝、只读时间显式 null 写入拒绝、资料/角色事务及隐藏脱敏投影。
- Java IamAuthorizationContractTest 17 项通过，新增三项字段的 string/date-time、readOnly 与非必填断言；OpenAPI 生成及 --check 为 132 路径/200 操作；Python 契约 7 项通过。
- 前端 MemberFieldUi/MemberDrawers/memberFieldAccess 共 14 项通过；补充保存后更新时间刷新断言后 MemberFieldUi 6 项再次通过，覆盖上海/纽约、空时间文案、只读分组和保存不携带时间。admin-common/platform 两包类型检查、3个源文件只读 ESLint及平台管理台构建通过。
- 运行使用已有 Gradle 缓存、Node 22.17.0 与 pnpm 10.12.4。构建存在既有图标扫描、重复 UnoCSS、crypto 外置及动态导入提示，无新增构建错误。没有运行或替换真实业务服务，没有数据库迁移或提交。
- 两端 diff 检查、前端依赖边界及文档检查通过。
- MT04 真实页面、登录/暂停恢复与身份范围验收保留待办；本轮不更新 current，不归档主 change。
