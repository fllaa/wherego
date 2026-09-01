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


## 2026-08-30  T0615  ux-receipt
- Goal: remove redundant post-save receipt attach dialog; enable fast in-sheet receipt capture and long-press receipt-first scan
- Files changed:
  - `:core:i18n` — receipt strings for source picker, attached options, and OCR banner
  - `:core:database` — `CaptureDraft.receiptId`, `LedgerStore.save` with `draftId` and `receiptId`, safe `occurredAtForDate`
  - `:core:designsystem` — `WheregoTabBar` `onScanReceipt` via `combinedClickable`, `WheregoTxRow` receipt indicator icon
  - `:feature:capture` — in-sheet photo chip with OCR suggestion banner and actions; removed blocking post-park modal; new `CaptureViewModelTest`
  - `:feature:home` & `:feature:stories` — `hasReceipt` propagated to transaction rows
  - `:app` — `WheregoNavHost` wired long-press `+` to fast scan; removed root `ReceiptAttachDialog`
- Commands:
  - `./gradlew testDebugUnitTest :app:assembleDebug` → SUCCESS
- Decisions:
  - Instant "Park it": saving closes the sheet immediately without post-save modal interruptions
  - Inline Photo: photo chip accessible during both create and edit in `CaptureSheet`
  - Fast Scan: long-pressing `+` opens camera $\to$ OCR auto-fills amount and opens sheet for 1-tap categorization
- Blocked: none

## 2026-09-01  T0753  ux-recurring-date
- Goal: a recurring bill's first due date is the user's pick, not forced to today
- Files changed:
  - `:core:database` — `PlanStore.newRule` takes `firstOn: LocalDate` (seeds `startOn` + `nextOn`)
    instead of deriving today from `zoneId`
  - `:feature:plan` — `PlanUiState.today`; `addRule` derives `dayOfMonth`/`weekday` from the picked
    date; new `BillSheet` with a `First due` chip row and `FirstDuePicker`; `AmountCategorySheet` is
    budget-only again (note plumbing dropped); `ChipPill` extracted from `CategoryChipRow`; rule card
    prints `dayTitle(nextOn)`
  - `:feature:settings` — `Me → Recurring` detail prints `dayTitle(nextOn)`, not the ISO string
  - `:core:i18n` — `plan_field_first_due`, `plan_chip_today`, `plan_chip_pick_date` (en + in)
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :app: :core:database: :core:model: :feature:capture:testDebugUnitTest` → SUCCESS
  - on `emulator-5554`: added `Wifi` 25rb first due `Sat 26 Sep`; card reads
    `Monthly · next Sat 26 Sep`; row is `dayOfMonth=26 startOn=nextOn=2026-09-26`; Home due inbox
    stayed empty; August greyed out in the calendar; `in-ID` app locale renders
    `Jatuh tempo pertama / Hari ini / Pilih tanggal`
- Decisions:
  - `dayOfMonth` now comes from the picked date. It was hardcoded `1` at the call site while the
    first hit was today, so a bill added on the 15th jumped to the 1st on its second hit
  - The calendar floor is today. An earlier first due is overdue the moment it is saved, and
    `DueReminder` coerces a negative delay to `0` — the notification would fire at save time
  - Frequency stays monthly (the sheet never offered a choice). `weekday` is derived anyway so the
    stored row is already right when a weekly toggle lands
- Blocked: none

## 2026-09-01  T0816  ux-hero-differentiation
- Goal: Home, Stories and Plan heroes all showed the same figure — the current month's spend. Not a
  data bug (all three computed it correctly, and the month steppers work), but three heroes and two
  near-identical captions answering one question. Give each screen its own number, and collapse the
  six copies of the month-spend rule that let them drift.
- Files changed:
  - `:core:model` — new `MonthSpend.byCategory`/`total`: the single month-spend predicate (expense +
    `occurredOn` inside the month, summed on `amountBaseMinor`), one private `inline forEachIn` doing
    one pass with no intermediate lists
  - `:core:database` — `LedgerStore.assembleHome` and `observeMonth` call it; `observeMonth` now
    builds one `SpendRow` per category instead of one per transaction; `PlanStore.observeBars` calls
    it and drops its own `start`/`end`
  - `:feature:plan` — `PlanViewModel` calls it, private `monthSpend` deleted; `PlanScreen` cap card
    leads with `capLabel`/`capAmountMinor` (remaining, or overage, or spend when no cap exists) and
    the foot label carries spent-of-cap
  - `:feature:stories` — hero leads with the previous-month delta (`vs August` / amount / `less`
    pill + `Rp X spent in September`); falls back to the old total-first layout when `deltaMinor` is
    null
  - `:core:i18n` — added `plan_cap_left_in_month`, `plan_cap_over_in_month`, `plan_cap_spent_of`,
    `stories_vs_prev`, `stories_total_spent_in`; `stories_delta_less`/`_more` are now bare words
    (the amount moved to the hero); removed `plan_cap_left_of`, `plan_cap_over_of`,
    `stories_than_prev` (en + in)
  - tests — `MonthSpendTest` (month edges inclusive, kind filter, base-currency field, grouping
    agrees with total); `LedgerStoreTest` now asserts SQL `sumExpenses` equals `MonthSpend.total`
- Commands:
  - `./gradlew assembleDebug testDebugUnitTest` → SUCCESS, 66 tests 0 failures
  - on `emulator-5554` (Sep 2026; Jul 200rb / Aug 3.5jt / Sep 1.2jt spend, Sep caps 3.4jt, Aug cap
    1jt): Home `Spent this month Rp 1.200.000`, Stories `vs August Rp 2.300.000 ↘ less`, Plan
    `Left to spend in September Rp 2.200.000` — three screens, three numbers
  - month variants checked: Stories August `vs July Rp 3.300.000 ↗ more`, Stories July falls back to
    `Spent in July` (no June to compare); Plan August `Over cap in August Rp 2.500.000`, Plan July
    `Spent in July` + `No caps set for July yet`
- Decisions:
  - `MonthSpend` lives in `:core:model`, not on `LedgerStore`: `PlanStore` and `PlanViewModel` both
    need it, and it is a pure domain rule with no Room dependency
  - `TransactionDao.sumExpenses` stays as the SQL copy — it is the only DB-side aggregate and avoids
    loading the table — so a test pins it to `MonthSpend.total` rather than deleting it
  - Plan keeps the meter as spend-against-cap even though the headline is now remaining; the bar
    filling up as the number counts down is the standard budget idiom
  - Plan falls back to raw spend when no cap exists, because there is no remainder to show; that is
    the one place the month's spend still appears outside Home
  - A delta of exactly 0 still reads `Rp 0 · less`, as it did before this change
- Blocked: none

## 2026-09-01  T0900  ux-plan-editing
- Goal: nothing on Plan was editable. Budgets, `Set aside` goals and recurring bills could only be
  added or removed — the `Edit` link swaps a row's trailing note for a `Remove` link and nothing
  more, so changing a cap meant deleting the row and retyping it. Chasing that turned up a data
  bug behind it: `PlanStore.upsertBudget` minted a fresh ULID on every call and `BudgetEntity`'s
  primary key is `id`, so `REPLACE` never matched. A second cap on a category it already had
  **inserted a duplicate**: two cards for one category, and both counted in `capTotalMinor`, so
  the hero silently doubled the month's cap.
- Files changed:
  - `:core:database` — `upsertBudget` → `setBudget(..., replacedId)`, keyed on
    (`categoryId`, `yearMonth`) via `listMonth`, reusing the matching row's id; new `updateRule`
    (moves `nextOn` + `dayOfMonth`, keeps `startOn`) and `updateGoal` (keeps the row's currency,
    never blanks the name); `GoalDao.get(id)`; deleted `upsertRule`, which had no callers
  - `:feature:plan` — `PlanViewModel.editBudget`/`editRule`/`editGoal`; `PlanScreen`'s three sheet
    booleans collapsed into one `PlanEditor` sealed interface carrying the tapped row, every
    budget/goal/bill row now opens its sheet pre-filled, `AmountCategorySheet`/`GoalSheet`/
    `BillSheet` take `initial*` + `title` + `confirmLabel`, `FirstDuePicker` → `DuePicker`, dead
    `softHex` param dropped from `RuleCard`
  - `:core:i18n` — `plan_sheet_edit_budget`, `plan_sheet_edit_bill`, `plan_sheet_edit_set_aside`,
    `plan_cta_save_it`, `plan_field_next_due` (en + in)
  - tests — new `PlanStoreTest`: one cap per category per month, `Overall` included; a cap moved to
    another category leaves one row; moved onto an occupied category it merges; `updateRule` keeps
    `startOn`; `updateGoal` keeps currency and name
- Commands:
  - `./gradlew :app:assembleDebug testDebugUnitTest` → SUCCESS, `PlanStoreTest` 6/6
  - on `emulator-5554` (Pixel_10_Pro, `pm clear`, Sep 2026, one Rp 25.000 Food out spend): set a
    Food out cap 500rb → tapped the card → `Edit budget` opened on `Rp 500.000` with Food out
    picked → saved 300rb → card `Rp 25.000 of 300.000`, hero `Rp 275.000`. Re-adding Food out at
    400rb through `Set a budget for another category` left **one** card at 400.000 (pre-fix: two
    cards, 700.000 cap). Moved that cap to Groceries → still one row. Goal `Umrah` 1jt/5jt edited
    to 2jt (Target survived the Now/Target toggle) → `40%`. Bill `Wifi` 250rb edited to 300rb with
    the due date moved to Sat 26 Sep under a `Next due` label. Pulled `wherego.db`: one budget row,
    goal still `IDR`, rule `dayOfMonth=26 nextOn=2026-09-26 startOn=2026-09-01`. In `Edit` mode a
    `Remove` tap still deletes and does not open the editor
- Decisions:
  - Row tap opens the editor; `Edit`/`Done` stays a delete-reveal toggle. The design has no edit
    frame, and tap-to-edit is what the capture sheet already does for a transaction
  - `setBudget` owns the one-cap-per-category-per-month invariant instead of a unique index: it is
    the only writer of `budgets` (nothing syncs them), so the fix needs no Room migration
  - `replacedId` rather than delete-then-insert at the call site: moving a cap onto a category that
    already has one has to merge, and that decision belongs next to the lookup
  - An edited bill keeps `startOn`. Moving the next hit is not rewriting when the bill began
  - The due-date calendar floor is `min(today, initialDue)`, so an already-overdue bill can open on
    its own date; a new bill still cannot start behind today
  - Sheet seeds use `remember(seed)`, so re-opening on a different row re-seeds instead of showing
    the previous row's numbers
  - `DigitBuffer.replace` is the prefill, same as `CaptureViewModel` uses for editing a transaction
- Not done / deferred: goals and recurring still have no `Edit` affordance of their own — the one
  toggle on the `Budgets` header drives all three sections, as the design frame has it
- Blocked: none


## 2026-09-01  T0918  ux-stories-balance
- Goal: user reported the Stories `Balance` section was "just a single line on the top" and showed
  nothing else. It was.
- Root cause: Vico's `LineChart` forces a zero-based axis —
  `minY = axisValuesOverrider?.getMinY(model) ?: minY ?: min(model.minY, 0f)`
  (`core-1.13.1-sources` → `chart/line/LineChart.kt:576`) — and the card set no
  `axisValuesOverrider`. A real balance sits far from zero, so August's Rp 4.630.000–5.050.000
  became an 8% band at the very top of a 160dp box, and `getDrawY` (`LineChart.kt:511`) maps a
  constant balance to `bounds.bottom - 1f * height`, i.e. exactly the top edge. Worse for a fresh
  profile: `starting=0` with no logs is a constant-zero series, `lengthY = 0`,
  `(y - minY) / lengthY = NaN`, and the path drew nothing. The card had no axes, no labels and no
  number either, so the section really was one header plus one hairline
- Files changed:
  - `:core:model` — `BalanceSpark` + `BalanceSeries.spark(points)`: normalises the series against
    its own low/high (`0f` = month low, `1f` = high), flat months collapse to `0.5f` instead of
    `NaN`, `zeroFraction` marks a sign flip, `null` under two plotted days
  - `:feature:stories` — `StoryBalance` in the ViewModel carries the fractions plus the now/low/high
    labels; series now ends at `minOf(end, today)`, so the current month no longer plots a flat tail
    out to the 31st; the card became a hand-drawn `Canvas` sparkline (88dp, `tealSoft` area, 2.5dp
    round-joined `teal` line, ink-ringed dot on the last day) titled in-card next to the balance,
    with `Low`/`High` under it — or `No change this month.` when the month never moved
  - `:core:i18n` — `stories_balance_low` / `_high` / `_flat`, en + in
  - `gradle/libs.versions.toml`, `:feature:stories/build.gradle.kts` — Vico dropped
  - tests — `S6Test`: a barely-moving month spans `0f..1f`, a flat month is all `0.5f` with no
    zero line, a negative dip puts `zeroFraction` at `0.4f`, one day yields `null`
- Commands:
  - `./gradlew :core:model:testDebugUnitTest :app:assembleDebug` → SUCCESS
  - on `emulator-5554` (Pixel_10_Pro, API 36): pushed a seeded `wherego.db` — starting balance 5jt,
    7 August logs, low 4.630.000 / high 5.050.000, an 8% band that used to be the hairline. August
    now draws a stepped line across the full card, `Rp 4.630.000` beside the title, `Low`/`High`
    beneath, dot on Aug 31. July (no logs, balance carried in) draws the mid-height rule with
    `No change this month.` September (today is the 1st, one plotted day) hides the card entirely
    instead of showing an empty one. Re-checked both in dark mode
- Decisions:
  - Vico went out rather than getting an `AxisValuesOverrider`. It was one naked line in one card —
    no axes, markers, scroll or animation — so the library was two artifacts of dependency for a
    path a `Canvas` draws in 50 lines, in the ink/teal language the rest of the app is drawn in
  - Normalisation lives in `:core:model`, not the composable: it is the part that was wrong, and it
    is the part a unit test can hold
  - The card hides itself only when the balance never left zero. A flat *non-zero* month is worth a
    line — it says the pot did not move — so it draws mid-height with the flat note instead of
    `Low X / High X`, which would read as two different numbers
  - Section title moved inside the card, matching `Where it went`. A `WheregoSectionHeader` above an
    empty-looking card is what made the emptiness read as a bug
  - The zero crossing is the one gridline drawn. Below it the pot is empty, which is worth a rule;
    every other value is already labelled by `Low`/`High`
- Not done / deferred: the sparkline has no touch readout — no per-day value on drag. `Balance` is
  still a demoted card the design frame does not have, so it stays a shape with three numbers
- Blocked: none

## 2026-09-01  T0940  sync-server-cursor
- Goal: two devices, each holding transactions parked while signed out, then Google sign-in on
  both. The answer is the union — ULID ids never collide (`LedgerStore.kt:112`) and every guest row
  is `dirty` — but only the device that signed in *second* got it
- Root cause: the pull watermark and the field it filtered lived in different clock domains.
  `pullTransactions` asked for `updatedAt > lastPullEpoch` (`FirestoreCloudDataSource` +
  `SyncEngine`), where `updatedAt` is stamped by the *authoring* device at park time and copied
  verbatim on push (`CloudCodec.kt:24`), while `lastPullEpoch` was `clock.millis()` on the
  *reading* device at pull time. A peer's backlog is by definition stamped in the past: device A
  signs in at 10:00 and pulls (watermark 10:00), device B pushes at 11:00 rows stamped 09:00, and
  A's next pull asks for `> 10:00` — B's rows are never delivered, not even late. Same root cause
  gave two more leaks: `markPulled` took its timestamp *after* the query returned, so writes during
  the round trip were skipped permanently, and any clock skew between devices dropped that much
  peer data on every pass
- Files changed:
  - `:core:sync` — every document now carries `syncedAt`, a `FieldValue.serverTimestamp()`, and the
    cursor is epoch nanos read off the last row a device actually received. `CloudPage<T>(rows,
    cursor)` replaces the bare list, so the watermark can never advance past an unseen row; it
    carries the caller's own cursor back on an empty page. Pulls read `Source.SERVER` — a cached
    snapshot reports a pending server stamp as null and *orders* it as null, which would corrupt
    the cursor. `pullProfile` lost its watermark entirely: one document, and
    `SyncMerge.decideProfile` was always the real gate
  - `:core:sync` — `SyncMerge.shouldClearDirty(row.updatedAt, row.updatedAt)` was a tautology, so
    an edit landing mid-push had its `dirty` flag cleared and never pushed again. `pushTransactions`
    now re-reads each row and only drops the flag when `updatedAt` has not moved
  - `:core:sync` — `FakeCloudDataSource` stamps and ranges the same way (its own monotonic clock),
    so debug and release builds agree on which rows a device has seen. `SyncWorker` gained a
    `NetworkType.CONNECTED` constraint now that pulls insist on the server
  - `:core:database` — Room **v10**: `sync_state.lastPullEpoch`/`lastPushEpoch` → `lastPullCursor`.
    `lastPushEpoch` was written every pass and read by nothing. `MIGRATION_9_10` drops the table,
    which resets every watermark on purpose: one full pull heals installs that already lost rows
  - `:core:sync` — `SyncEngine` no longer takes a `Clock`; nothing in it reads local time now
- Commands:
  - `./gradlew :app:assembleDebug testDebugUnitTest` → SUCCESS
  - `./gradlew :core:database:testDebugUnitTest :core:sync:testDebugUnitTest --rerun-tasks` →
    33 passed. New: `SyncEngineTest.aPeerBacklogArrivesEvenThoughItPredatesOurFirstPull`,
    `aSettledRowIsNotPulledTwice`, `anEditDuringThePushKeepsTheRowDirty`;
    `FakeCloudDataSourceTest` ×6; `SyncStateMigrationTest`
  - mutation-checked both new guards: swapping `page.cursor` back for a local-clock value fails
    `peer backlog must survive the cursor`; renaming the migration's column back to `lastPullEpoch`
    fails `migrationNineToTenBuildsTheTableRoomExpects`
- Decisions:
  - `updatedAt` stays exactly as it was — it is the last-write-wins input and it is fine at that
    job. It just cannot also be the transport's ordering key, because two devices author it
  - Cursor is nanos, not millis: Firestore `Timestamp` carries nanos, and truncating would
    re-deliver rows inside the same millisecond. `seconds * 1e9 + nanos` round-trips exactly and
    fits a `Long` until 2262
  - `sinceCursor == 0` reads the collection whole rather than ranging. Documents written before
    `syncedAt` existed match no range filter, so the first pull after upgrade delivers them and
    stamps them on the way out — deterministic, instead of hoping a future push covers them
  - `SyncStateMigrationTest` compares the migration's DDL against the table Room builds from the
    entity via `PRAGMA table_info`. `exportSchema` is off, so nothing else would catch a typo that
    only detonates on a user's device at launch
- Not done / deferred: `startingBalanceMinor` still merges last-write-wins on the profile, so two
  devices that each onboarded with a real opening balance keep the union of transactions but only
  one of the two balances. That is a product decision (sum? keep the larger? ask on the first
  multi-device sign-in?), not a merge bug, and it is untouched here. Pulls are still unpaginated
- Blocked: none

## 2026-09-01  T1230  balance-anchor
- Goal: close the item the cursor slice deferred — two devices that each onboarded with an opening
  balance kept the union of transactions but only one of the two balances. Interrogated the product
  decisions first; the answers are now `docs/dev-plan.md` §5.2 Money rules
- Root cause, and it was two bugs not one:
  - `startingBalanceOn` was written, synced and documented but **read by no computation**.
    `LedgerStore.currentBalance` and `BalanceSeries.points` applied every row whatever its date, so
    a spend backdated with the `Pick` chip was debited against a figure that already accounted for
    it. Single device, no sync involved
  - the opening balance was a scalar on the profile (last-write-wins) while transactions merge by
    union, so the losing device's spending was double-counted against the winning device's figure
  - `setBalanceTo` wrote `targetMinor - currentBalance()`, a **delta** derived from device-local
    state. Two phones both tapping `Set balance → 4.8jt` wrote `-200rb` and `+1.8jt` and merged to
    6.6jt — a shipped bug in `Adjust balance`, same family as the sync watermark: a value computed
    from one device's view, replicated as though it were a fact
- Files changed:
  - `:core:model` — `TransactionKind.RECONCILE`, an assertion ("as of `occurredOn`, everything
    totalled `amountMinor`"), plus `isActivity` so bookkeeping stays out of the streak.
    `BalanceSeries` gains `ORDER` (`occurredOn`, `createdAt`, `id` — `id` is a ULID, so two devices
    pick the same anchor with no coordination), `anchor`, `total`, and a `points` that seeds at the
    anchor and walks **both** directions: knowing the total on the 20th and every movement since
    the 5th also gives the 5th, the way a statement does. `signedBase` no longer treats "not an
    expense" as money in — reconcile and any unknown future kind move nothing
  - `:core:database` — Room **v11**: `MIGRATION_10_11` turns a non-zero `startingBalanceMinor` into
    a dirty `reconcile-<profileId>` row dated `startingBalanceOn`, then zeroes the scalar. Id is
    derived, not minted, so re-running cannot duplicate. `setBalanceTo` asserts the total and drops
    its `startingBalanceMinor` parameter; it also stops mis-scaling a non-IDR base
    (`baseCurrency` was defaulting to IDR while `currency` was the profile's). `importRows` keeps a
    `reconcile` CSV row instead of silently turning it into an expense on export/import round-trip.
    `assembleHome` excludes assertions from `weekLoggedCount` and `streakDays`
  - `:core:sync` — after a pull, if the anchor changed identity **and** the total moved, the pair is
    recorded for Home to ask about. Not "the anchor changed" (normal: every reconcile adds one) and
    not "two same-day claims" (that collapses into the total moving)
  - `:core:datastore` — `balanceConflict` holds `mine|theirs` device-locally; the phone that made
    the winning claim has nothing to ask
  - `:feature:home` — `BalanceClashDialog`; answering soft-deletes the rejected claim so the
    decision syncs. Reconcile rows render as `Balance set` and are **not** clickable or
    long-clickable: the capture sheet has no tab that can represent an assertion, and duplicating
    one would manufacture a second same-day anchor out of nothing. Swipe-delete still works
  - `:feature:settings` — onboarding writes the anchor instead of the scalar;
    `UserProfileStore.completeOnboarding` is down to currency + name + the tour flag
  - `:feature:stories` — `StoryBalance` carries `anchorFraction`/`anchorOn`; the sparkline draws a
    quiet `muted` rule at the reconciliation point under a caption, so a balance that ignores
    everything before that day reads as a reconciliation rather than arithmetic going wrong.
    `logCount` excludes assertions
  - `:core:i18n` — `kind_reconcile`, `stories_balance_anchor`, `home_clash_*`, en + in
- Commands:
  - `./gradlew :app:assembleDebug testDebugUnitTest` → SUCCESS, 103 passed
  - new: `BalanceSeriesTest` ×11 (anchor determinism on shuffled input, same-day tie-break,
    deleted anchor, pre-anchor exclusion, both-directions series, two devices asserting the same
    target landing on it, legacy delta still summed, unknown kind moves nothing),
    `OpeningBalanceMigrationTest` ×4 (real SQL on real Room tables, idempotence, zero-balance
    no-op, date fallback), `SyncEngineTest` +2 (a peer assertion that moves the balance asks; one
    that agrees does not)
  - on `emulator-5554` (Pixel_10_Pro, API 36), against the existing seeded database:
    `PRAGMA user_version` went 9 → **11**, so 9→10→11 ran and Room's post-migration schema check
    passed on real data. The profile's Rp 5.000.000 became `reconcile-<profileId>` dated
    `2026-08-01` and the scalar zeroed. `Adjust balance → 5.000.000` wrote a ULID `reconcile` row
    asserting the total (not a `-22.568.494` delta) and `Me` read back `Now Rp 5.000.000` with 15
    pre-anchor rows on file. Then the decisive one: a Rp 25.000 expense backdated to `2026-08-31`
    left the balance at **Rp 5.000.000** — old arithmetic would have shown 4.975.000. Home rendered
    the row as `Balance set` with `clickable="false" long-clickable="false"`; swipe-delete worked.
    Stories/August with the anchor in view drew the tick and
    `Counting from Sat 1 Aug — the day you set it.` (screenshotted); with the anchor outside the
    window the caption is absent and the line back-projects. Both test rows soft-deleted afterwards
- Decisions:
  - `RECONCILE` is a new kind rather than a rewrite of `ADJUSTMENT`. Converting existing deltas
    would need each device to compute a total from rows that may not have synced yet — a migration
    that bakes in a number ignoring the peer. Old deltas keep their meaning and keep being summed
  - assertion, not delta, is the whole point: deltas cannot compose across devices, assertions
    cannot stack
  - the anchor tie-break ends in `id` on purpose. ULIDs make it deterministic, so two devices reach
    the same anchor without a round trip
  - pre-anchor rows stay in Stories and every spend total. A habit tracker that deletes spending
    history the moment you reconcile has thrown away its reason to exist; the tick explains the gap
  - the prompt fires on "a peer's anchor took over **and** the total moved", not on every anchor
    change. With the anchor rule most collisions are unambiguous arithmetic, and a dialog the user
    learns to dismiss is worse than none
  - the profile scalar is zeroed, not dropped. Dropping needs a `user_profile` table recreate for no
    functional gain, and the cloud profile document still carries the field for a device with no
    anchor row yet
  - safe only because nothing has shipped (`versionCode = 1`): an older client would hit
    `signedBase`'s old `else -> +amount` and read an assertion as income. After release this would
    need a transitional zero-amount field
- Not done / deferred: the migration changes an existing user's balance on upgrade — that is the
  double-count fix landing, explained by the Stories tick but not announced. Pulls are still
  unpaginated
- Blocked: none

## 2026-09-01  T1310  capture-balance-tab
- Goal: the deferred item from `balance-anchor` — assertions could only be created from
  `Me → Adjust balance`, which always stamps today, so a wrong one could not be corrected and a
  past one ("on 1 Aug I had 5jt") could not be stated at all. Tapping a `Balance set` row did
  nothing
- Files changed:
  - `:core:model` — `PresetCategories.OTHER` replaces the `"cat_other"` literal that
    `setBalanceTo` and now the capture sheet both need. A reconcile row carries a category it
    never reads, because the ledger stores one on every row
  - `:feature:capture` — third pill in `KindToggle`. `CaptureUiState.isReconcile` drops the
    category requirement from `canSave` and hides what an assertion has no use for: the
    10rb/15rb/25rb quick-spend chips, the photo/OCR chip, the category scroller and its grid.
    Date chips stay — they are the point. `save` falls back to `PresetCategories.OTHER` only on
    that tab
  - `:feature:home` — `Balance set` rows are tappable again now that the sheet can represent
    one; `onLongClick` stays null, since `same again` on an assertion would manufacture a second
    same-day anchor
  - `:core:i18n` — `capture_tab_balance`, `capture_balance_helper`, en + in
- Commands:
  - `./gradlew :app:assembleDebug testDebugUnitTest` → SUCCESS, 106 passed
  - new `CaptureViewModelTest` ×3: the tab saves without a category but not while the buffer is
    blank, it writes the assertion on the picked day, and editing one corrects it in place rather
    than minting a second anchor
  - on `emulator-5554`: the toggle shows `Expense | Income | Balance`; the Balance tab shows
    `Not a spend — what the pot totals on this day.` with the quick-amount chips, photo chip and
    category scroller gone and `Today/Yesterday/Pick` kept. `4.000.000` + `Yesterday` + `Park it`
    wrote `reconcile|4000000|2026-08-31|cat_other` — a backdated assertion, which `Adjust balance`
    cannot produce. Tapping the resulting `Balance set` row reopened the sheet on the Balance tab
    with `4.000.000` pre-filled. Test row swipe-deleted afterwards
- Decisions:
  - `canSave` requires a non-blank buffer rather than `amountMinor > 0`. An explicit `0` is a real
    claim ("wallet is empty") and must save; an untouched sheet must not, because parking an anchor
    silently stops every earlier row counting toward the balance
  - the category scroller is hidden rather than filtered. `Category.matches("reconcile")` is empty
    for every category, so the row would have rendered as an empty scroller with a `More` button
  - no confirmation dialog on the tab. `Instant Park it` (T0930) is the rule; the helper line
    carries the warning instead, and a wrong assertion is one swipe from gone
  - `Adjust balance` stays. It is the discoverable path for the common case, and both write the
    same row through the same `save`
- Not done / deferred: the `Balance` tab is reachable from the `+` FAB, so an assertion is three
  taps from a mis-tap. The helper copy and the blank-buffer guard are the only friction
- Blocked: none
