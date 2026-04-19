package dev.akexorcist.githubviewer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repositories")
data class RepositoryEntity(
    @PrimaryKey val id: Long,
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
    val cachedAt: Long,
)
