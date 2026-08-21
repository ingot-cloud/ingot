# Design

## 方案摘要

新增 commons 枚举 `PolicySourceMode`（`LOCAL` / `REMOTE`），并提供给 `@ConditionalOnProperty` 使用的编译期常量 `VALUE_LOCAL="local"`、`VALUE_REMOTE="remote"`。事件投递不新建枚举，属性改为已有 `RecordingTarget`；YAML `remote` 别名由 `Converter<String, RecordingTarget>` 在绑定期消化。

## 数据模型与接口

### PolicySourceMode

包：`com.ingot.framework.commons.model.security`

消费者：

- `AccessProtectionProperties.mode`
- `InSecurityProperties.Session.mode`
- `CredentialSecurityProperties.PolicyConfig.mode`
- `AccountDomainProperties.mode`（删除嵌套 `PolicyMode`）
- Gateway 四域 `policy.mode`（删除各自 `Mode`）

域特有说明保留在字段 JavaDoc，不再复制内部枚举。

### RecordingTarget

属性：

- `SecurityEventProperties.target`：`RecordingTarget`，默认 `LOCAL`
- `SecurityEventProperties.shadowTargets`：`List<RecordingTarget>`

`RecordingTarget.fromValue(String)` 接受 `local` / `center` / `remote`（大小写不敏感）；空白视为 `LOCAL`；其他值抛 `IllegalArgumentException`。

`RecordingTargetConverter` 委托 `fromValue`，以 `@ConfigurationPropertiesBinding` Bean 注册，保证 `@EnableConfigurationProperties` 绑定 `List` 元素时同样生效。

`RecordingConfigResolver` 不再做字符串 parse：`target == null` 时视为 `LOCAL`；shadow 与主目标去重逻辑保持不变。

## 数据流与失败处理

YAML → Spring Binder → 枚举。`@ConditionalOnProperty` 仍比较 Environment 中的原始字符串，故 `havingValue` 必须是 `local` / `remote` 常量，不能写枚举名 `LOCAL`。

事件非法 `target` 在绑定期失败；单测直接调用 `fromValue` / Converter 覆盖别名与非法值。

## 迁移与回滚

- 无数据迁移、无 Nacos 变更。
- 回滚即还原 Java 类型；YAML 无需回滚。

## 测试策略

- `RecordingTarget.fromValue` / Converter：`local`、`center`、`remote`、空白、非法值。
- `RecordingConfigResolver`：缺省、center、shadow 去重（改用枚举 setter）。
- 现有 gateway / access / session / credential 装配测试保持 YAML `mode=local|remote`，行为不变。
