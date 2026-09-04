package com.luna.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luna.app.domain.model.CycleState
import com.luna.app.feature.home.components.LogPeriodDialog
import com.luna.app.feature.home.components.PhaseDonut
import com.luna.app.feature.home.components.SymptomSection
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaDeepNavy
import com.luna.app.ui.theme.LunaHairline
import com.luna.app.ui.theme.LunaTextPrimary
import com.luna.app.ui.theme.LunaTextSecondary
import kotlinx.datetime.LocalDate

private const val LOG_PERIOD = "Log a period"

@Composable
fun HomeScreen(
    onOpenCalendar: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogDialog by rememberSaveable { mutableStateOf(false) }

    // A phone left open overnight should roll over to the new day when it comes back.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // One app, one primary action — so it says which one. A bare "+" spent a whole
            // floating button saying nothing.
            ExtendedFloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = LunaBlush,
                contentColor = LunaDeepNavy,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(text = LOG_PERIOD, style = MaterialTheme.typography.titleMedium) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            DateHeader(
                today = state.today,
                cycleDay = state.cycle?.cycleDay,
                onOpenCalendar = onOpenCalendar
            )
            Spacer(Modifier.height(28.dp))

            if (!state.isLoading) {
                val cycle = state.cycle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // The ring is the one thing on this screen that is centred. Everything
                        // else hangs off the left margin, so there is a spine to read down.
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (cycle == null) EmptyCycleRing() else PhaseDonut(state = cycle)
                }
                if (cycle != null) CycleFootnote(cycle)
            } else {
                Spacer(Modifier.height(300.dp))
            }

            Spacer(Modifier.height(40.dp))

            SymptomSection(
                log = state.todayLog,
                onFlowSelected = viewModel::setFlowLevel,
                onPainToggled = viewModel::togglePainFlag,
                onEnergySelected = viewModel::setEnergy,
                onBodyToggled = viewModel::toggleBodyFlag
            )

            Spacer(Modifier.height(112.dp))
        }
    }

    if (showLogDialog) {
        LogPeriodDialog(
            today = state.today,
            onDismiss = { showLogDialog = false },
            onConfirm = { start, end ->
                viewModel.logPeriod(start, end)
                showLogDialog = false
            }
        )
    }
}

@Composable
private fun DateHeader(today: LocalDate, cycleDay: Int?, onOpenCalendar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = today.pretty(),
                style = MaterialTheme.typography.titleLarge,
                color = LunaTextPrimary
            )
            if (cycleDay != null) {
                Text(
                    text = "Cycle day $cycleDay",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LunaTextSecondary
                )
            }
        }
        IconButton(onClick = onOpenCalendar) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Open calendar",
                tint = LunaTextSecondary
            )
        }
    }
}

/**
 * Holds the ring's footprint before any period is logged, so the layout does not jump the first
 * time history appears.
 */
@Composable
private fun EmptyCycleRing() {
    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val stroke = 30.dp.toPx()
            val ringDiameter = size.minDimension - stroke
            drawArc(
                color = LunaHairline,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(
                    (size.width - ringDiameter) / 2f,
                    (size.height - ringDiameter) / 2f
                ),
                size = Size(ringDiameter, ringDiameter),
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 180.dp)
        ) {
            Text(
                text = "Nothing logged yet",
                style = MaterialTheme.typography.titleLarge,
                color = LunaTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap $LOG_PERIOD and the ring fills in.",
                style = MaterialTheme.typography.bodyMedium,
                color = LunaTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CycleFootnote(cycle: CycleState) {
    val text = if (cycle.isPersonalised) {
        "Your recent cycles average ${cycle.cycleLength} days, " +
            "with a ${cycle.periodLength}-day period."
    } else {
        "Still learning. Using ${cycle.cycleLength}-day estimates until you log two cycles."
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LunaTextSecondary,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun LocalDate.pretty(): String {
    val weekday = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$weekday $dayOfMonth $monthName"
}
