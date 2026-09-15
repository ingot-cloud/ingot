# 已验证公共类型快照

`schemas.json` 是当前模型组件（含泛型实例和既有 R 信封）的 OpenAPI components，由 `IamAuthorizationContractTest` 使用 swagger ModelConverters 从 Java 类型导出。它不是运行时接口证明，也不表示服务已提供这些接口。

`examples/` 包含已通过 Jackson 反序列化与嵌套 Bean Validation 的示例：共享只读角色、租户差异、分配、委派、字段规则、bootstrap、成员详情/分页、诊断、预览、升级冲突、审计、策略，以及角色创建/发布/升级和组织创建请求。示例 ID 均为测试数据，不是线上可调用对象。

来源：`ingot-framework/ingot-commons/src/main/java/com/ingot/framework/commons/model/iam/`；测试示例来源：同模块 `src/test/resources/iam/`。

再生成流程：

1. 执行 `./gradlew :ingot-framework:ingot-commons:test --tests '*Iam*ContractTest' --console=plain`。
2. 确认全部测试通过，执行 `python3 tools/iam/generate_routes.py` 与 `python3 tools/iam/build_contract.py` 同步 schemas 并生成目标 openapi.json。
3. 将同模块 `src/test/resources/iam/*.json` 同步到 examples，并核对 API.md。
4. 执行 `python3 tools/iam/build_contract.py --check` 与 `python3 tools/iam/test_contract.py` 检查快照、引用、域边界及错误信封。这些检查不替代完整 OpenAPI 规范验证器或实际控制器集成测试。

测试检查必填字段、字符串 ID/版本、ISO duration、UTC 时间、schema 引用完整性、泛型实际资源类型，以及校验方法不会被当作 JSON 属性。响应测试同时加载应用 InModule，验证统计数量仍为数字、隐藏字段省略、受限统计不伪造为零、R 信封保持稳定错误码。示例主要展示 data 内容，实际 HTTP 使用 R<T> 包装。OpenAPI 3.0 不能完整表达期限大小比较、字段可编辑性和差异结构等跨字段规则，实际请求仍须执行 Bean Validation 及业务校验。

## 2026-09-15 复评后的有效性

当前JSON快照包含77条路径、132个操作；implemented标记仅表示当时控制器接入，不能证明权限、错误状态或业务语义验收。账号/本人/辅助/RPC/Security的完整交付尚未闭合，T01/T03已重开。

本轮只改Spec，不重新生成JSON或把未实现字段写成已验证示例。平台登录不返回允许租户（共享DTO allows=[]）、完整对象能力、草稿预览、导出和HTTP错误等修订以API/FRONTEND/REMEDIATION为目标。后续完成代码和真实HTTP验证后重生快照与夹具；前端不得将现有快照作为本轮完整已验证基线。
