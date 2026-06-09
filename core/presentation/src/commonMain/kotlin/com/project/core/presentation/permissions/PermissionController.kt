package com.project.core.presentation.permissions

/**
 * Defines the contract for requesting native system permissions across different platforms.
 *
 * ## Strategy / Decisions
 * Uses the expect/actual pattern to create a unified common interface for permission requests, allowing the common UI to request permissions without knowing the underlying platform details or library (Moco) implementation.
 *
 * ## How It Works
 * 1. Defines an `expect class` with a suspendable function `requestPermission`.
 * 2. Utilizes custom domain enums (`Permission.NOTIFICATIONS`) and states (`PermissionState.GRANTED`, `DENIED`, etc.) to decouple the core logic from external libraries.
 * * @param permission The specific domain permission to request (e.g., Notifications).
 * @return The resulting `PermissionState` after the user interacts with the system dialog.
 */
expect class PermissionController {
    suspend fun requestPermission(permission: Permission): PermissionState
}
