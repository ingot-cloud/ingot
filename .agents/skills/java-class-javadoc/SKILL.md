---
name: java-class-javadoc
description: Generate, complete, and review Chinese JavaDoc for Java types and their public API (interface methods, public methods, ConfigurationProperties fields, and externally get/set fields) according to this repository's docs/standards/Javadoc.md. Use when adding or changing Java types, when asked to add missing comments, improve or audit JavaDoc, or check comment compliance. Do not use for private helpers or package comments.
---

# Java JavaDoc

## Workflow

1. Locate the repository root and read `docs/standards/Javadoc.md` before drafting comments. Treat that file as the source of truth if it differs from this skill.
2. Read the complete target type and enough surrounding code to understand its actual responsibility, collaborators, lifecycle, constraints, and intended callers.
3. Identify every required declaration in scope:
   - Types: `class`, `interface`, `enum`, `record`, `@interface`, and nested variants.
   - Public/external methods: interface methods and `public` methods used outside the type.
   - Public/external fields: `@ConfigurationProperties` fields and fields other modules get or set.
   Exclude `private` helpers, Lombok-generated getters/setters (comment the field instead), and package declarations.
4. Determine whether to create, complete, or review the JavaDoc.
5. Draft concise Chinese JavaDoc from code facts. Prefer precise responsibility and boundary statements over restating the name or listing members.
6. Validate every link, example, tag, and claim against the code before returning or applying the result.
7. When implementing or changing public API, write the comments into source as part of the change. When the user asks only to review, report findings or proposed JavaDoc without editing. When the user asks to apply or fix comments, edit the requested files.

## Content Rules

### Types

- Start with one `<p>...</p>` paragraph that explains in one sentence what the type does.
- Add a second paragraph only when design intent, core responsibilities, usage scenarios, boundaries, or constraints materially help the reader.
- Add a usage example only when a correct, useful example can be derived from visible APIs. Wrap code in `<pre>{@code ... }</pre>`.
- Add `@see` only for real, directly relevant symbols. Prefer `{@link ...}` for symbols referenced inside prose.
- Add `@apiNote` only for caller-facing restrictions, performance characteristics, lifecycle requirements, or usage guidance.
- Add `@implNote` only for implementation principles, internal invariants, design tradeoffs, or extension guidance.
- Order tags as `@author`, `@since`, optional `@see`, optional `@apiNote`, and optional `@implNote`.
- Place the JavaDoc immediately before annotations attached to the type, or immediately before the declaration when it has no annotations.

### Methods

- Write what the method does, plus constraints and failure semantics. Do not restate the method name.
- Use `@param`, `@return`, and `@throws` only when they add information the signature does not already show.
- Do not comment Lombok-generated getters or setters.

### Fields

- Write meaning, default, and legal values. For configuration fields, include YAML semantics or mode-specific behavior when it is not obvious.
- Comment the field, not the generated accessor.

### Shared

- Omit optional sections and tags when the code does not support meaningful content. Never emit placeholders such as "详细说明", "XxxService", or empty tags.
- Preserve useful existing information unless it is incorrect, obsolete, redundant, or inconsistent with the repository standard.

## Author And Version

Apply this precedence to type-level tags:

1. Use `@author` or `@since` values explicitly provided by the user or current task.
2. When updating existing JavaDoc, preserve its current non-empty `@author` and `@since` values.
3. For new JavaDoc with no explicit values, use `@author jy` and `@since 1.0.0`.

Do not infer replacement values from Git history, neighboring files, or the current date unless the user explicitly requests that policy.

## Review Checklist

- Confirm the comment describes behavior visible in the implementation or public contract.
- Confirm the type's first paragraph is a meaningful one-sentence summary.
- Confirm detailed prose adds design or usage value rather than repeating fields and methods.
- Confirm examples compile conceptually against visible constructors, factories, and methods.
- Confirm every `@see` and inline link resolves to a real symbol.
- Confirm caller guidance is in `@apiNote` and implementation guidance is in `@implNote`.
- Confirm required author and version tags follow the precedence rules.
- Confirm required public methods and external fields in scope have comments, and private helpers were left uncommented unless requested.

## Expected Scenarios

- For an uncommented type, generate a complete minimal JavaDoc with an accurate summary and default author/version tags, plus comments for its public methods and external fields.
- For an existing comment, retain its author/version unless explicitly overridden and improve only deficient content.
- For a complex public type, include API restrictions or implementation tradeoffs only when supported by the code, and omit invented examples or relationships.
- When adding a new public type during feature work, include type, public method, and configuration-field comments in the same change.
