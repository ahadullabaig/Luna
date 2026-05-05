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

**KSP/Kotlin version lockstep:** The KSP version prefix must always match the Kotlin version exactly. If you bump `kotlin`, update `ksp` in `gradle/libs.versions.toml` in the same commit. The current pairing is `kotlin = "2.1.20"` / `ksp = "2.1.20-1.0.31"`.

---

## Architecture

Single `app` module. MVVM. Package layout by feature, not by layer.

**Data flow:** Room DAOs → `CycleRepository` → use cases in `domain/usecase/` → `ViewModel` (exposes `StateFlow`) → stateless Composable screens that collect state and emit events. No `LiveData`, no RxJava.

**DI:** Hilt with `@HiltAndroidApp` on `LunaApp`, `@HiltViewModel` on ViewModels, `hiltViewModel()` at Composable call sites. All bindings are in `di/AppModule.kt`.

**Navigation:** Navigation Compose 2.9 with type-safe `@Serializable` route objects (`ui/nav/Routes.kt`). Routes must be `@Serializable` — the `kotlin-serialization` plugin is required for this to compile.

---

## Current milestone status

Milestones 1 (skeleton/theme) and 2 (data layer) are complete. The data layer is fully wired. `HomeScreen` currently shows a debug period counter and an "Insert Fake Period" button — this is intentional Milestone 2 scaffolding to be replaced in Milestone 3.

**Milestone 3 (Home screen)** is next: phase donut Canvas, `GetCurrentCycleState` use case, `CycleState` domain model, symptom chip UI wired to `DailyLogEntity`, and a FAB-triggered date picker that inserts `PeriodEntity`.

**Milestone 4:** Calendar screen with `kizitonwose/Calendar`, custom `PhaseDayCell`, phase projection.

**Milestone 5:** Empty states, app icon, signing config, release APK.

---

## Domain model and cycle logic

**Phase boundaries** (implemented as pure functions — no Room dependency, unit-testable):
- Menstrual: days 1 through `periodLength` (default 5)
- Follicular: `periodLength + 1` through `ovulationDay - 2`
- Ovulation: 3-day window, `ovulationDay - 1` through `ovulationDay + 1`
- Luteal: remainder through `cycleLength`

`ovulationDay = cycleLength - 14`. The luteal phase is ~14 days fixed; it's the follicular phase that varies with cycle length. This anchoring gives accurate results for non-28-day cycles.

**Averaging:** `cycleLength` = median gap over last 6 periods. `periodLength` = median over last 6 *completed* periods (exclude rows where `endDate` is null). Fall back to 28/5 if fewer than 2 completed cycles.

**Edge cases that must be handled explicitly:**
- No periods logged → empty state ("Log your first period to get started"), not a donut.
- Today is inside a logged period → phase is MENSTRUAL regardless of calculation.
- Today is past `nextPeriodStart` + 3-day grace → "Period overdue by X days" state.

`CycleState` and the use cases (`GetCurrentCycleState`, `GetPhaseForDate`) live in `domain/` and do not exist yet — they are Milestone 3 work.

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

## Phase donut (Milestone 3)

Custom Compose `Canvas` — do not reach for a chart library. The donut is `drawArc` calls (one per phase) plus a filled-circle marker for the current day. Start angle is `-90f` (12 o'clock). Sweep per phase = `(phaseDays / cycleLength) * 360f`. Center text ("Follicular / Day 8 / 12 days until period") goes in a `Box` wrapping the `Canvas` with `Alignment.Center`.

---

## Calendar screen (Milestone 4)

Uses `com.kizitonwose.calendar:compose`. Custom `PhaseDayCell` composable renders a background circle in the phase color with the day number in `LunaCream`. Phase for arbitrary dates: walk back to the nearest logged `PeriodEntity.startDate`, compute offset, project forward using `cycleLength` for future dates.

---

## What is intentionally out of scope

Notifications, widgets, export/backup, multi-user, ML insights. Unit tests are only warranted for `domain/usecase/` (pure functions). Skip Composable tests.
