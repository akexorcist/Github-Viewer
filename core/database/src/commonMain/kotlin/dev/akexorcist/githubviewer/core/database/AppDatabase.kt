package dev.akexorcist.githubviewer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.akexorcist.githubviewer.core.database.converter.StringListConverter
import dev.akexorcist.githubviewer.core.database.dao.RepositoryDao
import dev.akexorcist.githubviewer.core.database.dao.UserDao
import dev.akexorcist.githubviewer.core.database.entity.RepositoryEntity
import dev.akexorcist.githubviewer.core.database.entity.UserEntity

@TypeConverters(StringListConverter::class)
@Database(
    entities = [UserEntity::class, RepositoryEntity::class],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun repositoryDao(): RepositoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE repositories ADD COLUMN readmeContent TEXT DEFAULT NULL")
            }
        }
    }
}
