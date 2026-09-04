package com.luna.app.feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luna.app.domain.model.CyclePhase
import com.luna.app.domain.model.CycleState
import com.luna.app.domain.usecase.phaseSegments
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaTextPrimary
import com.luna.app.ui.theme.LunaTextSecondary
import com.luna.app.ui.theme.color
import com.luna.app.ui.theme.label

private const val START_ANGLE = -90f          // 12 o'clock
private const val SEGMENT_GAP_DEGREES = 1.6f  // hairline separation between phases

/**
 * The cycle as a ring, drawn with the app's one rule: **solid is what happened, hairline is what
 * Luna expects.**
 *
 * The ring used to be four equal-weight arcs with a dot marking today, which made it a chart of
 * proportions — it said how long each phase is, but not where you are without hunting for the
 * dot. Splitting every phase at the current cycle day turns the same drawing into a reading: the
 * solid run behind you is the cycle you have actually lived, the hairline ahead is a projection.
 * The boundary between the two *is* the marker, so the dot is gone.
 *
 * It is also the same distinction the calendar draws between a logged period day and an expected
 * one, which is the point — one rule, both screens.
 */
@Composable
fun PhaseDonut(
    state: CycleState,
    modifier: Modifier = Modifier,
    diameter: Dp = 280.dp,
    strokeWidth: Dp = 30.dp,
    futureStrokeWidth: Dp = 3.dp
) {
    val segments = remember(state.cycleLength, state.periodLength) {
        phaseSegments(state.cycleLength, state.periodLength)
    }

    // Sweep the ring in on first composition.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    val elapsedDays = state.cycleDay.coerceIn(1, state.cycleLength)
    val reading = remember(state) { heroReading(state) }
    val description = remember(state) { buildDonutDescription(state) }

    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val hairline = futureStrokeWidth.toPx()
            val ringDiameter = size.minDimension - stroke
            val topLeft = Offset(
                (size.width - ringDiameter) / 2f,
                (size.height - ringDiameter) / 2f
            )
            val arcSize = Size(ringDiameter, ringDiameter)
            val degreesPerDay = 360f / state.cycleLength
            val revealed = reveal.value * 360f

            var dayCursor = 0
            segments.forEach { (phase, days) ->
                val fullSweep = days * degreesPerDay
                val inset = if (fullSweep > SEGMENT_GAP_DEGREES * 2) SEGMENT_GAP_DEGREES else 0f
                val from = dayCursor * degreesPerDay + inset / 2f
                val to = (dayCursor + days) * degreesPerDay - inset / 2f
                // Where this phase changes from lived to predicted. Clamped into the segment, so
                // a phase entirely behind you is all solid and one entirely ahead is all hairline.
                val split = (elapsedDays * degreesPerDay).coerceIn(from, to)

                drawRevealedArc(from, split, revealed, phase.color, stroke, topLeft, arcSize)
                drawRevealedArc(split, to, revealed, phase.color, hairline, topLeft, arcSize)
                dayCursor += days
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.currentPhase.label,
                style = MaterialTheme.typography.titleMedium,
                color = LunaTextSecondary
            )
            Text(
                text = reading.value,
                style = MaterialTheme.typography.displayLarge,
                color = if (reading.alert) LunaBlush else LunaTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = reading.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = if (reading.alert) LunaBlush else LunaTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        }
    }
}

/**
 * Draws the part of an arc the reveal animation has reached, and nothing if it has not reached it
 * yet. [from] and [to] are degrees measured from [START_ANGLE], as is [revealed].
 */
private fun DrawScope.drawRevealedArc(
    from: Float,
    to: Float,
    revealed: Float,
    color: Color,
    width: Float,
    topLeft: Offset,
    arcSize: Size
) {
    val end = minOf(to, revealed)
    if (end <= from) return
    drawArc(
        color = color,
        startAngle = START_ANGLE + from,
        sweepAngle = end - from,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = width, cap = StrokeCap.Butt)
    )
}

/**
 * The one number in the middle of the ring, and the words that finish its sentence.
 *
 * Which number that is depends on where you are. Counting down to the next period is the useful
 * answer most of the time, but during a period it is a non-answer — what you want then is which
 * day of it you are on. Same slot, whichever figure is actually live.
 */
internal data class HeroReading(
    val value: String,
    val caption: String,
    val alert: Boolean = false
)

internal fun heroReading(state: CycleState): HeroReading = when {
    state.isOverdue -> HeroReading(
        value = state.overdueDays.toString(),
        caption = "${plural(state.overdueDays, "day")} late",
        alert = true
    )
    state.currentPhase == CyclePhase.MENSTRUAL -> HeroReading(
        value = state.cycleDay.toString(),
        caption = "day of your period"
    )
    state.daysUntilNextPeriod == 0 -> HeroReading(value = "Due", caption = "today")
    state.daysUntilNextPeriod < 0 -> HeroReading(value = "Due", caption = "any day now")
    else -> HeroReading(
        value = state.daysUntilNextPeriod.toString(),
        caption = "${plural(state.daysUntilNextPeriod, "day")} to your period"
    )
}

private fun plural(count: Int, word: String) = if (count == 1) word else "${word}s"

private fun buildDonutDescription(state: CycleState): String {
    val reading = heroReading(state)
    return "${state.currentPhase.label} phase, day ${state.cycleDay} of a " +
        "${state.cycleLength} day cycle. ${reading.value} ${reading.caption}."
}
