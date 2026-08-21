# Tasks

## 实施任务

- [x] T1：新增 `PolicySourceMode` 于 `ingot-commons`
  - 依赖：无
  - 验收：枚举含 `LOCAL` / `REMOTE` 与 `VALUE_LOCAL` / `VALUE_REMOTE`
- [x] T2：替换 access / session / credential / account / gateway 四域属性与 `@ConditionalOnProperty` / Java 比较
  - 依赖：T1
  - 验收：无内部 `Mode` / `PolicyMode`，无策略 mode 字符串硬编码
- [x] T3：`SecurityEventProperties` 改用 `RecordingTarget`；Converter + 精简 Resolver；更新测试
  - 依赖：无
  - 验收：`remote` 别名仍映射 `CENTER`；Resolver 不再 parse 字符串

## 验证任务

- [x] V1：相关模块单测通过

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 不更新（YAML 契约未变）
- [x] Change 已记录完成信息并归档
