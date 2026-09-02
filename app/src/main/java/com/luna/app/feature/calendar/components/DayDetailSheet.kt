package com.luna.app.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.luna.app.domain.model.DayInfo
import com.luna.app.ui.common.loggedSymptoms
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaCream
import com.luna.app.ui.theme.LunaDeepNavy
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
        containerColor = LunaDeepNavy,
        contentColor = LunaCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isToday) "TODAY" else date.weekdayName().uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LunaCream.copy(alpha = 0.45f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = date.pretty(),
                style = MaterialTheme.typography.titleLarge,
                color = LunaCream
            )

            Spacer(Modifier.height(20.dp))

            val phase = info.phase
            if (phase == null) {
                Text(
                    text = "Before your logged history — nothing to place this day against.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunaCream.copy(alpha = 0.5f)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(phase.color)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = phase.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = LunaCream
                    )
                    info.cycleDay?.let { day ->
                        Text(
                            text = "  ·  Day $day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LunaCream.copy(alpha = 0.5f)
                        )
                    }
                }
                if (info.isLoggedPeriod) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Period logged",
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
                    color = LunaCream.copy(alpha = 0.4f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    groups.forEach { (title, values) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = LunaCream.copy(alpha = 0.45f)
                            )
                            Text(
                                text = values.joinToString(" · "),
                                style = MaterialTheme.typography.bodyLarge,
                                color = LunaCream.copy(alpha = 0.85f)
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
