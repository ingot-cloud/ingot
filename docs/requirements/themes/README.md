# 跨域主题

本目录用于存放**跨多个模块**的平台级能力主题说明。每个主题描述动机、涉及模块与 ROADMAP 关联，不替代模块文档或 specs。

## 何时创建主题

- 一项能力横跨 2 个以上模块（如「多租户隔离」涉及 PMS、Auth、MyBatis 插件）。
- 需要在 ROADMAP 中引用统一叙事，避免在多个模块文档重复描述产品意图。

## 主题模板

新建 `themes/<theme-name>.md` 时建议包含：

1. **背景**：为什么需要这个跨域能力
2. **涉及模块**：链接 `docs/modules/` 与 `specs/current/`
3. **ROADMAP 关联**：链接 `ROADMAP.md` 中的条目 ID
4. **当前状态**：done / in-progress / planned

## 现有主题

（暂无独立主题文档。以下能力可在需要时拆出主题页：）

| 主题 | 涉及模块 | ROADMAP |
|------|----------|---------|
| 多租户隔离 | PMS、Auth、MyBatis | R-2026-003 |
| 安全纵深 | Auth、Gateway、Credential | R-2026-004 ~ R-2026-007 |
| 生产稳定性 | Guides / Performance / Troubleshooting | R-2026-001 ~ R-2026-002 |

按需在本目录追加 `multi-tenancy.md`、`security-in-depth.md` 等文件。
