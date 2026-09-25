# Orbit — a study companion, not a countdown app

Built for **Bal Bharati Hackathon 3.0** (Team Optimus Prime) from the
"Smart Study Tracker" idea: a study timer that pauses itself when you
leave the desk, nudges you back with your voice when you return, and
keeps a quiet record of goals, homework, and questions solved.

The original idea assumed a motion sensor. This build uses the phone's
**front camera + on-device face detection** instead — no extra hardware,
and it's a more direct answer to "is the student actually there" than
motion ever was.

This is a native Android app in Kotlin + Jetpack Compose, not a mockup.
Every screen is wired to a real Room database, a real CameraX + ML Kit
pipeline, and the real Android speech APIs.

## What's actually working

- **Study timer** that only counts while you're both running it *and*
  in front of the camera. Pausing is a first-class state, not just
  "stopped" — the ring visually freezes exactly where you left it.
- **Camera presence detection** (CameraX + ML Kit face detection, fully
  on-device, no frame ever leaves the phone). Step away for ~8 seconds
  and the timer pauses itself. Come back and — matching the original
  idea exactly — it doesn't silently resume; it speaks a short reminder
  and waits for you to tap or say "resume."
- **Voice commands** — tap the mic, say "start," "pause," "status," or
  "log a question," and Wick answers out loud. This is push-to-talk,
  not an always-listening "Hey Wick" — see [Honest limitations](#honest-limitations-and-the-next-build)
  for what a wake-word upgrade would need.
- **Goals & homework** — one list, filterable, with a hand-drawn
  checkmark instead of a system checkbox.
- **Progress** — a real weekly bar chart of study minutes (drawn on a
  `Canvas`, not a charting library), total time/sessions/streak, and
  six achievements that unlock from your actual session data.
- **A study streak** computed from real calendar days with a session.
- Every screen persists through **Room**, survives app restarts, and
  the repository layer (`data/repository/StudyRepository.kt`) is the
  one seam you'd touch to add the cloud sync the original pitch deck
  described — see below.

## Why it doesn't look like a template

The whole visual identity is built around one image: a desk lamp left
on after dark. That's where the app's name comes from, and it's why:

- The palette is a warm near-black (`#171512`) rather than true black
  or a cool slate, with a brass glow (`#D6A24A`) as the one accent —
  no default Material purple/blue anywhere. Full palette and the
  reasoning behind each color is in `ui/theme/Color.kt`.
- Two typefaces carry real personality: **Fraunces** (a warm editorial
  serif) for anything that should feel considered — screen titles, the
  timer itself — and **Manrope** for anything read quickly and often.
  Both are bundled as static weights cut from Google's variable font
  sources (see `app/src/main/res/font/`), so there's no runtime
  font-loading involved.
- Cards are flat with a single hairline border — no drop shadows, no
  "SaaS card kit" look.
- The bottom nav is plain text tabs with a sliding underline, not an
  icon rail.
- The timer isn't a countdown gauge; it's a clock face that laps every
  hour, with tick marks at each 5 minutes — because a study session
  doesn't have a "target," but a clock face still means something at
  a glance.
- The flame mark (app icon, and the living status light on the study
  screen) is one hand-drawn shape reused everywhere, not an icon-font
  glyph.

If you want to change any of this, `ui/theme/` is the entire design
system in four small files.

## Project structure

```
app/src/main/java/com/optimusprime/wick/
├── MainActivity.kt            entry point — just sets the theme + nav graph
├── WickApplication.kt         holds the one AppContainer (manual DI, no Hilt)
├── AppContainer.kt
├── data/
│   ├── db/                    Room entities, DAOs, the database itself
│   └── repository/            StudyRepository — the one seam to swap in cloud sync
├── engine/
│   ├── StudyClock.kt          plain Kotlin timer state machine (unit-testable, no Android)
│   ├── PresenceDetector.kt    CameraX + ML Kit face detection
│   └── VoiceCoordinator.kt    SpeechRecognizer + TextToSpeech wrapper
└── ui/
    ├── theme/                 the whole design system: Color, Type, Shape, Theme
    ├── components/            TimerDial, FlameIndicator, WeekBars, GoalRow, WickButton, WickCard, WickBottomBar
    ├── nav/                   WickNavGraph.kt — five destinations, one bottom bar
    └── screens/                permissions / home / session / goals / progress
```

Dependency injection is one small `AppContainer` class instead of
Hilt/Dagger — deliberately, so any of the three of you can point at
the whole object graph in one file when a judge asks how it's wired.

## Setup

1. **Android Studio** — Ladybug (2024.2) or newer, so it matches
   Kotlin 1.9.24 / AGP 8.5.0 / a JDK 17 toolchain out of the box.
2. Open this project's root folder directly (the one with
   `settings.gradle.kts`) — **not** the `app/` subfolder.
3. This repo does not ship the Gradle wrapper jar (it's a binary file
   and this build environment couldn't fetch it). Android Studio will
   offer to regenerate it the moment you open the project ("Gradle
   wrapper is missing — would you like the project to use the Gradle
   wrapper?" → yes), or you can run `gradle wrapper` once from a
   terminal if you have Gradle installed locally. Either way, first
   sync will need an internet connection to pull dependencies.
4. Run on a **real device** if you can — the camera + mic features
   are the whole point, and an emulator's virtual camera won't show a
   real face to ML Kit. Minimum Android 8.0 (API 26).
5. On first launch, Wick asks for camera + microphone permission, then
   runs a 5-second "can the camera see you" check before handing you
   into the app. That check is the *only* time you'll see a live
   camera preview — during a real session there's no viewfinder, only
   the small flame indicator.

### If Gradle sync complains about a version

`gradle/libs.versions.toml` pins every version in one place. The
riskiest pairing is Kotlin 1.9.24 ↔ Compose Compiler 1.5.14 — that's
a matched pair from JetBrains' own compatibility table, so if Android
Studio suggests a Kotlin upgrade, update `composeCompiler` in the
version catalog to match rather than letting them drift apart.

## How the pieces fit together (for judges, or for 3am future-you)

- `engine/StudyClock.kt` is deliberately plain Kotlin with zero Android
  imports — it only knows "running," "paused," and timestamps. That
  means it's trivially unit-testable and it's the one file you could
  hand to someone with zero Android experience and have them
  understand the whole pause/resume logic in a minute.
- `engine/PresenceDetector.kt` binds a CameraX `ImageAnalysis` use case
  and feeds every frame to ML Kit's face detector. It never emits an
  event — it just keeps `lastFaceSeenAtMs` up to date. `SessionViewModel`
  polls that value once a second (the same tick that drives the visible
  clock) and decides whether 8 seconds have passed. One timer loop
  drives both the display and the auto-pause logic.
- The "welcome back" voice prompt fires exactly once per absence —
  not once per second while you're back in frame — via a small
  `hasPromptedReturn` flag in `SessionViewModel`, reset the next time
  you actually leave again.
- Voice commands are plain keyword matching (`engine/VoiceCoordinator.kt`,
  `parseVoiceCommand`) rather than any NLU model — "pause" anywhere in
  what you said means pause. It's not clever, but it's 100% explainable
  in a demo and needs no network call.

## Honest limitations, and the next build

Things that are stubbed, simplified, or explicitly out of scope for
this pass — worth saying out loud rather than overclaiming:

- **No cloud sync yet.** The pitch deck's stack lists Firebase +
  Firestore; this build uses local Room storage so it works the
  instant you clone it, with no Firebase project to configure. Every
  data access already goes through `StudyRepository` — adding
  Firestore means writing a second implementation of that same
  interface and wiring it up in `AppContainer`, not touching any
  screen.
- **No always-on "Hey Wick" wake word.** Voice is push-to-talk. A real
  wake-word listener needs either a foreground service constantly
  running the mic (battery/privacy tradeoff) or an offline wake-word
  engine (Picovoice Porcupine, or Vosk) — both are a deliberate next
  step, not a quick add.
- **"Personalized study suggestions"** are a small rotating set of
  genuinely useful, curated study tips (see `HomeViewModel.kt`) plus
  the achievement/streak math — not a machine-learning recommendation
  engine. Framing it as more "AI-personalized" than that would be
  overselling it to judges who ask a follow-up question.
- **No scheduled reminder notifications** (e.g., "you haven't studied
  today") — the data needed to build this exists (`currentStreak()`,
  `sessionsForToday()`), it just needs a `WorkManager` job, which is a
  clean half-day add.
- **Homework and goals share one table** with a type flag, rather than
  being fully separate features — intentional, to keep the schema and
  the UI simple for the demo; splitting them later is a small change.

## A suggested demo flow

Matches the "Study Flow" from the pitch deck almost exactly:

1. Fresh install → permission screen → the camera calibration check
   (shows the judges the camera is really running ML Kit, live).
2. Home screen → "Start studying."
3. Let the timer run a few seconds, then physically get up and walk
   out of frame. Call out loud what should happen. ~8 seconds later,
   the timer visibly pauses on its own.
4. Walk back into frame — Wick speaks the "welcome back" line.
   Tap Resume.
5. Tap the mic, say "log a question" — Wick confirms out loud, counter
   ticks up.
6. Tap the mic again, say "status" — Wick reads back the elapsed time.
7. End the session, jump to **Goals**, check something off.
8. **Progress** tab — the week's bar chart now has today's bar in it,
   plus whichever achievement just unlocked.
