---
name: conventional-commits
description: Draft git commit messages that follow Conventional Commits 1.0.0 and this repository's conventions (Chinese subject, optional scope, Angular types). Use when creating a git commit, writing or rewriting a commit message, or when the user mentions Conventional Commits, 约定式提交, or 提交规范.
---

# Conventional Commits

This skill writes the commit **message**. Do not create a commit unless the user asked. Git safety (no force-push, no `--no-verify`, no amend unless allowed) stays in the user git rules.

Spec: [Conventional Commits 1.0.0](https://www.conventionalcommits.org/zh-hans/v1.0.0/). This file adds repository conventions; if they conflict with the spec's MUST rules, follow the spec.

## Workflow

1. Inspect `git status`, staged/unstaged diff, and recent `git log` subjects.
2. Pick **one** `type` from the table below by the change's primary intent, not by file count.
3. Add `scope` only when the change is clearly one module or domain.
4. Write a Chinese subject that states **why**, then an optional body for context the diff does not show.
5. Mark breaking API / compatibility changes with `!` and a `BREAKING CHANGE:` footer.
6. Pass the message via HEREDOC. Do not use `-m` twice to fake title/body.

## Format

```
<type>[optional scope][!]: <description>

[optional body]

[optional footer(s)]
```

- `type` is lowercase. Colon is ASCII `:`, then one space.
- Subject is one line, Chinese, no trailing period. Prefer ≤50 characters; do not truncate meaning.
- Body is separated from the subject by a blank line. Wrap for readability; explain motivation, risk, or migration.
- Footers use git trailer form (`Token: value`), except `BREAKING CHANGE: ` which may contain a space in the token.

## Types

| Type | Use when |
|---|---|
| `feat` | New user-facing or API capability |
| `fix` | Bug fix |
| `docs` | Docs or specs only (`specs/`, README, guides) |
| `style` | Formatting only; no behavior change |
| `refactor` | Restructure without adding a feature or fixing a bug |
| `perf` | Performance improvement |
| `test` | Tests only |
| `build` | Gradle, plugins, or dependency versions |
| `ci` | CI workflows and scripts |
| `chore` | Maintenance that is not build/ci: Nacos snapshots, SQL scripts, local tooling |
| `revert` | Reverts a previous commit; point to it in the body or `Refs:` footer |

Do not invent types. `feat` / `fix` are mandatory when the change is a feature or a bugfix.

If the tree mixes unrelated kinds (for example feature code and an unrelated spec archive) and the user did not require a single commit, split. If they required one commit, use the dominant type.

## Scope

Optional. Omit when the change is cross-cutting or the subject already names the area.

When used: one lowercase noun in parentheses, matching a module or domain (`oss`, `cache`, `security`, `pms`, `gateway`, `specs`). Do not use PascalCase (`User`, `TSS`).

```
feat(oss): ...
fix(security): ...
docs(specs): ...
```

## Breaking changes

Any type may be breaking. Mark it:

- `feat(api)!: ...` or `refactor!: ...` immediately before `:`, and/or
- a footer `BREAKING CHANGE: <what callers must change>`

Prefer both when callers need migration detail.

## Anti-patterns

```
docs: 更新SPEC
chore: update nacos
feat: xxx功能提交
refactor: 修改代码
```

Vague verbs (`更新`, `修改`, `完善`) with no object, English subjects, trailing `功能提交`, and file lists in the subject are all invalid.

## Examples

**Feature**

```
feat: 菜单 view_path 改为页面注册键并下线 customViewPath

前端已改为提交页面注册键，后端不再按 path 拼接源码路径，避免菜单路由与视图绑定耦合。
```

**Fix**

```
fix: OSS 预签名失败回退原始路径，避免拖死业务读接口

MinIO 配置 region 与连接超时，预签名改为本地签名，不再依赖 GetBucketLocation。
```

**Docs / spec archive**

```
docs: 归档菜单 view_path 变更并更新 current 基线

验收完成后将 20260903-pms-menu-view-path 移入 archive，并把页面注册键语义写入应用授权基线。
```

**Breaking**

```
feat(security)!: 凭证策略改为仅支持 remote 模式

BREAKING CHANGE: 删除 local 模式配置项；调用方必须改为 remote，并保证策略中心可达。
```

## Checklist

- [ ] Type matches intent (`feat` / `fix` not used as a dump category)
- [ ] Subject is Chinese, one line, no period, states why
- [ ] Scope omitted or a single lowercase noun
- [ ] Breaking change has `!` and/or `BREAKING CHANGE:`
- [ ] Body present only when it adds context
- [ ] Message is HEREDOC text, not a bullet list of files
