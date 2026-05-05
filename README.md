<div align="center">

<br>

# 🌙 Luna

### *Your cycle. Your device. No one else's business.*

<br>

[![Android](https://img.shields.io/badge/Android%208.0+-black?style=flat-square&logo=android&logoColor=FAA7C7)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin%202.1-black?style=flat-square&logo=kotlin&logoColor=FAA7C7)](https://kotlinlang.org)
[![No Internet](https://img.shields.io/badge/Internet%20Permission-None-black?style=flat-square&logoColor=FAA7C7)](/)
[![No Accounts](https://img.shields.io/badge/Accounts%20Required-None-black?style=flat-square)](/)
[![Local Only](https://img.shields.io/badge/Data%20Storage-On%20Device%20Only-black?style=flat-square)](/)

<br>

</div>

---

Your menstrual cycle is one of the most intimate windows into your health. It shouldn't be a data point in someone else's spreadsheet.

**Luna is a period tracker with one non-negotiable rule: your data lives on your phone, and nowhere else.** No cloud. No account. No analytics. No server that could be breached, subpoenaed, or sold. The app works completely offline, by design — not as a setting you have to find, but as the only mode that exists.

Open it. Track your cycle. Close it. Nothing left behind but the information you chose to keep.

<br>

---

## ✦ Why this exists

Most period-tracking apps are data businesses dressed up in pastel. They offer free tracking in exchange for your most sensitive health information — details about your flow, your pain, your energy, your body — which they sell to advertisers, share with third parties, or store on servers you have no visibility into. Several major apps have been caught doing exactly this.

Luna was built because the alternative is unacceptable. Cycle data can reveal whether you might be pregnant, what health conditions you may have, and life decisions you haven't made public. That information belongs to you.

The only data that cannot be leaked is data that was never collected in the first place.

<br>

---

## ✦ What Luna does

**Knows your cycle, not a textbook's.**
Luna calculates your phase boundaries from your own history — your actual cycle length, your actual period length — not a 28-day default that fits roughly 13% of people. The longer you use it, the more accurate it gets.

**Tells you exactly where you are.**
The home screen shows a phase donut: your entire cycle visualized as a ring, each arc colored by phase, a marker on today. Menstrual, follicular, ovulation, luteal — named, colored, and dated. Center text shows your current phase, cycle day, and days until your next period.

**Logs in one tap.**
Flow level, pain type, energy, physical symptoms — all laid out as selectable chips. Tap once to log, tap again to unlog. No Save button, no confirmation screen. The moment you tap, it's saved.

**Projects every day, past and future.**
The calendar view colors every date by its phase — historical periods shown as logged, future dates projected forward using your personal cycle average. Plan your life with your cycle in the room, not as a surprise guest.

**Predicts, but stays honest.**
If your period hasn't arrived and it's been more than three days since the predicted date, Luna says so plainly: *Period overdue by X days.* No silent re-predictions. No pretending everything is fine.

<br>

---

## ✦ Privacy — plainly stated

| What Luna collects | Where it goes |
|---|---|
| Period start and end dates | SQLite database on your device |
| Daily symptoms (flow, pain, energy, body) | SQLite database on your device |

Luna's `AndroidManifest.xml` declares zero internet permissions. The app is physically incapable of sending data anywhere — not because of a privacy policy, but because the networking code does not exist. You can verify this in the source.

No account means there is nothing to delete, no profile to request, no data to ask them to forget. You stop using the app; the data stays on your phone until you uninstall it.

<br>

---

## ✦ Getting started

**Requires Android 8.0 (API 26) or higher.**

### Install the APK

1. Download the latest release from the [Releases](../../releases) page
2. On your phone: **Settings → Apps → Install unknown apps** → enable for your file manager
3. Tap the downloaded APK and install
4. Open Luna — no sign-up prompt, no onboarding survey, no permissions requested beyond storage

You're tracking in under a minute.

### Build from source

```bash
git clone https://github.com/ahadullabaig/luna.git
cd luna
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Then install on a connected device:
```bash
./gradlew installDebug
```

<br>

---

## ✦ Logging your first period

1. On the home screen, tap the **+** button (bottom right)
2. Pick your period's start date from the date picker
3. If the period has ended, pick an end date — or leave it open if it's ongoing
4. Luna immediately calculates your phase and starts predicting forward

That's the entire onboarding. There is no step 5.

<br>

---

## ✦ Tech stack

*For the curious and the contributors.*

| Layer | Technology | Why |
|---|---|---|
| Language | Kotlin 2.1 | |
| UI | Jetpack Compose + Material 3 | Declarative, dark-theme native, no XML layouts |
| Architecture | MVVM + `StateFlow` | Single source of truth, lifecycle-aware |
| Database | Room 2.7 (SQLite) | Typed, reactive, device-local |
| Dependency injection | Hilt 2.54 | Compile-time verified, no reflection at runtime |
| Navigation | Navigation Compose 2.9 | Type-safe `@Serializable` route objects |
| Date/time | kotlinx-datetime | Clean `LocalDate`, no `java.util.Date` |
| Calendar view | kizitonwose/calendar | Custom day cells with phase-colored backgrounds |

Single-module project. No clean-architecture ceremony. MVVM is enough for a personal app you maintain alone.

<br>

---

<div align="center">

*Built with Kotlin. Runs completely offline.*
*No accounts. No tracking. No exceptions.*

<br>

**Luna** — a period tracker that works for you,
and reports to no one else.

<br>

</div>
