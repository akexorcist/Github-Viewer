package dev.akexorcist.githubviewer.data.repository

import dev.akexorcist.githubviewer.core.common.PAGE_SIZE
import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.core.database.dao.RepositoryDao
import dev.akexorcist.githubviewer.core.network.GitHubApiService
import dev.akexorcist.githubviewer.data.mapper.toDomain
import dev.akexorcist.githubviewer.data.mapper.toEntity
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.util.toAppError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

interface RepositoryRepository {
    fun getRepository(owner: String, repo: String, forceRefresh: Boolean = false): Flow<Result<Repository>>
    fun getUserRepositories(login: String, page: Int, forceRefresh: Boolean = false): Flow<Result<PageResult<Repository>>>
    suspend fun searchRepositories(query: String, page: Int): Result<PageResult<Repository>>
}

class RepositoryRepositoryImpl(
    private val apiService: GitHubApiService,
    private val repositoryDao: RepositoryDao,
) : RepositoryRepository {

    override fun getRepository(owner: String, repo: String, forceRefresh: Boolean): Flow<Result<Repository>> = flow {
        val fullName = "$owner/$repo"
        val cached = repositoryDao.getRepositoryByFullName(fullName)
        if (cached != null) {
            emit(Result.Success(cached.toDomain()))
        }
        if (forceRefresh || cached == null) {
            runCatching { apiService.getRepository(owner, repo) }
                .onSuccess { dto ->
                    val entity = dto.toEntity(cachedAt = Clock.System.now().toEpochMilliseconds())
                    repositoryDao.upsertRepository(entity)
                    emit(Result.Success(dto.toDomain()))
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    if (cached == null) {
                        emit(Result.Error(throwable.toAppError()))
                    }
                }
        }
    }

    override fun getUserRepositories(login: String, page: Int, forceRefresh: Boolean): Flow<Result<PageResult<Repository>>> = flow {
        val cached = if (page == 1) repositoryDao.getRepositoriesByOwner(login) else emptyList()
        if (cached.isNotEmpty()) {
            emit(
                Result.Success(
                    PageResult(
                        items = cached.map { it.toDomain() },
                        page = 1,
                        hasNextPage = cached.size >= PAGE_SIZE,
                    )
                )
            )
        }
        if (forceRefresh || page > 1 || cached.isEmpty()) {
            runCatching { apiService.getUserRepositories(login, page) }
                .onSuccess { dtos ->
                    val now = Clock.System.now().toEpochMilliseconds()
                    repositoryDao.upsertRepositories(dtos.map { it.toEntity(cachedAt = now) })
                    emit(
                        Result.Success(
                            PageResult(
                                items = dtos.map { it.toDomain() },
                                page = page,
                                hasNextPage = dtos.size >= PAGE_SIZE,
                            )
                        )
                    )
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    if (cached.isEmpty()) emit(Result.Error(throwable.toAppError()))
                }
        }
    }

    override suspend fun searchRepositories(query: String, page: Int): Result<PageResult<Repository>> =
        runCatching { apiService.searchRepositories(query, page) }
            .fold(
                onSuccess = { result ->
                    val items = result.items.map { it.toDomain() }
                    Result.Success(
                        PageResult(
                            items = items,
                            page = page,
                            hasNextPage = items.size >= PAGE_SIZE,
                        )
                    )
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Result.Error(throwable.toAppError())
                },
            )
}
