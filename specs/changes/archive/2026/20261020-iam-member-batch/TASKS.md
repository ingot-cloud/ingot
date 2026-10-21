# Tasks

## 实施任务

- [x] T1：编写本 change 规格
  - 依赖：无
  - 验收：README 为 approved/implementing，需求、设计、任务齐全
- [x] T2：新增 `iam_member_workbook` 到 004 与 029，更新生成与测试表计数
  - 依赖：T1
  - 验收：新库与已有库都能建表；026 基线不改
- [x] T3：commons 枚举与 DTO，provider EasyExcel、仓库、服务、API
  - 依赖：T2
  - 验收：模板、导出、导入、历史、三种产物下载符合 DESIGN
- [x] T4：路径解析、导出投影、导入逐行复用入职/资料/任职
  - 依赖：T3
  - 验收：单元测试覆盖 REQUIREMENTS 规则
- [x] T5：JSON 导出回归
  - 依赖：T4
  - 验收：`MemberExportServiceTest` 与相关 IAM 测试通过

## 验证任务

- [x] V1：provider 相关测试与 schema 生成检查

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新（验收后）
- [x] Change 已记录完成信息并归档（验收后）
