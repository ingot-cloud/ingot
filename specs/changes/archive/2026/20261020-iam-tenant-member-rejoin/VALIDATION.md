# Validation

## 自动化

- `TenantMemberOnboardingTest`：新建、复用、已移出重新加入、在职冲突
- `MemberRejoinTest`：关系清理、version、版本冲突、回滚
- `IamAuthorizationContractTest`：`TenantMemberRejoinInput`、`TenantMemberCreated.rejoined`

## 手工

1. 移出一名非拥有者成员。
2. 「添加成员」提交同一手机号、新姓名，成功且列表出现新姓名。
3. 筛选已移出为空（该人已是在职）。
4. 再次移出后，从已移出列表点「重新加入」，改部门后提交。
5. 该成员不再出现在旧用户组/负责人中，旧角色不再生效。
