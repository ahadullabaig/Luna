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

**Toolchain versions** live in `gradle/libs.versions.toml`. Current: `agp = "9.4.0"`, `kotlin = "2.2.10"`, `ksp = "2.3.6"`, Gradle `9.6.0`.

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

Milestones 1 (skeleton/theme), 2 (data layer), 3 (home screen) and 4 (calendar) are complete. The M2 debug scaffolding — the period counter and "Insert Fake Period" button — is gone, replaced by the real home screen, and the calendar is reachable from the date icon beside the home header.

A **design pass** then went over both screens: the solid/hollow provenance rule was promoted from the calendar to the whole app, the two 40%-alpha phase colours became opaque constants, cream collapsed from nineteen alphas to five named roles, and the app gained real typography. Six measured defects were fixed along the way — see the Design system, Typography, Phase donut and Calendar sections, which carry the "do not undo this" notes. **The redesign has been verified by compiler and unit tests only; nobody has run it on a device or emulator yet.** Give the two screens a look before trusting the layout.

**Milestone 5** is underway. `assembleRelease` now succeeds — it never had before, because `app/build.gradle.kts` referenced a `proguard-rules.pro` that did not exist. The launcher icon, the launch window and the signing config are in. What is left is the part only you can do: generate the keystore and write `keystore.properties` (see Release build below), then sideload.

**`allowBackup="true"` now matters.** It was harmless while the app only ran from Android Studio; a sideloaded release build means Android auto-backup can copy the Room database to the user's Google Drive, which is precisely what "local-only, no network" promises will not happen. Setting it to `false` is a one-line manifest change and is the first thing to do if that promise is meant literally.

**Known debt, deliberately unaddressed:** `fallbackToDestructiveMigration()` is still armed with `exportSchema = false`; `PeriodDao.getRecentPeriods()` and its `CycleRepository` passthrough are dead code (M3 reads full history as a `Flow` instead).

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
- No periods logged → empty state ("Nothing logged yet" / "Tap Log a period and the ring fills in."), not a donut.
- Today is inside a logged period → phase is MENSTRUAL regardless of calculation.
- Today is past `nextPeriodStart` + 3-day grace → overdue. This is stated **once**, by the ring's centre number turning blush and reading "N days late". There was briefly a separate `OverdueBanner` above the donut saying the same thing 20dp away; do not reintroduce it.

**Where this lives:** the pure functions are top-level in `domain/usecase/CycleMath.kt` — `computeCycleState(periods, today)` and `computePhaseForDate(date, periods)` are the entry points, and they take plain lists, not a repository. `GetCurrentCycleState` and `GetPhaseForDate` are thin `Flow` wrappers over `CycleRepository` that delegate to them. Keep new cycle logic in `CycleMath.kt`: the split is what makes it testable without mocking Room.

Tests are in `app/src/test/java/com/luna/app/` — 45 in total. 39 under `domain/usecase/` cover the medians, every phase boundary day-by-day, the three edge cases above, and the invariant that `phaseSegments()` always sums to `cycleLength`. The other 6 are `feature/home/components/HeroReadingTest` — `heroReading()` is pure and picks which number the ring shows, so it is testable without Compose and worth testing. That is the only exception to "tests are for `domain/usecase/` only": still no Composable tests.

---

## Data layer details

**`FlowLevel` naming:** The enum is `FlowLevel` (not `Flow`) to avoid collision with `kotlinx.coroutines.Flow`, which is imported in every ViewModel and repository file.

**Bitmasks for multi-select symptoms:** `painFlags` and `bodyFlags` in `DailyLogEntity` are `Int` bitmasks. Constants are in `PainFlag` and `BodyFlag` objects in `domain/model/Symptom.kt`. Room handles `Int` natively — no converter needed for the flags.

**TypeConverters** in `data/Converters.kt`: `LocalDate ↔ Long` (epoch days), `FlowLevel? ↔ String?`, `Energy? ↔ String?`. Room does **not** auto-convert enums — omitting these causes a runtime crash.

**Migration strategy:** `fallbackToDestructiveMigration()` is active during development. When real data matters, bump `version` in `LunaDatabase` and write a proper `Migration` object.

---

## Design system

Dark-only theme (`darkColorScheme`). Palette constants in `ui/theme/Color.kt`. Ratios are WCAG 2.1 against `LunaDeepNavy`:

| Constant | Hex | Role | On navy |
|---|---|---|---|
| `LunaDeepNavy` | `#070E36` | background, surface | — |
| `LunaNavyRaised` | `#0D1746` | sheets, chips — anything above the ground | — |
| `LunaBlush` | `#FAA7C7` | primary, menstrual phase | 10.15:1 |
| `LunaBlushMuted` | `#8E5C77` | follicular phase | 3.51:1 |
| `LunaSand` | `#F7E0A1` | secondary, ovulation phase | 14.34:1 |
| `LunaSandMuted` | `#80723F` | luteal phase | 3.91:1 |
| `LunaCream` | `#FCFAF0` | text, onBackground | 17.84:1 |

**The muted phases are opaque, and must stay that way.** They were `LunaBlush.copy(alpha = 0.4f)` and `LunaSand.copy(alpha = 0.4f)`, which is not what reached the screen: composited over the navy those land on `#684B70` and `#676261` — a muddy plum and a flat warm grey — and the first measures 2.51:1, under the 3:1 WCAG 1.4.11 asks of a graphical object. Between them they are 20 days of a 28-day cycle. Declaring them opaque is what lets them be tuned against the ground they actually sit on.

**Cream comes in three steps and two edges, and nothing else.** It used to appear at nineteen different alphas, several of them indistinguishable — .40, .45 and .50 span 3.6:1 to 5.0:1 and read as one colour. Pick a role, never a number:

| Token | Alpha | Use | On navy |
|---|---|---|---|
| `LunaTextPrimary` | 1.0 | what you read | 17.84:1 |
| `LunaTextSecondary` | .62 | support | 7.18:1 |
| `LunaTextFaint` | .40 | inert only — **never body copy** | 3.61:1 |
| `LunaOutline` | .22 | bounds something pressable | — |
| `LunaHairline` | .10 | divides content you cannot press | — |

`LunaTextFaint` at 14sp would fail AA for normal text, which is exactly why it is fenced off to inert things (adjacent-month numbers, a disabled control).

**Phase → color mapping** (used by donut and calendar day cells) lives in `ui/theme/PhaseColor.kt` as `CyclePhase.color`, with `onColor` for readable text on a filled swatch and `adjacentColor` (45% alpha) for a neighbouring month's days. Alpha is right for `adjacentColor` and wrong for the phases themselves: there the intent really is "this exact phase, but subordinate".

---

## Typography

Two bundled families in `res/font/`, wired up in `ui/theme/Type.kt`. **Instrument Serif** (one weight) for `displayLarge` / `headlineMedium` / `headlineSmall` / `titleLarge`; **IBM Plex Sans** (400/500/600) for everything you operate. Bundled as TTFs rather than pulled through the downloadable-fonts provider, which would put a Play Services round trip behind an app whose whole premise is that it never talks to anything. ~676KB total.

The scale used to be 24 / 22 / 16 / 16 / 14 / 14 / 12 / 11 in a single family — nearly flat, no display size. That flatness was why the home screen had no hierarchy to fix by rearranging: nothing was big enough to lead with. `displayLarge` (54sp serif) is the answer and appears **exactly once per screen**. `labelMedium` has lost its 1.2sp tracking along with the ALL-CAPS eyebrows it was cut for — labels are sentence case now.

---

## Phase donut (built — `feature/home/components/PhaseDonut.kt`)

Custom Compose `Canvas` — do not reach for a chart library. Start angle is `-90f` (12 o'clock). It draws from `phaseSegments()` rather than one arc per phase — a short cycle can drop a phase entirely, and run-length encoding day 1..`cycleLength` guarantees the sweeps sum to 360° in every case. Segments are separated by a `1.6f` degree gap, and the ring animates in once over 900ms.

**Every phase is drawn twice, split at the current cycle day: solid behind you, hairline ahead.** This is the app's central rule — *solid is what happened, hairline is what Luna expects* — and it is the same distinction the calendar draws between a logged period day and an expected one. One rule, both screens.

The ring used to be four equal-weight arcs with a cream dot marking today, which made it a chart of proportions: it said how long each phase is, but not where you are without hunting for the dot. Splitting at `cycleDay` turns the same drawing into a reading, and the boundary between solid and hairline *is* the marker — so the dot is gone, along with the neutral track ring behind it.

`drawRevealedArc()` takes degrees measured from `START_ANGLE` and clips them to how far the reveal animation has got, which is what keeps the two-arcs-per-phase split from needing its own animation bookkeeping.

**The centre number switches meaning with context** (`heroReading()`, pure and unit-tested). Counting down to the next period is the useful answer most of the time, but during a period it is a non-answer — what you want then is which day of it you are on. Overdue outranks both and turns the number blush. `daysUntilNextPeriod` of 0 or less reads "Due / today" or "Due / any day now" rather than rendering a bare `0` or a negative.

---

## Calendar screen (built — `feature/calendar/`)

Uses `com.kizitonwose.calendar:compose` 2.6.1, which is the **java.time** flavour — `CalendarDay.date` is a `java.time.LocalDate` and `rememberCalendarState` takes `java.time.YearMonth`, while the rest of the app speaks `kotlinx.datetime`. Convert at the boundary with `toKotlinLocalDate()` / `toJavaLocalDate()`; `minSdk = 26` means no desugaring is needed. Only core Material icons are on the classpath, so there is no `ChevronLeft` or `CalendarMonth` — use `DateRange` and the `AutoMirrored` arrows.

**`PhaseDayCell` encodes two things at once.** Colour is the phase. Solid-versus-hollow is provenance: a period day the user logged is a filled circle, one the app merely expects — next month's, or one inferred across a gap in logging — is a ring. Menstrual is the only phase a user can record, so it is the only one that carries the distinction; the other three stay flat fills. Never let a prediction render as a record.

**Today's ring sits at the cell's edge, not on the swatch — do not move it back.** Both rings were once `Modifier.border(1.5.dp, …, CircleShape)` on the same node, and chained borders draw at the same inset, so cream painted straight over blush: on a day that was both today *and* an expected period, the prediction silently vanished from the single cell most likely to be read. Two radii can carry two facts; two borders at one radius cannot. The outer ring is `fillMaxSize().padding(2.dp)` rather than a fixed dp so it can never outgrow a narrow cell.

**Adjacent-month cells keep their phase at `adjacentColor` strength** and follow the same solid/hollow rule. Blanking them meant a period spanning a month boundary looked truncated — open September and the 31st of August, a day actually on record, showed as an empty cell. Grey them for interaction, not for information. Because those cells now carry information, `outDateStyle` is `EndOfRow`: the sixth row was only ever padding, and there is no longer a reason to manufacture one.

The five-item legend is gone. The four colours are named on the home screen where the ring labels the phase you are in, so the only thing the grid still has to explain is what an outline means — `ProvenanceKey`, one line.

**`CycleProjection`** (`domain/usecase/CycleProjection.kt`) resolves the medians once and answers `infoFor(date)` per cell — a grid asks about 42 dates, and `computePhaseForDate` would redo the whole median derivation for each. It is a data class so Compose can skip recomposition when history has not changed; `computePhaseForDate` now delegates to it, so there is one implementation of the rule.

`rememberCalendarState` is keyed on its month bounds via `rememberSaveable(inputs = …)`, so changing them **recreates** the state and resets the scroll. The bounds come from `CalendarUiState.rangeStart/rangeEnd`, which move only when today's month changes — a plain midnight rollover leaves the scroll position alone.

---

## Release build

`./gradlew assembleRelease` works and produces `app/build/outputs/apk/release/app-release-unsigned.apk` (1.8MB, down from the 11MB debug APK). It is **unsigned** until you supply a keystore, and that is deliberate: `app/build.gradle.kts` reads signing details from `keystore.properties` at the repo root and only registers the `release` signing config when that file exists, so a fresh clone still builds rather than failing configuration on a missing secret.

To sign it, one time:

```bash
keytool -genkey -v -keystore luna-release.jks -keyalg RSA \
  -keysize 2048 -validity 10000 -alias luna
```

Then write `keystore.properties` (gitignored, alongside `*.jks` and `*.keystore`):

```
storeFile=luna-release.jks
storePassword=...
keyAlias=luna
keyPassword=...
```

**Back the keystore up somewhere outside the repo.** Losing it means never being able to update an installed copy of Luna — Android will refuse an APK signed by a different key, and the only way out is uninstalling, which takes the database with it.

**`proguard-rules.pro` holds exactly two rules, and both are load-bearing.** Everything else Luna depends on ships consumer rules inside its AAR, so do not add blanket `-keep class com.luna.app.**` rules — that turns `isMinifyEnabled` into a decoration.

- **`FlowLevel` and `Energy` constants are pinned by name.** `data/Converters.kt` writes them into SQLite with `.name` and reads them back with `valueOf`, which makes the constant names part of the on-disk format. R8 renaming `HEAVY` would make every previously written row throw on read — a crash that appears only in release, only on a device with existing data.
- **`@Serializable` objects keep `INSTANCE` and `serializer()`.** The navigation routes in `ui/nav/Routes.kt` are only ever reached reflectively, so R8 would otherwise strip them and the app would throw at the first `navigate()`.

Both are verified against `app/build/outputs/mapping/release/mapping.txt`, which shows `HEAVY -> HEAVY` and `HomeRoute INSTANCE -> INSTANCE`. If you change either mechanism, re-check the mapping rather than assuming.

## Launcher icon and launch window

`res/mipmap-anydpi-v26/ic_launcher.xml` is an adaptive icon with a monochrome layer for Android 13 themed icons. `minSdk = 26` means every device that can install Luna supports adaptive icons, so there is no ladder of legacy density PNGs and there does not need to be one.

The crescent in `res/drawable/ic_launcher_foreground.xml` is the region between two circular arcs, not one circle punched out of another. Subtracting a circle with `evenOdd` cannot work: the cutting circle has to reach outside the moon's edge to make a crescent at all, and every part of that overhang comes back filled.

`res/values/themes.xml` exists for one line — `android:windowBackground`. The system paints that between the launcher tap and Compose's first frame, and inheriting `DeviceDefault` left it at the device default, so a dark-only app opened with a white flash on a phone in light mode. For the same reason `MainActivity` passes `SystemBarStyle.dark(...)` to `enableEdgeToEdge()` explicitly: the no-argument form picks system-bar icon colours from the device's light/dark setting, which put dark icons on Luna's navy and swallowed the clock.

`res/values/colors.xml` duplicates two values from `ui/theme/Color.kt` because the system needs them as Android resources before any Compose code runs. Keep them in step by hand.

---

## What is intentionally out of scope

Notifications, widgets, export/backup, multi-user, ML insights. Unit tests are only warranted for `domain/usecase/` (pure functions). Skip Composable tests.
