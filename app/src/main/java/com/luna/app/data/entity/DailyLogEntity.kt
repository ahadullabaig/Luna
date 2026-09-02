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
) {
    /**
     * True when nothing at all is recorded. Toggling a chip on and then off leaves the row in
     * place with every column cleared, and such a day must not read as "logged".
     *
     * A getter with no backing field, so Room does not see it as a column.
     */
    val isEmpty: Boolean
        get() = flowLevel == null && energy == null && painFlags == 0 && bodyFlags == 0
}
