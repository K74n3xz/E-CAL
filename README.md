# E-CAL

E-CAL is a local-first Android calendar app built with Kotlin and Jetpack Compose. It supports event, attendee, attachment, and reminder management with local Room persistence, while modeling calendar data around iCalendar/RFC 5545 concepts.

## Features

- Month calendar view with busy-day markers
- Event list for the selected date
- Create, edit, and delete calendar events
- Event fields for summary, description, location, start/end time, all-day events, priority, transparency, and status
- Manage attendees and event attachments
- DISPLAY, AUDIO, and EMAIL reminders with absolute or event-relative triggers and optional repetition
- Local Room database storage
- Resilient reminder delivery through `AlarmManager`, WorkManager, notifications, and foreground services
- Reminder reconciliation after app startup and device reboot
- English and Simplified Chinese string resources

## Tech Stack

| Area                 | Technology                                                          |
|----------------------|---------------------------------------------------------------------|
| Language             | Kotlin                                                              |
| UI                   | Jetpack Compose, Material 3                                         |
| Architecture         | Multi-module, ports and adapters, use cases, ViewModel, Kotlin Flow |
| Navigation           | AndroidX Navigation 3                                               |
| Dependency injection | Hilt                                                                |
| Database             | Room                                                                |
| Calendar model       | ical4j, RFC 5545-inspired entities                                  |
| Calendar UI          | Kizitonwose Calendar Compose                                        |
| Reminders            | AlarmManager, WorkManager, foreground services, NotificationManager |
| Build and quality    | Gradle, Android Gradle Plugin, KSP, ktlint, Kover                   |

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
.\gradlew.bat :ci
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleProfileable
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :koverHtmlReportCoverage :koverXmlReportCoverage :koverLogCoverage
```

The `ci` task runs formatting and lint checks, JVM and Robolectric tests, the debug APK build, and aggregate coverage generation. Kover combines line coverage from the app and core modules. The HTML report is written to `build/reports/kover/html/`, and the JaCoCo-compatible XML report is written to `build/reports/kover/report.xml`. On-device `androidTest` coverage is not included.

On Android 12+, exact reminder timing depends on the `SCHEDULE_EXACT_ALARM` permission. On Android 13+, notification reminders also require `POST_NOTIFICATIONS`.

On some OEM-customized Android systems, such as Xiaomi HyperOS, E-CAL also requires its `Autostart` permission to be enabled for background reminder scheduling and recovery to work reliably. The setting name and location may vary by device.

## Architecture

E-CAL separates presentation and Android integrations from framework-independent calendar models and application logic.

```text
Compose UI and ViewModels (`app`)
              |
              v
Inbound ports: query services and use cases (`core:application`)
              |
              v
Outbound ports: repositories and platform capabilities (`core:application`)
        /                         \
       v                           v
Room adapters (`core:data`)   Android adapters (`app`)
        \                         /
         v                       v
       Calendar domain models (`core:model`)
```

Modules and responsibilities:

- `app` contains Compose screens, Navigation 3 entries, ViewModels, dependency wiring, and Android platform adapters.
- `core:model` defines framework-independent events, alarms, attendees, attachments, and related value types.
- `core:application` defines inbound query/use-case ports, outbound repository/platform ports, and their application services.
- `core:data` implements Room persistence, repository adapters, entity conversion, and attachment storage.
- `core:preference` owns the preference repository boundary and its Android implementation.

## Data Model

The project keeps domain calendar models separate from Room persistence entities. Repository adapters and conversion helpers own the mapping between those layers, so UI, ViewModels, and application use cases do not depend on the database structure.

Events support timed and all-day schedules plus RFC 5545-inspired status, transparency, priority, and recurrence data. Alarms support DISPLAY, AUDIO, and EMAIL actions, with attendees and attachments represented as reusable domain models.

## Navigation

The app uses AndroidX Navigation 3 with an explicit back stack. The month calendar is the entry point; event editing, attendee management, and attachment management are pushed as typed destinations. Saveable state and ViewModel-scoped navigation entries are enabled.

## Reminder Flow

```text
Event editor
   ↓
Save-event use case and alarm persistence
   ↓
Occurrence calculation and reconciliation
   ↓
AlarmManager / WorkManager scheduling
   ↓
DISPLAY notification / AUDIO playback / EMAIL handoff
```

Reminder occurrences track their desired and actual scheduling state so interrupted work can be reconciled safely. App startup and the boot receiver restart reconciliation, while a due-alarm worker provides a recoverable delivery path. The app requests notification and exact-alarm access on Android versions that require them; audio reminders use a media-playback foreground service.

## Project Highlights

- Navigation 3 back stack is used directly instead of a route-string based setup.
- Framework-independent models and application ports keep core behavior separate from Android and Room adapters.
- Room entities and domain models are separated by repository and converter layers.
- Event, alarm, attendee, and attachment models follow RFC 5545/iCalendar concepts.
- Reminder scheduling and recovery are connected end-to-end across alarm, worker, receiver, notification, audio, and email adapters.
- A minified profileable build includes Compose runtime tracing for performance analysis.
- Runtime permission handling covers notification and exact-alarm requirements where applicable.

## Contributing

Issues and pull requests are welcome. For larger changes, please describe the intended behavior and include tests where practical.
