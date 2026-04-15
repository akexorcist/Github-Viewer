package dev.akexorcist.githubviewer.presentation.profile

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PagingState
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.model.User
import kotlin.time.Instant

data class UserProfileUiState(
    val user: User? = null,
    val repositories: PagingState<Repository> = PagingState(),
    val isLoading: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val error: AppError? = null,
)

sealed class UserProfileSnackbarEvent {
    data object NoInternet : UserProfileSnackbarEvent()
}
