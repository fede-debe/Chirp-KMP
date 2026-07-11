# Chat Feature Flag Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gate the chat feature behind a single build-time `CHAT_ENABLED` flag (default off) so it's dormant-but-retained, mirroring the backend.

**Architecture:** Add a `CHAT_ENABLED` boolean to the shared `BuildKonfig` convention plugin (single source of truth, sourced from `local.properties`, default `false`). Apply BuildKonfig to `composeApp` so it can read the flag. Gate the two chat coupling points — `App.kt` (start destination) and `NavigationRoot.kt` (chat graph registration + login success routing) — leaving the chat module untouched and dormant.

**Tech Stack:** Kotlin Multiplatform, Compose Navigation, BuildKonfig (`com.codingfeline.buildkonfig`), Gradle convention plugins.

**Verification note:** This is a Gradle + navigation-wiring change; there is no unit-test harness for the Compose `startDestination`. Verification is **compile/build with the flag off, then on**, plus a manual smoke test at the end. That is the appropriate test for this change.

---

## File Structure

- **Modify** `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt` — add `CHAT_ENABLED` boolean field (default `false`).
- **Modify** `composeApp/build.gradle.kts` — apply the buildkonfig convention + set its package name.
- **Modify** `composeApp/src/commonMain/kotlin/com/project/chirp/App.kt` — gate `startDestination`.
- **Modify** `composeApp/src/commonMain/kotlin/com/project/chirp/navigation/NavigationRoot.kt` — gate chat graph registration + login-success routing; add dormant note.

---

## Task 1: Add `CHAT_ENABLED` to the BuildKonfig convention plugin

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt`

- [ ] **Step 1: Add the boolean field alongside `API_KEY`**

In the `defaultConfigs { ... }` block, after the existing `buildConfigField(FieldSpec.Type.STRING, "API_KEY", apiKey)` line, add:

```kotlin
// Feature flag: chat is dormant by default and enabled per-project via local.properties
// (CHAT_ENABLED=true). Unlike API_KEY it is optional — absent means chat off — so a fresh
// clone builds without extra setup. Single source of truth for the chat gate.
val chatEnabled = gradleLocalProperties(rootDir, rootProject.providers)
    .getProperty("CHAT_ENABLED") ?: "false"
buildConfigField(FieldSpec.Type.BOOLEAN, "CHAT_ENABLED", chatEnabled)
```

- [ ] **Step 2: Verify build-logic compiles**

Run: `./gradlew :convention:compileKotlin` (from `build-logic/`, or `./gradlew -p build-logic :convention:compileKotlin` from root)
Expected: `BUILD SUCCESSFUL`. (IDE fallback: the `build-logic` module compiles on Gradle sync with no red.)

- [ ] **Step 3: Commit**

```bash
git add build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt
git commit -m "build(buildkonfig): add CHAT_ENABLED boolean flag (default false)"
```

---

## Task 2: Give `composeApp` its own BuildKonfig

`composeApp` reads the flag in common code, so it needs its own generated `BuildKonfig` object (BuildKonfig objects are `internal` — a module cannot read another module's).

**Files:**
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Apply the buildkonfig convention plugin**

In the `plugins { }` block, add the alias under the existing ones:

```kotlin
plugins {
    alias(libs.plugins.convention.cmp.application)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.convention.buildkonfig)
}
```

- [ ] **Step 2: Pin the generated package to `composeApp`'s package**

At the end of the file (top level, matching how `core/shared/build.gradle.kts` does it), add:

```kotlin
buildkonfig {
    packageName = "com.project.chirp"
}
```

This makes the generated object `com.project.chirp.BuildKonfig` — the same package as `App.kt`, so no import is needed there.

- [ ] **Step 3: Verify it generates with both fields**

Run: `./gradlew :composeApp:generateBuildKonfig`
Expected: `BUILD SUCCESSFUL`, and the generated file contains both `API_KEY` and `CHAT_ENABLED`:
`find composeApp/build -name "BuildKonfig.kt" -path "*com/project/chirp*" -exec grep -l CHAT_ENABLED {} \;` should print a path.
(IDE fallback: Gradle sync, then `BuildKonfig` resolves inside `com.project.chirp`.)

- [ ] **Step 4: Commit**

```bash
git add composeApp/build.gradle.kts
git commit -m "build(composeApp): apply buildkonfig so it can read CHAT_ENABLED"
```

---

## Task 3: Gate the start destination in `App.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/project/chirp/App.kt`

- [ ] **Step 1: Replace the `if/else` start destination with a flag-aware `when`**

Find this block inside the `ChirpTheme { ... }` call:

```kotlin
            NavigationRoot(
                navController = navController,
                startDestination = if (state.isLoggedIn) {
                    ChatGraphRoutes.Graph
                } else {
                    AuthGraphRoutes.Graph
                },
            )
```

Replace the `startDestination = ...` argument with:

```kotlin
                startDestination = when {
                    !state.isLoggedIn -> AuthGraphRoutes.Graph
                    // Chat is gated by CHAT_ENABLED. When off, the consuming project wires its own
                    // post-login destination here (there is intentionally no placeholder screen).
                    BuildKonfig.CHAT_ENABLED -> ChatGraphRoutes.Graph
                    else -> TODO("Wire your app's post-login start destination")
                },
```

`BuildKonfig` resolves without an import (same `com.project.chirp` package). Leave the existing `import com.project.chat.presentation.navigation.ChatGraphRoutes` in place — chat stays a dependency.

- [ ] **Step 2: Verify common code compiles (flag off, the default)**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL`. Both `when` branches compile because `CHAT_ENABLED` is a runtime `val`, not a `const`. (IDE fallback: Build → Make Project, no red in `App.kt`.)

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/project/chirp/App.kt
git commit -m "feat(nav): gate post-login start destination behind CHAT_ENABLED"
```

---

## Task 4: Gate chat graph + login routing in `NavigationRoot.kt`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/project/chirp/navigation/NavigationRoot.kt`

- [ ] **Step 1: Gate the login-success routing and the chat graph registration**

Replace the `NavHost { ... }` body:

```kotlin
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        authGraph(
            navController = navController,
            onLoginSuccess = {
                navController.navigate(ChatGraphRoutes.Graph) {
                    popUpTo(AuthGraphRoutes.Graph) {
                        inclusive = true
                    }
                }
            },
        )
        chatGraph(
            navController = navController,
            onLogout = {
                navController.navigate(AuthGraphRoutes.Graph) {
                    popUpTo(ChatGraphRoutes.Graph) {
                        inclusive = true
                    }
                }
            },
        )
    }
```

with:

```kotlin
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        authGraph(
            navController = navController,
            onLoginSuccess = {
                if (BuildKonfig.CHAT_ENABLED) {
                    navController.navigate(ChatGraphRoutes.Graph) {
                        popUpTo(AuthGraphRoutes.Graph) {
                            inclusive = true
                        }
                    }
                } else {
                    // No placeholder by design: the consuming project routes to its own feature here.
                    TODO("Wire your app's post-login destination on login success")
                }
            },
        )
        // Chat is dormant unless CHAT_ENABLED. When off, its routes are never registered, so the
        // module stays fully retained but unreachable. Retained for possible future use.
        if (BuildKonfig.CHAT_ENABLED) {
            chatGraph(
                navController = navController,
                onLogout = {
                    navController.navigate(AuthGraphRoutes.Graph) {
                        popUpTo(ChatGraphRoutes.Graph) {
                            inclusive = true
                        }
                    }
                },
            )
        }
    }
```

- [ ] **Step 2: Add the `BuildKonfig` import**

At the top with the other imports, add:

```kotlin
import com.project.chirp.BuildKonfig
```

(`NavigationRoot.kt` is in `com.project.chirp.navigation`, a different package from the generated `com.project.chirp.BuildKonfig`, so it needs the import — unlike `App.kt`.)

- [ ] **Step 3: Verify compile with the flag OFF (default)**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Verify compile with the flag ON**

Add `CHAT_ENABLED=true` to `local.properties`, then:
Run: `./gradlew :composeApp:generateBuildKonfig :composeApp:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL` (chat wiring path compiles too).
Then **remove** the `CHAT_ENABLED=true` line from `local.properties` to restore the default-off state (never commit `local.properties`; it's gitignored).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/project/chirp/navigation/NavigationRoot.kt
git commit -m "feat(nav): register chat graph only when CHAT_ENABLED; gate login routing"
```

---

## Task 5: Manual smoke test (both states)

- [ ] **Step 1: Flag OFF — auth works, no chat**

With no `CHAT_ENABLED` in `local.properties` (default off), run the Android app (`androidApp`) from the IDE.
Expected: launches to the login/register flow, auth screens work. (Logging in with chat off reaches the `TODO(...)` — that's the intended "wire your own" signal, not a bug.)

- [ ] **Step 2: Flag ON — behaves like today**

Add `CHAT_ENABLED=true` to `local.properties`, rebuild, run.
Expected: login → chat, exactly as before this change. Then remove the line again.

- [ ] **Step 3: Confirm the chat module is untouched**

Run: `git diff main --stat -- feature/chat`
Expected: **no output** (the chat feature's own code was not modified — only its wiring in `composeApp`).
