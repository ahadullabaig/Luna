package com.luna.app.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.DayInfo
import com.luna.app.ui.theme.LunaCream
import com.luna.app.ui.theme.LunaTextFaint
import com.luna.app.ui.theme.LunaTextSecondary
import com.luna.app.ui.theme.adjacentColor
import com.luna.app.ui.theme.color
import com.luna.app.ui.theme.label
import com.luna.app.ui.theme.onColor
import kotlinx.datetime.LocalDate

private val CircleSize = 36.dp

/**
 * One day in the month grid.
 *
 * The swatch carries two pieces of information at once. Its **colour** is the phase. Whether it is
 * **solid or a hollow ring** separates record from projection: a period day the user actually
 * logged is filled, while a period day the app merely expects — next month's, or one inferred
 * across a gap in logging — is drawn as a ring. A ring is never a promise.
 *
 * The other three phases are always computed, never recorded, so they carry no such distinction
 * and stay as flat fills. That keeps the two things worth spotting — logged periods and expected
 * ones — as the only outlined marks on the grid.
 *
 * The dot beneath the number marks a day with symptoms recorded against it.
 */
@Composable
fun PhaseDayCell(
    date: LocalDate,
    info: DayInfo,
    isToday: Boolean,
    hasLog: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = info.phase
    // Menstrual is the one phase the user can actually record, so it is the one that distinguishes
    // a logged day from an expected one.
    val expectedPeriod = phase == CyclePhase.MENSTRUAL && !info.isLoggedPeriod
    val fill = phase?.color

    val contentColor = when {
        fill == null -> LunaTextFaint
        // The number takes the ring's own colour, so a predicted period reads as one mark.
        expectedPeriod -> fill
        else -> phase.onColor
    }

    Box(
        modifier = modifier
            // The whole cell is the tap target, not just the swatch inside it — on a phone that
            // is the difference between a 36dp target and a comfortable 48dp one.
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = describe(date, info, isToday, hasLog) },
        contentAlignment = Alignment.Center
    ) {
        // Today's ring sits at the cell's edge rather than on the swatch. Both rings used to be
        // a 1.5dp border on the same node, and chained borders draw at the same inset — so on a
        // day that was both today and an expected period, cream painted straight over blush and
        // the prediction vanished from the one cell most likely to be read. Two radii, two facts.
        if (isToday) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .border(1.5.dp, LunaCream, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(CircleSize)
                .clip(CircleShape)
                .then(
                    when {
                        fill == null -> Modifier
                        expectedPeriod -> Modifier.border(1.5.dp, fill, CircleShape)
                        else -> Modifier.background(fill)
                    }
                )
                // The number and dot are described by the cell above; drop them from the tree so
                // TalkBack reads one label per day instead of a bare digit.
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor
                )
                // The dot's space is reserved either way, so numbers do not shift between cells.
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (hasLog) contentColor else Color.Transparent)
                )
            }
        }
    }
}

/**
 * A day belonging to a neighbouring month: present so the weeks line up, and inert.
 *
 * It keeps its phase colour at reduced strength rather than being blanked. Dropping the phase
 * entirely meant a period spanning a month boundary looked truncated — open September and the
 * 31st of August, a day actually on record, showed as an empty cell. Grey it for interaction,
 * not for information.
 */
@Composable
fun AdjacentMonthCell(date: LocalDate, info: DayInfo, modifier: Modifier = Modifier) {
    val phase = info.phase
    val expectedPeriod = phase == CyclePhase.MENSTRUAL && !info.isLoggedPeriod
    val fill = phase?.adjacentColor

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(CircleSize)
                .clip(CircleShape)
                .then(
                    when {
                        fill == null -> Modifier
                        expectedPeriod -> Modifier.border(1.5.dp, fill, CircleShape)
                        else -> Modifier.background(fill)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (phase == null) LunaTextFaint else LunaTextSecondary
            )
        }
    }
}

/** The weekday initials above the grid. */
@Composable
fun WeekdayHeader(labels: List<String>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LunaTextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

private fun describe(date: LocalDate, info: DayInfo, isToday: Boolean, hasLog: Boolean): String {
    val parts = mutableListOf(if (isToday) "Today, ${date.dayOfMonth}" else "${date.dayOfMonth}")
    info.phase?.let { parts += it.label }
    if (info.isLoggedPeriod) parts += "period logged"
    if (hasLog) parts += "symptoms logged"
    return parts.joinToString(", ")
}
