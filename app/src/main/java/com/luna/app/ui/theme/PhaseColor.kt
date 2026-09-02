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

/**
 * Readable text on top of a filled [color] swatch.
 *
 * The two full-strength phases are light enough to need navy on them; the 40% variants sit over
 * the navy background and stay dark, so they take cream. Keeping the rule here means the donut
 * and the calendar cells can never disagree about it.
 */
val CyclePhase.onColor: Color
    get() = when (this) {
        CyclePhase.MENSTRUAL, CyclePhase.OVULATION -> LunaDeepNavy
        CyclePhase.FOLLICULAR, CyclePhase.LUTEAL -> LunaCream
    }
