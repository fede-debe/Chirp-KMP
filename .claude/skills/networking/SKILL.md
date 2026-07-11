---
name: networking
description: Use when making an HTTP API call, adding a remote endpoint, defining a request/response DTO, or mapping a DTO to a domain model in the Chirp KMP app — covers the Ktor client setup, the safeCall extensions, DTO conventions, and DTO→domain mapping.
---

# Networking (Chirp KMP)

## Convention

All HTTP goes through a single Ktor client created by `HttpClientFactory`
(`core/shared/.../data/networking/HttpClientFactory.kt`). The factory takes the **platform engine** as a parameter (OkHttp on Android, Darwin on iOS) and installs: `ContentNegotiation` (Json `ignoreUnknownKeys = true`), `HttpTimeout` (20s), `Logging` (wraps the `ChirpLogger` domain abstraction), `WebSockets` (20s ping), `DefaultRequest` (auto-attaches `x-api-key` from `BuildKonfig` + `Content-Type: application/json`), and `Auth` bearer (JWT load + automatic refresh on 401).

**Never call Ktor directly.** Use the typed extension functions in
`core/shared/.../data/networking/HttpClientExt.kt`: `HttpClient.get/post/put/delete`. They are `inline` with `reified` request/response types and return `Result<Response, DataError.Remote>` — no try/catch, no manual status handling:

- `safeCall { }` → `platformSafeCall` (`expect`/`actual`, catches platform network exceptions → `DataError.Remote.NO_INTERNET` etc.) → `responseToResult` (maps HTTP status → `DataError.Remote`).
- `constructRoute()` prepends `UrlConstants.BASE_URL_HTTP` to any relative `"/route"`, so call sites pass **relative routes only**.

**DTOs** live in `…data/dto/`, are `@Serializable` data classes, and are suffixed `Dto` (responses, e.g. `ChatDto`) or `Request` (request bodies, in `…data/dto/request/`, e.g. `CreateChatRequest`). DTOs never leave the data layer.

**Mappers** are extension functions (`fun ChatDto.toDomain(): Chat`) in `…data/mappers/XMappers.kt`. A `KtorXService` always maps the DTO to a domain model before returning, via `.map { it.toDomain() }`.

## Example

A complete service method (`feature/chat/.../data/chat/KtorChatService.kt`):

```kotlin
class KtorChatService(
    private val httpClient: HttpClient,
) : ChatService {

    override suspend fun createChat(otherUserIds: List<String>): Result<Chat, DataError.Remote> {
        return httpClient.post<CreateChatRequest, ChatDto>(
            route = "/chat",                                   // relative — BASE_URL prepended
            body = CreateChatRequest(otherUserIds = otherUserIds),
        ).map { it.toDomain() }                                // DTO → domain before returning
    }

    override suspend fun getChats(): Result<List<Chat>, DataError.Remote> {
        return httpClient.get<List<ChatDto>>(route = "/chat")
            .map { dtos -> dtos.map { it.toDomain() } }
    }
}
```

DTO + mapper:

```kotlin
@Serializable
data class ChatDto(val id: String, val participants: List<ChatParticipantDto>, val lastActivityAt: String, val lastMessage: ChatMessageDto?)

fun ChatDto.toDomain(): Chat = Chat(
    id = id,
    participants = participants.map { it.toDomain() },
    lastActivityAt = Instant.parse(lastActivityAt),   // String DTO → typed domain
    lastMessage = lastMessage?.toDomain(),
    …,
)
```

The optional trailing `builder: HttpRequestBuilder.() -> Unit` lambda handles one-off needs (e.g. `markAsRefreshTokenRequest()` in `HttpClientFactory`) without new helper functions.

## What to avoid

- ❌ Calling `client.get { url(...) }` directly in a service. Use the `HttpClient.get/post/…` extensions so you get `safeCall` + `Result` for free.
- ❌ `try/catch` around network calls in `commonMain` — KMP common code can't catch Java/Darwin exceptions; that's what `platformSafeCall` (expect/actual) is for.
- ❌ Returning a `Dto` from a `Service`. Map to a domain model with `.toDomain()` first.
- ❌ Hardcoding absolute URLs. Pass relative routes; `constructRoute` adds the base URL.
- ❌ Adding a new `@Serializable` field without remembering `ignoreUnknownKeys = true` already tolerates server fields you don't model.
