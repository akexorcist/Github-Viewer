package dev.akexorcist.githubviewer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.akexorcist.githubviewer.core.database.dao.RepositoryDao
import dev.akexorcist.githubviewer.core.database.dao.UserDao
import dev.akexorcist.githubviewer.core.database.entity.RepositoryEntity
import dev.akexorcist.githubviewer.core.database.entity.UserEntity

@Database(
    entities = [UserEntity::class, RepositoryEntity::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun repositoryDao(): RepositoryDao
}
