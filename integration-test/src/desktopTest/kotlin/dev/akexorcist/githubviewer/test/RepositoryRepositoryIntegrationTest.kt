package dev.akexorcist.githubviewer.test

import app.cash.turbine.test
import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// All tests share one in-memory database so each repository is only fetched from
// the network once. Subsequent tests hit the Room cache, staying within GitHub's
// unauthenticated rate limits (60 req/min regular, 10 req/min search).
class RepositoryRepositoryIntegrationTest {

    companion object {
        private val database = TestDependencies.createDatabase()
        private val apiService = TestDependencies.createApiService()
        private val repositoryRepository: RepositoryRepository =
            TestDependencies.createRepositoryRepository(apiService, database)
    }

    // ─── getRepository ────────────────────────────────────────────────────────

    @Test
    fun `getRepository returns correct domain model fields`() = runTest {
        repositoryRepository.getRepository(
            owner = TestEnvironment.testRepoOwner,
            repo = TestEnvironment.testRepo,
        ).test {
            val repo = awaitItem().shouldBeInstanceOf<Result.Success<Repository>>().data
            repo.name shouldBe TestEnvironment.testRepo
            repo.fullName shouldBe "${TestEnvironment.testRepoOwner}/${TestEnvironment.testRepo}"
            repo.ownerLogin shouldBe TestEnvironment.testRepoOwner
            repo.id shouldBeGreaterThan 0L
            repo.ownerAvatarUrl shouldStartWith "https://"
            repo.stars shouldBeGreaterThanOrEqualTo 0
            repo.forks shouldBeGreaterThanOrEqualTo 0
            repo.openIssues shouldBeGreaterThanOrEqualTo 0
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRepository emits cached data when forceRefresh is false and cache is warm`() = runTest {
        // Ensure cache is warm
        repositoryRepository.getRepository(
            owner = TestEnvironment.testRepoOwner,
            repo = TestEnvironment.testRepo,
            forceRefresh = false,
        ).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // Second call with forceRefresh=false must serve from cache only
        repositoryRepository.getRepository(
            owner = TestEnvironment.testRepoOwner,
            repo = TestEnvironment.testRepo,
            forceRefresh = false,
        ).test {
            val cached = awaitItem().shouldBeInstanceOf<Result.Success<Repository>>()
            cached.data.name shouldBe TestEnvironment.testRepo
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getRepository emits cache first then fresh network data when forceRefresh is true`() = runTest {
        // Ensure cache is warm before testing the two-emission sequence
        repositoryRepository.getRepository(
            owner = TestEnvironment.testRepoOwner,
            repo = TestEnvironment.testRepo,
            forceRefresh = false,
        ).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // forceRefresh=true: cache emission first, network emission second
        repositoryRepository.getRepository(
            owner = TestEnvironment.testRepoOwner,
            repo = TestEnvironment.testRepo,
            forceRefresh = true,
        ).test {
            val first = awaitItem().shouldBeInstanceOf<Result.Success<Repository>>() // from cache
            first.data.name shouldBe TestEnvironment.testRepo

            val second = awaitItem().shouldBeInstanceOf<Result.Success<Repository>>() // from network
            second.data.name shouldBe TestEnvironment.testRepo

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── getUserRepositories ──────────────────────────────────────────────────

    @Test
    fun `getUserRepositories page 1 returns repos with correct fields`() = runTest {
        repositoryRepository.getUserRepositories(
            login = TestEnvironment.testUser,
            page = 1,
            forceRefresh = true,
        ).test {
            val page = awaitItem().shouldBeInstanceOf<Result.Success<PageResult<Repository>>>().data
            page.items.shouldNotBeEmpty()
            page.page shouldBe 1
            val first = page.items.first()
            first.name.shouldNotBeBlank()
            first.fullName.shouldNotBeBlank()
            first.ownerLogin shouldBe TestEnvironment.testUser
            first.id shouldBeGreaterThan 0L
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUserRepositories emits cached repos when forceRefresh is false and cache is warm`() = runTest {
        // Ensure cache is populated (may already be warm from a previous test in this session)
        repositoryRepository.getUserRepositories(
            login = TestEnvironment.testUser,
            page = 1,
            forceRefresh = true,
        ).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // forceRefresh=false, page=1: should emit cached items only (no network call)
        repositoryRepository.getUserRepositories(
            login = TestEnvironment.testUser,
            page = 1,
            forceRefresh = false,
        ).test {
            val cached = awaitItem().shouldBeInstanceOf<Result.Success<PageResult<Repository>>>()
            cached.data.items.shouldNotBeEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUserRepositories page 2 returns repos`() = runTest {
        // Page > 1 always goes to network (regardless of cache state)
        repositoryRepository.getUserRepositories(
            login = TestEnvironment.testUser,
            page = 2,
            forceRefresh = true,
        ).test {
            val page = awaitItem().shouldBeInstanceOf<Result.Success<PageResult<Repository>>>().data
            page.items.shouldNotBeEmpty()
            page.page shouldBe 2
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── searchRepositories ───────────────────────────────────────────────────

    @Test
    fun `searchRepositories returns non-empty results with valid fields and correct pagination`() = runTest {
        val page = repositoryRepository.searchRepositories("kotlin multiplatform", page = 1)
            .shouldBeInstanceOf<Result.Success<PageResult<Repository>>>().data
        page.items.shouldNotBeEmpty()
        page.page shouldBe 1
        val first = page.items.first()
        first.name.shouldNotBeBlank()
        first.fullName.shouldNotBeBlank()
        first.id shouldBeGreaterThan 0
        first.stars shouldBeGreaterThanOrEqualTo 0
        page.hasNextPage shouldBe (page.items.size >= 30)
    }
}
