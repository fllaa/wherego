# Wherego

Android money tracker. Kotlin, Jetpack Compose, Room. Offline-first. One pot.

ApplicationId: `com.flla.wherego`

## Open in Android Studio

1. Install Android Studio (Ladybug or newer) with JDK 17+ (the bundled JBR is fine).
2. SDK: compile/target **35**, min **26**. Accept licenses if prompted.
3. **File → Open** this folder (`wherego`). Wait for Gradle sync.
4. Run the `app` configuration on an emulator (API 26+) or a phone.

From a terminal:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Install the debug APK from `app/build/outputs/apk/debug/`.

## Guest

First launch writes a local `UserProfile` row (ULID id, currency `IDR`, zone `Asia/Jakarta`). No Google account required.

## Slices

Work is sequenced in `docs/agent-loop-playbook.md`. S0–S6 are in tree. S3 talks to Firebase (`com.flla.wherego`). Capture still works as a guest.
