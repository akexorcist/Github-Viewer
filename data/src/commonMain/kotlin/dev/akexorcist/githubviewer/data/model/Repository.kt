package dev.akexorcist.githubviewer.data.model

data class Repository(
    val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val ownerAvatarUrl: String,
    val description: String?,
    val stars: Int,
    val forks: Int,
    val openIssues: Int,
    val watchers: Int,
    val language: String?,
    val topics: List<String>,
    val licenseName: String?,
    val pushedAt: String?,
)
