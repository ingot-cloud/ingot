# 设计

countGroupsOfMember当前在member_groups CTE后追加WHERE 1=1，再按GROUP_SCOPE追加可选AND条件。groupIds为null时保留恒真条件，被Druid拒绝。

用MyBatis的where动态标签包裹现有GROUP_SCOPE：全范围没有条件时不生成WHERE；受限范围自动去掉首个AND，形成WHERE mg.id IN参数列表。列表查询已有g.tenant_id绑定条件，继续追加同一GROUP_SCOPE，不改变CTE、排序或分页。GroupRepository对空可见集合的提前返回保持。

使用真实Mapper生成BoundSql并由Druid1.2.27的MySqlWallProvider检查，覆盖未限制和限制范围；继续执行关联查询语义测试，不关闭防护或以恒真参数绕过规则。不引入关联ID拼接回父列表的查询方式。

## 扩展检查设计

从持久化 Mapper 包加载全部接口，使用真实 MyBatis 生成注解及 XML 的 BoundSql；Provider 使用类型化参数场景显式覆盖分支。将结果交给按照仓库 nacos 三个环境配置构造的 MySqlWallProvider（允许多语句、注释及 none-base 语句，禁止 SELECT INTO 与 DDL，恒真检查保持 Druid 默认开启）；补充错误 SQL 对照，确保防护仍生效。手写 Wrapper 范围和 SQL 尾部按实际查询生成检查，并结合既有查询测试验证结果。

仅对实际被拦截的合法查询调整等价 SQL 写法，不改变业务条件或消除空范围的恒假谓词。发现超出等价 SQL 修复的行为变更时另行提出方案。回归在隔离测试环境运行，不连接用户数据库。

### 扩展检查复现与等价修复

真实 BoundSql 已复现 Druid 2113（part alway false condition not allow）：在其他条件后追加 AND 1=0 的空可见范围被拒绝。涉及候选、分配/委派/角色列表、角色工作区、负责人和 Wrapper 对象范围。将空集合条件写为目标列 IN (NULL)，该 SQL 集合没有可匹配值（包括目标列为 NULL 时），在这些正向 WHERE、AND、OR 范围条件中继续拒绝所有行；绝不将空范围改为不加筛选。普通范围仍使用原参数化 IN/EXISTS。负责人条款改为直接组合实际条件，去除 1=1 拼接占位。

隔离 MyBatis 夹具在 StatementHandler.prepare 前统一检查最终 SQL，覆盖 Wrapper、分页插件生成的计数和真实列表/DML；既有语义断言继续验证跨租户、继承、范围交并集、空页和计数。另用含 NULL 列的隔离数据验证空集合谓词及其与普通 OR 分支组合的结果。
