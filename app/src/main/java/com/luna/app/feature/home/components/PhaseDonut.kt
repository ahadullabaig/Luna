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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luna.app.domain.model.CycleState
import com.luna.app.domain.usecase.phaseSegments
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaCream
import com.luna.app.ui.theme.LunaDeepNavy
import com.luna.app.ui.theme.color
import com.luna.app.ui.theme.label
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = -90f          // 12 o'clock
private const val SEGMENT_GAP_DEGREES = 1.6f  // hairline separation between phases

/**
 * The cycle as a ring: one arc per phase, sized by that phase's share of the cycle, with a
 * marker on today. Hand-drawn on a [Canvas] rather than pulled from a chart library — this is
 * four arcs and a dot, and a dependency would be more code than the drawing.
 */
@Composable
fun PhaseDonut(
    state: CycleState,
    modifier: Modifier = Modifier,
    diameter: Dp = 280.dp,
    strokeWidth: Dp = 30.dp
) {
    val segments = remember(state.cycleLength, state.periodLength) {
        phaseSegments(state.cycleLength, state.periodLength)
    }

    // Sweep the ring in on first composition, then settle the marker on top.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    val markerDay = state.cycleDay.coerceIn(1, state.cycleLength)
    val description = buildDonutDescription(state)

    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val ringDiameter = size.minDimension - stroke
            val topLeft = Offset(
                (size.width - ringDiameter) / 2f,
                (size.height - ringDiameter) / 2f
            )
            val arcSize = Size(ringDiameter, ringDiameter)
            val progress = reveal.value

            // Faint track, so the ring reads as a whole while the arcs sweep in.
            drawArc(
                color = LunaCream.copy(alpha = 0.06f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )

            var angle = START_ANGLE
            segments.forEach { (phase, days) ->
                val fullSweep = days.toFloat() / state.cycleLength * 360f
                val inset = if (fullSweep > SEGMENT_GAP_DEGREES * 2) SEGMENT_GAP_DEGREES else 0f
                val drawnSweep = (fullSweep - inset) * progress
                if (drawnSweep > 0f) {
                    drawArc(
                        color = phase.color,
                        startAngle = angle + inset / 2f,
                        sweepAngle = drawnSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                }
                angle += fullSweep
            }

            // Marker sits at the centre of today's slice, not its leading edge.
            val markerAlpha = ((progress - 0.65f) / 0.35f).coerceIn(0f, 1f)
            if (markerAlpha > 0f) {
                val markerDegrees = START_ANGLE + (markerDay - 0.5f) / state.cycleLength * 360f
                val radians = markerDegrees * PI.toFloat() / 180f
                val radius = ringDiameter / 2f
                val marker = Offset(
                    center.x + radius * cos(radians),
                    center.y + radius * sin(radians)
                )
                // Navy well punches the marker out of the arc beneath it.
                drawCircle(
                    color = LunaDeepNavy.copy(alpha = markerAlpha),
                    radius = stroke / 2f - 2.dp.toPx(),
                    center = marker
                )
                drawCircle(
                    color = LunaCream.copy(alpha = markerAlpha),
                    radius = 7.dp.toPx(),
                    center = marker
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.currentPhase.label,
                style = MaterialTheme.typography.headlineSmall,
                color = LunaCream,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Day ${state.cycleDay}",
                style = MaterialTheme.typography.titleMedium,
                color = LunaCream.copy(alpha = 0.62f),
                textAlign = TextAlign.Center
            )
            Text(
                text = countdownText(state),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isOverdue) LunaBlush else LunaCream.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 168.dp)
            )
        }
    }
}

internal fun countdownText(state: CycleState): String = when {
    state.isOverdue -> "Overdue by ${state.overdueDays} ${plural(state.overdueDays, "day")}"
    state.daysUntilNextPeriod == 0 -> "Period expected today"
    state.daysUntilNextPeriod < 0 -> "Period expected any day now"
    else -> "${state.daysUntilNextPeriod} ${plural(state.daysUntilNextPeriod, "day")} until period"
}

private fun plural(count: Int, word: String) = if (count == 1) word else "${word}s"

private fun buildDonutDescription(state: CycleState): String =
    "${state.currentPhase.label} phase, day ${state.cycleDay} of a " +
        "${state.cycleLength} day cycle. ${countdownText(state)}."
