package dev.akexorcist.githubviewer.core.database.converter

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString("|")

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("|")
}
