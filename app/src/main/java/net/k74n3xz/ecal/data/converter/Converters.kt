package net.k74n3xz.ecal.data.converter

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun toInstant(value: String?): Instant? = value?.let { Instant.parse(it) }
}