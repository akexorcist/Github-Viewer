package dev.akexorcist.githubviewer.core.common

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val hasNextPage: Boolean,
)

data class PagingState<T>(
    val items: List<T> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val isLoadingMore: Boolean = false,
) {
    fun appendPage(result: PageResult<T>): PagingState<T> = copy(
        items = items + result.items,
        currentPage = result.page,
        hasNextPage = result.hasNextPage,
        isLoadingMore = false,
    )

    fun loadingMore(): PagingState<T> = copy(isLoadingMore = true)
}

const val PAGE_SIZE = 30
