package dev.akexorcist.githubviewer.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun createDatabase(context: Context): AppDatabase =
    Room.databaseBuilder<AppDatabase>(
        context = context,
        name = context.getDatabasePath("github_viewer.db").absolutePath,
    )
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
