# Wherego — Agent Loop Playbook

Use this file as the **instruction packet** for a long-running coding agent.
Do **not** use `Wherego_Money_Tracker_Plan.md` as the only prompt. That file is product intent. This file is executable work.

---

## Verdict on the original plan

| Original plan section | Agent-loop ready? | Why |
|---|---|---|
| Thesis / persona / tone | No | Taste, not a test |
| Feature layers | Partial | Scope is right, tasks are not atomic |
| 20s capture flow | Partial | Behavior is clear, UI states are not enumerated |
| Data model | Almost | Field list exists; no Room entities, converters, or indexes |
| Stack choices | Yes | Lock them. Do not reopen. |
| Slices 0–6 | No | Human weeks, not tickets |
| “Dogfood 7 days” | No | Requires a human with a phone |
| Play Store / privacy policy | No | Human + accounts |
| Firebase Auth / SHA-1 / Play Console | No | Human gate |

**You can run an agent loop on this product only after you split work into: machine-checkable slices, human gates, and a freeze list.**

---

## How to run this

### Recommended loop

1. Human pastes **this playbook + current slice file + repo tree** into the agent.
2. Agent implements **one slice only**.
3. Agent runs the slice’s verification commands and writes `AGENT_LOG.md`.
4. Human reviews the log + emulator smoke (or CI).
5. If green, human starts the next slice in a **new** context window with a fresh packet.
6. Never tell the agent “build the whole app.”

### Packet to paste every run

```text
You are implementing Wherego, an Android money tracker.
Follow Wherego_Agent_Loop_Playbook.md strictly.
Work ONLY on slice: <SLICE_ID>
Do not start later slices.
Do not add features not listed in this slice.
Do not change the stack.
If blocked by a human gate, stop and write BLOCKED.md.
When done, run the slice verification commands and append AGENT_LOG.md.
```

Attach: this playbook, `Wherego_Money_Tracker_Plan.md` (intent only), and the repo.

### Context hygiene

- One slice per session. New chat / new agent run per slice.
- If the session is long, the agent must commit after each green task inside the slice.
- Commit style: `feat(capture): numpad formats IDR` — not `wip`.
- If the agent is unsure, it chooses the **smaller** option and notes it in `AGENT_LOG.md`. It does not invent product features.

---

## Freeze list (do not reopen)

The agent must treat these as constants.

- App working name: `Wherego`
- ApplicationId: `com.flla.wherego`
- Package root: `com.flla.wherego`
- Platform: Android only
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Navigation: Navigation Compose
- DI: Hilt
- Local DB: Room (source of truth)
- Prefs: DataStore
- Async: Coroutines + WorkManager
- Charts (when slice allows): Vico
- Images: Coil
- minSdk 26, targetSdk 35, compileSdk 35
- Gradle Kotlin DSL
- Money: `Long` minor units, never `Float`/`Double`
- Default currency: `IDR` (scale 0)
- Default zone: `Asia/Jakarta`
- One pot. No accounts table.
- No bank sync, no Plaid, no scraping
- No household / multi-user editing
- No iOS, no KMP, no React Native
- No ads
- Soft-delete transactions (`deletedAt`)
- IDs: ULID strings generated on device
- Sync conflict: last-write-wins on `updatedAt`
- Playful tone, but **no mascot art until slice S2**. Use a colored circle placeholder named Go.

If a library is missing from this list, the agent may add a **tiny** well-known AndroidX lib. It may not add a second architecture framework (no Orbit, no MVI library, no SQLDelight).

---

## Human gates (agent must STOP)

Create `BLOCKED.md` with what the human must do. Do not fake these.

| Gate ID | Needed before | Human does |
|---|---|---|
| H0 | Slice S0 | Install Android Studio / JDK 17, accept SDK licenses |
| H1 | Slice S3 | Create Firebase project, add Android app `com.flla.wherego`, download `google-services.json` |
| H2 | Slice S3 | Add SHA-1 / SHA-256 debug keystore to Firebase, enable Google Sign-In |
| H3 | Slice S3 | Paste Firestore rules from this playbook into console |
| H4 | Closed test | Play Console account, $25, privacy policy URL |
| H5 | After S1 | Real-phone 20-second test (agent cannot judge “feels fast”) |
| H6 | Name lock | Check Play name collision; optionally rename |

Until H1–H3 are done, **S3 must use a fake `CloudDataSource`** that writes to local files or no-ops, behind an interface. Do not hardcode Firebase calls in feature modules.

---

## Definition of a finished slice

A slice is done only if all of these are true:

1. Every task checkbox in that slice is implemented.
2. `./gradlew :app:assembleDebug` succeeds.
3. Slice-specific `./gradlew` test command succeeds.
4. No files created outside the allowed module list for that slice.
5. `AGENT_LOG.md` lists commands run and results.
6. App has a guest/local user so the slice is usable **without** Google until S3.

If tests cannot be instrumented in this environment, the agent still writes the tests and records `TEST_ENV_MISSING` rather than deleting them.

---

## Repo shape the agent must create (S0) and then respect

```text
Wherego/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  AGENT_LOG.md
  BLOCKED.md                 # only if blocked
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/com/flla/wherego/
      WheregoApp.kt
      MainActivity.kt
      navigation/
      di/
    src/main/res/
    src/test/java/
    src/androidTest/java/
  core/model/
  core/database/
  core/datastore/
  core/common/
  core/designsystem/
  core/sync/                 # stub until S3
  feature/capture/
  feature/home/
  feature/stories/           # stub until S2
  feature/plan/              # stub until S4
  feature/settings/
  feature/auth/              # stub until S3
```

Start with **app + core:model + core:database + core:common + core:designsystem + feature:capture + feature:home**. Other modules may be empty placeholders. Do not implement their features early.

---

## Screen inventory (do not add screens off this list)

| ID | Screen | First live slice |
|---|---|---|
| A0 | Guest splash / first launch | S0 |
| C1 | Capture sheet (expense default) | S1 |
| C2 | Capture sheet income toggle | S1 |
| C3 | Edit existing transaction (same sheet) | S1 |
| H1 | Home | S1 |
| S1 | Stories month | S2 |
| N1 | Onboarding currency + starting balance | S2 |
| N2 | Category picker onboarding | S2 |
| ME | Settings | S2 |
| CAT | Category manager | S2 |
| AUTH | Google sign-in | S3 |
| B1 | Budgets | S4 |
| R1 | Recurring + due inbox | S4 |
| CAM | Receipt camera / confirm OCR | S5 |
| G1 | Goals | S6 |
| FX | Currency on capture | S6 |

No extra onboarding slides. No “insights lab.” No accounts screen.

---

## Data contracts the agent must implement

### Money

```kotlin
// core/model
data class Money(
    val amountMinor: Long,
    val currency: String, // ISO 4217
)

object CurrencyScale {
    fun scale(code: String): Int = when (code) {
        "IDR", "JPY", "KRW", "VND" -> 0
        else -> 2
    }
}
```

- IDR `18000` minor → display `Rp 18.000`
- Use `DecimalFormat` + `Locale("id", "ID")` for IDR
- Centralize in `MoneyFormatter`. No `toString()` on amounts in UI.

### Entities (Room)

```text
CategoryEntity
  id: String PK
  name: String
  emoji: String
  colorHex: String
  kind: String            // expense | income | both
  isPreset: Boolean
  archived: Boolean
  sortOrder: Int
  updatedAt: Long
  deletedAt: Long?

TransactionEntity
  id: String PK
  kind: String            // expense | income | adjustment
  amountMinor: Long
  currency: String
  fxRateToBase: String    // decimal string, "1" for v1
  amountBaseMinor: Long
  categoryId: String
  note: String
  occurredOn: String      // ISO-8601 date YYYY-MM-DD in user zone
  occurredAt: Long?       // nullable epoch millis
  recurringId: String?
  receiptId: String?
  createdAt: Long
  updatedAt: Long
  deletedAt: Long?
  dirty: Boolean

UserProfileEntity
  id: String PK           // local ULID until auth; then Firebase uid
  googleSub: String?
  email: String?
  displayName: String?
  photoUrl: String?
  baseCurrency: String    // default IDR
  localeTag: String       // default id-ID
  timeZoneId: String      // default Asia/Jakarta
  onboardingDone: Boolean
  startingBalanceMinor: Long
  startingBalanceOn: String?
  createdAt: Long
  updatedAt: Long

SyncStateEntity
  collection: String PK
  lastPullCursor: Long   # epoch nanos from the cloud's `syncedAt`, never a local clock
```

Indexes: `transactions(occurredOn)`, `transactions(dirty)`, `transactions(deletedAt)`, `categories(archived)`.

### Preset categories (seed exactly these ids)

Expense:

| id | name | emoji |
|---|---|---|
| cat_food_out | Food out | 🍜 |
| cat_groceries | Groceries | 🛒 |
| cat_transport | Transport | 🛵 |
| cat_bills | Bills | 📄 |
| cat_rent | Rent/Kos | 🏠 |
| cat_shopping | Shopping | 🛍️ |
| cat_health | Health | 💊 |
| cat_fun | Fun | 🎮 |
| cat_gifts | Gifts | 🎁 |
| cat_other | Other | 📦 |

Income:

| id | name | emoji |
|---|---|---|
| cat_salary | Salary | 💼 |
| cat_side | Side hustle | 🛠️ |
| cat_refund | Refund | ↩️ |
| cat_other_in | Other in | ✨ |

---

## Capture flow spec (S1 must match)

Default: expense.

1. Open sheet. Focus amount. Numpad visible. Preview `Rp 0`.
2. Digit taps append to a raw digit string (max 12 digits).
3. Backspace pops a digit.
4. Preview uses `MoneyFormatter`.
5. Category grid: 6 most recently used on row 1 if history exists; else presets in seed order, expenses only when kind=expense.
6. Note is a single line, optional, max 80 chars.
7. Date defaults to today in `Asia/Jakarta`. Chips: Today, Yesterday, Pick.
8. Primary button disabled while amount is 0 or no category selected.
9. Save: insert Room row, `dirty=true`, close sheet, Home list updates without refresh gesture.
10. After save, snackbar optional. Do not block.
11. Edit: same sheet, prefilled, save updates `updatedAt` + `dirty`.
12. Swipe delete on Home: set `deletedAt`, snackbar Undo 5s restores `deletedAt=null`.

**Out of scope for S1:** camera, OCR, budgets, recurring, FX picker, accounts, auth.

Target interaction count after amount is known: **2 taps** (category + save).

---

## Slices (run in order, one per agent loop)

### S0 — Spine

**Goal:** empty app that compiles, shows 4 tabs, teal/sand theme, dark mode follows system.

Tasks:

- [ ] Create project + version catalog with AndroidX, Compose BOM, Hilt, Room, DataStore, ULID lib (`azamsoft` or `ulidj` — pick one and pin).
- [ ] Application class `@HiltAndroidApp`.
- [ ] `MainActivity` with 4 tabs: Home, Stories, Plan, Me. Stories/Plan show “Soon”.
- [ ] Theme: seed teal `#0F766E`, sand `#FFF7ED`, ink `#1C1917`. Dynamic color **off**.
- [ ] Placeholder Go: 40dp circle on Home top-start.
- [ ] `UserProfile` row created on first launch (`id` = ULID).
- [ ] README with how to open in Android Studio.

Verify:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Stop. Do not build capture yet.

---

### S1 — 20-second loop  ← first real product slice

**Goal:** add expense/income to Room and see it on Home.

Tasks:

- [ ] Room DB v1: categories + transactions + profile.
- [ ] Seed presets if category table empty.
- [ ] `MoneyFormatter` + unit tests (IDR 0, 18000, 1250000; USD 199 later can wait but keep scale helper tested).
- [ ] Capture sheet from Home FAB.
- [ ] Numpad + category grid + optional note + date chips + income toggle.
- [ ] Home: month spent hero, Today list, Earlier this week.
- [ ] Edit by tapping a row.
- [ ] Swipe delete + undo.
- [ ] Duplicate: long-press row → “Same again” inserts a new row now.
- [ ] Unit tests: insert + month aggregate + soft delete.
- [ ] Guest works fully offline. No network permission required yet.

Verify:

```bash
./gradlew :app:assembleDebug
./gradlew :core:database:testDebugUnitTest :app:testDebugUnitTest
```

Human gate **H5** after this slice. Agent must not self-certify “under 20 seconds.”

---

### S2 — Story + settings + light play

**Goal:** a month view that answers “where did it go?”

Tasks:

- [ ] Stories: month pager, spend by category bars, top 3 categories, one generated sentence from template (no LLM).
- [ ] Sentence templates only, e.g. `"{cat1} {p1}% · {cat2} {p2}% · rest is quieter."`
- [ ] Onboarding once: base currency (default IDR), optional starting balance, confirm category set.
- [ ] Settings: dark mode override, display name local, starting balance edit (“set balance to” writes an `adjustment` transaction, do not rewrite history).
- [ ] Category manager: rename, emoji, color, archive. Cannot hard-delete if transactions exist.
- [ ] Log streak: count of distinct `occurredOn` or distinct local days with a save in last N days — pick **distinct calendar days with at least one non-deleted transaction in current timezone**, show on Home. No punishment copy.
- [ ] Go placeholder states: idle / happy (after save) / sleepy (no tx today). Still no custom artwork required.

Verify: unit tests for month aggregation + sentence builder + streak.

---

### S3 — Cloud sync + Google

**Blocked on H1 H2 H3.** If missing, implement interfaces + fake + `BLOCKED.md`.

Tasks:

- [ ] `AuthRepository` / `CloudDataSource` interfaces in `:core:sync`.
- [ ] Firebase Auth Google via Credential Manager. Link local profile id strategy:
  - Keep local ULID rows.
  - Store `firebaseUid` on profile.
  - Cloud documents keyed by `firebaseUid`, row id stays ULID.
- [ ] WorkManager `SyncWorker`: push `dirty=true`, then pull `updatedAt > lastPull`, apply, clear dirty if `updatedAt` unchanged.
- [ ] Firestore collections: `users/{uid}/transactions/{id}`, `users/{uid}/categories/{id}`, `users/{uid}/profile`.
- [ ] Soft-deleted rows sync (include `deletedAt`).
- [ ] Home cloud dot: synced / pending / offline.
- [ ] Crashlytics.
- [ ] Guest banner on Me: “Sign in to backup”. Capture never blocked.

Firestore rules (human pastes):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

Verify: fake unit tests for merge (remote newer wins; local dirty push). Human two-phone test is H-gate, not agent.

---

### S4 — Budgets, recurring, CSV export

Tasks:

- [ ] `BudgetEntity` monthly per category + optional overall (`categoryId` null).
- [ ] Home shows up to 3 bars if any budget exists.
- [ ] Recurring rules; `nextOn`; Due inbox on Home; confirm creates a transaction (`recurringId` set). `autoPost=false`.
- [ ] Local notification via WorkManager the morning of `nextOn`. Copy: “Wifi usually hits today. Log it?”
- [ ] CSV export from Me: `date,kind,amount,currency,category,note`. Share sheet.
- [ ] Plan tab becomes real (budgets + recurring).

No OCR yet. No goals yet.

---

### S5 — Receipt photo + OCR

Tasks:

- [ ] After save (or from edit): attach photo from camera/gallery.
- [ ] Store local file in app storage. Compress to max edge 1080 JPEG quality 70.
- [ ] ML Kit text recognition; parse largest plausible amount; prefill confirm dialog. Never auto-save OCR.
- [ ] Upload worker to Firebase Storage only if signed in. Path `users/{uid}/receipts/{id}.jpg`.
- [ ] Fail open: photo stays local if upload fails.

---

### S6 — Goals, FX, balance chart, CSV import, PDF

Tasks:

- [ ] Goals as earmarks (`allocatedMinor`). Do not add accounts.
- [ ] Capture allows currency ≠ base. Rate: manual field defaulting to cached table or `1`. Persist `fxRateToBase` string.
- [ ] Weekly FX cache worker only if non-IDR used. If no API key / network, manual rate remains valid.
- [ ] Balance over time line from starting balance + running sum. Vico chart on Stories.
- [ ] CSV import wizard: map columns, preview 5 rows, commit.
- [ ] Month PDF share (simple: month title, bars as text table, transaction list). Looks clean, not marketing.

---

## What the agent must never do

- Add multiple accounts, wallets, or bank sync “while we’re here.”
- Call an LLM from the app to write Stories copy.
- Store money as `Double`.
- Block capture behind login.
- Use Firestore as the only database.
- Enable Firebase listeners on every screen (pull on app start + after save + every 15 min while foreground).
- Add screenshots of real receipts into git.
- Commit `google-services.json` if the human said secrets stay local — prefer `google-services.json` in `app/` as Firebase expects, but never commit service account keys.
- Refactor the whole module graph mid-slice.
- Upgrade AGP/Kotlin mid-slice unless compile is blocked.
- Implement iOS, web, desktop.
- Write a new product spec that contradicts this playbook.

---

## AGENT_LOG.md format (append-only)

```markdown
# AGENT_LOG

## 2026-08-29  T1234  slice=S1
- Goal: ...
- Files changed: ...
- Commands:
  - `./gradlew :app:assembleDebug` → SUCCESS
  - `./gradlew :app:testDebugUnitTest` → 12 passed
- Decisions: picked ulid lib X because Y
- Not done / deferred: ...
- Blocked: none | see BLOCKED.md
```

---

## Suggested automation wrapper (human)

Pseudo-loop for a long-running agent runner:

```text
for slice in S0 S1 S2 S3 S4 S5 S6:
    if slice needs human gate and gate file missing:
        write BLOCKED.md; pause for human; continue
    run agent(packet=playbook+slice, timeout=2h, max_steps=80)
    run gradle verify
    if tests fail: run agent once more with "fix failures only"
    if tests fail again: stop and ping human
    git commit
    human quick smoke
```

Hard stop conditions for the runner:

- Two consecutive failed fix loops
- Agent edits files outside `Wherego/`
- Agent introduces a new module not in the tree
- Gradle still red after one repair pass
- Agent requests a stack change

---

## Prompt you can paste today (S0)

```text
Read /home/workdir/artifacts/Wherego_Agent_Loop_Playbook.md
and /home/workdir/artifacts/Wherego_Money_Tracker_Plan.md

Implement ONLY slice S0 in a new repo folder Wherego/.
Follow the freeze list and repo shape.
Do not implement capture, Room transactions, Firebase, or mascot art.
When finished, run the S0 verify commands and write AGENT_LOG.md.
If you cannot run Gradle here, still generate the full project files
and record TEST_ENV_MISSING with the exact commands a human should run.
```

After S0 is green on a machine with Android SDK, start a **new** loop with “Implement ONLY slice S1”.

---

## Bottom line

The original plan is the **why**. This playbook is the **what, in which order, and how to know you’re done**.

An agent loop will succeed if you:

1. Feed this playbook, not just the product essay.
2. Run **one slice per session**.
3. Keep Firebase / Play / “does it feel like 20 seconds?” as human gates.
4. Refuse bonus features.

If you want a next file, the highest leverage is a `S1_TASKS.md` broken into 15–25 even smaller tickets (one screen state each) for a more mechanical agent.
