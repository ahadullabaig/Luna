package com.luna.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────
//
// Contrast ratios below are WCAG 2.1 relative luminance against LunaDeepNavy.

val LunaDeepNavy = Color(0xFF070E36)          // background
val LunaNavyRaised = Color(0xFF0D1746)        // anything sitting above the background
val LunaBlush = Color(0xFFFAA7C7)             // menstrual, primary action     10.15:1
val LunaSand = Color(0xFFF7E0A1)              // ovulation                     14.34:1
val LunaCream = Color(0xFFFCFAF0)             // text                          17.84:1

/**
 * The two "in between" phases.
 *
 * These were once `LunaBlush.copy(alpha = 0.4f)` and `LunaSand.copy(alpha = 0.4f)`, which is not
 * what reached the screen: composited over the navy they land on #684B70 and #676261 — a muddy
 * plum and a flat warm grey — and the first of those measures 2.51:1, under the 3:1 WCAG asks of
 * a graphical object. Between them they are 20 days of a 28-day cycle.
 *
 * Declaring them opaque means they can be tuned against the ground they actually sit on. Both
 * read as a quieter version of their parent hue, clear 3:1 against the background, and take
 * LunaCream at better than 4.5:1 so a day number can sit on top.
 */
val LunaBlushMuted = Color(0xFF8E5C77)        // follicular                     3.51:1
val LunaSandMuted = Color(0xFF80723F)         // luteal                         3.91:1

// ── Cream, in three steps ────────────────────────────────────────────────────
//
// Cream used to appear at nineteen different alphas, several of them visually identical: .40,
// .45 and .50 span 3.6:1 to 5.0:1 and read as one colour. A ladder that fine does no work while
// making every new screen a fresh guess, so these four roles are the whole vocabulary. Pick a
// role, never a number.

val LunaTextPrimary = LunaCream                          // what you read       17.84:1
val LunaTextSecondary = LunaCream.copy(alpha = 0.62f)    // support             7.18:1
val LunaTextFaint = LunaCream.copy(alpha = 0.40f)        // inert only          3.61:1

/**
 * Two edge weights, which are not the same job: [LunaOutline] bounds something you can press,
 * [LunaHairline] divides content that you cannot.
 */
val LunaOutline = LunaCream.copy(alpha = 0.22f)
val LunaHairline = LunaCream.copy(alpha = 0.10f)
