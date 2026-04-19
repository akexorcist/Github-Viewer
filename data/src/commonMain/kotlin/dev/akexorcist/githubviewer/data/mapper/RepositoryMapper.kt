package dev.akexorcist.githubviewer.data.mapper

import dev.akexorcist.githubviewer.core.database.entity.RepositoryEntity
import dev.akexorcist.githubviewer.core.network.dto.RepositoryDto
import dev.akexorcist.githubviewer.data.model.Repository

fun RepositoryDto.toEntity(cachedAt: Long): RepositoryEntity = RepositoryEntity(
    id = id,
    name = name,
    fullName = fullName,
    ownerLogin = owner.login,
    ownerAvatarUrl = owner.avatarUrl,
    description = description,
    stars = stars,
    forks = forks,
    openIssues = openIssues,
    watchers = watchers,
    language = language,
    topics = topics,
    licenseName = license?.name,
    pushedAt = pushedAt,
    cachedAt = cachedAt,
)

fun RepositoryEntity.toDomain(): Repository = Repository(
    id = id,
    name = name,
    fullName = fullName,
    ownerLogin = ownerLogin,
    ownerAvatarUrl = ownerAvatarUrl,
    description = description,
    stars = stars,
    forks = forks,
    openIssues = openIssues,
    watchers = watchers,
    language = language,
    topics = topics,
    licenseName = licenseName,
    pushedAt = pushedAt,
)

fun RepositoryDto.toDomain(): Repository = Repository(
    id = id,
    name = name,
    fullName = fullName,
    ownerLogin = owner.login,
    ownerAvatarUrl = owner.avatarUrl,
    description = description,
    stars = stars,
    forks = forks,
    openIssues = openIssues,
    watchers = watchers,
    language = language,
    topics = topics,
    licenseName = license?.name,
    pushedAt = pushedAt,
)
