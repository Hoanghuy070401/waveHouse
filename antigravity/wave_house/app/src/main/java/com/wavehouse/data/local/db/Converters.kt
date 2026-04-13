package com.wavehouse.data.local.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",")

    @TypeConverter
    fun fromLong(value: Long?): Long? = value

    @TypeConverter
    fun toLong(value: Long?): Long? = value
}
