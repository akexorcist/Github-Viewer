package dev.akexorcist.githubviewer.data.repository

import dev.akexorcist.githubviewer.core.common.PAGE_SIZE
import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.core.database.dao.UserDao
import dev.akexorcist.githubviewer.core.network.GitHubApiService
import dev.akexorcist.githubviewer.data.mapper.toDomain
import dev.akexorcist.githubviewer.data.mapper.toEntity
import dev.akexorcist.githubviewer.data.model.SearchUserItem
import dev.akexorcist.githubviewer.data.model.User
import dev.akexorcist.githubviewer.data.util.toAppError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock

interface UserRepository {
    fun getUser(login: String, forceRefresh: Boolean = false): Flow<Result<User>>
    suspend fun searchUsers(query: String, page: Int): Result<PageResult<SearchUserItem>>
}

class UserRepositoryImpl(
    private val apiService: GitHubApiService,
    private val userDao: UserDao,
) : UserRepository {

    override fun getUser(login: String, forceRefresh: Boolean): Flow<Result<User>> = flow {
        val cached = userDao.getUser(login)
        if (cached != null) {
            emit(Result.Success(cached.toDomain()))
        }
        if (forceRefresh || cached == null) {
            runCatching { apiService.getUser(login) }
                .onSuccess { dto ->
                    val entity = dto.toEntity(cachedAt = Clock.System.now().toEpochMilliseconds())
                    userDao.upsertUser(entity)
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

    override suspend fun searchUsers(query: String, page: Int): Result<PageResult<SearchUserItem>> =
        runCatching { apiService.searchUsers(query, page) }
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

