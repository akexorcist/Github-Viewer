package dev.akexorcist.githubviewer.test.mock

import dev.akexorcist.githubviewer.core.common.AppError
import dev.akexorcist.githubviewer.core.common.PageResult
import dev.akexorcist.githubviewer.core.common.Result
import dev.akexorcist.githubviewer.data.model.Repository
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeRepositoryRepository(
    private val getRepositoryImpl: (owner: String, repo: String, forceRefresh: Boolean) -> Flow<Result<Repository>> = { _, _, _ -> emptyFlow() },
    private val getUserRepositoriesImpl: (login: String, page: Int, forceRefresh: Boolean) -> Flow<Result<PageResult<Repository>>> = { _, _, _ -> emptyFlow() },
    private val searchRepositoriesImpl: suspend (query: String, page: Int) -> Result<PageResult<Repository>> = { _, _ -> Result.Error(AppError.UnknownError) },
    private val getReadmeImpl: (owner: String, repo: String, forceRefresh: Boolean) -> Flow<Result<String>> = { _, _, _ -> emptyFlow() },
) : RepositoryRepository {
    override fun getRepository(owner: String, repo: String, forceRefresh: Boolean): Flow<Result<Repository>> =
        getRepositoryImpl(owner, repo, forceRefresh)

    override fun getUserRepositories(login: String, page: Int, forceRefresh: Boolean): Flow<Result<PageResult<Repository>>> =
        getUserRepositoriesImpl(login, page, forceRefresh)

    override suspend fun searchRepositories(query: String, page: Int): Result<PageResult<Repository>> =
        searchRepositoriesImpl(query, page)

    override fun getReadme(owner: String, repo: String, forceRefresh: Boolean): Flow<Result<String>> =
        getReadmeImpl(owner, repo, forceRefresh)
}
