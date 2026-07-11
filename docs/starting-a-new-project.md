# Starting a new project from this template

This KMP app is a reusable template. Chat is dormant behind a flag, backend URLs + the Android
deep-link host are config-driven, and no Firebase config is committed. Spinning up a new project is
**two kinds of work**:

- **Config** — values you set in `local.properties`, *no source edits* (the easy part).
- **Rebrand** — the `chirp` name/identity, which you rename in source (the involved part).

Do them in this order.

---

## 1. Get the template

Create the new repo from the template (GitHub **Use this template**, or `git archive` into a fresh
folder — same approach as the backend). You now have a history-free copy.

## 2. Configure (`local.properties`) — no source edits

```bash
cp local.properties.example local.properties
```
Fill in:
- `API_KEY` — your backend's `x-api-key` (**required**)
- `BASE_URL_HTTP` / `BASE_URL_WS` — your deployed backend (**required**; e.g.
  `https://yourapp.example.com/api` and `wss://yourapp.example.com/ws`)
- `DEEP_LINK_HOST` — your app-link domain (optional; defaults to `example.com`)
- `CHAT_ENABLED` — leave `false` unless you want chat (optional)

> These flow through **BuildKonfig** — `UrlConstants` and the Android manifest read them. Nothing to
> edit in code.

## 3. Firebase (per project — not committed)

Each app needs its own Firebase project; the config files are gitignored on purpose.
1. Create a Firebase project (Android + iOS apps) with **your** `applicationId` / bundle id (step 4).
2. Download and drop in:
   - `google-services.json` → `androidApp/`
   - `GoogleService-Info.plist` → `iosApp/iosApp/`

> The `google-services` Gradle plugin **fails the build if `google-services.json` is missing** — so
> the app won't build until this is done. That's expected.

## 4. Rebrand (`chirp` → your app) — source edits

`chirp`/`Chirp` appears across ~60 files. Rename carefully — prefer **Android Studio refactor** for
package/class renames over blind find-replace.

- **App name:** `settings.gradle.kts` → `rootProject.name`; `androidApp/src/main/res/values/strings.xml` → `app_name`.
- **Package / namespace:** `com.project.chirp` → `com.project.<yourapp>` (composeApp + androidApp). The
  feature/core modules use `com.project.auth` / `com.project.chat` / `com.project.core` — rename the
  `com.project` prefix if you want, but it's optional.
- **applicationId / bundle id:** the Android `applicationId` and the iOS bundle identifier (Xcode).
  These MUST be unique per app (Play Store / App Store) and match your Firebase apps.
- **`Application` class:** `androidApp/.../ChirpApplication.kt` and its `android:name` in the manifest.
- **Custom URL scheme:** `AndroidManifest.xml` has `android:scheme="chirp"` — change to your scheme.
  (The deep-link *host* is already config-driven via `DEEP_LINK_HOST`; the *scheme* is not.)
- **iOS + desktop deep links:** `iosApp/iosApp/Info.plist` and `conveyor.conf` still hardcode
  `chirp.adamapp.dev` — update to your domain.
- **Sanity check:** `grep -rn "chirp" . | grep -v /build/ | grep -v /.git/` and review each hit.

## 5. Chat: keep it dormant, or turn it on

Default is **off** (`CHAT_ENABLED=false`). With chat off, there is **no post-login screen** — the app
hits a `TODO(...)` after login. Wire your own first feature as the post-login destination in:
- `composeApp/.../App.kt` (`startDestination`)
- `composeApp/.../navigation/NavigationRoot.kt` (`onLoginSuccess`)

To enable chat instead, set `CHAT_ENABLED=true` in `local.properties` and it behaves like Chirp.

## 6. Build & run

- Android: run the `androidApp` configuration.
- Point `BASE_URL_*` at a **live** backend (a dead URL just times out on login).
- Backend side: stand it up from the **backend template** and its `setup-guide/` (separate repo).

---

### Quick reference — what's config vs rename

| Concern | How | Where |
|---|---|---|
| Backend URL | config | `local.properties` → `BASE_URL_HTTP/WS` |
| API key | config | `local.properties` → `API_KEY` |
| Deep-link **host** (Android) | config | `local.properties` → `DEEP_LINK_HOST` |
| Chat on/off | config | `local.properties` → `CHAT_ENABLED` |
| App name / package / applicationId | rename | source (step 4) |
| URL **scheme**, iOS/desktop deep links | rename | source (step 4) |
| Firebase | per-project files | `androidApp/`, `iosApp/iosApp/` (step 3) |
