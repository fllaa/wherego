# AGENT_LOG

## 2026-08-30  T0026  slice=S0
- Goal: empty app that compiles, 4 tabs, teal/sand theme, dark mode follows system, guest UserProfile on first launch
- Files changed: Gradle multi-module spine (`app`, `core/*`, `feature/*`), Hilt `WheregoApp`, `MainActivity` + splash, custom tab bar, Home chrome with 40dp Go circle, Room v1 `user_profile` only
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS (`app/build/outputs/apk/debug/app-debug.apk`)
  - `./gradlew :app:testDebugUnitTest` → 2 passed (`guestUsesJakartaIdrAndLocalId`, `ulidIsCrockford26`)
- Decisions:
  - ULID: `io.azam.ulidj:ulidj:2.0.0` (`ULID.random()`)
  - Palette from `docs/design-system.md` (paper `#FFF3E2`, ink `#0F2E2C`, teal `#10B5A0`) rather than playbook seed `#0F766E` / `#FFF7ED` / `#1C1917` — visual SoT is the design-system file; Go stays **40dp** per playbook S0
  - Fonts: platform `SansSerif`. Fredoka + Nunito Sans deferred (smaller option; no extra font binaries in S0)
  - Room v1 = `UserProfileEntity` only. Categories + transactions wait for S1 (playbook calls that “Room DB v1”; this slice only needed a profile row)
  - AGP **9.1.1** + Gradle **9.3.1** (cached). AGP 8.13.2 wanted Gradle 8.13.2 whose GitHub distribution 404’d. `android.builtInKotlin=false` so KSP 2.2.21 still works. Hilt **2.60.1** for AGP 9
  - Compose BOM **2025.06.01** + activity **1.10.1** so `compileSdk`/`targetSdk` stay **35** (2026.08 BOM requires API 37)
  - Dark: mapped cream→`#14201F` / ink→`#FFF3E2`, dynamic color off
  - No `INTERNET` permission. Capture/Firebase/mascot art not started
- Not done / deferred: Fredoka/Nunito, capture FAB, category seed, MoneyFormatter
- Blocked: none

## 2026-08-30  T0038  slice=S1
- Goal: add expense/income to Room and see it on Home (20-second loop)
- Files changed: `core/model` Money/CurrencyScale/MoneyFormatter/presets; Room v2 categories+transactions+migration 1→2; `LedgerStore`; capture sheet; Home FAB/list/swipe/undo/duplicate
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:database:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS (database 2 passed: insert+month aggregate+soft delete, duplicate; app 6 passed: IDR 0/18000/1250000 + scale + guest)
  - extra `./gradlew :core:model:testDebugUnitTest` → SUCCESS
- Decisions:
  - Room **v2** with `MIGRATION_1_2` (S0 was profile-only v1)
  - Transport emoji `🚕`, Fun `🎬` from design-system (playbook had 🛵/🎮)
  - Date chips Today / Yesterday / Pick (playbook) plus mock quick amounts 10rb/15rb/25rb and `000` key
  - Save copy `Park it`; week window Monday–today in `Asia/Jakarta`
  - Room tests via Robolectric SDK 34 + in-memory DB
  - No `INTERNET`. Guest still local-only
- Not done / deferred: Fredoka/Nunito, budget card chrome (S4), mascot art (S2)
- Blocked: none. Human gate **H5** (real-phone 20s feel) is not agent-certified

