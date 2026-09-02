package com.luna.app.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.domain.model.Energy
import com.luna.app.domain.model.FlowLevel
import com.luna.app.ui.common.bodyLabels
import com.luna.app.ui.common.energyLabels
import com.luna.app.ui.common.flowLabels
import com.luna.app.ui.common.painLabels
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaCream
import com.luna.app.ui.theme.LunaDeepNavy

/**
 * Today's symptoms as four groups of chips. There is no save button: every tap writes
 * immediately, and tapping a selected chip clears it again.
 */
@Composable
fun SymptomSection(
    log: DailyLogEntity?,
    onFlowSelected: (FlowLevel?) -> Unit,
    onPainToggled: (Int) -> Unit,
    onEnergySelected: (Energy?) -> Unit,
    onBodyToggled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        ChipGroup(title = "Flow") {
            flowLabels.forEach { (level, label) ->
                val selected = log?.flowLevel == level
                LunaChip(label = label, selected = selected) {
                    onFlowSelected(if (selected) null else level)
                }
            }
        }

        ChipGroup(title = "Pain") {
            painLabels.forEach { (flag, label) ->
                LunaChip(
                    label = label,
                    selected = (log?.painFlags ?: 0) and flag != 0
                ) { onPainToggled(flag) }
            }
        }

        ChipGroup(title = "Energy") {
            energyLabels.forEach { (energy, label) ->
                val selected = log?.energy == energy
                LunaChip(label = label, selected = selected) {
                    onEnergySelected(if (selected) null else energy)
                }
            }
        }

        ChipGroup(title = "Body") {
            bodyLabels.forEach { (flag, label) ->
                LunaChip(
                    label = label,
                    selected = (log?.bodyFlags ?: 0) and flag != 0
                ) { onBodyToggled(flag) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = LunaCream.copy(alpha = 0.5f),
            textAlign = TextAlign.Start
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun LunaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(percent = 50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = LunaCream.copy(alpha = 0.04f),
            labelColor = LunaCream.copy(alpha = 0.78f),
            selectedContainerColor = LunaBlush,
            selectedLabelColor = LunaDeepNavy
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) LunaBlush else LunaCream.copy(alpha = 0.22f)
        )
    )
}
