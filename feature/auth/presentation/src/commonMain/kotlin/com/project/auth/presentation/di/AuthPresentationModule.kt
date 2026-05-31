package com.project.auth.presentation.di

import com.project.auth.presentation.ui.emailVerification.EmailVerificationViewModel
import com.project.auth.presentation.ui.forgotPassword.ForgotPasswordViewModel
import com.project.auth.presentation.ui.login.LoginViewModel
import com.project.auth.presentation.ui.register.RegisterViewModel
import com.project.auth.presentation.ui.registerSuccess.RegisterSuccessViewModel
import com.project.auth.presentation.ui.resetPassword.ResetPasswordViewModel
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
    viewModelOf(::EmailVerificationViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::ResetPasswordViewModel)
}
