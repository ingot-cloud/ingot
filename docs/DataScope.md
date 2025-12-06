# 数据权限

## 数据权限类型
 * 全部数据权限：保持原始SQL，不进行数据权限过滤
 * 自定义数据权限：自定义过滤条件，定义多个部门数据权限
 * 当前部门以及子部门：当前部门数据权限以及所有子部门数据权限
 * 当前部门：当前部门数据权限
 * 本人：本人数据权限

## 使用

 1. 需要进行数据权限处理的表，给对应的实体类增加`@DataScopeTable`注解，或者将对应的表名配置到`ingot.mybatis.scope.tables`中
```yaml
ingot:
  mybatis:
    scope:
      tables:
        - t_test
```
 2. 确保实体类中存在`DataScopeProperties`中`scopeFieldName`和`userFieldName`字段
 3. 给需要进行数据权限处理的方法加`@DataScope`注解
 4. 配置角色的相关数据权限类型 