# 模块文档

已实现模块的架构设计、使用指南、API 参考与 FAQ。与 [specs/current/](../../specs/current/) 配合阅读：specs 描述线上行为事实，本目录描述如何使用与运维。

## 索引

| 模块 | 目录 | 说明 |
|------|------|------|
| PMS / 权限 | [pms/](./pms/) | 功能说明、数据权限、租户 |
| 认证授权 | [authorization-server/](./authorization-server/) | OAuth2、JWK、Token、BFF |
| 凭证安全 | [credential-security/](./credential-security/) | 密码策略、策略加载器 |
| 字典 | [dict/](./dict/) | 三级缓存、InvalidationBus |
| 安全策略中心 | [security-center/](./security-center/) | 网关限流与安全策略 |
| 对象存储 | [object-storage-service/](./object-storage-service/) | OSS 架构与迁移 |
| 任务调度 | [task-scheduler-system/](./task-scheduler-system/) | TSS 框架 |
| 社交 | [social/](./social/) | 第三方登录 |

## 与需求文档的关系

- 产品级原始意图见 [requirements/VISION.md](../requirements/VISION.md)
- 优化路线见 [requirements/ROADMAP.md](../requirements/ROADMAP.md)
- 单次变更规格见 [specs/changes/](../../specs/changes/)
