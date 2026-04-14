package dev.akexorcist.githubviewer.presentation.search

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.model.SearchUserItem

data class SearchUiState(
    val query: String = "",
    val users: SectionState<SearchUserItem> = SectionState(),
    val repositories: SectionState<Repository> = SectionState(),
)

data class SectionState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
)

sealed class SearchSnackbarEvent {
    data object NoInternet : SearchSnackbarEvent()
}
