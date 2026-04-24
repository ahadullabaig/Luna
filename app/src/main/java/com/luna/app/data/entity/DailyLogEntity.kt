package com.luna.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luna.app.domain.model.Energy
import com.luna.app.domain.model.FlowLevel
import kotlinx.datetime.LocalDate

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val date: LocalDate,
    val flowLevel: FlowLevel? = null,    // LIGHT, MEDIUM, HEAVY, or null
    val painFlags: Int = 0,              // bitmask: cramps, headache, backache, bloating
    val energy: Energy? = null,          // NEUTRAL, TIRED, ENERGETIC
    val bodyFlags: Int = 0               // bitmask: fever, nausea
)
