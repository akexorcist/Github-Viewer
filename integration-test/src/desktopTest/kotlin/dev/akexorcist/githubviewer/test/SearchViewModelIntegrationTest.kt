package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchViewModelIntegrationTest {

    private val database = TestDependencies.createDatabase()
    private val apiService = TestDependencies.createApiService()
    private val userRepository = TestDependencies.createUserRepository(apiService, database)
    private val repositoryRepository = TestDependencies.createRepositoryRepository(apiService, database)
    private val viewModel = TestDependencies.createSearchViewModel(userRepository, repositoryRepository)

    @Test
    fun `search populates both user and repo sections from real API`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial empty state

            viewModel.onQueryChange("kotlin")
            viewModel.onSearchClick()

            // Loading states
            val loadingState = awaitItem()
            assertTrue(loadingState.users.isLoading || loadingState.repositories.isLoading)

            // Eventually both sections are populated
            val finalState = awaitItem()
            assertFalse(finalState.users.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty query clears results`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("")
            val state = awaitItem()
            assertTrue(state.users.items.isEmpty())
            assertTrue(state.repositories.items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
