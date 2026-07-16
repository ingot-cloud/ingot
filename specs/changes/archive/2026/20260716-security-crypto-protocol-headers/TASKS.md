# Tasks

## 实施任务

- [x] T1：创建 active change 工件并标记 approved
  - 依赖：无
  - 验收：README/REQUIREMENTS/DESIGN/TASKS 齐全

- [x] T2：新增 `HybridHeaders`、`HybridProtocolVersion`
  - 依赖：T1
  - 验收：常量与枚举 API 符合 DESIGN

- [x] T3：精简 `InCryptoProperties`，改写 `HybridCryptoInterceptor`
  - 依赖：T2
  - 验收：无 `headers`/`modeValue`；拦截器使用常量与枚举

- [x] T4：单测与文档
  - 依赖：T3
  - 验收：拦截器单测通过；模块 README 更新

- [x] T5：更新 `specs/current` 并归档 change
  - 依赖：T4
  - 验收：current SPEC 反映新头名；change 移入 archive

## 验证任务

- [x] V1：执行 `ingot-security-crypto` 模块测试

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
