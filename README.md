# E-CAL

E-CAL is a local-first Android calendar app built with Kotlin and Jetpack Compose. It supports event editing, reminder notifications, and local persistence with Room, while modeling calendar data around iCalendar/RFC 5545 concepts.

## Features

- Month calendar view with busy-day markers
- Event list for the selected date
- Create, edit, and delete calendar events
- Event fields for summary, description, location, start/end time, all-day events, priority, transparency, and status
- DISPLAY reminders with absolute triggers or triggers relative to event start/end
- Local Room database storage
- Android reminder notifications through `AlarmManager` and `BroadcastReceiver`
- Reminder reconciliation after device reboot
- English and Simplified Chinese string resources

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | ViewModel, Repository, Kotlin Flow |
| Navigation | AndroidX Navigation 3 |
| Dependency injection | Hilt |
| Database | Room |
| Calendar model | ical4j, RFC 5545-inspired entities |
| Calendar UI | Kizitonwose Calendar Compose |
| Reminders | AlarmManager, PendingIntent, NotificationManager |
| Build | Gradle, Android Gradle Plugin, KSP |

Dependency versions are managed in `gradle/libs.versions.toml`.

## Requirements

- Android Studio and JDK versions compatible with the project's Gradle and Android Gradle Plugin configuration
- Android SDK matching the project's compile SDK
- Device or emulator matching the project's minimum SDK

See `app/build.gradle.kts`, `gradle/libs.versions.toml`, and the Gradle wrapper files for the authoritative SDK and build-tool requirements.

## Getting Started

Clone the repository and open it in Android Studio, then let Gradle sync the project.

Useful Gradle commands:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

On Android 12+, exact reminder timing depends on the `SCHEDULE_EXACT_ALARM` permission. On Android 13+, notification reminders also require `POST_NOTIFICATIONS`.

## Architecture

E-CAL is a single-module Android application under package `net.k74n3xz.ecal`.

```text
Compose UI
   ↓
ViewModel
   ↓
Repository
   ↓
Room DAO
   ↓
Room Database
```

Main responsibilities:

- Compose screens and components handle the month calendar and event editor UI.
- ViewModels expose UI state and coordinate event or reminder actions.
- Repositories isolate persistence details from UI-facing code.
- Room stores event and reminder entities locally.
- Conversion helpers keep app-facing models separate from persisted entities and iCalendar text.
- Reminder components bridge saved alarms to Android system notifications.

## Data Model

The project keeps app-facing calendar models separate from Room persistence entities. Repositories and conversion helpers own the mapping between those layers, so UI and ViewModel code do not depend on database structure.

The persistence layer also keeps room for iCalendar-compatible source data, which makes future import/export support easier to add without reshaping the UI-facing model every time storage details change.

## Navigation

The app uses AndroidX Navigation 3 with an explicit back stack. The month calendar is the entry point, and the event editor is pushed for both new and existing events. Saveable state and ViewModel-scoped navigation entries are enabled.

## Reminder Flow

```text
Event editor
   ↓
Alarm persistence
   ↓
AlarmManager scheduling
   ↓
Broadcast receiver
   ↓
Notification display
```

After a device reboot, a boot receiver marks persisted reminder occurrences for reconciliation and starts the scheduling flow again. The app also requests notification and exact-alarm access on Android versions that require them.

## Project Highlights

- Navigation 3 back stack is used directly instead of a route-string based setup.
- Room entities and app-facing models are separated by repository and converter layers.
- Event and alarm models are designed around RFC 5545/iCalendar fields.
- Reminder scheduling is connected end-to-end from event editing to Android notifications.
- Runtime permission handling covers notification and exact-alarm requirements where applicable.

## Contributing

Issues and pull requests are welcome. For larger changes, please describe the intended behavior and include tests where practical.