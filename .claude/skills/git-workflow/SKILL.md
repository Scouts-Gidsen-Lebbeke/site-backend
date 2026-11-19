---
name: git-workflow
description: Git rules for this repository. Use whenever you create or change files, make a branch, stage, commit, or are asked about pushing or PRs. Work goes on a feature branch, new files are staged as soon as they're created, commits get short focused messages, and nothing is pushed unless the user asks.
---

# Git workflow

Follow these rules for every code change in this repository unless the user explicitly says otherwise.

## 1. Start on a feature branch
Before you edit any file:

```bash
git status
git branch --show-current
```

- If the current branch is `main` (or `master`), create a branch first. Never commit directly to `main`.
  ```bash
  git fetch origin
  git switch -c <type>/<short-kebab-description> origin/main
  ```
  If the working tree already has uncommitted changes the user made, don't base the branch on `origin/main`. Use `git switch -c <name>` so their changes come along, and tell them.
- Branch prefixes: `feature/`, `fix/`, `refactor/`, `chore/`, `docs/`, `test/`. Examples: `feature/event-waitlist`, `fix/membership-sibling-discount`.
- If you're already on a feature branch that matches the task, keep using it. If the task is unrelated to that branch, ask before you start a new branch or keep going on this one.

## 2. Stage every new file right away
- After you create a file, stage it straight away with `git add <path>`. This keeps it from getting left out of a commit by accident.
- Stage modified files by explicit path when you commit them.
- **Don't use `git add -A`, `git add .` or `git commit -a`.** The working tree can contain the user's own unrelated changes, and those must not end up in your commits.
- Never stage:
  - secrets or local config: `.env`, `*.key`, credentials, `.claude/settings.local.json`
  - build output: `target/`
  - IDE files: `.idea/`, `*.iml`
  - large binaries the user didn't ask for

  If a file like this shows up as untracked and should never be committed, suggest adding it to `.gitignore`.

## 3. Commit small and often
- One commit per logical change. A refactor and a behavior change go in separate commits, and so does a formatting change.
- Before every commit, check what's staged:
  ```bash
  git diff --staged --stat
  git diff --staged
  ```
- Run the tests that cover the change before committing when that's practical. For example, `mvn test -Dtest=<RelevantTest>` (needs Docker), or at least `mvn compile`. Don't commit code that doesn't compile.
- Never skip hooks (`--no-verify`) and never bypass signing.

## 4. Commit messages
Keep them short and in the imperative mood:

```
<Short summary, imperative, ≤ 50 chars, no trailing period>

<Optional body after a blank line, wrapped at ~72 chars: why the
change was made, not a list of every line changed>

Co-Authored-By: ...   (attribution trailer, when one applies)
```

- Good: `Add sibling discount to membership price`, `Fix NPE when event has no calendar`
- Bad: `:(`, `fix`, `Try again?`, `WIP`, `Updated files`
- An optional type prefix such as `fix: ...` or `feat: ...` is fine if the branch already uses it. Match the existing style.
- Pass multi-line messages with a heredoc (in Bash) or a single-quoted here-string (in PowerShell), not several `-m` flags with escaped newlines.

## 5. Never push without being asked
- **Never run `git push` unless the user explicitly asks for it in the current request.** Earlier approval doesn't carry over to later pushes.
- When the work is done, report:
  - the branch name
  - the commits (`git log --oneline origin/main..HEAD`)
  - the command the user can run when they're ready, e.g. `git push -u origin <branch>`
- Don't open pull requests unless asked. Pushing to `main` starts a production deploy here (see CLAUDE.md), which is another reason never to push to `main`.

## 6. Avoid destructive operations
Don't run any of these without explicit confirmation from the user:
- `git reset --hard`, `git checkout -- <path>`, `git restore` on files you didn't change, `git clean -fd`
- `git push --force` (if a force push is ever approved, use `--force-with-lease`)
- rebasing or amending commits that have already been pushed
- deleting branches (`git branch -D`) or stashes (`git stash drop`)

Prefer adding a new commit over `--amend`. Only amend your own unpushed commit, and only to fix something you just did.

## 7. Housekeeping
- Keep the feature branch current with `git fetch origin` and then `git rebase origin/main`. Only rebase while the branch is unpushed; otherwise ask the user whether they prefer a merge.
- Resolve merge conflicts carefully and keep both sides' intent. If it's unclear how to resolve a conflict, ask instead of guessing.
- Don't change the global git config (`user.name`, `user.email`, `core.*`).
- Don't fight line endings. `.gitattributes` handles them, so don't commit diffs that only change CRLF/LF.
- Don't use interactive commands (`rebase -i`, `add -i`, `add -p`). They don't work in this environment.

## Checklist before saying "done"
1. You're on a feature branch, not `main`.
2. Every new file is staged and committed, and nothing unrelated was swept in.
3. `git status` shows only the user's own pre-existing changes, if any.
4. Commit messages are concise and descriptive.
5. Nothing was pushed, and the user has the push command.
