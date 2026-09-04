package com.luna.app.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.DayInfo
import com.luna.app.ui.common.loggedSymptoms
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaCream
import com.luna.app.ui.theme.LunaNavyRaised
import com.luna.app.ui.theme.LunaTextPrimary
import com.luna.app.ui.theme.LunaTextSecondary
import com.luna.app.ui.theme.color
import com.luna.app.ui.theme.label
import kotlinx.datetime.LocalDate

/**
 * What is known about one tapped day: where it sits in the cycle, and anything recorded against
 * it. Read-only by design — the home screen is the one place symptoms are edited, so there is no
 * second editor to keep in step with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    date: LocalDate,
    info: DayInfo,
    log: DailyLogEntity?,
    isToday: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LunaNavyRaised,
        contentColor = LunaCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isToday) "Today" else date.weekdayName(),
                style = MaterialTheme.typography.labelMedium,
                color = LunaTextSecondary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = date.pretty(),
                style = MaterialTheme.typography.titleLarge,
                color = LunaTextPrimary
            )

            Spacer(Modifier.height(20.dp))

            val phase = info.phase
            if (phase == null) {
                Text(
                    text = "This day is before anything you have logged, so there is nothing to " +
                        "place it against.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunaTextSecondary
                )
            } else {
                val expectedPeriod = phase == CyclePhase.MENSTRUAL && !info.isLoggedPeriod
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            // Same grammar as the grid: filled is recorded, outlined is expected.
                            .then(
                                if (expectedPeriod) {
                                    Modifier.border(1.5.dp, phase.color, CircleShape)
                                } else {
                                    Modifier.background(phase.color)
                                }
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = phase.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = LunaTextPrimary
                    )
                }
                info.cycleDay?.let { day ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Day $day of your cycle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LunaTextSecondary
                    )
                }
                if (phase == CyclePhase.MENSTRUAL) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (info.isLoggedPeriod) {
                            "Period logged"
                        } else {
                            "Period expected, not logged yet"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = LunaBlush
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val groups = log?.loggedSymptoms().orEmpty()
            if (groups.isEmpty()) {
                Text(
                    text = "No symptoms logged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunaTextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    groups.forEach { (title, values) ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = LunaTextSecondary
                            )
                            Text(
                                text = values.joinToString(", "),
                                style = MaterialTheme.typography.bodyLarge,
                                color = LunaTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LocalDate.pretty(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$dayOfMonth $monthName $year"
}

private fun LocalDate.weekdayName(): String =
    dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
