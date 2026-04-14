package dev.akexorcist.githubviewer.data.model

data class User(
    val login: String,
    val id: Long,
    val avatarUrl: String,
    val name: String?,
    val bio: String?,
    val location: String?,
    val blog: String?,
    val publicRepos: Int,
    val followers: Int,
    val following: Int,
)
