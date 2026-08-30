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

## 2026-08-30  T0045  slice=S2
- Goal: month story, onboarding, settings, streak, Go states
- Files changed: `MonthStory`/`LogStreak`; Stories month pager; onboarding N1/N2; Me settings (theme DataStore, display name, set-balance adjustment); category archive/rename; Home streak pill + Go idle/happy/sleepy
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:model:testDebugUnitTest` → SUCCESS (MonthStory 4, LogStreak 2, plus existing formatter/digit/profile)
  - `./gradlew :core:database:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS
- Decisions:
  - Sentence template uses top 2: `"{cat1} {p1}% · {cat2} {p2}% · rest is quieter."`; empty: `"New page. Nothing parked this month."`
  - Streak = distinct `occurredOn` among non-deleted txs (all-time), not consecutive
  - Categories: archive only, never hard-delete
  - Set balance writes signed `adjustment` on `cat_other` (does not rewrite starting balance)
  - Theme override in DataStore (`system`/`light`/`dark`); Go states via emoji in the 40dp circle (no custom art)
  - Onboarding shows for any profile with `onboardingDone=false` (includes S1 guests)
- Not done / deferred: Fredoka/Nunito, budgets (S4), Firebase (S3)
- Blocked: none. H5 still human.


## 2026-08-30  T0052  slice=S3
- Goal: cloud sync + Google — H1 H2 H3 missing, so interfaces + fake + BLOCKED.md
- Files changed: `AuthRepository`/`CloudDataSource`; `SyncMerge` LWW; `FakeAuthRepository`/`FakeCloudDataSource` (filesDir); `SyncEngine`+WorkManager 15min; Room v3 `sync_state`; Home cloud dot; Me “Sign in to backup”; AUTH stub
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:sync:testDebugUnitTest` → 4 passed (remote newer wins, local dirty push, clear dirty if updatedAt unchanged)
  - `./gradlew :core:database:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS
- Decisions:
  - No Firebase SDK, no `google-services.json`, no Crashlytics, no INTERNET permission
  - Guest cloud dot = Offline (no firebaseUid). Capture never blocked
  - Fake cloud writes `filesDir/wherego-cloud/{uid}/…` for when uid exists
  - WorkManager via EntryPoint (no HiltWorkerFactory)
  - Categories have no `dirty`; full push then LWW pull
- Not done / deferred: real Credential Manager, Firestore, Crashlytics — see BLOCKED.md
- Blocked: H1 H2 H3 — `BLOCKED.md`


## 2026-08-30  T0105  slice=S4
- Goal: budgets, recurring, CSV export; Plan tab live
- Files changed: Room v4 `budgets`/`recurring_rules`; `PlanStore`; Plan tab; Home budget card (max 3) + due inbox; `DueReminder` 08:00; Me Export CSV share sheet
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:model:testDebugUnitTest` → PlanTest 5 passed (due, advance weekly/monthly incl. Jan 31, CSV escape, compact rb, over-budget)
  - `./gradlew :core:database:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS
- Decisions:
  - Budgets keyed to current `yearMonth` only (no rollover)
  - Confirm due creates a tx with `recurringId`, then advances `nextOn`; `autoPost=false`
  - Reminder copy: `"{note} usually hits today. Log it?"` at 08:00 local; POST_NOTIFICATIONS declared, no runtime prompt
  - CSV columns `date,kind,amount,currency,category,note` (amount = minor units string); share as `text/csv`
  - Compact IDR: `rb`/`jt` for budget remaining labels
- Not done / deferred: OCR/S5, goals/FX/S6, runtime notification permission UI
- Blocked: none (H1–H3 still open for real Firebase; S4 does not need them)


## 2026-08-30  T0140  slice=S5
- Goal: receipt photo + OCR confirm; never auto-save OCR
- Files changed: Room v5 `receipts`; JPEG compress max edge 1080 q70; ML Kit latin OCR; camera/gallery after Park it and edit `photo` chip; `ReceiptUploader` fake + WorkManager fail-open
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:model:testDebugUnitTest` → OcrAmountTest 6 passed (largest IDR, skip year, million grouped, empty, USD scale, 1080 scale)
  - `./gradlew :core:database:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS
- Decisions:
  - Largest plausible amount, not “Total” heuristic — cash/change can beat the line total
  - Years 1900–2099 dropped when another number exists
  - OCR amount applies only on “Use it”; “Keep mine” leaves the parked amount
  - Upload path `users/{uid}/receipts/{id}.jpg` when signed in; FakeAuth is guest so worker no-ops; photo stays in `filesDir/receipts/`
  - No Coil thumbnail; no EXIF rotate; no Firebase Storage SDK
- Not done / deferred: S6 goals/FX/Vico/CSV import/PDF; real Storage after H1
- Blocked: H1–H3 still open (`BLOCKED.md`); S5 usable offline


## 2026-08-30  T0220  slice=S6
- Goal: goals, FX, Vico balance, CSV import, month PDF
- Files changed: Room v6 `goals`/`fx_rates`; Plan earmarks; capture currency+rate; Stories Vico line + Share PDF; Me Import CSV wizard; weekly Frankfurter cache worker (fail-open)
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:model:testDebugUnitTest` → S6Test 5 passed (USD→IDR BigDecimal, same-currency, running balance, quoted CSV preview, PDF lines)
  - `./gradlew :core:database:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS
- Decisions:
  - Goals are earmarks (`allocatedMinor`) on Plan, not accounts
  - `fxRateToBase` is a decimal string; convert with BigDecimal, never store money as Double
  - Rate defaults to cached table or `"1"`; weekly worker only if a non-IDR tx exists; no API key — Frankfurter, fail-open to manual
  - Capture cycles IDR/USD/SGD/EUR; rate field only when ≠ base
  - CSV import: map columns, preview 5, amount = minor units string; unmatched category → `cat_other` / `cat_other_in`
  - Month PDF is monospace text (title, bars, tx list), not marketing
  - Vico 1.13.1 line chart; float only for drawing
- Not done / deferred: real Firebase (H1–H3); Play (H4); 20s feel (H5)
- Blocked: none for S6; H1–H3 still in `BLOCKED.md`



## 2026-08-30  T0305  slice=S3
- Goal: H1–H3 closed — real Firebase Auth/Firestore/Crashlytics/Storage; package `com.flla.wherego`
- Files changed: package root `app.wherego` → `com.flla.wherego` (applicationId, namespaces, source trees); freeze list + H1 + README; `FirebaseAuthRepository` (Credential Manager + Google ID token); `FirestoreCloudDataSource`; `FirebaseReceiptUploader`; Crashlytics plugin; Room v7 `user_profile.firebaseUid`; Me signed-in/out; deleted `BLOCKED.md`
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS (`app/processDebugGoogleServices` ran)
  - `./gradlew :core:sync:testDebugUnitTest :core:database:testDebugUnitTest :core:model:testDebugUnitTest :app:testDebugUnitTest` → 42 passed, 0 failed
- Decisions:
  - applicationId/package locked to `com.flla.wherego` to match `google-services.json`
  - Keep local ULID profile PK; store `firebaseUid` on the row; cloud docs keyed by Firebase uid
  - Firestore paths: `users/{uid}/transactions/{id}`, `users/{uid}/categories/{id}`, `users/{uid}/profile/profile` (matches pasted rules `{document=**}`)
  - No snapshot listeners; still pull on start + after save + 15 min WorkManager
  - Feature modules still only see `AuthRepository` / `CloudDataSource`
  - `google-services.json` `oauth_client` is empty, so `default_web_client_id` was not generated. Sign-in code is live; Google picker needs a re-download of the json after the Web OAuth client exists (Authentication → Google)
- Not done / deferred: Play closed test (H4); 20s feel (H5)
- Blocked: none

## 2026-08-30  T0318  slice=S3
- Goal: confirm Google Sign-In client after human re-download of `google-services.json`
- Files changed: none in git (`google-services.json` stays gitignored)
- Commands:
  - `./gradlew :app:processDebugGoogleServices` → SUCCESS
  - generated `default_web_client_id` (type 3) present
- Decisions: no code change; `FirebaseAuthRepository.webClientId()` reads that string
- Not done / deferred: Play (H4); 20s feel (H5)
- Blocked: none


## 2026-08-30  T0324  slice=S3
- Goal: fix Google Sign-In `No credentials available` on first tap
- Files changed: `FirebaseAuthRepository` — one-tap then `GetSignInWithGoogleOption` account picker
- Commands:
  - `./gradlew :core:sync:compileDebugKotlin :app:assembleDebug` → SUCCESS
- Decisions: button path needs Sign in with Google picker; one-tap only sees previously authorized accounts
- Not done / deferred: Play (H4); 20s feel (H5)
- Blocked: none


## 2026-08-30  T0348  visual
- Goal: match Pencil Home/Capture/Stories/Plan/Me (`pencil-new.pen`)
- Files changed: Fredoka+Nunito Sans in `:core:designsystem`; Go 54dp; coral center FAB; outlined tx/budget cards; hero leftover pill; capture Rp prefix; Stories/Plan/Me card chrome
- Commands:
  - `./gradlew :app:assembleDebug :core:database:testDebugUnitTest :core:model:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS
- Decisions: follow design-system + Pencil over playbook 40dp Go; FAB lives on tab bar on every tab
- Not done / deferred: pixel-perfect Plan leftover hero / Me stats grid (no extra product fields)
- Blocked: none


## 2026-08-30  T0430  visual
- Goal: close the gap between `pencil-new.pen` and the app — the design gained 6 frames the
  T0348 pass never covered (`Sign In`, `Onboarding 1–4`, `Home · Dark`)
- Files changed:
  - `:core:designsystem` — `muted` `#78918E`→`#52706D`, `tealDeep` `#0A7F70`→`#076358`,
    dark palette remapped (`#141C1B` paper, `#1E2A28` surface, `#2E3B39` track,
    `#F2EFE7` ink, `#9DB3AF` muted, `#8FE8D8` tealDeep, `#14C7AE` teal, `#FF8360` coral);
    new `WheregoPrimaryButton`, `WheregoOnboardTopBar`, `WheregoCard`; 8 new type styles
  - `:core:datastore` — `ThemePreferences.welcomeSeen` (device-local, not synced)
  - `:core:model` — `MoneyFormatter.number()`; `CategoryPack` + `PresetCategories.packs`;
    `cat_rent`→"Rent & bills", `cat_other` emoji `📦`→`✨`, expense `sortOrder` reordered
    to the design's chip order (`cat_bills` last)
  - `:feature:auth` — new `WelcomeScreen` (first-run Sign In); `findActivity` now `internal`
  - `:feature:settings` — `OnboardingScreen` rewritten 2 steps → 4; `completeOnboarding`
    now takes `keptCategoryIds`
  - `:app` — `MainViewModel.welcomeSeen`; `MainActivity` gate splash → welcome → onboarding
    → nav host; `WheregoNavHost(openCaptureOnStart)`
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:model:testDebugUnitTest :core:database:testDebugUnitTest
    :core:sync:testDebugUnitTest :app:testDebugUnitTest` → SUCCESS (42 passed)
  - on `emulator-5554`: fresh install + `pm clear`, walked Sign In → 4 steps → Home;
    screenshots in `/tmp/wg/01..10`. Confirmed grouped `Rp 4.250.000` + coral caret,
    chip flow wrap 2/2/3/3, archive of unticked buckets (capture sheet shows 5 chips),
    "Log it now" lands on Home with the sheet open, and dark mode via `cmd uimode night yes`
- Decisions:
  - `.pen` beats `docs/design-system.md` where they disagree; the two drifted token rows in
    the doc were updated rather than left as a second source of truth
  - Sign In is a gate, never a wall: "Try it first, sign in later" writes `welcomeSeen` and
    drops into onboarding. Flag lives in DataStore, not the profile — skipping is a device
    decision and must not cost a Room migration or sync round-trip
  - `welcomeSeen` is `StateFlow<Boolean?>`; splash holds until the first read so the gate
    never flashes for a returning user
  - Bucket picker archives, never deletes: packs are a preselection over the seeded presets,
    so Me → Categories can restore anything unticked
  - Balance field is a `BasicTextField` + `VisualTransformation`, so the coral caret in the
    design is the real cursor. Both offset directions clamp to the end — the field is typed
    and backspaced as a whole amount
  - Category chips use `FlowRow`, not `chunked(2)`: the design's rows are width-packed
  - Taxonomy left intact (10 expense presets, ids unchanged). The design shows 9 chips with a
    merged "Rent & bills"; `cat_bills` survives as a 10th, unticked chip rather than forcing
    a Room remap of existing rows
- Not done / deferred: Play closed test (H4); 20s feel (H5) — still human gates
- Blocked: none


## 2026-08-30  T0520  visual
- Goal: I claimed Home/Stories/Plan/Me matched at T0348 and again at T0430 on the strength of that
  log entry. Challenged on it, screenshotted all four against `pencil-new.pen`: **Home matched,
  Stories/Plan/Me did not.** This entry closes that.
- What was actually wrong (device vs design):
  - Stories: no summary card, no month-over-month compare pill, no story card, breakdown rows had
    the wrong shape and no logs count, no day-grouped transaction list, and it showed a `Share PDF`
    link plus a balance chart the design does not have. Month read `Agustus 2026`, design `August`
  - Plan: no month pill, no deep-teal cap hero, budgets/goals rendered as bare text rows with no
    badge, meter or over/left note, no add affordance
  - Me: no stats card, no grouped setting rows, no section labels, no sign-out button; the display
    name field, theme chips and a whole numpad sat loose on the page
- Files changed:
  - `:core:designsystem` — new `WheregoShell.kt` (`WheregoPageHeader`, `WheregoMonthStepper`,
    `WheregoMonthPill`, `WheregoSectionHeader`, `WheregoSectionLabel`, `WheregoMeter`,
    `WheregoBadge`, `WheregoStatsCard`, `WheregoSettingsCard`/`Divider`/`Row`, `WheregoMeterCard`,
    `WheregoCapCard`); tokens `green pink onGreenSoft divider capFill capTrack capLabel`;
    8 more type styles; `WheregoTxRow` re-specced to r22 / pad12 / badge42
  - `:core:common` — `MonthPdfWriter` moved here from `:feature:stories` (two callers now)
  - `:core:model` — `Goal.targetMinor` + `fraction`/`percent`
  - `:core:database` — `GoalEntity.targetMinor`, Room **v8** + `MIGRATION_7_8`,
    `PlanStore.addGoal(..., targetMinor)`
  - `:feature:stories` — StoriesScreen/ViewModel rewritten
  - `:feature:plan` — PlanScreen/ViewModel rewritten
  - `:feature:settings` — MeScreen/SettingsViewModel rewritten (Onboarding untouched)
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:model: :core:database: :core:sync: :app:testDebugUnitTest` → 42 passed
  - on `emulator-5554`: seeded 3 real transactions through the capture sheet, then screenshotted
    all four tabs light + Home dark. Verified the Stories breakdown/day list, the Plan cap hero and
    empty states, the Me stats/rows, that `Appearance` and `Adjust balance` sheets still carry the
    controls they replaced, and that nothing ends underneath the FAB
- Decisions:
  - Three screens = three independent files, so this ran as 3 parallel subagents. Shared chrome was
    built serially FIRST and the agents were barred from `core/*`, `app/` and from running Gradle —
    concurrent builds would have fought over the lock and each other's edits
  - Goal needed a real `targetMinor`: the design shows 40% and 42% side by side, which cannot be
    shares of one pot. Without the column the percent pill is decoration, so it got a migration
  - Design omits Stories' balance chart and `Share PDF`. Deleting shipped S6 features on the
    strength of a mock is not my call: the chart is demoted to a `Balance` card at the very bottom,
    and the report moved to `Me → DATA → Month report PDF` where the design puts it
  - Design omits Plan's `Recurring`; it stays below `Set aside` because Me only links to it
  - The `All` filter pill and the `Edit` link are wired, not decoration — a dead control is worse
    than an absent one
  - Vico's default `lineChart()` drew a grey-on-grey box. Now an explicit teal `lineSpec` with no
    background shader. Pre-existing bug, visible once the chart sat in a card
  - 48dp bottom padding on all three scrollers: the FAB overhangs the tab bar by ~18dp and was
    covering the last row
- Not done / deferred: the receipt-attach dialog is still a default `AlertDialog` (no design frame
  exists for it); Play closed test (H4); 20s feel (H5)
- Blocked: none


## 2026-08-30  T0545  visual
- Goal: user spotted the `Sign In / Proof Card` is tilted in `pencil-new.pen` and flat in the app.
  Root cause was my own tooling: the `.pen` dumper I built printed fill/stroke/radius/layout but
  **never `rotation` or `effect`**, so I was design-matching against a lossy view of the file
- What the rescan found (11 root frames, every node with rotation or effect):
  - `rotation: -2.5` on `Sign In / Proof Card` — the reported bug
  - **13 hard shadows I had dropped everywhere**, all `{shadow, outer, #0F2E2C, offset y 4|5}`
    with no blur and no spread: every tab-bar FAB (4 screens), every onboarding `Primary Button`
    (4), Sign In's `Logo Badge` + `Proof Card` + `Google Button`, Onb1 `Go Badge`,
    Onb4 `FAB Mock`. Dark Home uses `#080D0C`
- Files changed:
  - `:core:designsystem` — new `WheregoHardShadow.kt` (`Modifier.wheregoHardShadow`); tokens
    `shadow` `#0F2E2C` / `darkShadow` `#080D0C` + dark remap; shadow applied inside
    `WheregoPrimaryButton` (y4) and `WheregoTabBar` FAB (y4)
  - `:feature:auth` — `PROOF_CARD_TILT`, `graphicsLayer { rotationZ }` + y5 shadow on the proof
    card, y5 on the logo badge, y4 on the Google button
  - `:feature:settings` — y4 on Onb1 `Go Badge`, y5 on Onb4 `FAB Mock`
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :core:model: :core:database: :core:sync: :app:testDebugUnitTest` → 42 passed
  - on `emulator-5554`: `pm clear` + full first-run walk; compared the device Sign In against a
    `TakeScreenshot(['SB2bK'])` render of the same node. Tilt direction and all three shadows
    match; onboarding CTA + Go Badge and the Home FAB shadows confirmed
- Decisions:
  - NOT `Modifier.shadow()`: that is a blurred elevation shadow tinted by the Material scheme,
    the opposite of this flat illustrated look. `wheregoHardShadow` draws an offset solid copy of
    the shape via `drawBehind` + `drawOutline`, and must sit BEFORE `clip`/`background`
  - Rotation sign: pen.dev documents rotation as counter-clockwise, Compose `rotationZ` is
    clockwise, so the sign flips — `-2.5` in the file becomes `rotationZ = +2.5f`. Confirmed by
    rendering the node and comparing, not by trusting the doc
  - Shadow lives inside the shared components, so the four FABs and four CTAs each get it from one
    place instead of eight call sites
- Follow-up worth doing: the dumper is the real defect. Any future `.pen` diff must print
  `rotation`, `effect`, `opacity`, `layoutPosition` and `strokeAlignment`, not just the box model
- Blocked: none

