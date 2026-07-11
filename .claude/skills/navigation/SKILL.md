---
name: navigation
description: Use when adding a screen/route, defining a navigation graph, passing arguments between screens, wiring deep links, or handling navigation between features in the Chirp KMP app — covers type-safe Compose Navigation routes, per-feature graph builders, and inter-feature wiring.
---

# Navigation (Chirp KMP)

## Convention

Navigation uses **type-safe Compose Navigation** with Kotlinx Serialization — no string routes.

**Routes** are a `sealed interface <Feature>GraphRoutes` whose members are `@Serializable` (`feature/auth/.../navigation/AuthGraphRoutes.kt`):
- a `data object Graph` for the nested-graph route,
- `data object` for argument-less screens (`Login`, `Register`),
- `data class` for screens that take arguments (`RegisterSuccess(val email: String)`, `ResetPassword(val token: String)`).

**Each feature owns a graph builder** — an extension `fun NavGraphBuilder.<feature>Graph(navController, onInterFeatureEvent: () -> Unit)` that calls `navigation<Graph>(startDestination = …) { composable<Route> { XRoot(...) } }`. Use the **`KClass` overload** of `navigation`/`composable`, not the `String` overload.

**Inter-feature navigation bubbles up.** A feature **never** navigates into another feature. It exposes a lambda (`onLoginSuccess`, `onLogout`); the app module (`composeApp/.../navigation/NavigationRoot.kt`) owns the `NavHost`, mounts each feature graph, and implements those lambdas — typically `navigate(OtherGraph) { popUpTo(ThisGraph) { inclusive = true } }` so the whole graph leaves the back stack. **Intra-feature** navigation (e.g. Register → RegisterSuccess) is done locally with the feature's `navController`.

**Arguments:** carried by the route `data class`. Read them either via `backStackEntry.toRoute<Route>()` (in the graph) or from the `SavedStateHandle` in the ViewModel (key must match the property name).

**Deep links:** attach `navDeepLink { uriPattern = … }` to a `composable`. Register **both** an `https://chirp.adamapp.dev/...` and a `chirp://...` scheme, and the `{placeholder}` must exactly match the route argument name.

## Example

Routes + graph builder (`feature/chat/.../navigation/ChatGraphRoutes.kt`):

```kotlin
sealed interface ChatGraphRoutes {
    @Serializable data object Graph : ChatGraphRoutes
    @Serializable data class ChatListDetailRoute(val chatId: String? = null) : ChatGraphRoutes
}

fun NavGraphBuilder.chatGraph(navController: NavController, onLogout: () -> Unit) {
    navigation<ChatGraphRoutes.Graph>(startDestination = ChatGraphRoutes.ChatListDetailRoute(null)) {
        composable<ChatGraphRoutes.ChatListDetailRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = "chirp://chat_detail/{chatId}" }),
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ChatGraphRoutes.ChatListDetailRoute>()  // typed args
            ChatListDetailAdaptiveLayout(initialChatId = route.chatId, onLogout = onLogout)
        }
    }
}
```

App-level wiring (`composeApp/.../navigation/NavigationRoot.kt`) — the only place features connect:

```kotlin
NavHost(navController, startDestination) {
    authGraph(navController, onLoginSuccess = {
        navController.navigate(ChatGraphRoutes.Graph) {
            popUpTo(AuthGraphRoutes.Graph) { inclusive = true }   // drop whole auth graph
        }
    })
    chatGraph(navController, onLogout = {
        navController.navigate(AuthGraphRoutes.Graph) { popUpTo(ChatGraphRoutes.Graph) { inclusive = true } }
    })
}
```

Intra-feature with state preservation (`AuthGraph.kt`): `navigate(Login) { popUpTo(Register){ inclusive = true; saveState = true }; launchSingleTop = true; restoreState = true }`.

## What to avoid

- ❌ Navigating from one feature directly to another. Expose a lambda and let `NavigationRoot` decide.
- ❌ String routes / the `String` overload of `navigation`/`composable`. Use `@Serializable` route classes + the `KClass` overload.
- ❌ Passing args via string concatenation. Put them on the route `data class` and read with `toRoute()` / `SavedStateHandle`.
- ❌ A deep-link `{placeholder}` that doesn't match the route argument name, or registering only one URI scheme — register both `https` and `chirp`.
- ❌ Manual deep-link parsing in `App.kt`. Let Compose Navigation extract arguments via `navDeepLink`.
