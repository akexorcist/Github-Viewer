package dev.akexorcist.githubviewer.data.di

import dev.akexorcist.githubviewer.core.network.GitHubApiService
import dev.akexorcist.githubviewer.data.repository.RepositoryRepository
import dev.akexorcist.githubviewer.data.repository.RepositoryRepositoryImpl
import dev.akexorcist.githubviewer.data.repository.UserRepository
import dev.akexorcist.githubviewer.data.repository.UserRepositoryImpl
import org.koin.dsl.module

val dataModule = module {
    single { GitHubApiService.create() }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<RepositoryRepository> { RepositoryRepositoryImpl(get(), get()) }
}
