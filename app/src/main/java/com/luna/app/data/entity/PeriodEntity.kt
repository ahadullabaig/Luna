package com.luna.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,        // stored as epoch days via TypeConverter
    val endDate: LocalDate?          // null while period is ongoing
)
