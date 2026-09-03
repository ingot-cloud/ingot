# Design

## 方案摘要

PMS 不再区分「默认视图 / 自定义视图」。`view_path` 只承载前端提交的页面或布局注册键。删除 `customViewPath` 全链路，去掉按 `path` 拼接源码路径的逻辑。存量数据用显式 CASE 映射升级，再 DROP 列。

## 数据模型与接口

### 删除

- Java：`PlatformMenu.customViewPath`、`AppMenuCreateDTO.customViewPath`、`AppMenuUpdateDTO.customViewPath`、`MenuTreeNodeVO.customViewPath`
- SQL：`platform_menu.custom_view_path`

### 语义

| 字段 | 含义 |
|---|---|
| `path` | 浏览器 URL，权限码仍由 path 派生 |
| `view_path` | 前端页面/布局注册键，与 path 独立，原样落库 |

`view_path` 列注释改为「页面注册键」。HTTP 路径不变：`POST/PUT /v1/platform/config/apps/{appId}/menus`。Spring 默认忽略未知 JSON，过渡期仍传 `customViewPath` 不影响。

### 创建 / 更新

- 删除 `BizMenuUtils.setViewPathAccordingToPath`
- `PlatformMenuServiceImpl.create`：Default 链接的 Directory/Menu 校验 `viewPath` 非空后 `save`
- `PlatformMenuServiceImpl.update`：不再按 path 重算；改为非 Default 链接时只自动生成 `path`，不改 `viewPath`
- 权限托管仍走 `ApplicationResourceServiceImpl`，不改

## 数据流与失败处理

```text
前端选择页面/布局
  → 提交 view_path + path
  → create/update 原样写入
  → 菜单树原样返回 viewPath
```

创建 Default 的 Directory/Menu 且 `viewPath` 为空时返回 `PlatformMenuServiceImpl.ViewPathNotNull`。更新未传 `viewPath` 时保持原值（MyBatis-Plus 忽略 null）。

## 迁移与回滚

脚本：`databases/migrations/021_menu_view_path_encoding.sql`、`rollback_021_menu_view_path_encoding.sql`。

1. 按旧文件路径 CASE 更新页面与目录 `view_path`（含种子中的 `onlinetoken` → `security.sessions`）
2. 按钮不改
3. `DROP COLUMN custom_view_path`
4. 同步 `databases/ingot_core.sql` 建表与种子

回滚：加回 `custom_view_path`（默认 0）+ 反向 CASE。未命中映射的行无法还原，依赖备份。

## 测试策略

- 现有 `BizMenuUtilsTest`（权限码与过滤，不覆盖已删方法）
- 对照 REQUIREMENTS 验收：创建必填、原样落库、更新 path 不改 view_path、树无 customViewPath、迁移后无 `@/pages/%` 页面行
