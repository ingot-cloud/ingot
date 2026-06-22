---
name: java-class-javadoc
description: Generate, complete, and review Chinese type-level JavaDoc for Java class, interface, enum, record, annotation, and nested type declarations according to this repository's docs/code-standards/Javadoc.md. Use when Codex is asked to add missing Java type comments, improve or audit existing type comments, or check type-level JavaDoc compliance. Do not use for method, constructor, field, or package comments.
---

# Java Class JavaDoc

## Workflow

1. Locate the repository root and read `docs/code-standards/Javadoc.md` before drafting comments. Treat that file as the source of truth if it differs from this skill.
2. Read the complete target type and enough surrounding code to understand its actual responsibility, collaborators, lifecycle, constraints, and intended callers.
3. Identify every requested type declaration: `class`, `interface`, `enum`, `record`, `@interface`, and nested variants. Exclude methods, constructors, fields, enum constants, and package declarations.
4. Determine whether to create, complete, or review the type-level JavaDoc.
5. Draft concise Chinese JavaDoc from code facts. Prefer precise responsibility and boundary statements over restating the type name or listing members.
6. Validate every link, example, tag, and claim against the code before returning or applying the result.
7. Modify Java files only when the user explicitly asks to apply or fix the comments. Otherwise, report findings or provide the proposed JavaDoc without editing source files.

## Content Rules

- Start with one `<p>...</p>` paragraph that explains in one sentence what the type does.
- Add a second paragraph only when design intent, core responsibilities, usage scenarios, boundaries, or constraints materially help the reader.
- Add a usage example only when a correct, useful example can be derived from visible APIs. Wrap code in `<pre>{@code ... }</pre>`.
- Add `@see` only for real, directly relevant symbols. Prefer `{@link ...}` for symbols referenced inside prose.
- Add `@apiNote` only for caller-facing restrictions, performance characteristics, lifecycle requirements, or usage guidance.
- Add `@implNote` only for implementation principles, internal invariants, design tradeoffs, or extension guidance.
- Omit optional sections and tags when the code does not support meaningful content. Never emit placeholders such as "详细说明", "XxxService", or empty tags.
- Order tags as `@author`, `@since`, optional `@see`, optional `@apiNote`, and optional `@implNote`.
- Place the JavaDoc immediately before annotations attached to the type, or immediately before the declaration when it has no annotations.
- Preserve useful existing information unless it is incorrect, obsolete, redundant, or inconsistent with the repository standard.

## Author And Version

Apply this precedence:

1. Use `@author` or `@since` values explicitly provided by the user or current task.
2. When updating existing JavaDoc, preserve its current non-empty `@author` and `@since` values.
3. For new JavaDoc with no explicit values, use `@author jy` and `@since 1.0.0`.

Do not infer replacement values from Git history, neighboring files, or the current date unless the user explicitly requests that policy.

## Review Checklist

- Confirm the comment describes behavior visible in the implementation or public contract.
- Confirm the first paragraph is a meaningful one-sentence summary.
- Confirm detailed prose adds design or usage value rather than repeating fields and methods.
- Confirm examples compile conceptually against visible constructors, factories, and methods.
- Confirm every `@see` and inline link resolves to a real symbol.
- Confirm caller guidance is in `@apiNote` and implementation guidance is in `@implNote`.
- Confirm required author and version tags follow the precedence rules.
- Confirm no method-level or field-level comments were changed outside the request.

## Expected Scenarios

- For an uncommented type, generate a complete minimal JavaDoc with an accurate summary and default author/version tags.
- For an existing comment, retain its author/version unless explicitly overridden and improve only deficient content.
- For a complex public type, include API restrictions or implementation tradeoffs only when supported by the code, and omit invented examples or relationships.
