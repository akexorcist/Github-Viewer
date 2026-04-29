package dev.akexorcist.githubviewer.test.mock

import app.cash.turbine.test
import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.presentation.repository.ReadmeState
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailSnackbarEvent
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailViewModel
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// Mock-based tests for RepositoryDetailViewModel.
// Covers ReadmeState transitions, error states, snackbar events, cache-first behaviour,
// and refresh semantics — all cases that are hard or impossible to trigger with a real HTTP client.
class RepositoryDetailViewModelMockTest {

    private fun createViewModel(
        owner: String = "akexorcist",
        repo: String = "test-repo",
        repoRepo: FakeRepositoryRepository = FakeRepositoryRepository(),
    ) = RepositoryDetailViewModel(owner, repo, repoRepo)

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial readme state is Loading`() = runTest {
        val repoRepo = FakeRepositoryRepository(
            getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
            getReadmeImpl = { _, _, _ -> Channel<Result<String>>().receiveAsFlow() }, // never emits
        )
        val viewModel = createViewModel(repoRepo = repoRepo)
        viewModel.uiState.test {
            val initial = awaitItem()
            initial.readme.shouldBeInstanceOf<ReadmeState.Loading>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Repository load ──────────────────────────────────────────────────────

    @Test
    fun `init loads repository successfully`() = runTest {
        val repo = testRepository()
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(repo) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repository == null && state.error == null) state = awaitItem()
            state.repository shouldBe repo
            state.error.shouldBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets isLoading false after repository loads`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading || (state.repository == null && state.error == null)) state = awaitItem()
            state.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init sets lastUpdatedAt after successful repository load`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.lastUpdatedAt == null && state.error == null) state = awaitItem()
            state.lastUpdatedAt.shouldNotBeNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init emits cached repository first then fresh network data`() = runTest {
        val cached = testRepository("cached-repo")
        val fresh = testRepository("fresh-repo")
        val repoChannel = Channel<Result<Repository>>(Channel.UNLIMITED)
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> repoChannel.receiveAsFlow() },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            // Send cached first and wait for Turbine to confirm it before sending fresh.
            // This prevents StateFlow conflation from skipping the cached state.
            repoChannel.send(Result.Success(cached))
            var state = awaitItem()
            while (state.repository?.name != "cached-repo" && state.error == null) state = awaitItem()
            state.repository?.name shouldBe "cached-repo"

            repoChannel.send(Result.Success(fresh))
            var updated = awaitItem()
            while (updated.repository?.name != "fresh-repo" && updated.error == null) updated = awaitItem()
            updated.repository?.name shouldBe "fresh-repo"
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Repository error states ──────────────────────────────────────────────

    @Test
    fun `repository NetworkError sends NoInternet snackbar`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> errorFlow(networkError) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.snackbarEvent.test {
            awaitItem().shouldBeInstanceOf<RepositoryDetailSnackbarEvent.NoInternet>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository HttpError sets top-level error state`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> errorFlow(httpError) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.error == null && state.repository == null && state.isLoading) state = awaitItem()
            state.error.shouldBeInstanceOf<AppError.HttpError>()
            state.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `repository RateLimitError sets top-level error state`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> errorFlow(rateLimitError) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.error == null && state.repository == null && state.isLoading) state = awaitItem()
            state.error.shouldBeInstanceOf<AppError.RateLimitError>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── README state transitions ─────────────────────────────────────────────

    @Test
    fun `readme loads to Loaded state with content`() = runTest {
        val content = "# Hello\nThis is the README."
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> successFlow(content) },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            val loaded = state.readme.shouldBeInstanceOf<ReadmeState.Loaded>()
            loaded.content shouldBe content
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `readme with empty content becomes Loaded with empty string (no README available)`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            val loaded = state.readme.shouldBeInstanceOf<ReadmeState.Loaded>()
            loaded.content shouldBe ""
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `readme NetworkError on initial load becomes ReadmeState Error`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> errorFlow(networkError) },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            val error = state.readme.shouldBeInstanceOf<ReadmeState.Error>()
            error.error.shouldBeInstanceOf<AppError.NetworkError>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `readme HttpError on initial load becomes ReadmeState Error`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> errorFlow(httpError) },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            state.readme.shouldBeInstanceOf<ReadmeState.Error>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `readme RateLimitError on initial load becomes ReadmeState Error`() = runTest {
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> errorFlow(rateLimitError) },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            state.readme.shouldBeInstanceOf<ReadmeState.Error>()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test
    fun `refresh does not clear repository during re-fetch`() = runTest(timeout = 15.seconds) {
        val repo = testRepository()
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(repo) },
                getReadmeImpl = { _, _, _ -> successFlow("") },
            ),
        )
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.repository == null && state.error == null) state = awaitItem()
            val initialLastUpdated = state.lastUpdatedAt

            viewModel.onRefresh()
            var refreshed = awaitItem()
            while (refreshed.lastUpdatedAt == initialLastUpdated) {
                refreshed.repository.shouldNotBeNull()
                refreshed = awaitItem()
            }
            refreshed.repository shouldBe repo
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh preserves Loaded readme while re-fetching — does not regress to Loading`() = runTest(timeout = 15.seconds) {
        val readmeChannel = Channel<Result<String>>(Channel.UNLIMITED)
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> readmeChannel.receiveAsFlow() },
            ),
        )

        viewModel.uiState.test {
            // Initial load: emit the first README
            readmeChannel.send(Result.Success("# Initial README"))
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            state.readme.shouldBeInstanceOf<ReadmeState.Loaded>()

            // Refresh: readme should stay Loaded, not go back to Loading
            viewModel.onRefresh()
            val afterRefreshTrigger = awaitItem()
            afterRefreshTrigger.readme.shouldNotBeInstanceOf<ReadmeState.Loading>()

            // Complete the refresh by emitting updated README
            readmeChannel.send(Result.Success("# Updated README"))
            var updated = awaitItem()
            while ((updated.readme as? ReadmeState.Loaded)?.content != "# Updated README" && updated.error == null) {
                updated = awaitItem()
            }
            val loaded = updated.readme.shouldBeInstanceOf<ReadmeState.Loaded>()
            loaded.content shouldBe "# Updated README"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh failure with existing readme keeps Loaded state`() = runTest(timeout = 15.seconds) {
        val readmeChannel = Channel<Result<String>>(Channel.UNLIMITED)
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> readmeChannel.receiveAsFlow() },
            ),
        )

        viewModel.uiState.test {
            // Initial load succeeds
            readmeChannel.send(Result.Success("# README"))
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            val loadedContent = (state.readme as ReadmeState.Loaded).content

            // Refresh fails for README
            viewModel.onRefresh()
            readmeChannel.send(Result.Error(networkError))
            var refreshed = awaitItem()
            // Drain until readme settles (no longer changing from the refresh)
            while (refreshed.readme is ReadmeState.Loading) refreshed = awaitItem()

            // Loaded state must be preserved — failure during refresh must not overwrite existing content
            val stillLoaded = refreshed.readme.shouldBeInstanceOf<ReadmeState.Loaded>()
            stillLoaded.content shouldBe loadedContent
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `readme Error then refresh retries loading`() = runTest(timeout = 15.seconds) {
        val readmeChannel = Channel<Result<String>>(Channel.UNLIMITED)
        val viewModel = createViewModel(
            repoRepo = FakeRepositoryRepository(
                getRepositoryImpl = { _, _, _ -> successFlow(testRepository()) },
                getReadmeImpl = { _, _, _ -> readmeChannel.receiveAsFlow() },
            ),
        )

        viewModel.uiState.test {
            // Initial load fails
            readmeChannel.send(Result.Error(httpError))
            var state = awaitItem()
            while (state.readme is ReadmeState.Loading && state.error == null) state = awaitItem()
            state.readme.shouldBeInstanceOf<ReadmeState.Error>()

            // Refresh: since readme is in Error (not Loaded), it should go back to Loading
            viewModel.onRefresh()
            val retrying = awaitItem()
            retrying.readme.shouldBeInstanceOf<ReadmeState.Loading>()

            // Refresh succeeds
            readmeChannel.send(Result.Success("# Recovered README"))
            var recovered = awaitItem()
            while (recovered.readme is ReadmeState.Loading && recovered.error == null) recovered = awaitItem()
            val loaded = recovered.readme.shouldBeInstanceOf<ReadmeState.Loaded>()
            loaded.content shouldBe "# Recovered README"
            cancelAndIgnoreRemainingEvents()
        }
    }
}
