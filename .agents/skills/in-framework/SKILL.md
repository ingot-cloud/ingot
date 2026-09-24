---
name: in-framework
description: >-
  Enforces Ingot backend framework coding rules: API wall-clock time in the
  client timezone with UTC storage, business enums, JavaDoc, OSS object paths,
  constructor injection, and named constants. Use when writing or changing Java
  business code, time fields, createdAt/updatedAt, enums, JavaDoc,
  OSS/avatar/attachment fields, Spring Bean injection, or magic literals.
---

# Ingot 框架编码规范

## Instructions

写或改本仓库 Java 业务代码时按本 skill 落盘。先对照检查清单，再按改动主题阅读 [references/](references/) 对应篇，不要凭记忆补全细则。

读多写少、来自远端的参考数据（策略、配置、字典、租户参数）走 [layered-cache](../layered-cache/SKILL.md)。提交信息走 [conventional-commits](../conventional-commits/SKILL.md)。

```
- [ ] 时间：接口墙钟按请求前端时区；识别不到用 Asia/Shanghai；库存 UTC；定时与到期判断走 Instant / UTC
- [ ] 枚举：@Getter + @RequiredArgsConstructor，@JsonValue/@EnumValue，getEnum 走 EnumUtils
- [ ] 魔法值：条件、装配、YAML、跨类比较不裸写字面量
- [ ] OSS：入库 bucket/objectName，响应 @OssUrl
- [ ] 注入：private final + @RequiredArgsConstructor；不新增字段/Setter 注入
- [ ] JavaDoc：类型、对外方法、对外字段按 docs/standards/Javadoc.md 一并写上
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

## Examples

新增租户状态枚举：按 [业务枚举](references/enum.md) 用 `EnumUtils` 建索引，不要手写 `HashMap`。

接口返回 `createdAt`：按 [接口时间](references/time.md) 转成请求前端当地时间；识别不到时区用 `Asia/Shanghai`。到期判断用 `Instant`，不要拿请求时区去比。
