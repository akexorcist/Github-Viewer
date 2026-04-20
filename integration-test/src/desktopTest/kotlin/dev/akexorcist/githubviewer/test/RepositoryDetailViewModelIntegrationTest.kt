package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailViewModel
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// Dependencies are shared across all tests in this class. The shared in-memory
// database acts as a cache: only the first ViewModel creation fetches from the
// network; all subsequent ones serve from the Room cache. This keeps the total
// API call count well within GitHub's unauthenticated rate limits.
class RepositoryDetailViewModelIntegrationTest {

    companion object {
        private val database = TestDependencies.createDatabase()
        private val apiService = TestDependencies.createApiService()
        private val repositoryRepository: RepositoryRepository =
            TestDependencies.createRepositoryRepository(apiService, database)
    }

    // Each test creates a fresh ViewModel to avoid shared ViewModel state,
    // while still benefiting from the shared database cache.
    private fun createViewModel(
        owner: String = TestEnvironment.testRepoOwner,
        repo: String = TestEnvironment.testRepo,
    ): RepositoryDetailViewModel =
        TestDependencies.createRepositoryDetailViewModel(owner, repo, repositoryRepository)

    // ─── init / initial load ─────────────────────────────────────────────────

    @Test
    fun `init loads repository and sets non-null repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repository == null && state.error == null) {
                state = awaitItem()
            }
            state.repository.shouldNotBeNull()
            state.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets isLoading to false after load completes`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading || (state.repository == null && state.error == null)) {
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
    fun `init loads repository with correct fields`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repository == null && state.error == null) {
                state = awaitItem()
            }
            val repo = state.repository.shouldNotBeNull()
            repo.name shouldBe TestEnvironment.testRepo
            repo.fullName shouldBe "${TestEnvironment.testRepoOwner}/${TestEnvironment.testRepo}"
            repo.ownerLogin shouldBe TestEnvironment.testRepoOwner
            repo.id shouldBeGreaterThan 0L
            repo.ownerAvatarUrl shouldStartWith "https://"
            repo.stars shouldBeGreaterThanOrEqualTo 0
            repo.forks shouldBeGreaterThanOrEqualTo 0
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── onRefresh ────────────────────────────────────────────────────────────

    @Test
    fun `onRefresh updates lastUpdatedAt with a newer timestamp`() = runTest {
        val viewModel = createViewModel()
        // Longer timeout: onRefresh triggers a network call on top of the still-running
        // init network fetch; both run concurrently and two HTTP round-trips are expected.
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
            refreshed.repository.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── README loading ──────────────────────────────────────────────────────

    @Test
    fun `init loads readme content after repository loads`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.readmeContent == null && state.error == null) {
                state = awaitItem()
            }
            state.readmeContent.shouldNotBeNull()
            state.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets isReadmeLoading to false after readme loads`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.readmeContent == null && state.error == null) {
                state = awaitItem()
            }
            state.isReadmeLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRefresh updates readme content`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test(timeout = 15.seconds) {
            var state = awaitItem()
            while (state.readmeContent == null && state.error == null) {
                state = awaitItem()
            }
            val firstReadme = state.readmeContent.shouldNotBeNull()

            viewModel.onRefresh()

            // After refresh, readme must still be non-null (cache serves first, then network update)
            var refreshed = awaitItem()
            while (refreshed.isReadmeLoading) {
                refreshed = awaitItem()
            }
            refreshed.readmeContent.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRefresh does not clear the loaded repository while re-fetching`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test(timeout = 15.seconds) {
            var state = awaitItem()
            while (state.repository == null && state.error == null) {
                state = awaitItem()
            }
            val initialRepo = state.repository.shouldNotBeNull()
            val initialLastUpdated = state.lastUpdatedAt

            viewModel.onRefresh()

            // Repository must never become null during the refresh
            var refreshed = awaitItem()
            while (refreshed.lastUpdatedAt == initialLastUpdated) {
                refreshed.repository.shouldNotBeNull()
                refreshed = awaitItem()
            }
            val repo = refreshed.repository.shouldNotBeNull()
            repo.fullName shouldBe initialRepo.fullName
            cancelAndIgnoreRemainingEvents()
        }
    }
}
