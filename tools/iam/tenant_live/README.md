# 租户当前授权迁移工具

只处理现有 IAM 目标模型，保留历史与审计；不会连接旧 PMS 表，也不会自动执行生成的转换 SQL。

- `export.py`：只读导出；账号/成员只含身份 ID、状态与技术锁。
- `migrate.py preflight`：最新权限、历史私有差异物化、拥有者、绑定、资源上限、改码冲突清单；有阻断退出 2。
- `migrate.py forward`：生成与来源摘要匹配、事务校验、可重跑的正转换。
- `migrate.py verify`：只读验证当前完整快照、角色/参数/委派归属。
- `migrate.py reverse`：根据当前新库生成旧固定引用，保留新角色、分配、委派与拥有者转交；来源变化阻断。

运行步骤、DDL 中断恢复、显式处置、P09 完整备份还原和 P10 新写入后的逆转换见 [迁移手册](../../../specs/changes/active/20261014-iam-tenant-live-authorization/MIGRATION.md)。实际库必须经维护窗口授权，不能使用 `databases/ingot_iam.sql` 升级已有库。

```sh
python3 -m unittest discover -s tools/iam/tenant_live -p test_migrate.py
python3 -m unittest discover -s tools/iam/tenant_live -p test_sql.py
```

SQL 测试需要本地 mysql:8.4，创建和销毁隔离容器；不下载镜像或使用实际数据库。

未上线且租户授权无需保留、021已执行的环境，可以直接使用 [开发准备说明](../../../specs/changes/active/20261014-iam-tenant-live-authorization/DEV-TEST-PREPARATION.md) 和 `databases/iam/dev/prepare_tenant_live_test.sql`。脚本停用旧租户授权、保留平台事实，并自动安装022约束；不生成正式迁移批次。

```sh
python3 -m unittest discover -s tools/iam/tenant_live -p test_dev_prepare.py
```

测试旧结构使用 `fixtures/pre_021/` 中冻结的002/003/004，不依赖提交前的HEAD；身份与辅助结构沿用当前manifest。开发准备夹具将目录显式置为旧publish码再验证按原ID切换，保证提交后和浅克隆中仍可复跑。
