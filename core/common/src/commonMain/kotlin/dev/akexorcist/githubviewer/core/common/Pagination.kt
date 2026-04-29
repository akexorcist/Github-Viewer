package dev.akexorcist.githubviewer.core.common

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val hasNextPage: Boolean,
)

data class PagingState<T>(
    val items: List<T> = emptyList(),
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
) {
    fun appendPage(result: PageResult<T>): PagingState<T> = copy(
        items = items + result.items,
        hasNextPage = result.hasNextPage,
        isLoadingMore = false,
        error = null,
    )

    fun loadingMore(): PagingState<T> = copy(isLoadingMore = true, error = null)
}

const val PAGE_SIZE = 30
