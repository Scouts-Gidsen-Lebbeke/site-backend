---
name: code-style
description: Code style rules for this repository. Use whenever you write or change Kotlin code (production or test), including small fixes. Covers comments: no inline, trailing or KDoc/Javadoc comments, only TODOs.
---

# Code style

## Comments: only TODOs
The user does not want explanatory comments in the code. When you write or change code:

- **Don't add** inline comments (`// ...`), trailing comments (`foo() // ...`), block comments, or KDoc/Javadoc (`/** ... */`). This applies to new code and to lines you touch, including justifications next to `@Suppress`.
- **The only allowed comment is a TODO**, in the existing form: `// TODO: <what still needs doing>`.
- Make the code explain itself instead: pick precise names for functions, variables and parameters, and extract a well-named private function or `val` when a step needs explaining.
- Put the *why* where the user reads it: in the commit message body, the PR description, or your reply. Not in the code.
- Tests keep their `// given`, `// when`, `// then` section markers (required by `.claude/rules/testing.md`). Those are structure, not explanation.
- Don't remove comments the user wrote unless they ask. When you rewrite a line that carries a user comment, leave that comment as it was rather than rephrasing it.

## Checklist before committing
Check the diff for added comment lines that are not a `TODO` or a test section marker, and remove them:

```bash
git diff --staged -U0 | grep -E '^\+.*(//|/\*|^\+\s*\*)' | grep -vE '// (TODO|given|when|then|when / then)'
```

This should print nothing.
