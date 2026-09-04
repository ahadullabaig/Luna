package com.luna.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.luna.app.domain.model.CyclePhase

/**
 * Phase → colour, shared by the donut and the calendar day cells.
 *
 * Pink half of the wheel is period-adjacent, gold half is ovulation-adjacent; the muted variants
 * mark the two long "in between" phases so the two events read as the accents. All four are
 * opaque — see the note on [LunaBlushMuted] for why the muted pair are not alpha.
 */
val CyclePhase.color: Color
    get() = when (this) {
        CyclePhase.MENSTRUAL -> LunaBlush
        CyclePhase.FOLLICULAR -> LunaBlushMuted
        CyclePhase.OVULATION -> LunaSand
        CyclePhase.LUTEAL -> LunaSandMuted
    }

/**
 * The same colour, weakened for a day that belongs to a neighbouring month.
 *
 * Alpha is the right tool here, unlike in [color]: the intent really is "this exact phase, but
 * subordinate", so compositing toward the background is the meaning rather than an accident.
 */
val CyclePhase.adjacentColor: Color
    get() = color.copy(alpha = 0.45f)

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
 * The two full-strength phases are light enough to need navy on them; the muted pair are dark
 * enough to take cream, at 5.08:1 and 4.57:1. Keeping the rule here means the donut and the
 * calendar cells can never disagree about it.
 */
val CyclePhase.onColor: Color
    get() = when (this) {
        CyclePhase.MENSTRUAL, CyclePhase.OVULATION -> LunaDeepNavy
        CyclePhase.FOLLICULAR, CyclePhase.LUTEAL -> LunaCream
    }
