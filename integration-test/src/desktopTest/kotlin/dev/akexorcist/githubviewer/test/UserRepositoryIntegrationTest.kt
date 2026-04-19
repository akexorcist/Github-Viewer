package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.SearchUserItem
import dev.akexorcist.githubviewer.data.model.User
import dev.akexorcist.githubviewer.data.repository.UserRepository
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// All tests share one in-memory database so each user is only fetched from the
// network once. Subsequent tests hit the Room cache, staying within GitHub's
// unauthenticated rate limits (60 req/min regular, 10 req/min search).
class UserRepositoryIntegrationTest {

    companion object {
        private val database = TestDependencies.createDatabase()
        private val apiService = TestDependencies.createApiService()
        private val userRepository: UserRepository =
            TestDependencies.createUserRepository(apiService, database)
    }

    // ─── getUser ──────────────────────────────────────────────────────────────

    @Test
    fun `getUser emits user with correct fields`() = runTest {
        userRepository.getUser(TestEnvironment.testUser).test {
            val result = awaitItem()
            val user = result.shouldBeInstanceOf<Result.Success<User>>().data
            user.login shouldBe TestEnvironment.testUser
            user.id shouldBeGreaterThan 0L
            user.avatarUrl shouldStartWith "https://"
            user.publicRepos shouldBeGreaterThanOrEqualTo 0
            user.followers shouldBeGreaterThanOrEqualTo 0
            user.following shouldBeGreaterThanOrEqualTo 0
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUser emits cached data when forceRefresh is false and cache is warm`() = runTest {
        // Ensure the cache is warm (may already be warm from a previous test in this session)
        userRepository.getUser(TestEnvironment.testUser, forceRefresh = false).test {
            awaitItem() // network or cache
            cancelAndIgnoreRemainingEvents()
        }

        // Second call with forceRefresh=false must serve from cache only (no network call)
        userRepository.getUser(TestEnvironment.testUser, forceRefresh = false).test {
            val cached = awaitItem().shouldBeInstanceOf<Result.Success<User>>()
            cached.data.login shouldBe TestEnvironment.testUser
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUser emits cache first then fresh network data when forceRefresh is true`() = runTest {
        // Ensure the cache is warm before testing the cache-first + network sequence
        userRepository.getUser(TestEnvironment.testUser, forceRefresh = false).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // forceRefresh=true: expect cache emission first, then a fresh network emission
        userRepository.getUser(TestEnvironment.testUser, forceRefresh = true).test {
            val first = awaitItem().shouldBeInstanceOf<Result.Success<User>>() // from cache
            first.data.login shouldBe TestEnvironment.testUser

            val second = awaitItem().shouldBeInstanceOf<Result.Success<User>>() // from network
            second.data.login shouldBe TestEnvironment.testUser

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── searchUsers ──────────────────────────────────────────────────────────

    @Test
    fun `searchUsers returns non-empty results with valid fields and correct pagination`() = runTest {
        val result = userRepository.searchUsers(TestEnvironment.testUser, page = 1)
        val page = result.shouldBeInstanceOf<Result.Success<PageResult<SearchUserItem>>>().data
        page.items.shouldNotBeEmpty()
        page.page shouldBe 1
        val first = page.items.first()
        first.login.shouldNotBeBlank()
        first.id shouldBeGreaterThan 0L
        first.avatarUrl shouldStartWith "https://"
        // hasNextPage reflects whether there are 30+ results
        page.hasNextPage shouldBe (page.items.size >= 30)
    }
}
