---
name: architecture
description: Use when adding a feature, module, class, or deciding which layer/package code belongs in for the Chirp KMP app — covers module structure, the domain/data/presentation layering, class naming, and Koin DI wiring.
---

# Architecture (Chirp KMP)

## Convention

Chirp is a Compose Multiplatform app (Android + iOS + Desktop) built from **6 Gradle modules** (see `settings.gradle.kts`):

```
:composeApp   – app entry, NavHost, DI aggregation, MainViewModel
:androidApp   – Android Application + MainActivity wrapper
:core:shared  – domain + data shared by all features (auth, networking, util, Result/DataError)
:core:ui      – design system (com.project.core.designsystem.*) + presentation utils (com.project.core.presentation.*)
:feature:auth – login / register / verify / reset flows
:feature:chat – chat list, detail, profile, websocket, Room database
```

Inside every feature/core module, code is split by **layer via package** (not sub-module):

| Layer | Package | Holds | Backend analogue |
|-------|---------|-------|------------------|
| `domain` | `…domain.*` | Interfaces (`ChatService`, `ChatRepository`, `ChatConnectionClient`), models (`Chat`, `ChatMessage`), `util` (`Result`, `DataError`). Pure Kotlin, **no** Ktor/Compose/Room. | api + domain (contracts) |
| `data` | `…data.*` | Interface implementations, DTOs, mappers, Room `database/`. | service + infra |
| `presentation` | `…presentation.*` | ViewModels, Screens, UI models, presentation mappers, `navigation/`, `di/`. | (UI — no backend equivalent) |

> There is **no backend module in this repo** — the "api/domain/service/infra" split maps onto the mobile layering above. A `KtorXService` is the remote data source (≈ infra), a Room `XDao` is the local source (≈ infra), an `OfflineFirstXRepository` combines them (≈ service), and `domain` holds the contracts + models (≈ api/domain).

**Dependency direction:** `feature → core:shared` (+ `core:ui`, auto-added by `CmpFeatureConventionPlugin`); `composeApp` wires features together. `domain` never depends on `data` or `presentation`.

**Class naming** (enforced in code, see `KtorAuthService` / `KtorChatService` KDoc):
- Name a single remote data source after its tech + role: `KtorChatService`, `KtorAuthService` — **never** `…Impl`.
- Reserve `Repository` for classes combining ≥2 sources: `OfflineFirstChatRepository`, `OfflineFirstMessageRepository`.
- Abstract tech out of domain names: the websocket contract is `ChatConnectionClient`, not `ChatWebSocketService`.

**DI:** each layer ships a Koin module (`chatDataModule`, `authPresentationModule`, `corePresentationModule`…), aggregated in `composeApp/.../di/initKoin.kt`. Platform-specific bindings go through `expect val platformXModule: Module`.

## Example

A vertical slice for "chats" (all under `feature/chat/`):

```
domain/chat/ChatService.kt          interface ChatService { suspend fun getChats(): Result<List<Chat>, DataError.Remote> … }
domain/chat/ChatRepository.kt        interface ChatRepository { fun getChats(): Flow<List<Chat>>; suspend fun fetchChats(): … }
data/chat/KtorChatService.kt         class KtorChatService(httpClient) : ChatService           // remote source
data/chat/OfflineFirstChatRepository class …Repository(database, chatService, …) : ChatRepository // combines Room + remote
data/di/ChatDataModule.kt            singleOf(::KtorChatService) bind ChatService::class
                                     singleOf(::OfflineFirstChatRepository) bind ChatRepository::class
```

`ChatRepository` (domain) documents the rule: reads stream from the DB (`getChats(): Flow`), writes sync the DB (`fetchChats()`), and the UI only observes the DB — single source of truth.

## What to avoid

- ❌ Naming an implementation `XServiceImpl` / `XRepositoryImpl`. Name the tech (`Ktor…`) or strategy (`OfflineFirst…`).
- ❌ Calling a single-source remote wrapper a `Repository`. That word is reserved for multi-source classes.
- ❌ Importing Ktor, Room, or Compose types into a `domain` package.
- ❌ Having `feature:auth` depend on `feature:chat` (or any feature depending on another). Features only depend on `core:*`; cross-feature wiring lives in `composeApp`.
- ❌ Returning a network/DB response straight to the UI from a repository — go through the database so `Flow` reads stay the single source of truth.
