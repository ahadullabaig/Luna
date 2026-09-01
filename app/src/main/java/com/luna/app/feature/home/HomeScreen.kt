package com.luna.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.luna.app.ui.theme.LunaCream
import com.luna.app.ui.theme.LunaDeepNavy
import kotlinx.datetime.LocalDate

@Composable
fun HomeScreen(
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
            FloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = LunaBlush,
                contentColor = LunaDeepNavy
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Log a period")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            DateHeader(state.today)
            Spacer(Modifier.height(24.dp))

            if (!state.isLoading) {
                val cycle = state.cycle
                if (cycle == null) {
                    EmptyCycleRing()
                } else {
                    if (cycle.isOverdue) {
                        OverdueBanner(cycle)
                        Spacer(Modifier.height(20.dp))
                    }
                    PhaseDonut(state = cycle)
                    Spacer(Modifier.height(20.dp))
                    CycleFootnote(cycle)
                }
            } else {
                Spacer(Modifier.height(280.dp))
            }

            Spacer(Modifier.height(36.dp))
            HorizontalDivider(color = LunaCream.copy(alpha = 0.08f))
            Spacer(Modifier.height(28.dp))

            SymptomSection(
                log = state.todayLog,
                onFlowSelected = viewModel::setFlowLevel,
                onPainToggled = viewModel::togglePainFlag,
                onEnergySelected = viewModel::setEnergy,
                onBodyToggled = viewModel::toggleBodyFlag
            )

            Spacer(Modifier.height(96.dp))
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
private fun DateHeader(today: LocalDate) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelMedium,
            color = LunaCream.copy(alpha = 0.45f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = today.pretty(),
            style = MaterialTheme.typography.titleLarge,
            color = LunaCream
        )
    }
}

/**
 * Holds the donut's footprint before any period is logged, so the layout does not jump the
 * first time history appears.
 */
@Composable
private fun EmptyCycleRing() {
    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val stroke = 30.dp.toPx()
            val ringDiameter = size.minDimension - stroke
            drawArc(
                color = LunaCream.copy(alpha = 0.07f),
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
        Text(
            text = "Log your first period to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = LunaCream.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 170.dp)
        )
    }
}

@Composable
private fun OverdueBanner(cycle: CycleState) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LunaBlush.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Period overdue by ${cycle.overdueDays} " +
                if (cycle.overdueDays == 1) "day" else "days",
            style = MaterialTheme.typography.titleMedium,
            color = LunaBlush,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        )
    }
}

@Composable
private fun CycleFootnote(cycle: CycleState) {
    val text = if (cycle.isPersonalised) {
        "Based on your history · ${cycle.cycleLength}-day cycle, " +
            "${cycle.periodLength}-day period"
    } else {
        "Using ${cycle.cycleLength}/${cycle.periodLength} defaults " +
            "until you log a few more cycles"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LunaCream.copy(alpha = 0.4f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun LocalDate.pretty(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$dayOfMonth $monthName"
}
