package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import dev.akexorcist.githubviewer.core.common.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRepositoryIntegrationTest {

    private val database = TestDependencies.createDatabase()
    private val apiService = TestDependencies.createApiService()
    private val userRepository = TestDependencies.createUserRepository(apiService, database)

    @Test
    fun `getUser emits data from real GitHub API`() = runTest {
        userRepository.getUser(TestEnvironment.testUser).test {
            val result = awaitItem()
            assertIs<Result.Success<*>>(result)
            val user = (result as Result.Success).data
            assertNotNull(user)
            assertTrue(user.login.isNotBlank())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUser caches result in Room then emits fresh data`() = runTest {
        userRepository.getUser(TestEnvironment.testUser, forceRefresh = false).test {
            // First emission: network (cache is empty on first run)
            val first = awaitItem()
            assertIs<Result.Success<*>>(first)

            // Second call hits cache first
            cancelAndIgnoreRemainingEvents()
        }

        userRepository.getUser(TestEnvironment.testUser, forceRefresh = false).test {
            // Should emit cached data immediately as first emission
            val cached = awaitItem()
            assertIs<Result.Success<*>>(cached)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchUsers returns results from real GitHub API`() = runTest {
        val result = userRepository.searchUsers(TestEnvironment.testUser, page = 1)
        assertIs<Result.Success<*>>(result)
        val page = (result as Result.Success).data
        assertTrue(page.items.isNotEmpty())
    }
}
