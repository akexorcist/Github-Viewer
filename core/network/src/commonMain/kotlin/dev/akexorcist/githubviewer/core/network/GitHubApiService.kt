package dev.akexorcist.githubviewer.core.network

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PAGE_SIZE
import dev.akexorcist.githubviewer.core.network.dto.RepositoryDto
import dev.akexorcist.githubviewer.core.network.dto.SearchRepositoriesResultDto
import dev.akexorcist.githubviewer.core.network.dto.SearchUsersResultDto
import dev.akexorcist.githubviewer.core.network.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class GitHubApiService(private val client: HttpClient) {

    suspend fun searchUsers(query: String, page: Int): SearchUsersResultDto =
        client.get("$BASE_URL/search/users") {
            parameter("q", query)
            parameter("page", page)
            parameter("per_page", PAGE_SIZE)
        }.bodyOrThrow()

    suspend fun searchRepositories(query: String, page: Int): SearchRepositoriesResultDto =
        client.get("$BASE_URL/search/repositories") {
            parameter("q", query)
            parameter("page", page)
            parameter("per_page", PAGE_SIZE)
        }.bodyOrThrow()

    suspend fun getUser(login: String): UserDto =
        client.get("$BASE_URL/users/$login").bodyOrThrow()

    suspend fun getUserRepositories(login: String, page: Int): List<RepositoryDto> =
        client.get("$BASE_URL/users/$login/repos") {
            parameter("page", page)
            parameter("per_page", PAGE_SIZE)
            parameter("sort", "updated")
        }.bodyOrThrow()

    suspend fun getRepository(owner: String, repo: String): RepositoryDto =
        client.get("$BASE_URL/repos/$owner/$repo").bodyOrThrow()

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        checkRateLimit(this)
        if (status == HttpStatusCode.NotFound) throw AppException(AppError.NotFoundError)
        if (!status.isSuccess()) throw AppException(
            AppError.HttpError(status.value, status.description)
        )
        return body()
    }

    private fun checkRateLimit(response: HttpResponse) {
        val remaining = response.headers["X-RateLimit-Remaining"]?.toIntOrNull()
        val resetAt = response.headers["X-RateLimit-Reset"]?.toLongOrNull()
        if (remaining == 0 && resetAt != null) {
            throw AppException(AppError.RateLimitError(Instant.fromEpochSeconds(resetAt)))
        }
    }

    companion object {
        private const val BASE_URL = "https://api.github.com"

        fun create(): GitHubApiService = GitHubApiService(
            createHttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                    })
                }
                install(Logging) {
                    level = LogLevel.NONE
                }
                defaultRequest {
                    header("Accept", "application/vnd.github+json")
                    header("X-GitHub-Api-Version", "2022-11-28")
                }
            }
        )
    }
}

class AppException(val error: AppError) : Exception(error.toString())
