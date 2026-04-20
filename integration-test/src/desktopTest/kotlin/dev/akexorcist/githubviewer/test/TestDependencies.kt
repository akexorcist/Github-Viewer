package dev.akexorcist.githubviewer.test

import dev.akexorcist.githubviewer.core.database.AppDatabase
import dev.akexorcist.githubviewer.core.database.createInMemoryDatabase
import dev.akexorcist.githubviewer.core.network.GitHubApiService
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import dev.akexorcist.githubviewer.data.repository.RepositoryRepositoryImpl
import dev.akexorcist.githubviewer.data.repository.UserRepository
import dev.akexorcist.githubviewer.data.repository.UserRepositoryImpl
import dev.akexorcist.githubviewer.presentation.profile.UserProfileViewModel
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailViewModel
import dev.akexorcist.githubviewer.presentation.search.SearchViewModel

object TestDependencies {
    fun createDatabase(): AppDatabase = createInMemoryDatabase()

    // Passes GITHUB_TOKEN when available to raise rate limits:
    //   unauthenticated: 60 req/hour core, 10 req/min search
    //   authenticated:  5000 req/hour core, 30 req/min search
    // enableLogging=true prints each request URL and response status to stdout
    // so test runs can be verified against the real GitHub API calls made.
    fun createApiService(): GitHubApiService = GitHubApiService.create(
        token = TestEnvironment.githubToken,
        enableLogging = true,
    )

    fun createUserRepository(
        apiService: GitHubApiService = createApiService(),
        database: AppDatabase = createDatabase(),
    ): UserRepository = UserRepositoryImpl(apiService, database.userDao())

    fun createRepositoryRepository(
        apiService: GitHubApiService = createApiService(),
        database: AppDatabase = createDatabase(),
    ): RepositoryRepository = RepositoryRepositoryImpl(apiService, database.repositoryDao())

    fun createSearchViewModel(
        userRepository: UserRepository,
        repositoryRepository: RepositoryRepository,
    ): SearchViewModel = SearchViewModel(userRepository, repositoryRepository)

    fun createUserProfileViewModel(
        login: String,
        userRepository: UserRepository,
        repositoryRepository: RepositoryRepository,
    ): UserProfileViewModel = UserProfileViewModel(login, userRepository, repositoryRepository)

    fun createRepositoryDetailViewModel(
        owner: String,
        repo: String,
        repositoryRepository: RepositoryRepository,
    ): RepositoryDetailViewModel = RepositoryDetailViewModel(owner, repo, repositoryRepository)
}
