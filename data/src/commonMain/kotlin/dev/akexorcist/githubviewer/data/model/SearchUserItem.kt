package dev.akexorcist.githubviewer.data.model

data class SearchUserItem(
    val login: String,
    val id: Long,
    val avatarUrl: String,
    val name: String?,
)
