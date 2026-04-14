package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import dev.akexorcist.githubviewer.core.common.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RepositoryRepositoryIntegrationTest {

    private val database = TestDependencies.createDatabase()
    private val apiService = TestDependencies.createApiService()
    private val repositoryRepository = TestDependencies.createRepositoryRepository(apiService, database)

    @Test
    fun `getRepository fetches from real GitHub API`() = runTest {
        repositoryRepository.getRepository(
            owner = TestEnvironment.testRepoOwner,
            repo = TestEnvironment.testRepo,
        ).test {
            val result = awaitItem()
            assertIs<Result.Success<*>>(result)
            val repo = (result as Result.Success).data
            assertNotNull(repo)
            assertTrue(repo.name.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchRepositories returns results from real GitHub API`() = runTest {
        val result = repositoryRepository.searchRepositories("kotlin multiplatform", page = 1)
        assertIs<Result.Success<*>>(result)
        val page = (result as Result.Success).data
        assertTrue(page.items.isNotEmpty())
    }

    @Test
    fun `getUserRepositories fetches paginated repos`() = runTest {
        repositoryRepository.getUserRepositories(
            login = TestEnvironment.testUser,
            page = 1,
            forceRefresh = true,
        ).test {
            val result = awaitItem()
            assertIs<Result.Success<*>>(result)
            val page = (result as Result.Success).data
            assertTrue(page.items.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
