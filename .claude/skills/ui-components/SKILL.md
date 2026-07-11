---
name: ui-components
description: Use when building or restyling a composable, picking colors/typography, adding a design-system component, or theming a screen in the Chirp KMP app — covers the Chirp* component conventions, ChirpTheme/ExtendedColors, and the Color/Type token system.
---

# UI components & theming (Chirp KMP)

## Convention

The design system lives in `core/ui` under `com.project.core.designsystem.*`. Reusable components are prefixed **`Chirp`** (`ChirpButton`, `ChirpTextField`, `ChirpPasswordTextField`, `ChirpSnackbarScaffold`, `ChirpAdaptiveFormLayout`…) and grouped by kind under `…components/<group>/`.

**Component rules:**
- **Wrap Material 3**, don't rebuild from raw `Box`/`Column` — you keep touch targets & accessibility for free (`ChirpButton` wraps M3 `Button`).
- **Variants via enum**, not exposed colors: `ChirpButtonStyle { PRIMARY, SECONDARY, DESTRUCTIVE_PRIMARY, DESTRUCTIVE_SECONDARY, TEXT }`. The component maps the enum → `ButtonColors`/`BorderStroke` internally.
- **Slots** are nullable composable lambdas: `leadingIcon: @Composable (() -> Unit)? = null` (null = absent).
- **Parameter order:** required params first, then `modifier: Modifier = Modifier`, then optional params with defaults.
- **`@Preview`s live in the same file**, wrapped in `ChirpTheme`, usually covering light + dark and key states.
- **Text input uses `TextFieldState`**, never a raw `String` + `onValueChange` (avoids out-of-order characters when state is hoisted into a `StateFlow`).
- Baseline corner radius is `RoundedCornerShape(8.dp)`.

**Theming** (`…designsystem/theme/`):
- `ChirpTheme.kt` wraps `MaterialTheme(colorScheme, typography)` and provides `ExtendedColors` through `CompositionLocalProvider`.
- **Raw hex colors** are defined once in `Color.kt` (`ChirpBrand500`, `ChirpBase1000`…) and assigned to M3 slots in `Theme.kt` (`LightColorScheme`/`DarkColorScheme`).
- **Extended (non-M3) tokens** — button states, text variants, surfaces, accent & chat-bubble "cake" colors — live in the `@Immutable ExtendedColors` data class, exposed via `staticCompositionLocalOf` and reached with `MaterialTheme.colorScheme.extended.<token>`.
- **Typography** (`Type.kt`): custom `FontFamily` (Plus Jakarta Sans from `Res.font`) mapped onto a full M3 `Typography`; extra styles (`labelXSmall`, `titleXSmall`) added as `Typography` extension properties. Use `MaterialTheme.typography.<style>`.

## Example

Enum-driven, slot-based component (`core/ui/.../components/buttons/ChirpButton.kt`):

```kotlin
@Composable
fun ChirpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,                       // modifier after required params
    style: ChirpButtonStyle = ChirpButtonStyle.PRIMARY,  // variant via enum
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,       // nullable slot
) {
    val colors = when (style) {
        ChirpButtonStyle.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.extended.disabledFill,  // extended token
            …,
        )
        …
    }
    Button(onClick, modifier, enabled, shape = RoundedCornerShape(8.dp), colors = colors) { … }
}

@Preview @Composable
fun ChirpPrimaryButtonPreview() = ChirpTheme(darkTheme = true) {
    ChirpButton(text = "Hello world!", onClick = {}, style = ChirpButtonStyle.PRIMARY)
}
```

Accessing tokens in any composable:

```kotlin
MaterialTheme.colorScheme.primary                    // M3 slot
MaterialTheme.colorScheme.extended.textPlaceholder   // extended token
MaterialTheme.typography.titleSmall                  // typography style
```

## What to avoid

- ❌ Hardcoded `Color(0xFF…)` or magic `.dp` text sizes in screens. Use `MaterialTheme.colorScheme[.extended]` and `MaterialTheme.typography`.
- ❌ Using raw M3 `Button`/`TextField` directly in feature screens. Use the `Chirp*` wrappers.
- ❌ Exposing raw `color`/`border` params on a component. Add a value to the variant `enum` instead.
- ❌ `String` + `onValueChange` text fields. Use `TextFieldState` (the design-system fields require it).
- ❌ Defining a new color inline. Add the hex to `Color.kt`, then map it in `Theme.kt` (M3 slot) or `ExtendedColors` (custom token).
- ❌ Prop-drilling custom colors. They flow through the `LocalExtendedColors` CompositionLocal.
