package dev.akexorcist.githubviewer.di

import dev.akexorcist.githubviewer.core.database.AppDatabase
import dev.akexorcist.githubviewer.core.database.createDatabase
import dev.akexorcist.githubviewer.data.di.dataModule
import dev.akexorcist.githubviewer.presentation.repository.RepositoryDetailViewModel
import dev.akexorcist.githubviewer.presentation.profile.UserProfileViewModel
import dev.akexorcist.githubviewer.presentation.search.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single { createDatabase(androidContext()) }
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().repositoryDao() }
}

val presentationModule = module {
    viewModel { SearchViewModel(get(), get()) }
    viewModel { (login: String) -> UserProfileViewModel(login, get(), get()) }
    viewModel { (owner: String, repo: String) -> RepositoryDetailViewModel(owner, repo, get()) }
}

val appModules = listOf(databaseModule, dataModule, presentationModule)
