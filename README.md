<div align="center">

<br>

# 🌙 Luna

### *Your cycle. Your device. No one else's business.*

<br>

[![Android](https://img.shields.io/badge/Android%208.0+-black?style=flat-square&logo=android&logoColor=FAA7C7)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin%202.2-black?style=flat-square&logo=kotlin&logoColor=FAA7C7)](https://kotlinlang.org)
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
Luna calculates your phase boundaries from your own history — your actual cycle length, your actual period length — not a 28-day default that fits roughly 13% of people. The luteal phase is held near-fixed at 14 days and the follicular phase absorbs the variation, which is what makes the predictions hold up for cycles that aren't 28 days long.

**Shows you where you are, not just how long things last.**
The home screen is a ring of your whole cycle, each phase in its own colour. Every phase is drawn twice, split at today: **solid behind you, a hairline ahead.** The boundary between them is where you are standing. There is no marker dot to hunt for, because the drawing itself is the marker.

**Answers the question you actually have.**
The number in the centre of the ring changes meaning with your situation. Normally it counts down to your next period. During your period that would be a non-answer, so it switches to which day of it you're on. If you're late, it says so — in blush, reading *"3 days late"* — and that outranks everything else.

**Logs in one tap.**
Flow and energy are scales, so they're segmented bars that read light-to-heavy and tired-to-energetic in order. Pain and body symptoms are pick-any, so they stay as separate chips. Tap once to log, tap again to unlog. No Save button, no confirmation screen. The moment you tap, it's written.

**Never lets a guess look like a fact.**
The calendar colours every date by its phase, past and future. A period day **you logged** is a filled circle; one Luna merely **expects** is a hollow ring. Same colour, different weight — and the same solid-versus-hairline rule the home screen ring uses. You can always tell what the app knows from what it is guessing.
<br>

---

## ✦ Privacy — plainly stated

| What Luna collects | Where it goes |
|---|---|
| Period start and end dates | SQLite database on your device |
| Daily symptoms (flow, pain, energy, body) | SQLite database on your device |

**Luna's `AndroidManifest.xml` declares no permissions at all** — not internet, not storage, not one. The app is physically incapable of sending your data anywhere, not because of a privacy policy but because the networking code does not exist. You can verify it in one command: `grep -c uses-permission app/src/main/AndroidManifest.xml` returns `0`.

**Backup is off, in both of the places Android keeps it.** `allowBackup="false"` opts out of Google's cloud backup. That used to be the whole story, but Android 12 split device-to-device transfer into a separate mechanism that ignores it — so `res/xml/data_extraction_rules.xml` excludes every data domain from both cloud backup *and* device transfer. Without that second half, setting up a new phone from an old one would have copied your cycle history across a network Luna otherwise never touches.

**Even the fonts are local.** Luna's two typefaces are bundled into the APK rather than fetched through Android's downloadable-fonts provider, which would have put a Google Play Services round trip behind the launch of an app whose entire premise is that it never talks to anything.

No account means there is nothing to delete, no profile to request, no data to ask anyone to forget. You stop using the app; the data stays on your phone until you uninstall it.

**The other side of that promise:** there is no backup, no export, and no sync — deliberately. If you uninstall Luna or lose the phone, the data is gone. That is the cost of the guarantee, and it is a real cost. It is stated here rather than buried.
<br>

---

## ✦ Getting started

**Requires Android 8.0 (API 26) or higher.**

### Build and install from source

```bash
git clone https://github.com/ahadullabaig/luna.git
cd luna
./gradlew installDebug     # builds and installs on a connected device
```

Or build the APK and move it across yourself:

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk  (~11 MB)
```

On the phone: **Settings → Apps → Install unknown apps** → enable it for whatever file manager you're using, then tap the APK.

### Build a release APK

The release build is minified and resource-shrunk — about **1.8 MB**, a sixth of the debug build. It needs a signing key, which is yours to generate and never leaves your machine:

```bash
keytool -genkeypair -v -keystore luna-release.jks -alias luna \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Luna"
```

Then create `keystore.properties` in the repo root — it's gitignored, along with the keystore itself:

```properties
storeFile=luna-release.jks
storePassword=your_password
keyAlias=luna
keyPassword=your_password
```

```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

If the output is named `app-release-unsigned.apk`, Gradle didn't find `keystore.properties`. That fallback is intentional — a fresh clone builds without a key rather than failing on a missing secret.

> **Back the keystore up somewhere outside the repo.** Android identifies an app by its signing certificate. Sign a future version with a different key and the install is refused outright; the only way through is uninstalling, which takes the database with it. Lose the keystore and you lose the ability to ever update your own install.
<br>

---

## ✦ Logging your first period

1. On the home screen, tap **Log a period** (bottom right)
2. Pick the first day of your period from the date picker
3. Tap a second day to set the end — or save with just one if it's still going
4. Luna immediately calculates your phase and starts projecting forward

That's the entire onboarding. There is no step 5.

Until you've logged two complete cycles, Luna says so rather than pretending: the footnote under the ring reads *"Still learning. Using 28-day estimates until you log two cycles."* After that it switches to your own medians.
<br>

---

## ✦ Tech stack

*For the curious and the contributors.*

| Layer | Technology | Why |
|---|---|---|
| Language | Kotlin 2.2 | |
| UI | Jetpack Compose + Material 3 | Declarative, dark-theme native, no XML layouts |
| Architecture | MVVM + `StateFlow` | Single source of truth, lifecycle-aware |
| Database | Room 2.7 (SQLite) | Typed, reactive, device-local |
| Dependency injection | Hilt 2.54 | Compile-time verified, no reflection at runtime |
| Navigation | Navigation Compose 2.9 | Type-safe `@Serializable` route objects |
| Date/time | kotlinx-datetime | Clean `LocalDate`, no `java.util.Date` |
| Calendar view | kizitonwose/calendar | Custom day cells with phase-coloured backgrounds |
| Typography | Instrument Serif + IBM Plex Sans | Bundled as TTFs — no font-provider network call |
| Build | AGP 9.4 / Gradle 9.6 / JDK 21 toolchain | |

Single-module project. No clean-architecture ceremony. MVVM is enough for a personal app you maintain alone.

The cycle maths lives in `domain/usecase/CycleMath.kt` as plain top-level functions over plain lists — no Room dependency, no repository, no mocks needed. That split is what makes the 45 unit tests possible; they cover every phase boundary day by day, the median derivations, and the edge cases where a phase drops out of a short cycle entirely.

```bash
./gradlew test    # 45 tests
./gradlew lint
```
<br>

---

## ✦ What Luna deliberately does not do

Notifications. Widgets. Export or backup. Multi-user profiles. Machine-learning "insights". Cloud anything.

Some of these are missing because they'd break the privacy guarantee. The rest are missing because this is a personal app maintained by one person, and every feature is something that can break at 2am.
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
