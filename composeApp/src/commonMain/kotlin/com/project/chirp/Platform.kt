package com.project.chirp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform