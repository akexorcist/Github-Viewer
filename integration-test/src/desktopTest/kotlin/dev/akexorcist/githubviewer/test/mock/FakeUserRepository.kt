package dev.akexorcist.githubviewer.test.mock

import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.SearchUserItem
import dev.akexorcist.githubviewer.data.model.User
import dev.akexorcist.githubviewer.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeUserRepository(
    private val getUserImpl: (login: String, forceRefresh: Boolean) -> Flow<Result<User>> = { _, _ -> emptyFlow() },
    private val searchUsersImpl: suspend (query: String, page: Int) -> Result<PageResult<SearchUserItem>> = { _, _ -> Result.Error(dev.akexorcist.githubviewer.core.common.AppError.UnknownError) },
) : UserRepository {
    override fun getUser(login: String, forceRefresh: Boolean): Flow<Result<User>> =
        getUserImpl(login, forceRefresh)

    override suspend fun searchUsers(query: String, page: Int): Result<PageResult<SearchUserItem>> =
        searchUsersImpl(query, page)
}
