# Requirements

## 用户场景

组织管理员打开租户「部门」页，需要看到每个可见部门的直属在职人数，以便判断空部门与编制规模。不必进入详情或再查成员列表。

## 业务规则

1. `memberCount` 只统计本部门直属任职，且成员状态为 `ACTIVE`。不含下级部门，不去重跨部门任职以外的本行（同一成员在本部门只有一条任职）。
2. `SUSPENDED`、`REMOVED` 不计入。删除空部门的既有校验仍统计全部任职（含暂停），避免暂停成员所在部门被误删。
3. 只在租户管理部门树 `GET /v1/tenant/departments?purpose=MANAGED_DEPARTMENT` 填充 `memberCount`。无直属 ACTIVE 成员时填 `0`，不省略。接口一次返回树，不分页。唯一根部门与直属一级部门平级。
4. `GET /v1/tenant/departments/{id}`、通讯录部门列表、策略/选择器中的 `DepartmentRecord` 省略 `memberCount`。
5. `navigationOnly` 骨架仍不携带成员数量。
6. 列表对当前页部门 ID 一次批量 `GROUP BY`，禁止按部门 N+1 查询。
7. 计数不改变对象能力、范围求值或授权。

## 边界与非目标

- 不引入部门主管或部门状态。
- 不改 purpose 白名单或删除限制语义。管理部门列表改为树响应。
- 客户端不得用成员列表自行累加人数冒充服务端计数。

## 验收标准

- [ ] 列表中有 2 个 ACTIVE、1 个 SUSPENDED 直属成员的部门，`memberCount` 为 2
- [ ] 仅有下级部门成员、本部门无 ACTIVE 任职时，`memberCount` 为 0
- [ ] 详情 JSON 不含 `memberCount`
- [ ] 通讯录/策略构造的 `DepartmentRecord` 不含 `memberCount`
- [ ] 可见部门只发一次计数 SQL
- [ ] 根部门与直属一级部门在树响应中平级
- [ ] 契约 schemas 含可选 `memberCount`，且不在 required 中
