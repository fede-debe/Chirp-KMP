# Branching Workflow

How to name branches so the `notion-task-sync.yml` and `pr-sync-claude.yml` workflows do the right thing. This file documents the **workflow** — branch naming, splitting work, sub-branching. Automation internals live in `.github/workflows/`.

---

## TL;DR

```
<type>/<feature-slug>--<task-slug>
```

- `type` ∈ `feature`, `bug`, `improve` — anything else is ignored by Notion sync
- `feature-slug` — kebab-case, must match an existing Feature name in the Notion Features DB (after normalization)
- `--` — literal double dash
- `task-slug` — kebab-case, describes the change, ≤ 5 words

**Before pushing:** the Feature must already exist in Notion. If it doesn't, `notion-task-sync` fails with `core.setFailed('Feature "<n>" not found in Notion. Create it first, then re-run.')`.

---

## What the automation actually does

| Event | Workflow | Result |
|---|---|---|
| Push to `feature/**`, `bug/**`, `improve/**` | `notion-task-sync.yml` | Creates a Notion Task (`🔨 In Progress`), inherits Platform + Projects from Feature |
| PR opened / reopened → `main` | `pr-sync-claude.yml` | Task → `👀 In Review`; Claude Sonnet 4.5 writes the PR body from the diff; Goal field synced (first 500 chars) |
| PR merged → `main` | `pr-sync-claude.yml` | Task → `✅ Done` |
| PR closed without merge → `main` | `pr-sync-claude.yml` | Task → `📦 Archived` |
| PR opened / merged to `feature/**` | _none_ | **Not tracked.** See "Sub-branching" below. |

**You don't write PR descriptions.** `pr-sync-claude.yml` generates them. Just open the PR with an empty or placeholder body.

**Branch types outside the 3 tracked ones** (`fix/`, `chore/`, `docs/`, etc.) still work for code — they just don't create Notion Tasks and get labelled `Chore/infra` in the PR sync logs. Use them for things that don't need Notion tracking (fixing the automation itself, trivial cleanup).

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

If unsure which Feature your branch will match, open Notion → Tasks DB → `Branch` column: pick the slug used by existing Tasks on the same Feature.

---

## Default workflow: flat branches to main

This is what every merged PR in Chirp (#19–#23) has used. Use this unless you have a specific reason not to.

### 1. Create the Feature in Notion (if new)

- Projects Hub → Features DB → New row
- `Name`, `Project`, `Platform`, `Priority`, `Status = Backlog`

### 2. Cut the branch from main

```bash
git checkout main
git pull
git checkout -b feature/architecture-setup--version-catalog
```

### 3. Push to trigger the automation

```bash
git commit --allow-empty -m "chore: start version catalog"
git push -u origin feature/architecture-setup--version-catalog
```

`notion-task-sync` runs → Task appears in Notion with `🔨 In Progress`, linked to the Feature, Platform + Projects inherited.

### 4. Work, commit, push

Normal git. Further pushes to the same branch don't re-create the Task (workflow is idempotent — it checks `Branch` field).

### 5. Open the PR to main

```bash
gh pr create --base main --fill
```

Leave the body empty or minimal. `pr-sync-claude` will overwrite it with a generated `## What / ## Changes / ## Notes` description. Task flips to `👀 In Review`.

### 6. Squash merge

Task flips to `✅ Done`. Delete remote branch. Stale `✅ Done` tasks can be flipped to `📦 Archived` manually when the context is no longer useful.

---

## Big features: still flat, same slug

Module 6 Authentication (37 lectures) and Module 7 Chat (78 lectures) are too big for one PR. The cleanest way to handle these **with the current automation** is multiple flat branches sharing the same feature slug.

```
Feature in Notion: 🔐 Auth & Users

Branches (each a separate PR to main, each its own Notion Task):
  feature/auth-users--login-screen
  feature/auth-users--signup-flow
  feature/auth-users--password-reset
  feature/auth-users--session-management
  feature/auth-users--token-refresh
  feature/auth-users--logout
```

All Tasks link back to the one Feature. In Notion the Feature row's `Tasks` relation grows as you ship. The Feature Status is manual — flip it to Done yourself once the last sub-task ships.

### Why not integration branches?

`copilot-instructions.md` says sub-branches should merge into a parent `feature/*` branch, but `pr-sync-claude.yml` only runs on PRs to `main`. So if `feature/auth-users--login-screen` PRs into `feature/auth-users` (not main), the status never transitions off `🔨 In Progress`. There's a real gap between the stated policy and the implementation.

If you do want integration branches later, see "Upgrade path" at the bottom.

---

## Keeping a long-running branch in sync with main

When main advances while you're mid-feature:

```bash
git checkout feature/auth-users--login-screen
git fetch origin
git merge origin/main      # or: git rebase origin/main
git push
```

Merge is what the existing history uses (see commit `3b6367e | Merge branch 'main' into feature/architecture-setup--end-to-end-test`). Rebase is fine too, just force-push with `--force-with-lease` afterwards.

---

## Checklist before pushing

- [ ] Feature exists in Notion (correct `Name`, `Project`, `Platform`)
- [ ] Branch type is `feature`, `bug`, or `improve` (not `fix` — those aren't tracked)
- [ ] Shape is `<type>/<feature-slug>--<task-slug>`
- [ ] `--` (double dash) present between feature and task
- [ ] Feature slug, after lowercasing and replacing `-` with space, matches a Feature name in Notion
- [ ] Task slug ≤ 5 words
- [ ] Branch cut from up-to-date `main`

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Workflow fails: `Feature "<n>" not found in Notion` | Slug doesn't match any Feature (check workflow logs for the list of available Features) | Create the Feature in Notion, or rename the branch, then push again |
| No Notion Task appeared, no workflow run | Branch prefix isn't `feature/`, `bug/`, or `improve/` | Rename the branch |
| No Notion Task appeared, workflow ran but logged "does not follow convention" | Missing `--` between feature and task | Rename the branch |
| Task has wrong Platform / Project | Feature in Notion has wrong Platform / Project | Fix the Feature in Notion — the Task was snapshotted at create time and won't auto-re-sync, so either fix manually or re-create the Task |
| `Goal` field is truncated mid-sentence | Claude's PR description was >500 chars | Expected — truncation is in the workflow (PR #22 added it to avoid Notion API errors) |
| PR description is wrong / missing | Claude API call failed | Re-open the PR (close + reopen) to re-trigger `pr-sync-claude`; or edit the PR body manually |

---

## Chirp course mapping

The existing `settings.gradle.kts` already includes `:feature:auth:*` and `:feature:chat:*` modules, so the Notion Features mirror that:

| Course module | Notion Feature | Branch pattern |
|---|---|---|
| 3 — Architecture Theory | _none_ | theory only, no branches |
| 4 — Gradle & Multi-Module Setup | `Architecture setup` (exists) | ~6 flat branches |
| 5 — Project-Wide Utility | create `Project-wide utility` | ~3 flat branches |
| 6 — Authentication | `🔐 Auth & Users` (exists) | ~8 flat branches |
| 7 — Chat | create `Chat` | ~12 flat branches |

### Module 4 suggested split

Maps to the actual modules in `settings.gradle.kts` and `build-logic/`:

```
feature/architecture-setup--version-catalog        # gradle/libs.versions.toml
feature/architecture-setup--kmp-module-structure   # composeApp + core:* + feature:*
feature/architecture-setup--build-logic-module     # build-logic include
feature/architecture-setup--android-conventions    # android-application + android-application-compose plugins
feature/architecture-setup--kmp-library-convention # kmp-library + cmp-feature plugins
feature/architecture-setup--buildkonfig            # BuildKonfig plugin
```

Rule of thumb: 1 branch ≈ 1–3 lectures, one clear deliverable, squash merge.

---

## Applying to new projects

The convention is project-agnostic. For each new project:

1. Create the Project row in Projects Hub
2. Copy `.github/workflows/notion-task-sync.yml` and `.github/workflows/pr-sync-claude.yml` from Chirp
3. Set repo secrets: `NOTION_TOKEN`, `NOTION_TASKS_DB_ID`, `NOTION_FEATURES_DB_ID`, `ANTHROPIC_API_KEY`
4. Seed 4–6 top-level Features in Notion: `Architecture setup`, `Infrastructure`, plus 2–4 domain-specific ones
5. First branch is always `feature/architecture-setup--project-setup`
6. Copy this file to `docs/BRANCHING.md`

---

## Upgrade path: wire up integration branches

Only needed if you want `copilot-instructions.md`'s sub-branching policy (sub-branches → parent `feature/*` → main) to actually work end-to-end with Notion.

Changes needed to `.github/workflows/pr-sync-claude.yml`:

1. Expand the trigger:
   ```yaml
   on:
     pull_request:
       branches: [ main, 'feature/**', 'bug/**', 'improve/**' ]
       types: [ opened, reopened, closed ]
   ```

2. Decide the semantic for sub-PR → parent merges — options:
   - Same as main merge: Task → `✅ Done` (simplest; what individual sub-tasks deserve)
   - New intermediate status: Task → `🧪 On Integration` (more accurate; needs the status added to the Notion DB)

3. Leave the integration branch itself untracked (no `--` in name = no Task), or name it `feature/auth-users--integration` so it gets its own umbrella Task that flips Done only when the final PR merges to main.

Don't do this until you actually need it for a real feature. For the course, flat branches are enough.
