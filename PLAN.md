# Luna — Implementation Plan

A local-first Android period tracker. Single user (you), no backend, no analytics, no accounts. Data never leaves the device.

---

## 1. Guiding Principles

Because this is a personal app with intimate data, two principles drive every decision:

1. **Local-only.** No cloud, no sync, no network permissions. Room database on device, full stop. This also means zero auth complexity.
2. **No over-engineering.** Single-module project, MVVM, no multi-module Gradle gymnastics, no clean-architecture ceremony. Staff-engineer judgment call: for a one-person app you maintain alone, layering that you'd demand in a production team codebase becomes drag.

Everything below follows from those two.

---

## 2. Tech Stack

**Language & UI:** Kotlin 2.0+ with Jetpack Compose and Material 3. Compose is non-negotiable for a greenfield 2026 Android app — XML layouts are legacy.

**Architecture:** MVVM. `ViewModel` holds UI state as a `StateFlow`, screens are stateless Composables that collect state and emit events. No `LiveData`, no RxJava.

**Persistence:** Room with KSP (not KAPT — KAPT is deprecated for new projects). One database, two tables (detailed in §5).

**DI:** Hilt. Overkill for three screens, but you'll thank yourself the moment you add a fourth.

**Navigation:** Navigation Compose with type-safe routes (the `@Serializable` route classes introduced in 2.8).

**Serialization:** `org.jetbrains.kotlin.plugin.serialization` Gradle plugin + `kotlinx-serialization-json` runtime. Required for type-safe Navigation Compose route classes — without it, `@Serializable` on route objects does not compile.

**Date/time:** `kotlinx-datetime`. Avoid `java.time` boilerplate; `LocalDate` from kotlinx is cleaner and serializes natively.

**Charts:** Custom Compose `Canvas` for the donut. **Do not pull in a chart library for this.** A phase donut is 4–5 `drawArc` calls plus a pointer — a dependency is more code than the implementation.

**Calendar:** `com.kizitonwose.calendar:compose` — the standard Compose calendar library. Handles month paging, custom day cell rendering, week/month views. You'll render your own day cells for the phase highlighting.

**Build:** Gradle with Kotlin DSL (`build.gradle.kts`), version catalogs (`libs.versions.toml`). AGP 8.8+.

**minSdk:** 26. All dependencies (Room, kotlinx-datetime, Compose) support this floor; going lower adds complexity with no real benefit for a personal app.

---

## 3. Project Structure

Single `app` module. Package layout by feature, not by layer — easier to navigate when each screen's code lives together.

```
app/src/main/java/com/luna/app/
├── LunaApp.kt                    // @HiltAndroidApp
├── MainActivity.kt               // setContent { LunaTheme { LunaNavHost() } }
├── ui/
│   ├── theme/
│   │   ├── Color.kt              // palette constants
│   │   ├── Theme.kt              // LunaTheme composable + ColorScheme
│   │   └── Type.kt
│   ├── nav/
│   │   └── LunaNavHost.kt        // NavHost with Home + Calendar routes
│   └── common/                   // shared composables (PhaseChip, etc.)
├── feature/
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   ├── HomeUiState.kt
│   │   └── components/
│   │       ├── PhaseDonut.kt     // the Canvas donut
│   │       └── SymptomSection.kt
│   └── calendar/
│       ├── CalendarScreen.kt
│       ├── CalendarViewModel.kt
│       └── components/
│           └── PhaseDayCell.kt
├── data/
│   ├── LunaDatabase.kt
│   ├── entity/
│   │   ├── PeriodEntity.kt
│   │   └── DailyLogEntity.kt
│   ├── dao/
│   │   ├── PeriodDao.kt
│   │   └── DailyLogDao.kt
│   └── repo/
│       └── CycleRepository.kt
├── domain/
│   ├── model/
│   │   ├── CyclePhase.kt         // enum: MENSTRUAL, FOLLICULAR, OVULATION, LUTEAL
│   │   ├── CycleState.kt         // current phase + cycle day + predicted next
│   │   └── Symptom.kt            // sealed type tree for Flow/Pain/Energy/Body
│   └── usecase/
│       ├── GetCurrentCycleState.kt
│       └── GetPhaseForDate.kt
└── di/
    └── AppModule.kt              // Hilt module providing DB + DAOs
```

---

## 4. Design System

Translate the palette into a Material 3 `ColorScheme`. The app is dark-themed because the main BG (`#070E36` — deep navy) is dark. Don't fight it with a light theme.

```kotlin
// ui/theme/Color.kt
val LunaDeepNavy = Color(0xFF070E36)   // background
val LunaBlush    = Color(0xFFFAA7C7)   // primary highlight (periods, primary actions)
val LunaSand     = Color(0xFFF7E0A1)   // secondary highlight (ovulation, accents)
val LunaCream    = Color(0xFFFCFAF0)   // text / on-surface
```

Map to `darkColorScheme`:
- `background`, `surface` → `LunaDeepNavy`
- `primary` → `LunaBlush`, `onPrimary` → `LunaDeepNavy`
- `secondary` → `LunaSand`, `onSecondary` → `LunaDeepNavy`
- `onBackground`, `onSurface` → `LunaCream`

**Phase color mapping** (used by donut and calendar):
- Menstrual → `LunaBlush`
- Follicular → `LunaBlush` at 40% alpha
- Ovulation → `LunaSand`
- Luteal → `LunaSand` at 40% alpha

This gives you visual harmony: pink half = cycle's "period-adjacent" phases, gold half = "ovulation-adjacent" phases.

Typography: stick with the Material 3 default type scale initially. Optionally add a display font (e.g., a serif like Fraunces via Google Fonts Compose integration) for the phase name on the home screen — nice polish, skip for v1.

---

## 5. Data Model

Two entities. Kept flat and boring on purpose.

**`PeriodEntity`** — one row per bleeding episode:
```kotlin
@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,        // stored as epoch days via TypeConverter
    val endDate: LocalDate?          // null while period is ongoing
)
```

**`DailyLogEntity`** — one row per day the user logs symptoms (date is the primary key):
```kotlin
@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val date: LocalDate,
    val flowLevel: FlowLevel? = null,    // LIGHT, MEDIUM, HEAVY, or null
    val painFlags: Int = 0,              // bitmask: cramps, headache, backache, bloating
    val energy: Energy? = null,          // NEUTRAL, TIRED, ENERGETIC
    val bodyFlags: Int = 0               // bitmask: fever, nausea
)
```

> **Naming note:** the field and enum are called `FlowLevel` / `flowLevel` (not `Flow`) to avoid collision with `kotlinx.coroutines.Flow`, which is imported in every ViewModel and repository file.

**Why bitmasks for pain/body?** Symptoms are multi-select (you can have cramps AND a headache). A bitmask `Int` stores this in one column without a relation table. Simpler than a `List<PainType>` with a type converter, and Room handles `Int` natively.

```kotlin
object PainFlag {
    const val CRAMPS    = 1 shl 0
    const val HEADACHE  = 1 shl 1
    const val BACKACHE  = 1 shl 2
    const val BLOATING  = 1 shl 3
}
```

**TypeConverters** — add one for `LocalDate ↔ Long` (epoch days). Also add explicit `@TypeConverter` functions for `FlowLevel` and `Energy` enums (e.g., `FlowLevel? → String?` and back). Room does **not** automatically convert enums to strings — omitting these converters causes a runtime crash.

**Seeding/migration:** start with `fallbackToDestructiveMigration()` during development. Once you start caring about your data (probably after a couple of real cycles logged), bump schema version and write a proper `Migration`.

---

## 6. Cycle Logic

The brain of the app. All pure functions, no Room dependency — unit-testable, called from the repository/usecase layer.

```kotlin
enum class CyclePhase { MENSTRUAL, FOLLICULAR, OVULATION, LUTEAL }

data class CycleState(
    val currentPhase: CyclePhase,
    val cycleDay: Int,              // 1-based day in current cycle
    val cycleLength: Int,           // typically 28, computed from history
    val periodLength: Int,          // typically 5, computed from history
    val nextPeriodStart: LocalDate  // prediction
)
```

**Phase boundaries** (assuming 28-day cycle; generalize by scaling):
- Menstrual: days 1 through `periodLength` (default 5)
- Follicular: `periodLength + 1` through `cycleLength / 2 - 2` (~day 6–13)
- Ovulation: 3-day window centered on `cycleLength / 2` (~day 13–15)
- Luteal: remainder through `cycleLength` (~day 16–28)

**Averaging logic:** `cycleLength` = median gap between consecutive `PeriodEntity.startDate`s over the last 6 cycles. `periodLength` = median of `endDate - startDate` over the last 6 completed cycles — exclude any row where `endDate` is null (period still ongoing), since its true length is unknown; if the in-progress period is the only one logged, fall back to the default. If fewer than 2 completed cycles are available, fall back to 28 / 5.

**Edge cases to handle explicitly:**
- No periods logged yet → show empty state ("Log your first period to get started"), not a donut.
- Current date is inside a logged period → phase is MENSTRUAL regardless of calculation.
- Current date is *past* `nextPeriodStart` prediction + 3-day grace period → show "Period overdue by X days" rather than pretending you're still in luteal. The 3-day buffer absorbs normal cycle variance without false alarms.

Write these as pure functions in `domain/usecase/`, inject the repo, return `CycleState` as a `Flow<CycleState>` that re-emits when the DB changes.

---

## 7. Feature: Home Screen

Two sections stacked in a scrollable column:

### 7.1 Phase donut

A 28-segment circle (or `cycleLength` segments), each segment colored by phase. A small indicator (filled dot or caret outside the ring) marks the current day. Center of the donut displays:
- Phase name (e.g., "Follicular") in large Cream text
- Cycle day (e.g., "Day 8") below it
- Days until next period, smaller, below that

**Drawing approach in Compose `Canvas`:**

```kotlin
Canvas(modifier = Modifier.size(280.dp)) {
    val stroke = 36.dp.toPx()
    val diameter = size.minDimension - stroke
    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
    val arcSize = Size(diameter, diameter)

    var startAngle = -90f  // start at 12 o'clock
    phases.forEach { (phase, days) ->
        val sweep = (days.toFloat() / cycleLength) * 360f
        drawArc(
            color = phase.color(),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        startAngle += sweep
    }

    // current day marker: small filled circle on the ring
    val markerAngleRad = Math.toRadians((currentDayAngle - 90f).toDouble())
    val radius = diameter / 2
    val center = Offset(size.width / 2, size.height / 2)
    val markerPos = Offset(
        center.x + (radius * cos(markerAngleRad)).toFloat(),
        center.y + (radius * sin(markerAngleRad)).toFloat()
    )
    drawCircle(color = LunaCream, radius = 10.dp.toPx(), center = markerPos)
}
```

Put the center text in a `Box` that wraps the `Canvas` with `Alignment.Center`. Animate `startAngle` or the marker position with `animateFloatAsState` if you want the "it's a new day" transition to feel alive.

### 7.2 Symptom logging section

A vertical stack of four groups. Each group has a label and a row of selectable chips:

- **Flow** (single-select): Light, Medium, Heavy
- **Pain** (multi-select): Cramps, Headache, Backache, Bloating
- **Energy** (single-select): Tired, Neutral, Energetic
- **Body** (multi-select): Fever, Nausea

Use `FilterChip` from Material 3. Selected state uses `LunaBlush` background + `LunaDeepNavy` text. Unselected uses transparent background with `LunaCream` outline.

Saves are implicit: tapping a chip fires `viewModel.toggleSymptom(...)`, which upserts the `DailyLogEntity` for today immediately. No "Save" button — you want zero friction.

---

## 8. Feature: Calendar Screen

A month view powered by `kizitonwose/Calendar`. Each day cell is a custom composable `PhaseDayCell` that:
1. Queries (via the ViewModel) which phase that date falls into.
2. Renders a background circle in that phase's color, with the day number on top in `LunaCream`.
3. If the date has logged flow, shows a small dot indicator at the bottom of the cell.
4. Tapping a day navigates to a detail bottom sheet (optional for v1) showing symptoms logged that day.

Header shows month name and year in `LunaCream`, with chevron buttons to page months. A "Today" button in the top-right jumps back to the current month.

Computing phase for arbitrary past/future dates: walk backwards from that date to find the nearest logged period start, then compute the offset. For dates beyond the last logged period (future), project forward using `cycleLength`.

---

## 9. Dependencies (`libs.versions.toml` excerpt)

```toml
[versions]
agp = "8.8.0"
kotlin = "2.1.20"
compose-bom = "2025.04.00"
room = "2.7.0"
hilt = "2.54"
hilt-navigation = "1.2.0"
nav-compose = "2.9.0"
ksp = "2.1.20-1.0.31"
kotlinx-datetime = "0.6.1"
kotlinx-serialization = "1.7.3"
kizitonwose-calendar = "2.6.1"
coroutines = "1.10.0"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.15.0" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version = "2.9.0" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version = "1.10.0" }

compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3" }

nav-compose = { module = "androidx.navigation:navigation-compose", version.ref = "nav-compose" }

room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hilt-navigation" }

kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

kizitonwose-calendar = { module = "com.kizitonwose.calendar:compose", version.ref = "kizitonwose-calendar" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**Always verify these on Maven Central before your first build** — versions above reflect mid-2025 stable releases. The KSP version must match the Kotlin version exactly (`kotlin`/`ksp` version prefixes must be identical), so if you upgrade Kotlin, update KSP in lockstep.

---

## 10. Milestones

Ship in five small PRs-to-self. Each milestone produces a runnable app; never let the project sit broken.

**Milestone 1 — Skeleton & theme (half a day).**
New project, Compose + Material 3 + Hilt wired up, `LunaTheme` renders a `Scaffold` with the navy background and a "Hello Luna" centered in cream. NavHost set up with two empty routes. Goal: verify the theme looks right on a real device.

**Milestone 2 — Data layer (half a day).**
Room database, both entities, both DAOs, TypeConverter for `LocalDate`, repository class, Hilt module. Write a tiny debug button that inserts a fake period and reads it back into a log. Zero UI polish.

**Milestone 3 — Home screen (1–2 days).**
Phase donut Canvas, phase calculation use case, symptom chips wired to `DailyLogEntity`. Log-a-period flow: a floating action button that opens a date picker and inserts a `PeriodEntity`. At the end of this milestone, the app is functionally useful to you.

**Milestone 4 — Calendar screen (1 day).**
Integrate `kizitonwose/Calendar`, custom `PhaseDayCell`, month navigation, phase projection for future months. Optional: tap-a-day detail sheet.

**Milestone 5 — Polish + signed APK (half a day).**
Empty states, app icon, animations on phase transitions. Generate signing key, configure release build, produce the APK, sideload. Done.

Total: roughly 4–5 focused days of work.

---

## 11. Building the APK

Once Milestone 5 code is ready:

**Generate a signing keystore** (one-time, keep this file safe — back it up outside the repo):
```bash
keytool -genkey -v -keystore luna-release.jks -keyalg RSA \
  -keysize 2048 -validity 10000 -alias luna
```

**Configure `app/build.gradle.kts`** with a `signingConfigs` block reading from a local `keystore.properties` file (gitignored). Wire it into the `release` `buildType`. Enable `minifyEnabled = true` and `isShrinkResources = true` for the release build — free size win.

**Build:**
```bash
./gradlew assembleRelease
```

Output lands at `app/build/outputs/apk/release/app-release.apk`. Transfer to phone (USB, email to yourself, Syncthing, whatever), enable "Install unknown apps" for the file manager you're using, tap the APK, install.

**If you'd rather not manage signing manually,** Android Studio's *Build → Generate Signed Bundle / APK* wizard walks through keystore creation and build in one flow. Same outcome.

---

## 12. Things I Deliberately Left Out

Worth naming so you can opt into them later without surprise:

- **Notifications / period reminders.** Useful, but adds WorkManager + notification channels + permission handling. Save for v2.
- **Widgets.** Cool, but Glance is another learning curve. Save for v3.
- **Export/backup.** At some point you'll want to not lose years of data when you change phones. A simple "export JSON to Downloads" button is ~30 lines — add when you care.
- **Partner/couple sharing, cycle insights via ML, sex/mood tracking, contraception logging.** All valid features, all out of scope for "build it for yourself in a week."
- **Accessibility audit.** Compose gives you a lot for free via Material 3 defaults, but do a TalkBack pass before you consider it "done."
- **Unit tests.** Write them for `domain/usecase/` (pure functions, high-value). Skip tests for Composables — not worth the maintenance cost for a solo project.

---

That's the whole blueprint. When you're ready to start coding, I'd suggest going Milestone 1 → 2 first and showing me the result — the theme and data layer are where small decisions compound, and it's cheaper to course-correct early than after you've built the donut on top of a wrong foundation.
