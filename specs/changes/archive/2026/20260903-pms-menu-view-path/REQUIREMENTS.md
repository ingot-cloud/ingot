# Requirements

## 用户场景

管理台从当前运行 App 已扫描的页面或布局中选择节点，提交 `view_path` 注册键与独立的浏览器 `path`。PMS 原样落库，查询菜单树时原样返回该键，不再根据 `path` 拼接 `@/pages{path}/IndexPage.vue`。

运维对已有环境执行迁移：页面与目录的旧文件路径改为注册键，并删除 `custom_view_path` 列。

## 业务规则

- `view_path` 是前端页面/布局注册键，与菜单 `path` 独立；创建与更新时原样保存，后端不解析、不按 `path` 生成。
- 创建 Directory / Menu 且 `linkType=Default` 时 `viewPath` 必填。
- 创建 Button 时 `viewPath` 可选；按钮注册键前端路由不使用。
- 更新时仅当请求显式传入 `viewPath` 才覆盖；修改 `path` 不得重算 `viewPath`。
- 改为 IFrame / External 时不再清空或改写 `viewPath`；由前端提交对应布局键。
- 删除 `customViewPath`：DTO、VO、实体与数据库列一并移除。过渡期前端仍传该 JSON 字段时由框架忽略。
- 存量 `menu_type=1` 的 `@/pages/...` 与 `menu_type=0` 的 `@/layouts/...` 按已知映射改为注册键。按钮行不改 `view_path`。
- 不修改菜单 `path`（含在线用户 `/platform/security/onlinetoken`）。

## 边界与非目标

- 后端不校验注册键格式，也不维护 prefix 公式。
- 不改权限生命周期、权限码、菜单树过滤与排序。
- 注释与 Spec 不引用临时前端文档，不抄编码公式。
- 未命中 CASE 映射的旧值保持原样；回滚依赖反向 CASE 与备份。

## 验收标准

- [x] 创建 Directory/Menu（默认链接）且未传 `viewPath` 失败
- [x] 创建时传入注册键与任意 `path`，库中 `view_path` 仍为该注册键，不是 `@/pages...`
- [x] 仅更新 `path` 时 `view_path` 不变
- [x] 菜单树响应不再包含 `customViewPath`
- [x] 迁移后不存在 `menu_type='1'` 且 `view_path LIKE '@/pages/%'` 的有效行
- [x] `021` 与 `rollback_021` 可在测试库执行；全新库仅靠 `ingot_core.sql` 无 `custom_view_path` 列
- [x] 现有 `BizMenuUtilsTest` 通过
