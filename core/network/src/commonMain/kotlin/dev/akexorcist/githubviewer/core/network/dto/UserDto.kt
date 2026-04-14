package dev.akexorcist.githubviewer.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("login") val login: String,
    @SerialName("id") val id: Long,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("name") val name: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("blog") val blog: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    @SerialName("followers") val followers: Int = 0,
    @SerialName("following") val following: Int = 0,
)
