package net.k74n3xz.ecal.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendee")
data class AttendeeEntity(@PrimaryKey val id: Long?, val name: String?, val description: String?, val email: String)
