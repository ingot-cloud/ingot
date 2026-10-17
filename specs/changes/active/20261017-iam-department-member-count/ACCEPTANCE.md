# Acceptance

| ID | 条件 | 证据 |
|----|------|------|
| A1 | 部门有 2 ACTIVE + 1 SUSPENDED 直属成员时列表 `memberCount=2` | `DepartmentQueryRepositoryTest` / `DepartmentServiceTest` 已通过 |
| A2 | 仅下级有成员时本部门 `memberCount=0` | 计数结果不含无任职部门，列表回填 0 |
| A3 | 详情省略 `memberCount` | `DepartmentServiceTest.getOmitsMemberCount` |
| A4 | 通讯录/策略构造省略 `memberCount` | `PolicyService` 传 null |
| A5 | 当前页一次 `GROUP BY` 计数 | `DepartmentQueryRepositoryTest` 断言一条 SQL |
| A6 | schemas 含可选 `memberCount` | contracts 已更新；`IamAuthorizationContractTest` 已通过 |
