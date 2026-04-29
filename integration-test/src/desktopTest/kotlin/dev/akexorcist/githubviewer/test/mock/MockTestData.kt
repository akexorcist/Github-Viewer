package dev.akexorcist.githubviewer.test.mock

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.model.SearchUserItem
import dev.akexorcist.githubviewer.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

fun testUser(login: String = "akexorcist") = User(
    login = login,
    id = 1L,
    avatarUrl = "https://avatars.githubusercontent.com/u/1",
    name = "Test User",
    bio = "Android Developer",
    location = "Bangkok",
    blog = null,
    publicRepos = 20,
    followers = 100,
    following = 50,
)

fun testSearchUserItem(login: String = "akexorcist", id: Long = 1L) = SearchUserItem(
    login = login,
    id = id,
    avatarUrl = "https://avatars.githubusercontent.com/u/$id",
    name = "Test User $id",
)

fun testRepository(name: String = "test-repo", id: Long = 1L) = Repository(
    id = id,
    name = name,
    fullName = "akexorcist/$name",
    ownerLogin = "akexorcist",
    ownerAvatarUrl = "https://avatars.githubusercontent.com/u/1",
    description = "A test repository",
    stars = 42,
    forks = 10,
    openIssues = 3,
    watchers = 42,
    language = "Kotlin",
    topics = listOf("android", "kotlin"),
    licenseName = "Apache 2.0",
    pushedAt = "2024-01-15T10:30:00Z",
)

fun testPageResult(
    items: List<Repository>,
    page: Int = 1,
    hasNextPage: Boolean = false,
) = PageResult(items = items, page = page, hasNextPage = hasNextPage)

fun testUserPageResult(
    items: List<SearchUserItem>,
    page: Int = 1,
    hasNextPage: Boolean = false,
) = PageResult(items = items, page = page, hasNextPage = hasNextPage)

// Emits cached value first, then the network-fresh value — mirrors the real cache-first pattern.
// yield() between emissions ensures StateFlow surfaces both to the collector before conflation.
fun <T> cacheThenNetwork(cached: T, fresh: T): Flow<Result<T>> = flow {
    emit(Result.Success(cached))
    kotlinx.coroutines.yield()
    emit(Result.Success(fresh))
}

fun <T> successFlow(value: T): Flow<Result<T>> = flowOf(Result.Success(value))

fun <T> errorFlow(error: AppError): Flow<Result<T>> = flowOf(Result.Error(error))

val networkError = AppError.NetworkError(Exception("No internet"))
val rateLimitError = AppError.RateLimitError(kotlin.time.Clock.System.now())
val httpError = AppError.HttpError(500, "Internal Server Error")
