package dev.akexorcist.githubviewer.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object SearchRoute

@Serializable
data class UserProfileRoute(val login: String)

@Serializable
data class RepositoryDetailRoute(val owner: String, val repo: String)
