# Chat Sub-Feature Flags Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four independently-toggleable chat sub-feature flags (voice, typing, attachments, admin), gated send-side, surfaced through one `ChatFeatureFlags` object.

**Architecture:** Four `BuildKonfig` booleans (default off) added to the convention plugin, surfaced via a `ChatFeatureFlags` object in `feature/chat/commonMain`. Each feature's send-side entry point is wrapped in `if (ChatFeatureFlags.x)`. Nested under `CHAT_ENABLED` (only meaningful when chat is on). Chirp sets all four `true` in `local.properties` to stay full-featured.

**Tech Stack:** Kotlin Multiplatform, Compose, BuildKonfig, Koin, Gradle convention plugins.

**Verification note:** Like `CHAT_ENABLED`, there's no unit-test harness for these Compose entry points. Verification is **compile with flags off, then on**, plus a visual check that each control appears only when its flag is on. That is the appropriate test here.

**Spec correction:** the spec listed the admin entry as `ManageChatButtonSection`; the real send-side entry is the **"Chat members" dropdown item** in `ChatDetailHeader` (`ManageChatButtonSection` lives *inside* the manage screen). We gate the dropdown item. (This also hides member *viewing*, since that screen is the whole management surface — intended.)

---

## File Structure

- **Modify** `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt` — add 4 boolean flags.
- **Create** `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/config/ChatFeatureFlags.kt` — the flags object.
- **Modify** `feature/chat/.../ui/chatDetail/components/MessageBox.kt` — gate mic (voice) + attach (attachments) buttons.
- **Modify** `feature/chat/.../ui/chatDetail/ChatDetailViewModel.kt` — gate `observeOutgoingTyping()`.
- **Modify** `feature/chat/.../ui/chatDetail/components/ChatDetailHeader.kt` — gate the "Chat members" dropdown item (admin).
- **Modify** `local.properties` (Chirp, gitignored) — set all four `= true`.

---

## Task 1: Add the four flags to the convention plugin

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt`

- [ ] **Step 1: Add the four boolean fields after `CHAT_ENABLED`**

In `defaultConfigs { ... }`, after the existing `buildConfigField(FieldSpec.Type.BOOLEAN, "CHAT_ENABLED", chatEnabled)` line, add:

```kotlin
// Chat sub-features, only meaningful when CHAT_ENABLED=true. Default off for the template;
// a project turns on exactly the chat capabilities it wants via local.properties.
for (name in listOf("CHAT_VOICE_ENABLED", "CHAT_TYPING_ENABLED", "CHAT_ATTACHMENTS_ENABLED", "CHAT_ADMIN_ENABLED")) {
    val value = gradleLocalProperties(rootDir, rootProject.providers).getProperty(name) ?: "false"
    buildConfigField(FieldSpec.Type.BOOLEAN, name, value)
}
```

- [ ] **Step 2: Verify build-logic compiles**

Run: `./gradlew -p build-logic :convention:compileKotlin -q`
Expected: exit 0. (IDE fallback: Gradle sync, no red in `build-logic`.)

- [ ] **Step 3: Verify the fields generate**

Run: `./gradlew :feature:chat:generateBuildKonfig -q && find feature/chat/build -name BuildKonfig.kt -exec grep -l CHAT_VOICE_ENABLED {} \;`
Expected: prints a path (the four fields exist in `com.project.feature.chat.BuildKonfig`).

- [ ] **Step 4: Commit**

```bash
git add build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt
git commit -m "build(buildkonfig): add 4 chat sub-feature flags (default off)"
```

---

## Task 2: Create the `ChatFeatureFlags` object

**Files:**
- Create: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/config/ChatFeatureFlags.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.project.chat.presentation.config

import com.project.feature.chat.BuildKonfig

/**
 * Single source of truth for the chat sub-feature gates (send-side). Values come from BuildKonfig
 * (local.properties, default false) and are only meaningful when CHAT_ENABLED is true. UI reads
 * these instead of touching BuildKonfig directly.
 */
object ChatFeatureFlags {
    val voice: Boolean = BuildKonfig.CHAT_VOICE_ENABLED
    val typing: Boolean = BuildKonfig.CHAT_TYPING_ENABLED
    val attachments: Boolean = BuildKonfig.CHAT_ATTACHMENTS_ENABLED
    val admin: Boolean = BuildKonfig.CHAT_ADMIN_ENABLED
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :feature:chat:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/config/ChatFeatureFlags.kt
git commit -m "feat(chat): add ChatFeatureFlags (single source for chat sub-feature gates)"
```

---

## Task 3: Gate voice (mic) + attachments (attach) in `MessageBox`

Both buttons live in `MessageBox`, which is rendered at two call sites in `ChatDetailScreen` — gating inside `MessageBox` covers both.

**Files:**
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/MessageBox.kt`

- [ ] **Step 1: Import the flags**

Add with the other imports:
```kotlin
import com.project.chat.presentation.config.ChatFeatureFlags
```

- [ ] **Step 2: Wrap the attach button (around line 124)**

Wrap the existing attach `IconButton(onClick = onAttachClick) { Icon(...) }` block in:
```kotlin
if (ChatFeatureFlags.attachments) {
    IconButton(onClick = onAttachClick) {
        // ...existing Icon(...) body unchanged...
    }
}
```

- [ ] **Step 3: Wrap the mic button (around line 135)**

Wrap the existing mic `IconButton(onClick = onMicClick) { Icon(...) }` block in:
```kotlin
if (ChatFeatureFlags.voice) {
    IconButton(onClick = onMicClick) {
        // ...existing Icon(...) body unchanged...
    }
}
```

(Leave the `RecordingBar` / recording-in-progress UI as-is — it only renders when `recording != null`, which can't happen without the mic button.)

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :feature:chat:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/MessageBox.kt
git commit -m "feat(chat): gate mic/attach composer buttons behind voice/attachments flags"
```

---

## Task 4: Gate typing emission in `ChatDetailViewModel`

**Files:**
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailViewModel.kt`

- [ ] **Step 1: Import the flags**

Add with the other imports:
```kotlin
import com.project.chat.presentation.config.ChatFeatureFlags
```

- [ ] **Step 2: Gate the emission call**

In the `.onStart { ... }` block (around line 191), change:
```kotlin
                observeOutgoingTyping()
```
to:
```kotlin
                if (ChatFeatureFlags.typing) observeOutgoingTyping()
```
Leave `observeTypingUsers()` (the incoming/display side) untouched — send-side only.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :feature:chat:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/ChatDetailViewModel.kt
git commit -m "feat(chat): emit typing indicators only when CHAT_TYPING_ENABLED"
```

---

## Task 5: Gate the admin entry in `ChatDetailHeader`

**Files:**
- Modify: `feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/ChatDetailHeader.kt`

- [ ] **Step 1: Import the flags**

Add with the other imports:
```kotlin
import com.project.chat.presentation.config.ChatFeatureFlags
```

- [ ] **Step 2: Conditionally include the "Chat members" (manage) item**

In the `ChirpDropDownMenu(... items = listOf(...))` (around line 116–130), the first `DropDownItem` (the `chat_members` one, `onClick = onManageChatClick`) is the manage-chat entry. Build the list conditionally so it's present only when admin is on:
```kotlin
items = buildList {
    if (ChatFeatureFlags.admin) {
        add(
            DropDownItem(
                title = stringResource(Res.string.chat_members),
                icon = vectorResource(Res.drawable.users_icon),
                contentColor = MaterialTheme.colorScheme.extended.textSecondary,
                onClick = onManageChatClick,
            ),
        )
    }
    add(
        DropDownItem(
            title = leaveOrDeleteTitle,
            icon = vectorResource(DesignSystemRes.drawable.log_out_icon),
            contentColor = MaterialTheme.colorScheme.extended.destructiveHover,
            onClick = onLeaveChatClick,
        ),
    )
},
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :feature:chat:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add feature/chat/src/commonMain/kotlin/com/project/chat/presentation/ui/chatDetail/components/ChatDetailHeader.kt
git commit -m "feat(chat): show manage-chat (members) entry only when CHAT_ADMIN_ENABLED"
```

---

## Task 6: Keep Chirp full-featured + verify both states

**Files:**
- Modify: `local.properties` (gitignored — Chirp's local config)

- [ ] **Step 1: Turn all four on for Chirp**

Append (or set) in `local.properties`:
```
CHAT_VOICE_ENABLED=true
CHAT_TYPING_ENABLED=true
CHAT_ATTACHMENTS_ENABLED=true
CHAT_ADMIN_ENABLED=true
```

- [ ] **Step 2: Verify compile with flags ON**

Run: `./gradlew :feature:chat:generateBuildKonfig :feature:chat:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Verify compile with flags OFF (template default)**

Temporarily comment out / remove the four lines from `local.properties`, then:
Run: `./gradlew :feature:chat:generateBuildKonfig :feature:chat:compileCommonMainKotlinMetadata`
Expected: `BUILD SUCCESSFUL` (both branches of every `if` compile).
Then restore the four `=true` lines (Chirp stays full-featured).

- [ ] **Step 4: Visual smoke (optional, in Android Studio)**

With all four on: mic + attach buttons present, typing works, "Chat members" in the ⋯ menu.
Flip one off in `local.properties`, rebuild: that one control disappears; the others stay.

- [ ] **Step 5: (No commit — `local.properties` is gitignored.)**

Confirm nothing is staged: `git status --short` shows a clean tree.
