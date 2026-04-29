package dev.akexorcist.githubviewer.presentation.repository

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.data.model.Repository
import kotlin.time.Instant

sealed interface ReadmeState {
    data object Loading : ReadmeState
    data class Loaded(val content: String) : ReadmeState
    data class Error(val error: AppError) : ReadmeState
}

data class RepositoryDetailUiState(
    val repository: Repository? = null,
    val isLoading: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val error: AppError? = null,
    val readme: ReadmeState = ReadmeState.Loading,
)

sealed interface RepositoryDetailSnackbarEvent {
    data object NoInternet : RepositoryDetailSnackbarEvent
}
