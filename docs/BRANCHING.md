# Branching Workflow

How to name branches so the `notion-task-sync.yml` and `pr-sync-claude.yml` workflows do the right thing. This file documents the **workflow** — branch naming, splitting work, sub-branching. Automation internals live in `.github/workflows/`.

---

## Quick start

Every project, every time. Two patterns: simple (one PR) and nested (big feature with sub-tasks).

### Pattern A — simple change, one PR to main

Use when the whole Feature is one PR. Small scope, single concern.

```
git checkout main && git pull
git checkout -b feature/infrastructure--update-ci-badge
# ...work, commit...
git push -u origin feature/infrastructure--update-ci-badge
gh pr create --base main --fill
# squash merge on GitHub
```

### Pattern B — big feature with sub-branches

Use for Features too big for one PR (Auth, Chat, anything multi-lecture on the course).

**Structure:**
```
main
 └── feature/auth-users                        ← parent branch, cut from main
      ├── feature/auth-users--login-screen    ← sub-branch, cut from parent
      ├── feature/auth-users--signup-flow     ← sub-branch, cut from parent
      └── feature/auth-users--session-mgmt    ← sub-branch, cut from parent
```

**Step 1 — Feature must exist in Notion**

Open Projects Hub → Features DB. Create the Feature (e.g. `🔐 Auth & Users`) if it doesn't exist. Set `Name`, `Project`, `Platform`, `Priority`, `Status = Backlog`.

**Step 2 — Create the parent branch** (once per feature)

```
git checkout main && git pull
git checkout -b feature/auth-users
git push -u origin feature/auth-users
```

No `--` in the name = no Notion Task created (correct — the parent is just a git grouping). Feature progress is tracked via its sub-Tasks.

**Step 3 — Create a sub-branch from the parent**

```
git checkout feature/auth-users
git pull
git checkout -b feature/auth-users--login-screen
git commit --allow-empty -m "chore: start login screen"
git push -u origin feature/auth-users--login-screen
```

`notion-task-sync` fires → Task created in Notion: `🔨 In Progress`, linked to `🔐 Auth & Users`, Platform + Project inherited.

**Step 4 — Work, commit, push. When ready, open sub-PR into parent**

```
gh pr create --base feature/auth-users --fill
```

Note the `--base feature/auth-users` (not `main`). `pr-sync-claude` fires → writes the PR body → Task flips to `👀 In Review`.

**Step 5 — Squash merge the sub-PR on GitHub**

Task flips to `✅ Done`. Delete the sub-branch.

**Step 6 — Repeat Steps 3–5 for each sub-task.**

Rebase or merge the parent onto main occasionally to stay current:
```
git checkout feature/auth-users
git fetch origin
git merge origin/main
git push
```

**Step 7 — When all sub-tasks are merged, ship the parent to main**

```
gh pr create --base main --fill
```

Parent PR gets a Claude-generated description (covering all the accumulated work). No Notion Task exists for the parent — that's correct, the Feature's own Status in Notion is what you flip manually to `Done`.

Merge method for the parent PR: **merge commit** (not squash) so individual sub-Task commits are preserved in `main` history. Or squash if you prefer a linear history — your call.

---

### Branch naming rules (both patterns)

- **Type:** `feature` · `bug` · `improve` — NOT `fix` (not tracked)
- **Feature slug:** Notion Feature name, lowercase, spaces → `-`, emoji stripped (`🔐 Auth & Users` → `auth-users`)
- **Task slug:** ≤ 5 words, kebab-case
- **`--`** literal double dash between feature and task (sub-branches only)
- **Parent branch:** `<type>/<feature-slug>` with no `--` and no task slug

**The 3 things that silently skip Notion Task creation:**
- Feature doesn't exist in Notion
- Wrong prefix (`fix/`, `chore/`, `docs/`)
- Missing `--` (this is *intentional* for parent branches)

---

## What the automation actually does

| Event | Workflow | Result |
|---|---|---|
| Push to `feature/**`, `bug/**`, `improve/**` with `--` in name | `notion-task-sync.yml` | Creates Notion Task (`🔨 In Progress`), inherits Platform + Projects from Feature |
| Push to parent branch (`feature/<slug>` without `--`) | `notion-task-sync.yml` | Logs `does not follow convention`, skips (correct) |
| PR opened / reopened — any target (main, parent branch, etc.) | `pr-sync-claude.yml` | If branch has a Notion Task: Status → `👀 In Review`. Claude writes PR body from diff against base branch. Goal field synced (first 500 chars). |
| PR merged | `pr-sync-claude.yml` | If branch has a Task: Status → `✅ Done` |
| PR closed without merge | `pr-sync-claude.yml` | If branch has a Task: Status → `📦 Archived` |
| Parent PR (`feature/<slug>` → main) | `pr-sync-claude.yml` | Claude writes description. No Notion Task update (parent has no Task). |

**Branch types outside the 3 tracked ones** (`fix/`, `chore/`, `docs/`, etc.) still work for code — they just don't create Notion Tasks and get labelled `Chore/infra` in the PR sync logs. Use them for work that doesn't need Notion tracking (fixing the automation itself, one-off cleanup, docs).

---

## Feature slug ↔ Notion Feature matching

The workflow normalizes both sides before matching:

```
lowercased, - and _ → space, non-alphanumeric stripped,
spaces collapsed, trimmed — then matched with === or substring either way
```

| Notion Feature | Normalized | Slug to use |
|---|---|---|
| `Architecture setup` | `architecture setup` | `architecture-setup` |
| `🔐 Auth & Users` | `auth users` | `auth-users` |
| `Infrastructure` | `infrastructure` | `infrastructure` |
| `📊 Home Dashboard` | `home dashboard` | `home-dashboard` |

If unsure which Feature your branch will match, open Notion → Tasks DB → `Branch` column and copy the slug from existing Tasks on the same Feature.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Workflow fails: `Feature "<n>" not found in Notion` | Slug doesn't match any Feature (check workflow logs for the list of available Features) | Create the Feature in Notion, or rename the branch, then push again |
| No Notion Task appeared, no workflow run | Branch prefix isn't `feature/`, `bug/`, or `improve/` | Rename the branch |
| No Notion Task appeared, workflow ran but logged "does not follow convention" | Missing `--` between feature and task | If this is a sub-branch: rename. If it's a parent branch: expected, no action needed. |
| Task has wrong Platform / Project | Feature in Notion has wrong Platform / Project | Fix the Feature in Notion — the Task was snapshotted at create time and won't auto-re-sync, so either fix the Task manually or re-create it |
| Sub-PR opened but Task stuck at `🔨 In Progress` | `pr-sync-claude.yml` trigger doesn't include the base branch pattern | Check `on: pull_request: branches:` includes `'feature/**'`, `'bug/**'`, `'improve/**'` |
| PR description shows diff against wrong baseline | Older workflow hardcoded `origin/main` as base | Confirm the workflow uses `baseRef` (dynamic base). See `.github/workflows/pr-sync-claude.yml`. |
| `Goal` field truncated mid-sentence | Claude's PR description was >500 chars | Expected — truncation is in the workflow to avoid Notion API errors |
| PR description missing, logs `authentication_error: invalid x-api-key` | `ANTHROPIC_API_KEY` secret is expired, revoked, or out of credits | Rotate the key in console.anthropic.com, update the GitHub Secret, close + reopen the PR to re-trigger |
| PR description missing, other Claude API error | Transient / rate limit | Close + reopen the PR, or edit the body manually |

---

## Chirp course mapping

The existing `settings.gradle.kts` already includes `:feature:auth:*` and `:feature:chat:*` modules. Notion Features mirror that:

| Course module | Notion Feature | Pattern |
|---|---|---|
| 3 — Architecture Theory | _none_ | theory only, no branches |
| 4 — Gradle & Multi-Module Setup | `Architecture setup` (exists) | **B** (nested): parent `feature/architecture-setup` + 6 sub-branches |
| 5 — Project-Wide Utility | create `Project-wide utility` | **B**: parent + 3 sub-branches |
| 6 — Authentication | `🔐 Auth & Users` (exists) | **B**: parent + ~8 sub-branches |
| 7 — Chat | create `Chat` | **B**: parent + ~12 sub-branches |

### Module 4 suggested split

```
feature/architecture-setup                                 # parent
 ├── feature/architecture-setup--version-catalog          # gradle/libs.versions.toml
 ├── feature/architecture-setup--kmp-module-structure     # composeApp + core:* + feature:*
 ├── feature/architecture-setup--build-logic-module       # build-logic include
 ├── feature/architecture-setup--android-conventions      # android-application + compose plugins
 ├── feature/architecture-setup--kmp-library-convention   # kmp-library + cmp-feature plugins
 └── feature/architecture-setup--buildkonfig              # BuildKonfig plugin
```

Rule of thumb: 1 sub-branch ≈ 1–3 lectures, one clear deliverable, squash merge into parent.

---

## Applying to new projects (PetApp and future)

The convention is project-agnostic. For each new project:

1. Create the Project row in Projects Hub
2. Copy `.github/workflows/notion-task-sync.yml` and `.github/workflows/pr-sync-claude.yml` from Chirp
3. Set repo secrets: `NOTION_TOKEN`, `NOTION_TASKS_DB_ID`, `NOTION_FEATURES_DB_ID`, `ANTHROPIC_API_KEY`
4. Seed 4–6 top-level Features in Notion: `Architecture setup`, `Infrastructure`, plus 2–4 domain-specific ones
5. First branch is always `feature/architecture-setup--project-setup` (or a parent `feature/architecture-setup` + sub-branches for bigger setups)
6. Copy this file to `docs/BRANCHING.md`

The **Quick start** at the top is what you'll use day-to-day. Everything below it is reference.
