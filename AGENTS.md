# AGENTS.md

## Goal

Build an Android app that changes ringer mode based on nearby WiFi SSIDs, with per-rule trigger actions for IN_RANGE and OUT_OF_RANGE.

## Design principals

### Rules

* Think before coding. State your assumptions out loud. If the request is ambiguous, ask. If a simpler approach exists, push back. Stop when you are confused, name what is unclear, do not just pick one interpretation and run.
* Simplicity first. Write the minimum code that solves the problem. No speculative abstractions. No flexibility nobody asked for. The test: would a senior engineer call this overcomplicated.
* Surgical changes. Touch only what the task requires. Do not improve neighboring code. Do not refactor what is not broken. Every changed line should trace back to the request.
* Goal-driven execution. Turn vague instructions into verifiable targets before writing a line. "Add validation" becomes "write tests for invalid inputs, then make them pass."
* Follow the SOLID principals
* Maintain a running list of what we've done or decided in the context markdown file.  Just add to it.
* Always read the context document and keep it up to date as we make changes to the application.

## Constraints
- Pixel 10 Pro, Android 16 (API 36)
- Gradle wrapper, AGP 8.2.2, Kotlin 1.9.22
- Material Components 1.12.0, SDK 36, build-tools 36.0.0
- Build/test/lint: `./gradlew assembleDebug`, `test`, `lint`
- 80% branch coverage on `org.dorkmaster` package (carried over from template, app pkg is `com.ssidringer`)

## Key decisions
- **SharedPreferences + org.json** over Room (no kapt/annotation-processor fragility)
- **Single trigger per rule** (one condition-action pair per rule)
- **OUT_OF_RANGE only when no IN_RANGE rules active** — prevents overriding active rule
- **Location on all API levels** (removed `maxSdkVersion="32"`)
- **SSID+trigger is the unique key** (same SSID, different triggers = separate rules)
- **Drag-and-drop reordering** persisted via `saveAll()`

### Testing

* Target at least 80% branch coverage on org.dorkmaster package (core classes: McpHandler, DbService, DatabaseConfig, PluginLoader). The org.dorkmaster.provider package is excluded — provider classes are thin wrappers (string constants, URL templates) that don't benefit from per-provider unit tests; their integration behavior is exercised via H2CompatProviderTest and the embedded integration tests.
* Every change must have a build and test run (mvn clean verify) before it is considered complete.
* If an android device is available via ADB please install the application after every successful build.
## Build

./gradlew assembleDebug         # Build debug APK
./gradlew test                   # Run unit tests
./gradlew lint                   # Run lint checks

## Requirements

ANDROID_HOME must point to an Android SDK with platform 36 and build-tools 36.0.0
