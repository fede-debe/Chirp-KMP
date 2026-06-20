package com.project.core.presentation.permissions

import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

/**
 * Mobile-specific implementation of the PermissionController utilizing the Moco permissions library.
 *
 * ## Strategy / Decisions
 * Centralizes the Android and iOS permission logic into a single `mobileMain` source set. Moco is used to abstract away the boilerplate associated with handling native OS permission dialogs.
 *
 * ## How It Works
 * 1. Maps the domain-level `Permission` (e.g., Notifications) to Moco's specific requirement (`RemoteNotification`).
 * 2. Invokes Moco's internal permission controller inside a `try/catch` block.
 * 3. On success, returns `PermissionState.GRANTED`.
 * 4. On failure, catches Moco-specific exceptions and maps them to our domain `PermissionState`.
 *
 * ## Technical Details
 * - **Exception Hierarchy Constraint:** The order of the `catch` blocks is critical. `DeniedAlwaysException` is a subtype of `DeniedException`. Therefore, `DeniedAlwaysException` MUST be caught first to accurately return `PermissionState.PERMANENTLY_DENIED`. If `DeniedException` is caught first, it will swallow the permanent denial.
 * - **Cancellation:** Handles `RequestCanceledException` to gracefully manage scenarios where the user dismisses the system dialog without making a choice.
 */
actual class PermissionController(
    private val mokoPermissionsController: PermissionsController,
) {
    actual suspend fun requestPermission(permission: Permission): PermissionState {
        return try {
            mokoPermissionsController.providePermission(permission.toMokoPermission())
            PermissionState.GRANTED
        } catch (_: DeniedAlwaysException) {
            PermissionState.PERMANENTLY_DENIED
        } catch (_: DeniedException) {
            PermissionState.DENIED
        } catch (_: RequestCanceledException) {
            PermissionState.DENIED
        }
    }
}

fun Permission.toMokoPermission(): dev.icerock.moko.permissions.Permission {
    return when (this) {
        Permission.NOTIFICATIONS -> dev.icerock.moko.permissions.Permission.REMOTE_NOTIFICATION
    }
}
