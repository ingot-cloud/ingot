# 类处置、职责与命名清单

精确源文件清单在P0生成的 `class-inventory.json` 中维护；字段包括原路径、原全限定名、处置、目标、理由、命名结论和阶段。清单覆盖全部现有IAM主代码及commons IAM模型，新增替代类型另列。

API保留21/迁移9/删除97的批准分组、commons共享闭包及关联模块边界见DESIGN。保留表示功能继续存在，所有类型仍需命名审查。更名后同步Javadoc、注入、配置和序列化；共享枚举遵循in-framework门禁。

实施结果中拆分类的详细归属见 `service-splits.json`、`tenant-service-splits.json`；新增替代/拆分类型逐项见 `added-types.json`。`final-naming-mapping.json` 记录版本应用链最后一批更名。`class-inventory.json` 的目标均必须在当前生产源码解析成功。
