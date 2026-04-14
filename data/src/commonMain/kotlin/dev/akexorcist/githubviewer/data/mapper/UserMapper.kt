package dev.akexorcist.githubviewer.data.mapper

import dev.akexorcist.githubviewer.core.database.entity.UserEntity
import dev.akexorcist.githubviewer.core.network.dto.SearchUserItemDto
import dev.akexorcist.githubviewer.core.network.dto.UserDto
import dev.akexorcist.githubviewer.data.model.SearchUserItem
import dev.akexorcist.githubviewer.data.model.User

fun UserDto.toEntity(cachedAt: Long): UserEntity = UserEntity(
    login = login,
    id = id,
    avatarUrl = avatarUrl,
    name = name,
    bio = bio,
    location = location,
    blog = blog,
    publicRepos = publicRepos,
    followers = followers,
    following = following,
    cachedAt = cachedAt,
)

fun UserEntity.toDomain(): User = User(
    login = login,
    id = id,
    avatarUrl = avatarUrl,
    name = name,
    bio = bio,
    location = location,
    blog = blog,
    publicRepos = publicRepos,
    followers = followers,
    following = following,
)

fun UserDto.toDomain(): User = User(
    login = login,
    id = id,
    avatarUrl = avatarUrl,
    name = name,
    bio = bio,
    location = location,
    blog = blog,
    publicRepos = publicRepos,
    followers = followers,
    following = following,
)

fun SearchUserItemDto.toDomain(): SearchUserItem = SearchUserItem(
    login = login,
    id = id,
    avatarUrl = avatarUrl,
    name = name,
)
