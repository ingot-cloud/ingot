# Tasks

## 实施任务

- [x] T1：创建 active change 工件并标记 approved
  - 依赖：无
  - 验收：四套工件齐全，README 状态为 approved

- [x] T2：修改 HeaderConstants 常量值与字段名
  - 依赖：T1
  - 验收：三个 Header 值正确，`INNER_CLIENT_REAL_IP` / `INNER_USER_ID` 已定义

- [x] T3：替换 Java 代码引用与 JavaDoc
  - 依赖：T2
  - 验收：无 `CLIENT_REAL_IP` / `X_USER_ID` 引用

- [x] T4：更新 gateway-rule-client / security / gateway JavaDoc 字面量
  - 依赖：T2
  - 验收：无旧 Header 名字面量

- [x] T5：更新文档（GATEWAY-RATE-LIMIT、DEVICE-FINGERPRINT、BFF-AUTH-FLOW、E2E）
  - 依赖：T2
  - 验收：文档 Header 名与代码一致

## 验证任务

- [x] V1：Gradle 编译 + rg 残留检查
  - 依赖：T3、T4、T5
  - 验收：编译通过，无业务代码残留

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
