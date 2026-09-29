# 关联列表查询

列出某对象已经绑定、引用或加入的记录时，按关联关系查，不要把 ID 拼回父资源列表。

## 规则

1. **独立接口**：组成员、成员所属组、角色被授权对象等走 `GET /{target}/{id}/members` 这类路径，分页默认 20。
2. **SQL 跟关系走**：`JOIN` / `EXISTS` 关联表（如 `iam_platform_group_member`），按 target ID 过滤。不要 `WHERE id IN (?,?,…)` 去查父表再冒充关系列表。
3. **`ids` 不是列表**：父资源列表的 `ids` 只给表单少量回显（手里已有 2～3 个 ID 换名称）。组有 100 人就传 100 个 ID 是错误用法。
4. **前端对齐**：选择器右侧「已绑定」与主列表一样翻页或加载更多，禁止循环翻页或 `pageSize=200` 拼全集。

## 对照

```
# ✅ 当前组的直接成员
GET /v1/platform/groups/{id}/members?page=1&pageSize=20

# ❌ 把组成员 ID 塞回人员列表
GET /v1/platform/members?page=1&pageSize=20&ids=1,2,…,100
```

候选池（还能加谁）仍用父资源分页，例如 `GET /v1/platform/members`；已绑定集合必须用关联接口。
