# Requirements

## 用户场景

1. 运维在 Nacos 继续写 `ingot.security.<domain>.policy.mode=local|remote` 或 `ingot.security.access.mode` / `ingot.security.session.mode` / `ingot.security.credential.policy.mode` / `ingot.security.account.mode`，装配结果与改前一致。
2. 运维继续写 `ingot.security.event.target=local|center`；历史值 `remote` 仍视为上报中心。
3. 开发在 Java 中用枚举比较与 `@ConditionalOnProperty` 常量，不再散落 `"local"` / `"remote"` 字面量。

## 业务规则

1. 策略来源与事件投递是两套词汇，不得合成一个三值枚举。
2. `PolicySourceMode.LOCAL` 对应 YAML `local`，`REMOTE` 对应 `remote`；代码缺省仍为 `LOCAL`。
3. `RecordingTarget.LOCAL` 对应 YAML `local`，`CENTER` 对应 `center`；`remote` 为 `CENTER` 的兼容别名。
4. 非法 YAML 值在绑定或解析时失败，不得静默当成 local。

## 边界与非目标

- 不改 Nacos 配置文件、不改生产默认值。
- 不改策略加载、降级阶梯、事件投递的运行时行为。
- Java setter 类型从 `String` 变为枚举，仅仓库内调用方。
- 不处理字典客户端、发号器等无关 mode。

## 验收标准

- [x] 各策略属性字段类型为 `PolicySourceMode`，无内部重复 `Mode` / `PolicyMode`
- [x] 相关 `@ConditionalOnProperty` 的 `havingValue` 引用 `PolicySourceMode.VALUE_LOCAL` / `VALUE_REMOTE`
- [x] `SecurityEventProperties.target` / `shadowTargets` 类型为 `RecordingTarget`
- [x] YAML `target=remote` 仍解析为 `CENTER`
- [x] 相关模块单测通过
