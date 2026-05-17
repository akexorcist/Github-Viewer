package dev.akexorcist.githubviewer.test.mock

import app.cash.turbine.test
import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.presentation.search.SearchSnackbarEvent
import dev.akexorcist.githubviewer.presentation.search.SearchViewModel
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// Mock-based tests for SearchViewModel.
// All search triggers use onSearchClick() (bypasses the 500ms debounce) so results
// are deterministic without real-time dependencies.
// Covers error states, snackbar events, pagination, and section independence —
// all edge cases that the real-HTTP integration tests cannot trigger on demand.
class SearchViewModelMockTest {

    private fun createViewModel(
        userRepo: FakeUserRepository = FakeUserRepository(),
        repoRepo: FakeRepositoryRepository = FakeRepositoryRepository(),
    ) = SearchViewModel(userRepo, repoRepo)

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial state has empty query, empty sections, no loading`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        state.query shouldBe ""
        state.users.items.shouldBeEmpty()
        state.repositories.items.shouldBeEmpty()
        state.users.isLoading.shouldBeFalse()
        state.repositories.isLoading.shouldBeFalse()
        state.users.error.shouldBeNull()
        state.repositories.error.shouldBeNull()
    }

    // ─── Query changes ────────────────────────────────────────────────────────

    @Test
    fun `onQueryChange updates query in state`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("kotlin")
            val state = awaitItem()
            state.query shouldBe "kotlin"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blank query clears sections without triggering search`() = runTest {
        val searchCount = mutableListOf<String>()
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { q, _ ->
                searchCount += q
                Result.Success(testUserPageResult(listOf(testSearchUserItem())))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("  ")
            var state = awaitItem()
            while (state.query != "  ") state = awaitItem()
            state.users.items.shouldBeEmpty()
            state.repositories.items.shouldBeEmpty()
            searchCount.shouldBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchClick with blank query does nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("")
            viewModel.onSearchClick()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setting query to empty clears both sections`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Success(testUserPageResult(listOf(testSearchUserItem())))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(listOf(testRepository())))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("kotlin")
            viewModel.onSearchClick()
            var state = awaitItem()
            while ((state.users.items.isEmpty() && state.users.error == null) ||
                (state.repositories.items.isEmpty() && state.repositories.error == null)) state = awaitItem()
            state.users.items.shouldNotBeEmpty()
            state.repositories.items.shouldNotBeEmpty()

            viewModel.onQueryChange("")
            var cleared = awaitItem()
            while (cleared.users.items.isNotEmpty() || cleared.repositories.items.isNotEmpty()) {
                cleared = awaitItem()
            }
            cleared.users.items.shouldBeEmpty()
            cleared.repositories.items.shouldBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Successful search ────────────────────────────────────────────────────

    @Test
    fun `onSearchClick populates both sections with results`() = runTest {
        val users = listOf(testSearchUserItem("alice", 1), testSearchUserItem("bob", 2))
        val repos = listOf(testRepository("repo-a", 1), testRepository("repo-b", 2))
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Success(testUserPageResult(users))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(repos))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while ((state.users.items.isEmpty() && state.users.error == null) ||
                (state.repositories.items.isEmpty() && state.repositories.error == null)) state = awaitItem()
            state.users.items shouldBe users
            state.repositories.items shouldBe repos
            state.users.error.shouldBeNull()
            state.repositories.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchClick sets isLoading true then false for each section`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Success(testUserPageResult(listOf(testSearchUserItem())))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(listOf(testRepository())))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            // Drain past query-change state to the loading state
            var loading = awaitItem()
            while (!loading.users.isLoading && !loading.repositories.isLoading) loading = awaitItem()
            loading.users.isLoading.shouldBeTrue()
            loading.repositories.isLoading.shouldBeTrue()
            var done = awaitItem()
            while (done.users.isLoading || done.repositories.isLoading) done = awaitItem()
            done.users.isLoading.shouldBeFalse()
            done.repositories.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hasNextPage is true when section returns 30 or more items`() = runTest {
        val thirtyUsers = (1..30).map { testSearchUserItem("user$it", it.toLong()) }
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Success(testUserPageResult(thirtyUsers, hasNextPage = true))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(emptyList()))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.users.isLoading || (state.users.items.isEmpty() && state.users.error == null)) state = awaitItem()
            state.users.hasNextPage.shouldBeTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Error states ─────────────────────────────────────────────────────────

    @Test
    fun `users NetworkError sets users section error and sends snackbar`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Error(networkError)
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(emptyList()))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.users.error == null) state = awaitItem()
            state.users.error.shouldBeInstanceOf<AppError.NetworkError>()
            state.users.items.shouldBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.snackbarEvent.test {
            awaitItem().shouldBeInstanceOf<SearchSnackbarEvent.NoInternet>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repositories RateLimitError sets repositories section error without snackbar`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Success(testUserPageResult(emptyList()))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Error(rateLimitError)
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.repositories.error == null) state = awaitItem()
            state.repositories.error.shouldBeInstanceOf<AppError.RateLimitError>()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.snackbarEvent.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `users HttpError sets users section error without snackbar`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Error(httpError)
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(emptyList()))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.users.error == null) state = awaitItem()
            state.users.error.shouldBeInstanceOf<AppError.HttpError>()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.snackbarEvent.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `new search clears previous section errors`() = runTest {
        var callCount = 0
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                callCount++
                if (callCount == 1) Result.Error(httpError)
                else Result.Success(testUserPageResult(listOf(testSearchUserItem())))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(emptyList()))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.users.error == null) state = awaitItem()
            state.users.error.shouldNotBeNull()

            viewModel.onQueryChange("kotlin")
            viewModel.onSearchClick()
            var refreshed = awaitItem()
            while (refreshed.users.items.isEmpty()) refreshed = awaitItem()
            refreshed.users.error.shouldBeNull()
            refreshed.users.items.shouldNotBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Section independence ─────────────────────────────────────────────────

    @Test
    fun `users error does not affect repositories section`() = runTest {
        val repos = listOf(testRepository())
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Error(networkError)
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(repos))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            // Wait until users section has an error AND repos section has items or error
            while (state.users.error == null || (state.repositories.items.isEmpty() && state.repositories.error == null)) state = awaitItem()
            state.users.error.shouldNotBeNull()
            state.repositories.items shouldBe repos
            state.repositories.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Pagination ───────────────────────────────────────────────────────────

    @Test
    fun `onLoadMoreUsers appends next page to users section`() = runTest {
        val page1 = (1..30).map { testSearchUserItem("user$it", it.toLong()) }
        val page2 = listOf(testSearchUserItem("user31", 31))
        var page = 0
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, p ->
                page = p
                if (p == 1) Result.Success(testUserPageResult(page1, hasNextPage = true))
                else Result.Success(testUserPageResult(page2, hasNextPage = false))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(emptyList()))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.users.isLoading || (state.users.items.isEmpty() && state.users.error == null)) state = awaitItem()
            state.users.items shouldHaveSize 30

            viewModel.onLoadMoreUsers()
            var more = awaitItem()
            while (more.users.isLoadingMore || more.users.items.size <= 30) more = awaitItem()
            more.users.items shouldHaveSize 31
            more.users.hasNextPage.shouldBeFalse()
            more.users.isLoadingMore.shouldBeFalse()
            more.users.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onLoadMoreRepositories appends next page to repositories section`() = runTest {
        val page1 = (1..30).map { testRepository("repo$it", it.toLong()) }
        val page2 = listOf(testRepository("repo31", 31))
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Success(testUserPageResult(emptyList()))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, p ->
                if (p == 1) Result.Success(testPageResult(page1, hasNextPage = true))
                else Result.Success(testPageResult(page2, hasNextPage = false))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.repositories.isLoading || (state.repositories.items.isEmpty() && state.repositories.error == null)) state = awaitItem()
            state.repositories.items shouldHaveSize 30

            viewModel.onLoadMoreRepositories()
            var more = awaitItem()
            while (more.repositories.isLoadingMore || more.repositories.items.size <= 30) more = awaitItem()
            more.repositories.items shouldHaveSize 31
            more.repositories.hasNextPage.shouldBeFalse()
            more.repositories.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onLoadMoreUsers does nothing when hasNextPage is false`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                Result.Success(testUserPageResult(listOf(testSearchUserItem()), hasNextPage = false))
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(emptyList()))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.users.isLoading || state.repositories.isLoading || (state.users.items.isEmpty() && state.users.error == null)) state = awaitItem()
            state.users.hasNextPage.shouldBeFalse()

            viewModel.onLoadMoreUsers()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load more error sets isLoadingMore false and section error`() = runTest {
        val page1 = (1..30).map { testSearchUserItem("user$it", it.toLong()) }
        var callCount = 0
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(searchUsersImpl = { _, _ ->
                callCount++
                if (callCount == 1) Result.Success(testUserPageResult(page1, hasNextPage = true))
                else Result.Error(networkError)
            }),
            repoRepo = FakeRepositoryRepository(searchRepositoriesImpl = { _, _ ->
                Result.Success(testPageResult(emptyList()))
            }),
        )
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("android")
            viewModel.onSearchClick()
            var state = awaitItem()
            while (state.users.isLoading || (state.users.items.isEmpty() && state.users.error == null)) state = awaitItem()

            viewModel.onLoadMoreUsers()
            var more = awaitItem()
            // Phase 1: skip stale states from the concurrent repos fetch that may arrive
            // after the users drain exits but before load-more emits its own states.
            while (!more.users.isLoadingMore && more.users.error == null) more = awaitItem()
            // Phase 2: wait for load-more to finish (covers both conflated and sequential cases)
            while (more.users.isLoadingMore) more = awaitItem()
            more.users.isLoadingMore.shouldBeFalse()
            more.users.error.shouldBeInstanceOf<AppError.NetworkError>()
            more.users.items shouldHaveSize 30
            cancelAndIgnoreRemainingEvents()
        }
    }
}
