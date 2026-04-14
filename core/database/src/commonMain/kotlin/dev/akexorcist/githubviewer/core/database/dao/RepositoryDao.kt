package dev.akexorcist.githubviewer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.akexorcist.githubviewer.core.database.entity.RepositoryEntity

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM repositories WHERE ownerLogin = :ownerLogin ORDER BY stars DESC")
    suspend fun getRepositoriesByOwner(ownerLogin: String): List<RepositoryEntity>

    @Query("SELECT * FROM repositories WHERE id = :id")
    suspend fun getRepository(id: Long): RepositoryEntity?

    @Query("SELECT * FROM repositories WHERE fullName = :fullName")
    suspend fun getRepositoryByFullName(fullName: String): RepositoryEntity?

    @Upsert
    suspend fun upsertRepository(repository: RepositoryEntity)

    @Upsert
    suspend fun upsertRepositories(repositories: List<RepositoryEntity>)
}
