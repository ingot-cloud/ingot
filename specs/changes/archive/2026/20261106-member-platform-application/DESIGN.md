# 目录与授权设计

| 名称 | URL | 视图键 | 图标 | 菜单读取码后缀 |
| --- | --- | --- | --- | --- |
| App 用户管理 | `/member` | `layout.main` | `ep:iphone` | 三页读取码 ANY |
| 用户管理 | `/member/users` | `member.user` | `ep:user` | `user:query` |
| 角色管理 | `/member/roles` | `member.role` | `ep:avatar` | `role:query` |
| 权限管理 | `/member/permissions` | `member.permission` | `ep:key` | `permission:query` |

根 routeName 为 `member.root`，页面 routeName 与视图键相同。目录作用域为 PLATFORM，操作码前缀 `platform:member:`。Member 与 IAM 显式注册 `user`、`role`、`permission` 三个 ResourceKey，保留控制器的 20 个精确码。三资源支持 ALL；无对象候选和独立字段策略。Member 平台管理接口使用 AuthorizationAccess 检查可信平台身份、操作及 ALL 范围，读取遵循 SDK 热窗口，写入 fresh。其余 Member 接口沿用各自的安全链。

用户角色与角色权限沿用各自独立 GET 和全量替换 PUT，确认空集合时清除可维护的绑定。前端保存前验证候选与已绑定集合加载成功，内置角色保留只读语义，权限父子压缩保持既有行为。

平台会话没有租户 ID。新增绑定记录沿用 Member 历史默认租户存储值，以满足既有关系表的非空列。只补写实体字段，IAM 身份和网关请求继续按平台域契约解析。

冷启动生成器声明新增目录。036 增量仅写入该应用、资源、操作、菜单及关联，按自然键核对归属、同名菜单和路径；冲突导致事务回滚。现有角色版本与授权分配保留。通过会话变量预演回滚。回退时停写并按明确的应用编码禁用目录，保留权限引用，刷新 IAM 授权与路由缓存并重新 bootstrap。重新启用前校验应用归属和升级结果。发布在备份后显式执行迁移，同批更新 IAM、Member 和平台前端。
