package com.luna.app.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.domain.model.Energy
import com.luna.app.domain.model.FlowLevel
import com.luna.app.ui.common.bodyLabels
import com.luna.app.ui.common.energyLabels
import com.luna.app.ui.common.flowLabels
import com.luna.app.ui.common.painLabels
import com.luna.app.ui.theme.LunaBlush
import com.luna.app.ui.theme.LunaDeepNavy
import com.luna.app.ui.theme.LunaHairline
import com.luna.app.ui.theme.LunaNavyRaised
import com.luna.app.ui.theme.LunaOutline
import com.luna.app.ui.theme.LunaTextSecondary

private val PillShape = RoundedCornerShape(percent = 50)

/**
 * Today's symptoms. There is no save button: every tap writes immediately, and tapping the
 * current answer clears it again.
 *
 * The four groups do not all behave the same way, so they no longer all look the same way. Flow
 * and Energy are single-select *and* ordinal — light to heavy, tired to energetic — which a
 * connected bar states just by being connected. Pain and Body are pick-any, so they stay as
 * separate pills. Twelve identical chips with two different behaviours behind them was the one
 * thing form should never do.
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
        Group(title = "Flow") {
            SegmentedRow(
                options = flowLabels,
                selection = log?.flowLevel,
                onSelect = onFlowSelected
            )
        }

        Group(title = "Pain") {
            ChipRow {
                painLabels.forEach { (flag, label) ->
                    LunaChip(
                        label = label,
                        selected = (log?.painFlags ?: 0) and flag != 0
                    ) { onPainToggled(flag) }
                }
            }
        }

        Group(title = "Energy") {
            SegmentedRow(
                options = energyLabels,
                selection = log?.energy,
                onSelect = onEnergySelected
            )
        }

        Group(title = "Body") {
            ChipRow {
                bodyLabels.forEach { (flag, label) ->
                    LunaChip(
                        label = label,
                        selected = (log?.bodyFlags ?: 0) and flag != 0
                    ) { onBodyToggled(flag) }
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = LunaTextSecondary
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

/**
 * One connected bar of mutually exclusive options, in the order they were given — which for both
 * groups that use it is a scale, so the order carries meaning.
 *
 * Pressing the selected option clears it, matching the chips.
 */
@Composable
private fun <T> SegmentedRow(
    options: List<Pair<T, String>>,
    selection: T?,
    onSelect: (T?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(PillShape)
            .border(1.dp, LunaOutline, PillShape)
    ) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selection
            Segment(
                label = label,
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else value) },
                modifier = Modifier.weight(1f)
            )
            // A rule only earns its place between two unfilled segments; next to a blush one it
            // is noise.
            val nextSelected = options.getOrNull(index + 1)?.first == selection
            if (index < options.lastIndex && !isSelected && !nextSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(LunaHairline)
                )
            }
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) LunaBlush else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) LunaDeepNavy else LunaTextSecondary
        )
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
        shape = PillShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = LunaNavyRaised,
            labelColor = LunaTextSecondary,
            selectedContainerColor = LunaBlush,
            selectedLabelColor = LunaDeepNavy
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) LunaBlush else LunaOutline
        )
    )
}
