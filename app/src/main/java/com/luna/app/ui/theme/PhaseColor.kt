package com.luna.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.luna.app.domain.model.CyclePhase

/**
 * Phase → colour, shared by the donut and (from M4) the calendar day cells.
 *
 * Pink half of the wheel is period-adjacent, gold half is ovulation-adjacent; the softer 40%
 * variants mark the two long "in between" phases so the two events read as the accents.
 */
val CyclePhase.color: Color
    get() = when (this) {
        CyclePhase.MENSTRUAL -> LunaBlush
        CyclePhase.FOLLICULAR -> LunaBlush.copy(alpha = 0.4f)
        CyclePhase.OVULATION -> LunaSand
        CyclePhase.LUTEAL -> LunaSand.copy(alpha = 0.4f)
    }

val CyclePhase.label: String
    get() = when (this) {
        CyclePhase.MENSTRUAL -> "Menstrual"
        CyclePhase.FOLLICULAR -> "Follicular"
        CyclePhase.OVULATION -> "Ovulation"
        CyclePhase.LUTEAL -> "Luteal"
    }
