---
name: in-framework
description: >-
  Enforces Ingot backend framework coding rules: ISO-8601 API instants with UTC processing and storage, business enums, JavaDoc, OSS object paths,
  constructor injection, named constants, and association-list queries. Use when
  writing or changing Java business code, time fields, createdAt/updatedAt,
  enums, JavaDoc, OSS/avatar/attachment fields, Spring Bean injection, magic
  literals, IAM custom-resource integration, or APIs that list a target's bound or related records.
---

# Ingot 框架编码规范

## Instructions

写或改本仓库 Java 业务代码时按本 skill 落盘。先对照检查清单，再按改动主题阅读 [references/](references/) 对应篇，不要凭记忆补全细则。

读多写少、来自远端的参考数据（策略、配置、字典、租户参数）走 [layered-cache](../layered-cache/SKILL.md)。提交信息走 [conventional-commits](../conventional-commits/SKILL.md)。

```
- [ ] 时间：API 带偏移 ISO，响应 UTC Z，无偏移输入 400；存储和到期判断 UTC；前端本地展示；Cron 显式业务时区
- [ ] 枚举：@Getter + @RequiredArgsConstructor，@JsonValue/@EnumValue，getEnum 走 EnumUtils
- [ ] 魔法值：条件、装配、YAML、跨类比较不裸写字面量
- [ ] OSS：入库 bucket/objectName，响应 @OssUrl
- [ ] 注入：private final + @RequiredArgsConstructor；不新增字段/Setter 注入
- [ ] JavaDoc：类型、对外方法、对外字段按 docs/standards/Javadoc.md 一并写上
- [ ] 关联列表：target 的绑定数据走独立接口，SQL 按关联表过滤分页，禁止用 ids 回查父列表
```

## 索引

| 改动主题 | 阅读 |
| --- | --- |
| 时间字段、createdAt/updatedAt、定时或过期判断 | [references/time.md](references/time.md) |
| 业务枚举 | [references/enum.md](references/enum.md) |
| 魔法值、配置取值 | [references/literals.md](references/literals.md) |
| 头像、附件、OSS | [references/oss.md](references/oss.md) |
| Spring Bean 依赖 | [references/injection.md](references/injection.md) |
| 类型或对外 API 注释 | [references/javadoc.md](references/javadoc.md) |
| 自定义资源、多应用权限、对象范围、字段脱敏/编辑控制 | [references/iam-resource-extension.md](references/iam-resource-extension.md) |
| 关联对象列表、绑定查询、ids 回显 | [references/association-lists.md](references/association-lists.md) |

## Examples

新增租户状态枚举：按 [业务枚举](references/enum.md) 用 `EnumUtils` 建索引，不要手写 `HashMap`。

接口返回 `createdAt`：按 [接口时间](references/time.md) 输出 ISO-8601 UTC，前端本地格式化。到期判断用 `Instant` / UTC，日期、日内时间和 Cron 单独表达业务语义。
