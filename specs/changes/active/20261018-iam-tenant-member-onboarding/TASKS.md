# Tasks

## 实施任务

- [x] T1：新增 `TenantMemberCreateInput` 与 `TenantMemberCreated`
  - 依赖：无
  - 验收：commons 模型可被 Jackson 与校验注解消费
- [x] T2：实现 `TenantMemberOnboarding` 并改租户创建接口
  - 依赖：T1
  - 验收：按手机号匹配或新建账号后加入组织
- [x] T3：补测试并手工更新契约
  - 依赖：T2
  - 验收：五种场景测试通过；routes/schemas/openapi 与实现一致

## 验证任务

- [x] V1：运行成员相关 gradle 测试与契约测试

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 未提前改写
- [ ] Change 完成后归档
