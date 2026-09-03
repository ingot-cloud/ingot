# Tasks

## 实施任务

- [x] T1：删除 `customViewPath` 并改为原样落库 `viewPath`
  - 依赖：无
  - 验收：DTO/VO/实体无该字段；create/update 不按 path 推导；Default 的 Directory/Menu 创建时 `viewPath` 必填
- [x] T2：删除 `BizMenuUtils.setViewPathAccordingToPath`，更新字段注释与 i18n
  - 依赖：T1
  - 验收：无按 `@/pages` 拼接的实现；注释不引用临时前端文档
- [x] T3：新增 `021` 迁移与 rollback，同步 `ingot_core.sql`
  - 依赖：无
  - 验收：页面/目录映射完整（含 onlinetoken）；DROP `custom_view_path`；种子无该列

## 验证任务

- [x] V1：`BizMenuUtilsTest` 通过，对照 REQUIREMENTS 验收项核对

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新（验收后）
- [ ] Change 已记录完成信息并归档
