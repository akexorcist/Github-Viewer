package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import dev.akexorcist.githubviewer.data.repository.UserRepository
import dev.akexorcist.githubviewer.presentation.profile.UserProfileViewModel
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldStartWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// Dependencies are shared across all tests in this class. The shared in-memory
// database acts as a cache: only the first ViewModel creation fetches from the
// network; all subsequent ones serve from the Room cache. This keeps the total
// API call count well within GitHub's unauthenticated rate limits.
class UserProfileViewModelIntegrationTest {

    companion object {
        private val database = TestDependencies.createDatabase()
        private val apiService = TestDependencies.createApiService()
        private val userRepository: UserRepository =
            TestDependencies.createUserRepository(apiService, database)
        private val repositoryRepository: RepositoryRepository =
            TestDependencies.createRepositoryRepository(apiService, database)
    }

    // Each test creates a fresh ViewModel to avoid shared ViewModel state,
    // while still benefiting from the shared database cache.
    private fun createViewModel(login: String = TestEnvironment.testUser): UserProfileViewModel =
        TestDependencies.createUserProfileViewModel(login, userRepository, repositoryRepository)

    // ─── init / initial load ─────────────────────────────────────────────────

    @Test
    fun `init loads user profile and sets non-null user`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.user == null && state.error == null) {
                state = awaitItem()
            }
            val user = state.user.shouldNotBeNull()
            user.login shouldBe TestEnvironment.testUser
            state.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets isLoading to false after load completes`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading || (state.user == null && state.error == null)) {
                state = awaitItem()
            }
            state.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets lastUpdatedAt after successful load`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.lastUpdatedAt == null && state.error == null) {
                state = awaitItem()
            }
            state.lastUpdatedAt.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init loads user profile with correct fields`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.user == null && state.error == null) {
                state = awaitItem()
            }
            val user = state.user.shouldNotBeNull()
            user.login shouldBe TestEnvironment.testUser
            user.avatarUrl shouldStartWith "https://"
            user.publicRepos shouldBeGreaterThanOrEqualTo 0
            user.followers shouldBeGreaterThanOrEqualTo 0
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init loads first page of repositories`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.error == null) {
                state = awaitItem()
            }
            state.repositories.items.shouldNotBeEmpty()
            state.repositories.items.first().name.shouldNotBeBlank()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── onRefresh ────────────────────────────────────────────────────────────

    @Test
    fun `onRefresh updates lastUpdatedAt with a newer timestamp`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test(timeout = 15.seconds) {
            var state = awaitItem()
            while (state.lastUpdatedAt == null && state.error == null) {
                state = awaitItem()
            }
            val firstUpdatedAt = state.lastUpdatedAt.shouldNotBeNull()

            viewModel.onRefresh()

            var refreshed = awaitItem()
            while (refreshed.lastUpdatedAt == firstUpdatedAt) {
                refreshed = awaitItem()
            }
            val updatedAt = refreshed.lastUpdatedAt.shouldNotBeNull()
            updatedAt shouldBeGreaterThanOrEqualTo firstUpdatedAt
            refreshed.user.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRefresh does not clear the loaded user while re-fetching`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test(timeout = 15.seconds) {
            var state = awaitItem()
            while (state.user == null && state.error == null) {
                state = awaitItem()
            }
            state.user.shouldNotBeNull()
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

    // ─── pagination ───────────────────────────────────────────────────────────

    @Test
    fun `onLoadMoreRepositories appends next page when hasNextPage is true`() = runTest {
        val viewModel = createViewModel()
        // Longer timeout: ViewModel init fires a concurrent page-1 network refresh while the
        // load-more fetches page 2; if the page-1 refresh arrives first it resets state and
        // the test must wait for the page-2 response as well — two sequential HTTP calls.
        viewModel.uiState.test(timeout = 15.seconds) {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.error == null) {
                state = awaitItem()
            }

            if (!state.repositories.hasNextPage) {
                cancelAndIgnoreRemainingEvents()
                return@test
            }

            val page1Count = state.repositories.items.size
            viewModel.onLoadMoreRepositories()

            var moreState = awaitItem()
            while (moreState.repositories.isLoadingMore || moreState.repositories.items.size <= page1Count) {
                moreState = awaitItem()
            }

            moreState.repositories.items.size shouldBeGreaterThanOrEqualTo page1Count + 1
            moreState.repositories.isLoadingMore.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onLoadMoreRepositories does nothing when hasNextPage is false`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repositories.items.isEmpty() && state.error == null) {
                state = awaitItem()
            }

            if (state.repositories.hasNextPage) {
                cancelAndIgnoreRemainingEvents()
                return@test
            }

            val countBefore = state.repositories.items.size
            viewModel.onLoadMoreRepositories()
            cancelAndIgnoreRemainingEvents()

            viewModel.uiState.value.repositories.items.size shouldBe countBefore
            viewModel.uiState.value.repositories.isLoadingMore.shouldBeFalse()
        }
    }
}
