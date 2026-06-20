# Simplify Module Structure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce the project from 12 Gradle modules to 6 by merging related modules, with zero changes to Kotlin source code or package names.

**Architecture:** Pure Gradle boundary changes — files move between module directories, package declarations inside `.kt` files stay identical, no import statements change (except a targeted compose.resources package fix in Task 5). Each merge is one atomic commit so git bisect can identify any regression. Old module directories are deleted only after the new ones build clean.

**Tech Stack:** Kotlin Multiplatform, Gradle Kotlin DSL, Compose Multiplatform, Convention Plugins

**Module map:**
```
BEFORE (12)                          AFTER (6)
────────────────────────────────     ────────────────────────────────
:androidApp                     →    :androidApp             (unchanged)
:composeApp                     →    :composeApp             (deps updated)
:core:domain    ─┐              →    :core:shared            (new)
:core:data      ─┘
:core:designsystem ─┐           →    :core:ui                (new)
:core:presentation  ─┘
:feature:auth:domain       ─┐   →    :feature:auth           (new)
:feature:auth:presentation ─┘
:feature:chat:domain       ─┐
:feature:chat:data          │   →    :feature:chat           (new)
:feature:chat:database      │
:feature:chat:presentation ─┘
```

---

## Task 1: Setup — branch + stale worktree cleanup

**Files:**
- Delete: `.claude/worktrees/upbeat-kepler-d7bdf0/` (stale worktree, safe to remove)

- [ ] **Step 1: Switch to main and pull**
```bash
git checkout main
git pull
```
Expected: `Already up to date.` or shows new commits from remote.

- [ ] **Step 2: Create feature branch**
```bash
git checkout -b feature/simplify-module-structure
```
Expected: `Switched to a new branch 'feature/simplify-module-structure'`

- [ ] **Step 3: Remove stale worktree**
```bash
git worktree remove --force .claude/worktrees/upbeat-kepler-d7bdf0
```
Expected: no output. If it errors with "not a worktree", run `rm -rf .claude/worktrees/upbeat-kepler-d7bdf0` instead.

- [ ] **Step 4: Verify clean state**
```bash
git status
```
Expected: `nothing to commit, working tree clean`

---

## Task 2: Merge `:feature:auth:domain` + `:feature:auth:presentation` → `:feature:auth`

**Files:**
- Create: `feature/auth/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `composeApp/build.gradle.kts`
- Delete (after verification): `feature/auth/domain/` and `feature/auth/presentation/`

### Why this first
`feature:auth` has zero dependents (only `composeApp` uses it), and the auth modules have no internal cross-dependencies with the chat modules. Safest first merge.

### What moves where
```
feature/auth/domain/src/commonMain/kotlin/com/project/auth/domain/
  └── EmailValidator.kt

feature/auth/presentation/src/commonMain/kotlin/com/project/auth/presentation/
  └── di/, navigation/, ui/

feature/auth/presentation/src/commonMain/composeResources/
  └── (strings, drawable resources)

→ ALL of the above go into feature/auth/src/
  (no platform actuals exist for auth — no androidMain/iosMain/desktopMain)
```

- [ ] **Step 1: Create the new module directory structure**
```bash
mkdir -p feature/auth/src/commonMain/kotlin
```

- [ ] **Step 2: Move EmailValidator from auth:domain**
```bash
rsync -a feature/auth/domain/src/commonMain/kotlin/ feature/auth/src/commonMain/kotlin/
```

- [ ] **Step 3: Move all of auth:presentation**
```bash
rsync -a feature/auth/presentation/src/ feature/auth/src/
```
This merges both `commonMain` trees. Since packages are different (`auth.domain` vs `auth.presentation`), no file collisions occur.

- [ ] **Step 4: Create `feature/auth/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    androidLibrary {
        namespace = "com.project.feature.auth"
        compileSdk = 36
        minSdk = 26

        androidResources {
            enable = true
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.core.domain)
                implementation(projects.core.designsystem)
                implementation(projects.core.presentation)
                implementation(libs.bundles.koin.common)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.project.auth.presentation"
}
```

> **Note:** `projects.core.domain`, `projects.core.designsystem`, `projects.core.presentation` are referenced here because the core module merges have not happened yet. After Tasks 4 and 5 these will change to `projects.core.shared` and `projects.core.ui`. The plan covers that update in Task 4 Step 4 and Task 5 Step 5.
>
> The `projects.feature.auth.domain` dependency is gone — EmailValidator now lives in this same module.

- [ ] **Step 5: Update `settings.gradle.kts` — replace old auth entries with new one**

Remove these two lines:
```kotlin
include(":feature:auth:presentation")
include(":feature:auth:domain")
```
Add this line in their place:
```kotlin
include(":feature:auth")
```

Full updated include block (replace the entire include section):
```kotlin
include(":composeApp")
include(":androidApp")
include(":core:presentation")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:auth")
include(":feature:chat:presentation")
include(":feature:chat:domain")
include(":feature:chat:data")
include(":feature:chat:database")
```

- [ ] **Step 6: Update `composeApp/build.gradle.kts` — replace auth sub-module refs**

In `composeApp/build.gradle.kts`, find:
```kotlin
implementation(projects.feature.auth.domain)
implementation(projects.feature.auth.presentation)
```
Replace with:
```kotlin
implementation(projects.feature.auth)
```

- [ ] **Step 7: Update `composeApp/src/commonMain/kotlin/com/project/chirp/di/initKoin.kt`**

The import `com.project.auth.presentation.di.authPresentationModule` stays identical — the package name didn't change, only the Gradle module boundary changed. **No edit needed here.**

Verify by reading the file and confirming no references to `:feature:auth:domain` or `:feature:auth:presentation` remain in any source file:
```bash
grep -r "feature.auth.domain\|feature.auth.presentation" composeApp/src/ feature/ core/ --include="*.kt"
```
Expected: no results (Gradle project accessors don't appear in `.kt` files, only in `.gradle.kts` files).

- [ ] **Step 8: Gradle sync — verify no red errors in the IDE or run:**
```bash
./gradlew :feature:auth:assemble
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Build Android to verify auth screens still work**
```bash
./gradlew :androidApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 10: Delete old auth module directories**
```bash
rm -rf feature/auth/domain
rm -rf feature/auth/presentation
```

- [ ] **Step 11: Commit**
```bash
git add -A
git commit -m "refactor: merge feature:auth:domain + feature:auth:presentation into :feature:auth"
```

---

## Task 3: Merge 4 chat modules → `:feature:chat`

**Files:**
- Create: `feature/chat/build.gradle.kts`
- Create: `feature/chat/src/nativeInterop/cinterop/network.def` (moved from chat:data)
- Modify: `settings.gradle.kts`
- Modify: `composeApp/build.gradle.kts`
- Delete (after verification): `feature/chat/domain/`, `feature/chat/data/`, `feature/chat/database/`, `feature/chat/presentation/`

### What moves where
```
feature/chat/domain/src/       → feature/chat/src/        (models + interfaces)
feature/chat/data/src/         → feature/chat/src/        (repositories, services, DTOs)
feature/chat/database/src/     → feature/chat/src/        (Room entities, DAOs)
feature/chat/presentation/src/ → feature/chat/src/        (screens, ViewModels, mappers)

feature/chat/data/src/nativeInterop/ → feature/chat/src/nativeInterop/

Packages: com.project.chat.domain.*, com.project.chat.data.*, 
          com.project.chat.database.*, com.project.chat.presentation.*
          All UNCHANGED.
```

No file name collisions — all four modules use different package roots.

- [ ] **Step 1: Merge all source sets from the four modules**
```bash
rsync -a feature/chat/domain/src/    feature/chat/src/
rsync -a feature/chat/data/src/      feature/chat/src/
rsync -a feature/chat/database/src/  feature/chat/src/
rsync -a feature/chat/presentation/src/ feature/chat/src/
```

- [ ] **Step 2: Move the cinterop definition file**

The `feature/chat/data/build.gradle.kts` had `defFile(file("src/nativeInterop/cinterop/network.def"))`. After the rsync above, this file already lives at `feature/chat/src/nativeInterop/cinterop/network.def`. Verify:
```bash
ls feature/chat/src/nativeInterop/cinterop/network.def
```
Expected: file exists.

- [ ] **Step 3: Create `feature/chat/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.convention.cmp.feature)
    alias(libs.plugins.convention.room)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {
    androidLibrary {
        namespace = "com.project.feature.chat"
        compileSdk = 36
        minSdk = 26

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.core.domain)
                implementation(projects.core.designsystem)
                implementation(projects.core.presentation)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
                implementation(libs.material3.adaptive)
                implementation(libs.material3.adaptive.layout)
                implementation(libs.material3.adaptive.navigation)
                implementation(libs.jetbrains.compose.backhandler)
                implementation(libs.kotlinx.datetime)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.android)
                implementation(libs.androidx.lifecycle.process)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging)
            }
        }
    }

    targets.withType<KotlinNativeTarget> {
        compilations.getByName("main") {
            cinterops {
                create("network") {
                    defFile(file("src/nativeInterop/cinterop/network.def"))
                }
            }
        }
    }
}
```

> **Note:** References to `projects.core.domain`, `projects.core.designsystem`, `projects.core.presentation` are temporary — they will be updated to `projects.core.shared` and `projects.core.ui` in Tasks 4 and 5. The intra-chat cross-module deps (`projects.feature.chat.domain`, `projects.feature.chat.database`) are gone because everything is in one module.

- [ ] **Step 4: Update `settings.gradle.kts` — replace chat sub-module entries**

Remove:
```kotlin
include(":feature:chat:presentation")
include(":feature:chat:domain")
include(":feature:chat:data")
include(":feature:chat:database")
```
Add:
```kotlin
include(":feature:chat")
```

Full updated include block:
```kotlin
include(":composeApp")
include(":androidApp")
include(":core:presentation")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":feature:auth")
include(":feature:chat")
```

- [ ] **Step 5: Update `composeApp/build.gradle.kts` — replace chat sub-module refs**

Find:
```kotlin
implementation(projects.feature.chat.data)
implementation(projects.feature.chat.domain)
implementation(projects.feature.chat.presentation)
implementation(projects.feature.chat.database)
```
Replace with:
```kotlin
implementation(projects.feature.chat)
```

- [ ] **Step 6: Build Android**
```bash
./gradlew :androidApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Build Desktop**
```bash
./gradlew :composeApp:desktopJar
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Delete old chat module directories**
```bash
rm -rf feature/chat/domain
rm -rf feature/chat/data
rm -rf feature/chat/database
rm -rf feature/chat/presentation
```

- [ ] **Step 9: Commit**
```bash
git add -A
git commit -m "refactor: merge 4 feature:chat:* modules into single :feature:chat"
```

---

## Task 4: Merge `:core:domain` + `:core:data` → `:core:shared`

**Files:**
- Create: `core/shared/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `composeApp/build.gradle.kts`
- Modify: `feature/auth/build.gradle.kts`
- Modify: `feature/chat/build.gradle.kts`
- Delete (after verification): `core/domain/` and `core/data/`

### What moves where
```
core/domain/src/commonMain/   → core/shared/src/commonMain/   (com.project.core.domain.*)
core/domain/src/desktopMain/  → core/shared/src/desktopMain/  (ThemePreference, ThemePreferences)
core/data/src/commonMain/     → core/shared/src/commonMain/   (com.project.core.data.*)
core/data/src/androidMain/    → core/shared/src/androidMain/
core/data/src/iosMain/        → core/shared/src/iosMain/
core/data/src/desktopMain/    → core/shared/src/desktopMain/  (merges with domain desktop files)
```

No collisions — `com.project.core.domain` and `com.project.core.data` are separate packages.

`core:data/build.gradle.kts` had `implementation(projects.core.domain)` — this dep disappears because they're now in the same module.

- [ ] **Step 1: Merge source trees**
```bash
rsync -a core/domain/src/ core/shared/src/
rsync -a core/data/src/   core/shared/src/
```

- [ ] **Step 2: Create `core/shared/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    androidLibrary {
        namespace = "com.project.core.shared"
        compileSdk = 36
        minSdk = 26
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)
                implementation(libs.touchlab.kermit)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.android)
                implementation(libs.ktor.client.okhttp)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        desktopMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}
```

> The `projects.core.domain` dep that existed in `core:data` is intentionally omitted — those types now live in the same module.

- [ ] **Step 3: Update `settings.gradle.kts`**

Remove:
```kotlin
include(":core:domain")
include(":core:data")
```
Add:
```kotlin
include(":core:shared")
```

Full updated include block:
```kotlin
include(":composeApp")
include(":androidApp")
include(":core:presentation")
include(":core:designsystem")
include(":core:shared")
include(":feature:auth")
include(":feature:chat")
```

- [ ] **Step 4: Update `feature/auth/build.gradle.kts`**

Replace:
```kotlin
implementation(projects.core.domain)
implementation(projects.core.designsystem)
implementation(projects.core.presentation)
```
With:
```kotlin
implementation(projects.core.shared)
implementation(projects.core.designsystem)
implementation(projects.core.presentation)
```
(`core.designsystem` and `core.presentation` will become `core.ui` after Task 5 — keep them here for now.)

- [ ] **Step 5: Update `feature/chat/build.gradle.kts`**

Replace:
```kotlin
implementation(projects.core.domain)
implementation(projects.core.designsystem)
implementation(projects.core.presentation)
```
With:
```kotlin
implementation(projects.core.shared)
implementation(projects.core.designsystem)
implementation(projects.core.presentation)
```

- [ ] **Step 6: Update `composeApp/build.gradle.kts`**

Replace:
```kotlin
implementation(projects.core.data)
implementation(projects.core.domain)
```
With:
```kotlin
implementation(projects.core.shared)
```

Leave `projects.core.designsystem` and `projects.core.presentation` untouched for now.

- [ ] **Step 7: Build Android**
```bash
./gradlew :androidApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Build Desktop**
```bash
./gradlew :composeApp:desktopJar
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Delete old core module directories**
```bash
rm -rf core/domain
rm -rf core/data
```

- [ ] **Step 10: Commit**
```bash
git add -A
git commit -m "refactor: merge :core:domain + :core:data into :core:shared"
```

---

## Task 5: Merge `:core:designsystem` + `:core:presentation` → `:core:ui`

**Files:**
- Create: `core/ui/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `build-logic/convention/src/main/kotlin/CmpFeatureConventionPlugin.kt`
- Modify: `composeApp/build.gradle.kts`
- Modify: `feature/auth/build.gradle.kts`
- Modify: `feature/chat/build.gradle.kts`
- Modify: a small set of files inside `core/presentation/` that import `Res` from their own package (covered in Step 5)
- Delete (after verification): `core/designsystem/` and `core/presentation/`

### What moves where
```
core/designsystem/src/commonMain/  → core/ui/src/commonMain/   (com.project.core.designSystem.*)
core/designsystem/src/androidMain/ → core/ui/src/androidMain/  (preview files)
core/presentation/src/commonMain/  → core/ui/src/commonMain/   (com.project.core.presentation.*)
core/presentation/src/desktopMain/ → core/ui/src/desktopMain/
core/presentation/src/mobileMain/  → core/ui/src/mobileMain/   (PermissionController actuals)
```

No package collisions — `com.project.core.designSystem` ≠ `com.project.core.presentation`.

### The compose.resources package
`core:designsystem` used `packageOfResClass = "com.project.core.designsystem"` (public).
`core:presentation` used `packageOfResClass = "com.project.core.presentation"` (private).

The merged module `core:ui` can only have ONE `Res` class. We use `"com.project.core.ui"` as the new unified package. This requires a targeted find-replace in files that imported `Res` from either old package.

- [ ] **Step 1: Find all files that import from the old resource packages**
```bash
grep -r "com.project.core.designsystem.generated.resources\|com.project.core.presentation.generated.resources" \
     core/ feature/ composeApp/ --include="*.kt" -l
```
Note every file listed — these need import updates in Step 5.

- [ ] **Step 2: Merge source trees**
```bash
rsync -a core/designsystem/src/ core/ui/src/
rsync -a core/presentation/src/  core/ui/src/
```

- [ ] **Step 3: Update compose.resources imports in moved files**

For every file found in Step 1, replace:
- `com.project.core.designsystem.generated.resources` → `com.project.core.ui.generated.resources`
- `com.project.core.presentation.generated.resources` → `com.project.core.ui.generated.resources`

Run the replace across all affected files in the new location:
```bash
find core/ui/src -name "*.kt" -exec sed -i '' \
  's/com\.project\.core\.designsystem\.generated\.resources/com.project.core.ui.generated.resources/g; 
   s/com\.project\.core\.presentation\.generated\.resources/com.project.core.ui.generated.resources/g' {} \;
```

- [ ] **Step 4: Create `core/ui/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.convention.cmp.library)
}

kotlin {
    androidLibrary {
        namespace = "com.project.core.ui"
        compileSdk = 36
        minSdk = 26

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(projects.core.shared)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                implementation(libs.material3.adaptive)
                implementation(libs.jetbrains.lifecycle.compose)
                implementation(libs.bundles.koin.common)
            }
        }

        val mobileMain by getting {
            dependencies {
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.compose)
                implementation(libs.moko.permissions.notifications)
            }
        }

        androidMain {
            dependsOn(mobileMain)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.project.core.ui"
}
```

> `projects.core.presentation` and `projects.core.designsystem` deps from the old `core:designsystem/build.gradle.kts` (`core:designsystem` depended on `core:presentation`) are gone — everything is in one module now.

- [ ] **Step 5: Update `settings.gradle.kts`**

Remove:
```kotlin
include(":core:presentation")
include(":core:designsystem")
```
Add:
```kotlin
include(":core:ui")
```

Final settings.gradle.kts include block:
```kotlin
include(":composeApp")
include(":androidApp")
include(":core:shared")
include(":core:ui")
include(":feature:auth")
include(":feature:chat")
```

- [ ] **Step 6: Update `CmpFeatureConventionPlugin.kt`**

In `build-logic/convention/src/main/kotlin/CmpFeatureConventionPlugin.kt`, replace:
```kotlin
"commonMainImplementation"(project(":core:presentation"))
"commonMainImplementation"(project(":core:designsystem"))
```
With:
```kotlin
"commonMainImplementation"(project(":core:ui"))
```

- [ ] **Step 7: Update `feature/auth/build.gradle.kts`**

Replace:
```kotlin
implementation(projects.core.designsystem)
implementation(projects.core.presentation)
```
With:
```kotlin
// core:ui is now auto-added by CmpFeatureConventionPlugin — no explicit dep needed
```

The final `feature/auth/build.gradle.kts` commonMain dependencies block becomes:
```kotlin
commonMain {
    dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(projects.core.shared)
        implementation(libs.bundles.koin.common)
    }
}
```

- [ ] **Step 8: Update `feature/chat/build.gradle.kts`**

Remove the same two explicit deps:
```kotlin
implementation(projects.core.designsystem)
implementation(projects.core.presentation)
```

The final `feature/chat/build.gradle.kts` commonMain dependencies block becomes:
```kotlin
commonMain {
    dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(projects.core.shared)
        implementation(libs.bundles.ktor.common)
        implementation(libs.koin.core)
        implementation(libs.material3.adaptive)
        implementation(libs.material3.adaptive.layout)
        implementation(libs.material3.adaptive.navigation)
        implementation(libs.jetbrains.compose.backhandler)
        implementation(libs.kotlinx.datetime)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}
```

- [ ] **Step 9: Update `composeApp/build.gradle.kts`**

Replace:
```kotlin
implementation(projects.core.designsystem)
implementation(projects.core.presentation)
```
With:
```kotlin
implementation(projects.core.ui)
```

- [ ] **Step 10: Build Android**
```bash
./gradlew :androidApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

If it fails with unresolved `Res` imports, check the grep from Step 1 — find the file and update its import manually.

- [ ] **Step 11: Build iOS framework (macOS only)**
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 12: Build Desktop**
```bash
./gradlew :composeApp:desktopJar
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 13: Delete old core module directories**
```bash
rm -rf core/designsystem
rm -rf core/presentation
```

- [ ] **Step 14: Commit**
```bash
git add -A
git commit -m "refactor: merge :core:designsystem + :core:presentation into :core:ui"
```

---

## Task 6: Final verification — all platforms

- [ ] **Step 1: Full clean build Android**
```bash
./gradlew clean :androidApp:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Full clean build Desktop**
```bash
./gradlew clean :composeApp:desktopJar
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Full iOS framework build (macOS only)**
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Verify final module count**
```bash
grep "^include" settings.gradle.kts
```
Expected output — exactly these 6 lines:
```
include(":composeApp")
include(":androidApp")
include(":core:shared")
include(":core:ui")
include(":feature:auth")
include(":feature:chat")
```

- [ ] **Step 5: Verify no references to old module paths remain in build files**
```bash
grep -r "core:domain\|core:data\|core:designsystem\|core:presentation\|auth:domain\|auth:presentation\|chat:domain\|chat:data\|chat:database\|chat:presentation" \
     --include="*.kts" --include="*.kt" \
     --exclude-dir=build --exclude-dir=".gradle" .
```
Expected: no results. Any hit is a missed reference to clean up.

- [ ] **Step 6: Final commit (if any leftover fixes made)**
```bash
git add -A
git commit -m "refactor: fix remaining references after module simplification"
```

---

## Summary

| Phase | What changes | What stays the same |
|---|---|---|
| Task 2 | `feature/auth/*` Gradle boundaries | All `.kt` source code, package names |
| Task 3 | `feature/chat/*` Gradle boundaries | All `.kt` source code, package names |
| Task 4 | `core/shared` Gradle boundaries | All `.kt` source code, package names |
| Task 5 | `core/ui` Gradle boundaries + `compose.resources` package + CmpFeatureConventionPlugin | All `.kt` source code except targeted `Res` import replacements |

**Risk level:** Low. Every task ends with a working build before old files are deleted. The branch is disposable if anything goes wrong — `git checkout main` restores the original.

**Not covered in this plan (follow-up):**
- Internal folder reorganization within modules (e.g. `model/chat/`, `model/message/` sub-grouping)
- Package name refactoring (e.g. `com.project.core.domain` → `com.project.core.shared`)
- Adding new features (Google Sign-In, onboarding, etc.)
