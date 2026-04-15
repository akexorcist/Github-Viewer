package dev.akexorcist.githubviewer.core.common

import kotlin.time.Instant

sealed class AppError {
    data class NetworkError(val cause: Throwable) : AppError()
    data class HttpError(val code: Int, val message: String) : AppError()
    data class RateLimitError(val resetAt: Instant) : AppError()
    data object NotFoundError : AppError()
    data object UnknownError : AppError()
}
