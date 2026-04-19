package dev.akexorcist.githubviewer.presentation.repository

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.data.model.Repository
import kotlin.time.Instant

data class RepositoryDetailUiState(
    val repository: Repository? = null,
    val isLoading: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val error: AppError? = null,
)

sealed interface RepositoryDetailSnackbarEvent {
    data object NoInternet : RepositoryDetailSnackbarEvent
}
