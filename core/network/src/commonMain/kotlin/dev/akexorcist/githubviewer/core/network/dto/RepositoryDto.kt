package dev.akexorcist.githubviewer.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepositoryDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("owner") val owner: OwnerDto,
    @SerialName("description") val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("stargazers_count") val stars: Int = 0,
    @SerialName("forks_count") val forks: Int = 0,
    @SerialName("open_issues_count") val openIssues: Int = 0,
    @SerialName("watchers_count") val watchers: Int = 0,
    @SerialName("language") val language: String? = null,
    @SerialName("topics") val topics: List<String> = emptyList(),
    @SerialName("license") val license: LicenseDto? = null,
    @SerialName("pushed_at") val pushedAt: String? = null,
)

@Serializable
data class OwnerDto(
    @SerialName("login") val login: String,
    @SerialName("avatar_url") val avatarUrl: String,
)

@Serializable
data class LicenseDto(
    @SerialName("name") val name: String,
)
