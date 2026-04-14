package dev.akexorcist.githubviewer.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchUsersResultDto(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("items") val items: List<SearchUserItemDto>,
)

@Serializable
data class SearchUserItemDto(
    @SerialName("login") val login: String,
    @SerialName("id") val id: Long,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class SearchRepositoriesResultDto(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("items") val items: List<RepositoryDto>,
)
