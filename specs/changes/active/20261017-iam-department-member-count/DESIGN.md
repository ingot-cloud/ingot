# Design

## 方案摘要

在既有 `DepartmentRecord` 上增加可选整型 `memberCount`。`GET /v1/tenant/departments` 一次列出范围内全部部门并组树，按全部可见部门 ID 批量统计 `iam_member_department` ∩ `iam_tenant_member.status = ACTIVE`，再写入记录。唯一根部门与直属一级部门在响应中平级。`get` 与 `PolicyService` 的通讯录/策略构造保持无计数。

## 数据模型与接口

`DepartmentRecord` 追加：

```text
Integer memberCount  // 可选；列表填 0 或正整数；详情与 directory 为 null，JSON 省略
```

不改表。计数 SQL：

```sql
SELECT md.department_id, COUNT(*)
  FROM iam_member_department md
  JOIN iam_tenant_member m
    ON m.tenant_id = md.tenant_id AND m.id = md.member_id
 WHERE md.tenant_id = ?
   AND m.status = :ACTIVE
   AND md.department_id IN (...)
 GROUP BY md.department_id
```

`status` 绑定 `MemberStatus.ACTIVE`，禁止调用点裸写 `"ACTIVE"`。

兼容：旧客户端忽略未知字段；新客户端在详情缺失时显示 `-`。

## 数据流与失败处理

1. `list` 鉴权与范围不变，不再分页。
2. 无记录则跳过计数 SQL。
3. 计数失败随列表失败，不 fail-open 为 0。
4. 未出现在计数结果中的部门视为 0。

删除路径继续使用现有 `memberCount(tenantId, departmentId)`（全部任职），与列表口径分离。

## 迁移与回滚

无 DDL。回滚后端即停止输出 `memberCount`，前端按缺失显示 `-`。

## 测试策略

- Repository：ACTIVE/SUSPENDED/他部门/空集合
- Service：列表写入计数、详情不带计数
- `IamAuthorizationContractTest`：生成 schemas 含可选 `memberCount`
