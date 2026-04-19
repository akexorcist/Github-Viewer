package dev.akexorcist.githubviewer.data.util

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.network.AppException

fun Throwable.toAppError(): AppError = when (this) {
    is AppException -> error
    else -> AppError.NetworkError(this)
}
