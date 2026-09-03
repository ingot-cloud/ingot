# 菜单 view_path 编码化与 customViewPath 下线

> 状态：completed

## 元数据

- Change ID：`20260903-pms-menu-view-path`
- 领域：`pms`
- 负责人：jy
- 创建日期：2026-09-03
- 目标发布日期：TBD

## 目标

前端已改为从运行中 App 的页面/布局选择器提交页面注册键，不再依赖后端按 `path` 拼接源码路径。去掉已无用的 `customViewPath`（含物理列），`view_path` 原样落库，并迁移存量 `@/pages` / `@/layouts` 值。

## 范围

### 包含

- 删除 `customViewPath` 接口字段、实体字段与 `platform_menu.custom_view_path` 列
- 创建/更新菜单原样保存 `viewPath`，不再按 `path` 推导
- 存量页面与目录 `view_path` 迁移为注册键
- 同步 `ingot_core.sql` 初始化种子

### 不包含

- 后端实现或校验注册键编码公式
- 修改菜单 `path`、权限码或菜单树过滤
- 引用临时前端约定文档

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-09-03
- 关联提交或 PR：`b2be5380`
- 更新的 current capability：`specs/current/pms/application-authorization`
- 与原设计的差异：无
- 取消原因：
