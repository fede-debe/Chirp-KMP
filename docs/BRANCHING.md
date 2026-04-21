# Branching Workflow

How to name branches so the `notion-task-sync.yml` and `pr-sync-claude.yml` workflows do the right thing. This file documents the **workflow** — branch naming, splitting work, sub-branching. Automation internals live in `.github/workflows/`.

---

## Quick start

Every project, every time. Two patterns: simple (one PR) and nested (big feature with sub-tasks).

### Pattern A — simple change, one PR to main

Use when the whole Feature is one PR. Small scope, single concern.

```
git checkout main && git pull
git checkout -b feature/<feature-slug>--<task-slug>
# ...work, commit, push...
gh pr create --base main --fill
```

### Pattern B — big feature with sub-branches

Use for Features too big for one PR (Auth, Chat, anything multi-lecture on the course).

**Structure:**
```
main
 └── feature/<feature-slug>                       ← parent branch, cut from main
      ├── feature/<feature-slug>--<task-a>       ← sub-branch, cut from parent
      ├── feature/<feature-slug>--<task-b>       ← sub-branch, cut from parent
      └── feature/<feature-slug>--<task-c>       ← sub-branch, cut from parent
```

**Step 1 — Feature must exist in Notion, with a Slug**

Projects Hub → Features DB. Find or create the Feature:
- `Name` — human-readable (`🔐 Auth & Users`)
- `Slug` — unique, lowercase kebab, project-prefixed (`chirp-auth-users`)
- `Project`, `Platform`, `Priority`, `Status = Backlog`

The `Slug` is what the workflow matches branches against. Not the `Name`. Two Features can share a Name (`🔐 Auth & Users` for Chirp and PetApp), but **Slug must be globally unique** across the whole Features DB.

**Step 2 — Create the parent branch** (once per feature)

```
git checkout main && git pull
git checkout -b feature/<feature-slug>
git push -u origin feature/<feature-slug>
```

No `--` in the name = no Notion Task. The parent is just a git grouping.

**Step 3 — Create a sub-branch from the parent**

```
git checkout feature/<feature-slug>
git pull
git checkout -b feature/<feature-slug>--<task-slug>
git commit --allow-empty -m "chore: start <task-slug>"
git push -u origin feature/<feature-slug>--<task-slug>
```

`notion-task-sync` queries the Features DB for `Slug = <feature-slug>` exact match, then creates a Task linked to that Feature. Platform + Project inherited.

**Step 4 — Work, commit, push, open sub-PR into parent**

```
gh pr create --base feature/<feature-slug> --fill
```

Task → `👀 In Review`. Claude writes the PR body. Merge on GitHub. Task → `✅ Done`.

**Step 5 — Repeat Steps 3–4 for each sub-task.**

**Step 6 — Ship parent to main when all sub-tasks are merged**

```
git checkout feature/<feature-slug>
git pull
gh pr create --base main --fill
```

Merge on GitHub. Cleanup:

```
git checkout main && git pull
git branch -D feature/<feature-slug>
git push origin --delete feature/<feature-slug>
```

---

### Branch naming rules

- **Type:** `feature` · `bug` · `improve` — NOT `fix` (not tracked)
- **Feature slug:** must **exactly match** the `Slug` property of a Feature in Notion
- **Task slug:** ≤ 5 words, kebab-case
- **`--`** literal double dash between feature-slug and task-slug (sub-branches only)
- **Parent branch:** `<type>/<feature-slug>` with no `--` and no task slug

### Slug naming convention

Project-prefix everything so every slug is globally unique:

```
<project-short>-<feature-kebab>
```

Examples:
| Notion Feature Name | Project | Slug |
|---|---|---|
| `🔐 Auth & Users` | Chirp | `chirp-auth-users` |
| `🔐 Auth & Users` | PetApp | `petapp-auth-users` |
| `🔺 Architecture setup` | Chirp | `chirp-architecture-setup` |
| `📊 Home Dashboard` | PetApp | `petapp-home-dashboard` |
| `🏗 Infrastructure` | Infrastructure (shared) | `infrastructure` |

Shared / cross-project Features (like `Infrastructure`) can skip the prefix if they're the only one with that slug. Everything else gets the project prefix.

---

## What the automation actually does

| Event | Workflow | Result |
|---|---|---|
| Push to `feature/**`, `bug/**`, `improve/**` with `--` in name | `notion-task-sync.yml` | Queries Features DB for `Slug = <feature-slug>` exact. Creates Task (`🔨 In Progress`), inherits Platform + Projects. Fails loudly if slug not found or slug not unique. |
| Push to parent branch (`feature/<slug>` without `--`) | `notion-task-sync.yml` | Logs `does not follow convention`, skips (correct) |
| PR opened / reopened — any target (main, parent branch) | `pr-sync-claude.yml` | If branch has a Notion Task: Status → `👀 In Review`. Claude writes PR body from diff. Goal field synced (first 500 chars). |
| PR merged | `pr-sync-claude.yml` | If branch has a Task: Status → `✅ Done` |
| PR closed without merge | `pr-sync-claude.yml` | If branch has a Task: Status → `📦 Archived` |

**Branch types outside the 3 tracked ones** (`fix/`, `chore/`, `docs/`, etc.) still work for code — they just don't create Notion Tasks. Use them for work that doesn't belong to a Feature (fixing the automation itself, one-off cleanup, docs).

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Workflow fails: `Feature with Slug "<x>" not found in Notion` | The branch's feature-slug doesn't match any Feature's Slug property (check logs for the list of available slugs) | Add/fix the Slug on the Feature in Notion, or rename the branch |
| Workflow fails: `Slug "<x>" is not unique in Notion` | Two or more Features share the same Slug | Open the Features DB, make slugs unique, re-push |
| No Notion Task appeared, no workflow run | Branch prefix isn't `feature/`, `bug/`, or `improve/` | Rename the branch |
| No Notion Task appeared, workflow logged "does not follow convention" | Missing `--` between feature and task | If this is a sub-branch: rename. If it's a parent branch: expected, no action. |
| Task has wrong Platform / Project | Feature in Notion has wrong Platform / Project | Fix the Feature — Task was snapshotted at create time and won't auto-re-sync. Fix Task manually or re-create it. |
| Sub-PR opened but Task stuck at `🔨 In Progress` | `pr-sync-claude.yml` trigger doesn't include the base branch pattern | Check `on: pull_request: branches:` includes `'feature/**'`, `'bug/**'`, `'improve/**'` |
| `Goal` field truncated mid-sentence | Claude's PR description was >500 chars | Expected — truncation is in the workflow to avoid Notion API errors |
| PR description missing, logs `authentication_error: invalid x-api-key` | `ANTHROPIC_API_KEY` secret is expired, revoked, or out of credits | Rotate the key, update the GitHub Secret, close + reopen the PR to re-trigger |

---

## Chirp course mapping

`settings.gradle.kts` already includes `:feature:auth:*` and `:feature:chat:*` modules. Notion Features mirror that:

| Course module | Feature Name | Slug | Pattern |
|---|---|---|---|
| 3 — Architecture Theory | _none_ | — | theory only |
| 4 — Gradle & Multi-Module Setup | `🔺 Architecture setup` (exists) | `chirp-architecture-setup` | **B**: parent + ~6 sub-branches |
| 5 — Project-Wide Utility | create `Project-wide utility` | `chirp-project-wide-utility` | **B**: parent + ~3 sub-branches |
| 6 — Authentication | `🔐 Auth & Users` (exists) | `chirp-auth-users` | **B**: parent + ~8 sub-branches |
| 7 — Chat | create `Chat` | `chirp-chat` | **B**: parent + ~12 sub-branches |

### Module 4 suggested split

```
feature/chirp-architecture-setup                                  # parent
 ├── feature/chirp-architecture-setup--version-catalog
 ├── feature/chirp-architecture-setup--kmp-module-structure
 ├── feature/chirp-architecture-setup--build-logic-module
 ├── feature/chirp-architecture-setup--android-conventions
 ├── feature/chirp-architecture-setup--kmp-library-convention
 └── feature/chirp-architecture-setup--buildkonfig
```

Rule of thumb: 1 sub-branch ≈ 1–3 lectures, one clear deliverable, squash merge into parent.

---

## Applying to new projects (PetApp and future)

The convention is project-agnostic. For each new project:

1. Create the Project row in Projects Hub
2. Copy `.github/workflows/notion-task-sync.yml` and `.github/workflows/pr-sync-claude.yml` from Chirp
3. Set repo secrets: `NOTION_TOKEN`, `NOTION_TASKS_DB_ID`, `NOTION_FEATURES_DB_ID`, `ANTHROPIC_API_KEY`
4. Seed 4–6 top-level Features in Notion with project-prefixed slugs:
   - `<project>-architecture-setup`
   - `<project>-auth-users`
   - `<project>-<domain-feature-1>`, etc.
5. First branch is always `feature/<project>-architecture-setup--project-setup`
6. Copy `docs/BRANCHING.md` and `docs/GIT_CHEATSHEET.md`

The **Quick start** at the top is what you'll use day-to-day. Everything below is reference.
