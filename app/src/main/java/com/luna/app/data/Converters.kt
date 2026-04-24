package com.luna.app.data

import androidx.room.TypeConverter
import com.luna.app.domain.model.Energy
import com.luna.app.domain.model.FlowLevel
import kotlinx.datetime.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? {
        return value?.toEpochDays()?.toLong()
    }

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.fromEpochDays(it.toInt()) }
    }

    @TypeConverter
    fun fromFlowLevel(value: FlowLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toFlowLevel(value: String?): FlowLevel? {
        return value?.let { FlowLevel.valueOf(it) }
    }

    @TypeConverter
    fun fromEnergy(value: Energy?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEnergy(value: String?): Energy? {
        return value?.let { Energy.valueOf(it) }
    }
}
