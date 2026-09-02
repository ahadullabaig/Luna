# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## What this app is

Luna is a **local-only Android period tracker** — no network, no backend, no accounts. Room database on device, full stop. All design decisions flow from two principles: local-only data, and no over-engineering for a solo-maintained app.

---

## Build commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (minified + shrunk)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# Install debug APK on connected device
./gradlew installDebug

# Run lint
./gradlew lint

# Run unit tests
./gradlew test
```

**Toolchain versions** live in `gradle/libs.versions.toml`. Current: `agp = "9.3.2"`, `kotlin = "2.2.10"`, `ksp = "2.3.6"`, Gradle `9.5.0`.

KSP no longer carries a Kotlin version prefix — KSP2 versions independently, so `ksp = "2.3.6"` pairs with `kotlin = "2.2.10"` with no lockstep rule to honour. (Earlier revisions of this file claimed the prefix must match; that was true of KSP1's `2.1.20-1.0.31` scheme and is no longer.) If a Kotlin bump breaks annotation processing, bump `ksp` to its own latest rather than hunting for a matching prefix.

**Memory:** `kspDebugKotlin` will die with `OutOfMemoryError: Metaspace` under the default daemon heap. `gradle.properties` sets `org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=1024m`; don't remove it.

---

## Architecture

Single `app` module. MVVM. Package layout by feature, not by layer.

**Data flow:** Room DAOs → `CycleRepository` → use cases in `domain/usecase/` → `ViewModel` (exposes `StateFlow`) → stateless Composable screens that collect state and emit events. No `LiveData`, no RxJava.

**DI:** Hilt with `@HiltAndroidApp` on `LunaApp`, `@HiltViewModel` on ViewModels, `hiltViewModel()` at Composable call sites. All bindings are in `di/AppModule.kt`.

**Navigation:** Navigation Compose 2.9 with type-safe `@Serializable` route objects (`ui/nav/Routes.kt`). Routes must be `@Serializable` — the `kotlin-serialization` plugin is required for this to compile.

---

## Current milestone status

Milestones 1 (skeleton/theme), 2 (data layer) and 3 (home screen) are complete. The M2 debug scaffolding — the period counter and "Insert Fake Period" button — is gone, replaced by the real home screen.

**Milestone 4 (Calendar screen)** is next: `kizitonwose/Calendar`, a custom `PhaseDayCell`, and phase projection across arbitrary months. See the Calendar screen section below.

**Milestone 5:** Empty states, app icon, signing config, release APK. Note `app/build.gradle.kts` already references `proguard-rules.pro`, which does not exist — `assembleRelease` fails until M5 creates it.

**Known debt, deliberately unaddressed:** `allowBackup="true"` in the manifest contradicts the local-only privacy claim; `fallbackToDestructiveMigration()` is still armed with `exportSchema = false`; `PeriodDao.getRecentPeriods()` and its `CycleRepository` passthrough are dead code (M3 reads full history as a `Flow` instead).

---

## Domain model and cycle logic

**Phase boundaries** (implemented as pure functions — no Room dependency, unit-testable):
- Menstrual: days 1 through `periodLength` (default 5)
- Follicular: `periodLength + 1` through `ovulationDay - 2`
- Ovulation: 3-day window, `ovulationDay - 1` through `ovulationDay + 1`
- Luteal: remainder through `cycleLength`

`ovulationDay = cycleLength - 14`. The luteal phase is ~14 days fixed; it's the follicular phase that varies with cycle length. This anchoring gives accurate results for non-28-day cycles.

**Averaging:** `cycleLength` = median gap over last 6 periods. `periodLength` = median over last 6 *completed* periods (exclude rows where `endDate` is null). Fall back to 28/5 if fewer than 2 completed cycles.

Period length is **inclusive of both endpoints** — `startDate.daysUntil(endDate) + 1`, so a period that starts and ends on the same day is 1 day, not 0. PLAN.md §6 writes this as `endDate - startDate` and is off by one; the code is correct, the doc is not.

**Edge cases that must be handled explicitly:**
- No periods logged → empty state ("Log your first period to get started"), not a donut.
- Today is inside a logged period → phase is MENSTRUAL regardless of calculation.
- Today is past `nextPeriodStart` + 3-day grace → "Period overdue by X days" state.

**Where this lives:** the pure functions are top-level in `domain/usecase/CycleMath.kt` — `computeCycleState(periods, today)` and `computePhaseForDate(date, periods)` are the entry points, and they take plain lists, not a repository. `GetCurrentCycleState` and `GetPhaseForDate` are thin `Flow` wrappers over `CycleRepository` that delegate to them. Keep new cycle logic in `CycleMath.kt`: the split is what makes it testable without mocking Room.

Tests are in `app/src/test/java/com/luna/app/domain/usecase/` — 26 covering the medians, every phase boundary day-by-day, the three edge cases above, and the invariant that `phaseSegments()` always sums to `cycleLength`.

---

## Data layer details

**`FlowLevel` naming:** The enum is `FlowLevel` (not `Flow`) to avoid collision with `kotlinx.coroutines.Flow`, which is imported in every ViewModel and repository file.

**Bitmasks for multi-select symptoms:** `painFlags` and `bodyFlags` in `DailyLogEntity` are `Int` bitmasks. Constants are in `PainFlag` and `BodyFlag` objects in `domain/model/Symptom.kt`. Room handles `Int` natively — no converter needed for the flags.

**TypeConverters** in `data/Converters.kt`: `LocalDate ↔ Long` (epoch days), `FlowLevel? ↔ String?`, `Energy? ↔ String?`. Room does **not** auto-convert enums — omitting these causes a runtime crash.

**Migration strategy:** `fallbackToDestructiveMigration()` is active during development. When real data matters, bump `version` in `LunaDatabase` and write a proper `Migration` object.

---

## Design system

Dark-only theme (`darkColorScheme`). Four palette constants in `ui/theme/Color.kt`:

| Constant | Hex | Role |
|---|---|---|
| `LunaDeepNavy` | `#070E36` | background, surface |
| `LunaBlush` | `#FAA7C7` | primary, menstrual phase |
| `LunaSand` | `#F7E0A1` | secondary, ovulation phase |
| `LunaCream` | `#FCFAF0` | text, onBackground |

**Phase → color mapping** (used by donut and calendar day cells):
- Menstrual → `LunaBlush`
- Follicular → `LunaBlush` at 40% alpha
- Ovulation → `LunaSand`
- Luteal → `LunaSand` at 40% alpha

---

## Phase donut (built — `feature/home/components/PhaseDonut.kt`)

Custom Compose `Canvas` — do not reach for a chart library. The donut is `drawArc` calls (one per phase) plus a filled-circle marker for the current day. Start angle is `-90f` (12 o'clock). Sweep per phase = `(phaseDays / cycleLength) * 360f`. Center text ("Follicular / Day 8 / 12 days until period") goes in a `Box` wrapping the `Canvas` with `Alignment.Center`.

As built, it draws from `phaseSegments()` rather than one arc per phase — a short cycle can drop a phase entirely, and run-length encoding day 1..`cycleLength` guarantees the sweeps sum to 360° in every case. Segments are separated by a `1.6f` degree gap over a 6%-alpha track ring, and the sweep animates in once over 900ms.

---

## Calendar screen (Milestone 4)

Uses `com.kizitonwose.calendar:compose`. Custom `PhaseDayCell` composable renders a background circle in the phase color with the day number in `LunaCream`. Phase for arbitrary dates: walk back to the nearest logged `PeriodEntity.startDate`, compute offset, project forward using `cycleLength` for future dates.

---

## What is intentionally out of scope

Notifications, widgets, export/backup, multi-user, ML insights. Unit tests are only warranted for `domain/usecase/` (pure functions). Skip Composable tests.
