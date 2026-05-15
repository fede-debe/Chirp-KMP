package com.project.auth.presentation.di

import com.project.auth.presentation.register.RegisterViewModel
import com.project.auth.presentation.register_success.RegisterSuccessViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Dependency injection module for Authentication UI components.
 *
 * ## Strategy / Decisions
 * * **ViewModel Injection:** Uses `viewModelOf` to handle the lifecycle of view models.
 *   This ensures that Koin respects the platform-specific lifecycle of ViewModels (e.g.,
 *   surviving configuration changes on Android).
 *
 * ## Alternatives / Why Not
 * * **Manual ViewModel Instantiation:** Rejected because Koin manages the dependency
 *   graph automatically, preventing "constructor hell" where one would manually pass
 *   services into every screen.
 */
val authPresentationModule = module {
    viewModelOf(::RegisterViewModel)
    viewModelOf(::RegisterSuccessViewModel)
}
