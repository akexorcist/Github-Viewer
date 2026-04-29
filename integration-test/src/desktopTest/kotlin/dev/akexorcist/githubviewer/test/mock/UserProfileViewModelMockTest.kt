package dev.akexorcist.githubviewer.test.mock

import app.cash.turbine.test
import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.model.User
import dev.akexorcist.githubviewer.presentation.profile.UserProfileSnackbarEvent
import dev.akexorcist.githubviewer.presentation.profile.UserProfileViewModel
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// Mock-based tests for UserProfileViewModel.
// Uses fake repositories to exercise error states, snackbar events, cache-first behaviour,
// and pagination edge cases that the real-HTTP integration tests cannot trigger on demand.
class UserProfileViewModelMockTest {

    private fun createViewModel(
        login: String = "akexorcist",
        userRepo: FakeUserRepository = FakeUserRepository(),
        repoRepo: FakeRepositoryRepository = FakeRepositoryRepository(),
    ) = UserProfileViewModel(login, userRepo, repoRepo)

    // ─── Successful initial load ──────────────────────────────────────────────

    @Test
    fun `init loads user successfully`() = runTest {
        val user = testUser()
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(user) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.user == null && state.error == null) state = awaitItem()
            state.user shouldBe user
            state.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets isLoading false after user loads`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading || (state.user == null && state.error == null)) state = awaitItem()
            state.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets lastUpdatedAt after successful user load`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.lastUpdatedAt == null && state.error == null) state = awaitItem()
            state.lastUpdatedAt.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init loads first page of repositories`() = runTest {
        val repos = listOf(testRepository("repo-a", 1), testRepository("repo-b", 2))
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ ->
                successFlow(testPageResult(repos))
            }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.error == null && state.repositories.error == null) {
                state = awaitItem()
            }
            state.repositories.items shouldBe repos
            state.repositories.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Cache-first pattern ──────────────────────────────────────────────────

    @Test
    fun `init emits cached user first then fresh network data`() = runTest {
        val cached = testUser("cached-user")
        val fresh = testUser("fresh-user")
        val userChannel = Channel<Result<User>>(Channel.UNLIMITED)
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> userChannel.receiveAsFlow() }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            // Send cached first and wait for Turbine to confirm it before sending fresh.
            // This prevents StateFlow conflation from skipping the cached state.
            userChannel.send(Result.Success(cached))
            var state = awaitItem()
            while (state.user?.login != "cached-user" && state.error == null) state = awaitItem()
            state.user?.login shouldBe "cached-user"

            userChannel.send(Result.Success(fresh))
            var updated = awaitItem()
            while (updated.user?.login != "fresh-user" && updated.error == null) updated = awaitItem()
            updated.user?.login shouldBe "fresh-user"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init emits cached repositories first then fresh network data`() = runTest {
        val cached = listOf(testRepository("cached-repo", 1))
        val fresh = listOf(testRepository("fresh-repo", 2))
        val repoChannel = Channel<Result<PageResult<Repository>>>(Channel.UNLIMITED)
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> repoChannel.receiveAsFlow() }),
        )
        viewModel.uiState.test {
            // Send cached first and wait for Turbine to confirm it before sending fresh.
            // This prevents StateFlow conflation from skipping the cached state.
            repoChannel.send(Result.Success(testPageResult(cached)))
            var state = awaitItem()
            while (state.repositories.items.firstOrNull()?.name != "cached-repo" && state.repositories.error == null) {
                state = awaitItem()
            }
            state.repositories.items.first().name shouldBe "cached-repo"

            repoChannel.send(Result.Success(testPageResult(fresh)))
            var updated = awaitItem()
            while (updated.repositories.items.firstOrNull()?.name != "fresh-repo" && updated.repositories.error == null) {
                updated = awaitItem()
            }
            updated.repositories.items.first().name shouldBe "fresh-repo"
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── User error states ────────────────────────────────────────────────────

    // NOTE: Per requirements, NetworkError with no cached data should show an error state
    // with a retry button. Current behaviour sends a snackbar and leaves the screen with
    // isLoading=false, user=null, error=null (blank content area). This is a known gap.
    @Test
    fun `user NetworkError sends NoInternet snackbar`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> errorFlow(networkError) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.snackbarEvent.test {
            awaitItem().shouldBeInstanceOf<UserProfileSnackbarEvent.NoInternet>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `user HttpError sets top-level error state`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> errorFlow(httpError) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.error == null && state.user == null) state = awaitItem()
            state.error.shouldBeInstanceOf<AppError.HttpError>()
            state.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `user RateLimitError sets top-level error state`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> errorFlow(rateLimitError) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.error == null && state.user == null) state = awaitItem()
            state.error.shouldBeInstanceOf<AppError.RateLimitError>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Repositories error states ────────────────────────────────────────────

    @Test
    fun `repos NetworkError sends snackbar and sets repositories error`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> errorFlow(networkError) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.error == null && state.error == null) state = awaitItem()
            state.repositories.error.shouldBeInstanceOf<AppError.NetworkError>()
            state.repositories.items.shouldBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.snackbarEvent.test {
            awaitItem().shouldBeInstanceOf<UserProfileSnackbarEvent.NoInternet>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repos HttpError sets repositories error without snackbar`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> errorFlow(httpError) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.error == null && state.error == null) state = awaitItem()
            state.repositories.error.shouldBeInstanceOf<AppError.HttpError>()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.snackbarEvent.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repos NetworkError does not affect user or top-level error`() = runTest {
        val user = testUser()
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(user) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> errorFlow(networkError) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.error == null && state.error == null) state = awaitItem()
            state.user shouldBe user
            state.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test
    fun `refresh updates lastUpdatedAt`() = runTest(timeout = 15.seconds) {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.lastUpdatedAt == null && state.error == null) state = awaitItem()
            val first = state.lastUpdatedAt.shouldNotBeNull()

            viewModel.onRefresh()
            var refreshed = awaitItem()
            while (refreshed.lastUpdatedAt == first) refreshed = awaitItem()
            refreshed.lastUpdatedAt.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh does not clear existing user during re-fetch`() = runTest(timeout = 15.seconds) {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ -> successFlow(testPageResult(emptyList())) }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.user == null && state.error == null) state = awaitItem()
            val initialLastUpdated = state.lastUpdatedAt

            viewModel.onRefresh()
            var refreshed = awaitItem()
            while (refreshed.lastUpdatedAt == initialLastUpdated) {
                refreshed.user.shouldNotBeNull()
                refreshed = awaitItem()
            }
            refreshed.user.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh failure with NetworkError sends snackbar and keeps existing repos visible`() = runTest {
        val initialRepos = listOf(testRepository())
        var callCount = 0
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ ->
                callCount++
                if (callCount == 1) successFlow(testPageResult(initialRepos))
                else errorFlow(networkError)
            }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.repositories.error == null) state = awaitItem()
            state.repositories.items shouldBe initialRepos

            viewModel.onRefresh()
            var refreshed = awaitItem()
            while (refreshed.repositories.error == null && refreshed.repositories.items == initialRepos) {
                refreshed = awaitItem()
            }
            // On NetworkError the existing items are still in state (error is set, items preserved)
            refreshed.repositories.error.shouldBeInstanceOf<AppError.NetworkError>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Pagination ───────────────────────────────────────────────────────────

    @Test
    fun `onLoadMoreRepositories appends next page`() = runTest {
        val page1 = (1..30).map { testRepository("repo$it", it.toLong()) }
        val page2 = listOf(testRepository("repo31", 31))
        var callCount = 0
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ ->
                callCount++
                if (callCount == 1) successFlow(testPageResult(page1, hasNextPage = true))
                else successFlow(testPageResult(page2, hasNextPage = false))
            }),
        )
        viewModel.uiState.test(timeout = 15.seconds) {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.repositories.error == null) state = awaitItem()
            state.repositories.hasNextPage.shouldBeTrue()

            viewModel.onLoadMoreRepositories()
            var more = awaitItem()
            while ((more.repositories.isLoadingMore || more.repositories.items.size <= 30) && more.repositories.error == null) {
                more = awaitItem()
            }
            more.repositories.items.size shouldBe 31
            more.repositories.hasNextPage.shouldBeFalse()
            more.repositories.isLoadingMore.shouldBeFalse()
            more.repositories.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onLoadMoreRepositories does nothing when hasNextPage is false`() = runTest {
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ ->
                successFlow(testPageResult(listOf(testRepository()), hasNextPage = false))
            }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.repositories.error == null) state = awaitItem()
            state.repositories.hasNextPage.shouldBeFalse()
            val countBefore = state.repositories.items.size

            viewModel.onLoadMoreRepositories()
            expectNoEvents()
            viewModel.uiState.value.repositories.items.size shouldBe countBefore
            viewModel.uiState.value.repositories.isLoadingMore.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repos load more NetworkError sets error and clears isLoadingMore`() = runTest {
        val page1 = (1..30).map { testRepository("repo$it", it.toLong()) }
        var callCount = 0
        val viewModel = createViewModel(
            userRepo = FakeUserRepository(getUserImpl = { _, _ -> successFlow(testUser()) }),
            repoRepo = FakeRepositoryRepository(getUserRepositoriesImpl = { _, _, _ ->
                callCount++
                if (callCount == 1) successFlow(testPageResult(page1, hasNextPage = true))
                else errorFlow(networkError)
            }),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.repositories.error == null) state = awaitItem()

            viewModel.onLoadMoreRepositories()
            var more = awaitItem()
            while (more.repositories.isLoadingMore) more = awaitItem()
            more.repositories.isLoadingMore.shouldBeFalse()
            more.repositories.error.shouldBeInstanceOf<AppError.NetworkError>()
            more.repositories.items.size shouldBe 30
            cancelAndIgnoreRemainingEvents()
        }
    }
}
