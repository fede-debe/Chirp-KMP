package com.project.core.data.networking

import com.project.core.shared.BuildKonfig

// Endpoints come from BuildKonfig (sourced from local.properties, required), so no backend URL is
// hardcoded in source. Set BASE_URL_HTTP / BASE_URL_WS per project in local.properties.
object UrlConstants {
    val BASE_URL_HTTP = BuildKonfig.BASE_URL_HTTP
    val BASE_URL_WS = BuildKonfig.BASE_URL_WS
}
