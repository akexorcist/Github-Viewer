package dev.akexorcist.githubviewer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.akexorcist.githubviewer.core.database.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE login = :login")
    suspend fun getUser(login: String): UserEntity?

    @Upsert
    suspend fun upsertUser(user: UserEntity)
}
