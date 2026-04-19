package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import dev.akexorcist.githubviewer.data.repository.UserRepository
import dev.akexorcist.githubviewer.presentation.search.SearchViewModel
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldStartWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

// Base resources (DB, HTTP client) are shared across tests; the ViewModel is
// re-created per test to guarantee a clean query state and empty result sections.
class SearchViewModelIntegrationTest {

    companion object {
        private val database = TestDependencies.createDatabase()
        private val apiService = TestDependencies.createApiService()
    }

    private lateinit var userRepository: UserRepository
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var viewModel: SearchViewModel

    @BeforeTest
    fun setup() {
        userRepository = TestDependencies.createUserRepository(apiService, database)
        repositoryRepository = TestDependencies.createRepositoryRepository(apiService, database)
        viewModel = TestDependencies.createSearchViewModel(userRepository, repositoryRepository)
    }

    // ─── initial state ────────────────────────────────────────────────────────

    @Test
    fun `initial state has empty query and empty sections`() = runTest {
        val state = viewModel.uiState.value
        state.query shouldBe ""
        state.users.items.shouldBeEmpty()
        state.repositories.items.shouldBeEmpty()
        state.users.isLoading.shouldBeFalse()
        state.repositories.isLoading.shouldBeFalse()
        state.users.error.shouldBeNull()
        state.repositories.error.shouldBeNull()
    }

    // ─── query changes (no API calls) ────────────────────────────────────────

    @Test
    fun `onQueryChange updates query in state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            viewModel.onQueryChange("jetbrains")
            val updated = awaitItem()
            updated.query shouldBe "jetbrains"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blank query clears sections without triggering search`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("   ")
            val state = awaitItem()
            state.users.items.shouldBeEmpty()
            state.repositories.items.shouldBeEmpty()
            state.users.isLoading.shouldBeFalse()
            state.repositories.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchClick with blank query does nothing`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSearchClick() // query is "" from initial state
            // No new emission expected
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty query clears sections`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            // Set a non-empty query to transition state, then clear it
            viewModel.onQueryChange("kotlin")
            awaitItem()
            viewModel.onQueryChange("")
            val cleared = awaitItem()
            cleared.query shouldBe ""
            cleared.users.items.shouldBeEmpty()
            cleared.repositories.items.shouldBeEmpty()
            cleared.users.isLoading.shouldBeFalse()
            cleared.repositories.isLoading.shouldBeFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── full search flow (max 4 Search API calls: initial + optional load-more) ─
    //
    // All search, field-validation, pagination, and post-search clear behaviours
    // are combined here to stay within GitHub's unauthenticated Search API limit
    // (10 req/min). Splitting them into separate tests would require a fresh
    // search per test (2 extra calls each), easily hitting the rate limit.

    @Test
    fun `search flow — results, field validation, and pagination`() = runTest {
        // Longer timeout: two concurrent search calls (users + repos) plus optional
        // load-more pages; each awaitItem must not exceed this per-item budget.
        viewModel.uiState.test(timeout = 15.seconds) {
            awaitItem() // initial empty state

            viewModel.onQueryChange("android")
            awaitItem() // consume query change

            viewModel.onSearchClick()

            // Wait until both sections finish loading
            var state = awaitItem()
            (state.users.isLoading || state.repositories.isLoading) shouldBe true
            while (state.users.isLoading || state.repositories.isLoading) {
                state = awaitItem()
            }

            // Both sections populated
            state.users.items.shouldNotBeEmpty()
            state.repositories.items.shouldNotBeEmpty()
            state.users.error.shouldBeNull()
            state.repositories.error.shouldBeNull()

            // Field validation
            val user = state.users.items.first()
            user.login.shouldNotBeBlank()
            user.id shouldBeGreaterThan 0L
            user.avatarUrl shouldStartWith "https://"

            val repo = state.repositories.items.first()
            repo.name.shouldNotBeBlank()
            repo.fullName.shouldNotBeBlank()
            repo.id shouldBeGreaterThan 0L

            // hasNextPage matches item count
            state.users.hasNextPage shouldBe (state.users.items.size >= 30)
            state.repositories.hasNextPage shouldBe (state.repositories.items.size >= 30)

            // ── Load more users (1 extra API call) ───────────────────────────
            if (state.users.hasNextPage) {
                val page1UserCount = state.users.items.size
                viewModel.onLoadMoreUsers()
                var moreState = awaitItem()
                while (moreState.users.isLoadingMore || moreState.users.items.size <= page1UserCount) {
                    moreState = awaitItem()
                }
                moreState.users.items.size shouldBeGreaterThan page1UserCount
                moreState.users.isLoadingMore.shouldBeFalse()
                state = moreState
            }

            // ── Load more repositories (1 extra API call) ─────────────────────
            if (state.repositories.hasNextPage) {
                val page1RepoCount = state.repositories.items.size
                viewModel.onLoadMoreRepositories()
                var moreState = awaitItem()
                while (moreState.repositories.isLoadingMore || moreState.repositories.items.size <= page1RepoCount) {
                    moreState = awaitItem()
                }
                moreState.repositories.items.size shouldBeGreaterThan page1RepoCount
                moreState.repositories.isLoadingMore.shouldBeFalse()
            }

            // ── Clear the query and verify sections reset ──────────────────────
            // onQueryChange("") issues two _uiState.update calls: one for query and
            // one for clearing sections. Turbine may deliver them as separate emissions,
            // so drain until we reach the fully-cleared state.
            viewModel.onQueryChange("")
            var cleared = awaitItem()
            while (cleared.users.items.isNotEmpty() || cleared.repositories.items.isNotEmpty()) {
                cleared = awaitItem()
            }
            cleared.query shouldBe ""
            cleared.users.items.shouldBeEmpty()
            cleared.repositories.items.shouldBeEmpty()
            cleared.users.isLoading.shouldBeFalse()
            cleared.repositories.isLoading.shouldBeFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
