package com.luna.app.ui.common

import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.domain.model.BodyFlag
import com.luna.app.domain.model.Energy
import com.luna.app.domain.model.FlowLevel
import com.luna.app.domain.model.PainFlag

/**
 * Display names for every symptom, in the order they are offered on the home screen.
 *
 * Shared so the calendar's read-only day sheet names a symptom exactly as the chip that recorded
 * it — two lists would drift the first time one of them is reworded.
 */
val flowLabels: List<Pair<FlowLevel, String>> = listOf(
    FlowLevel.LIGHT to "Light",
    FlowLevel.MEDIUM to "Medium",
    FlowLevel.HEAVY to "Heavy"
)

val painLabels: List<Pair<Int, String>> = listOf(
    PainFlag.CRAMPS to "Cramps",
    PainFlag.HEADACHE to "Headache",
    PainFlag.BACKACHE to "Backache",
    PainFlag.BLOATING to "Bloating"
)

val energyLabels: List<Pair<Energy, String>> = listOf(
    Energy.TIRED to "Tired",
    Energy.NEUTRAL to "Neutral",
    Energy.ENERGETIC to "Energetic"
)

val bodyLabels: List<Pair<Int, String>> = listOf(
    BodyFlag.FEVER to "Fever",
    BodyFlag.NAUSEA to "Nausea"
)

/** What was recorded on this day, grouped for display. Groups with nothing recorded are dropped. */
fun DailyLogEntity.loggedSymptoms(): List<Pair<String, List<String>>> = listOf(
    "Flow" to listOfNotNull(flowLabels.firstOrNull { it.first == flowLevel }?.second),
    "Pain" to painLabels.filter { painFlags and it.first != 0 }.map { it.second },
    "Energy" to listOfNotNull(energyLabels.firstOrNull { it.first == energy }?.second),
    "Body" to bodyLabels.filter { bodyFlags and it.first != 0 }.map { it.second }
).filter { (_, values) -> values.isNotEmpty() }
