# 应用中心化授权改造任务

## 实施顺序

- [ ] P0：完成[基线与迁移护栏](./phases/00-baseline.md)
- [ ] P1：完成[兼容性数据库扩展](./phases/01-schema.md)
- [ ] P2：完成[应用中心化资源管理](./phases/02-application-resource.md)
- [ ] P3：完成[权限树与鉴权引擎](./phases/03-permission-engine.md)
- [ ] P4：完成[租户应用与角色授权](./phases/04-tenant-role.md)
- [ ] P5：完成[菜单交付与正式切换](./phases/05-menu-cutover.md)
- [ ] P6：完成[旧模型清理](./phases/06-cleanup.md)

每个阶段必须满足其验收标准和退出条件后，才能开始下一阶段。

## 最终验收

- [ ] 所有阶段验收完成
- [ ] 自动化测试和迁移验证通过
- [ ] 实际数据模型、接口和鉴权行为与 DESIGN 一致
- [ ] `specs/current/` 已根据最终实现建立或更新能力基线
- [ ] 完成信息已记录，change 已移入 archive

