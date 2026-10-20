# 开发与隔离验证

日期：2026-10-20。未连接实际业务库，未提交Git，未执行部署验收；current保持原基线。

## 交付与事实

- manifest全部63表、494字段已具备中文COMMENT。七个权威DDL文件与HEAD逐字去除COMMENT后完全一致，不修改字段类型、默认值、主键、索引、外键或生成表达式。
- 数据字典、完整初始化和026说明升级SQL均可重现；结构预检快照由本地无网络MySQL生成。预检单连接批量读取63表，全部通过后才允许ALTER；缺表/结构漂移阻断，不修改清单外表。
- 预检仅折叠COMMENT、运行时自增高水位、无语义约束排序，以及MySQL ALTER产生的等价ASCII常量引导符。列字符集、COLLATE、非ASCII文字、表达式、常量及字符串内部空格差异仍保留。
- 成员部门两个入口共用差量仓库；不变关系零DML；首次主部门绑定一次INSERT；切主先解除旧主。生命周期仓库仍拒绝null，查询仓库保留null清空。
- 组成员、部门来源、菜单操作、套餐应用保留未变行；部门来源属性原地UPDATE。负责人未换主时不清空重设。业务删除仍清关联；版本、审计、权限与失效规则不改。
- 逐表报告包含全部63表的键宽、标识来源、有效索引（含外键补建）、外键依赖、业务入口、重写情况和分级结论。无显式主键、宽字符串主键、重叠索引仅列候选，没有实际业务负载/碎片测量。

## 验证命令与结果

| 验证 | 结果 |
|---|---|
| `python3 tools/iam/generate_database.py --check` | 通过 |
| `python3 tools/iam/generate_database_review.py --check` | 通过 |
| `python3 tools/iam/test_schema_comments.py` | 7项通过：注释覆盖、生成漂移、嵌套表达式、预检拒绝、ASCII等价及真实差异、字面量空格、报告覆盖 |
| `python3 tools/iam/test_database_sources.py` | 4项通过 |
| `python3 tools/iam/test_schema_comments_mysql.py` | MySQL 8.4：3项通过，预检/升级/重复执行/新库对比/行保留/漂移拒绝/主关系及版本竞争 |
| `IAM_SCHEMA_MYSQL_IMAGE=docker-registry.ingotcloud.top/mysql:8.0.44 python3 tools/iam/test_schema_comments_mysql.py` | MySQL 8.0.44：相同3项通过 |
| `python3 databases/iam/test_identity_schema.py` | 28项通过 |
| `python3 databases/iam/test_bootstrap_seed.py` | 20个不同用例分批通过；最终19项通过，剩余应用ID用例校准旧菜单数量后单独重跑通过 |
| `./gradlew :ingot-service:ingot-iam:ingot-iam-provider:compileTestJava --offline --console=plain` | 通过 |
| `git diff --check` | 通过 |

Java测试统一使用`./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --offline --console=plain -I tools/iam/tenant_live/verification.gradle`，通过`--tests '*类名'`选择以下用例：

| 类 | 数量 | 结果 |
|---|---:|---|
| RelationWriteTest | 6 | 通过；实际MyBatis写入计数及行创建标记，含首次绑定、无变化、混合增删、主标记切换、清空、租户隔离、重复拒绝及回滚 |
| MemberLifecycleTest | 10 | 通过；保留旧/新对象范围、版本冲突、审计回滚、所有者保护及失效 |
| TenantMemberOnboardingTest | 6 | 通过；账号匹配、入职、任职与审计原子性 |
| CatalogServiceTest | 23 | 通过 |
| GroupServiceTest | 7 | 通过 |
| DepartmentLeaderRepositoryTest | 6 | 通过 |
| DepartmentLeaderServiceTest | 11 | 通过 |
| MemberQueryRepositoryTest | 9 | 通过 |
| GroupRepositoryTest | 5 | 通过 |
| CatalogInUseTest | 2 | 通过 |
| EntitlementServiceTest | 3 | 通过 |

共88个不同Java用例分批通过；最终成员写入调整后重跑RelationWriteTest、MemberLifecycleTest、TenantMemberOnboardingTest共22项通过。没有将H2结果当作MySQL生成列或锁行为证据；MySQL使用独立容器SQL验证唯一约束与父对象锁/版本条件竞争，Java用例验证服务端拒绝和回滚契约。

## 验证期间修正

- MySQL MODIFY不可重复声明列内PRIMARY KEY，026保留既有索引并显式保持原隐含NOT NULL。
- MySQL MODIFY规范化ASCII CHECK常量引导符；预检只接纳已验证的等价形式，不放宽实际结构差异。
- 首轮多个MySQL容器并发触及本机进程内存限制，已清理本任务遗留容器并串行重跑；新增隔离夹具限制缓冲池和连接数。资源失败不计为通过。
- 冷启动旧测试仍断言58表/154操作/36菜单/46租户操作，而当前目录为63/155/38/47；测试断言已校准（组织菜单从13调整为15），保留禁用的历史`iam-tenant:role:upgrade`和空图标语义。历史补丁只比较非注释结构，旧补丁不反向补入新COMMENT。正式种子及生成源未修改。

## 部署与验收边界

已有库先备份、停写并禁止并行DDL；按迁移README执行显式选库预检，再按需`--apply`。不直接导入026或完整初始化。注释ALTER可能等待元数据锁，整批DDL不具备事务回滚，失败处理后可重跑。后端可独立部署和回退；本次不改关系标识及公共API。

开发/隔离验证已完成，现为validating，部署/产品验收确认前不更新current或归档。后续主键或人群/开通/委派结构优化须单独确认。
