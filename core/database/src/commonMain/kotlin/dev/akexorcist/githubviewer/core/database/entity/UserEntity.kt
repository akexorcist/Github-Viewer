package dev.akexorcist.githubviewer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val login: String,
    val id: Long,
    val avatarUrl: String,
    val name: String?,
    val bio: String?,
    val location: String?,
    val blog: String?,
    val publicRepos: Int,
    val followers: Int,
    val following: Int,
    val cachedAt: Long,
)
