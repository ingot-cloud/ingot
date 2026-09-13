# 已验证公共类型快照

`schemas.json` 是当前 21 个公共 DTO 的 OpenAPI components，由 `IamAuthorizationContractTest` 使用 swagger ModelConverters 从 Java 类型导出。它不是完整的端点 OpenAPI 文档，也不表示服务已提供这些接口。

`examples/` 包含已通过 Jackson 反序列化与嵌套 Bean Validation 的 5 份示例：共享只读角色、租户差异、指定管理部门分配、委派、手机号只读脱敏规则。示例 ID 均为测试数据，不是线上可调用对象。

来源：`ingot-framework/ingot-commons/src/main/java/com/ingot/framework/commons/model/iam/`；测试示例来源：同模块 `src/test/resources/iam/`。

再生成流程：

1. 执行 `./gradlew :ingot-framework:ingot-commons:test --tests '*Iam*ContractTest' --console=plain`。
2. 确认全部测试通过，将模块 `build/iam-contract/schemas.json` 同步到本目录。
3. 将同模块 `src/test/resources/iam/*.json` 同步到 examples，并核对 API.md。

测试检查必填字段、字符串 ID/版本、ISO duration、schema 引用完整性，以及校验方法不会被当作 JSON 属性。OpenAPI 3.0 不能完整表达期限大小比较、字段可编辑性和差异结构等跨字段规则，实际请求仍须执行 Bean Validation 及业务校验。

尚未覆盖 bootstrap、完整资源详情、预览/升级/诊断/审计响应以及全部端点请求和响应。上述内容完成并与控制器导出一致之前，不宣称 T01/T03/T13 完成。
