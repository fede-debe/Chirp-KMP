# Chirp

A Kotlin Multiplatform chat application targeting Android and iOS, built with Compose Multiplatform.

## Project Structure

| Module | Purpose |
|---|---|
| `androidApp` | Android app entry point (launcher, `MainActivity`) |
| `composeApp` | Shared KMP library — all UI, features, and business logic |
| `iosApp` | iOS app entry point (Xcode project) |
| `core/*` | Shared infrastructure (data, domain, designsystem, presentation) |
| `feature/*` | Feature modules (auth, chat) |
| `build-logic` | Convention plugins for consistent Gradle configuration |

## Running the App

**Android** — select `androidApp` in the Android Studio run dropdown and hit Run, or:
```shell
./gradlew :androidApp:assembleDebug
```

**iOS** — open `/iosApp` in Xcode and run, or use the iOS run configuration in Android Studio.

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
