package com.project.core.domain.util

class DataErrorException(
    val error: DataError,
) : Exception()
