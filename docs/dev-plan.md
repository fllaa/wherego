# Wherego

**Money tracker — product, UX, data, tech, and roadmap plan**

Working title. Rename anytime. The job of the name: make “where did my money go?” feel light, not shameful.

Solo Android · Offline-first · Playful · Kotlin

---

## Snapshot

| Field | Lock |
|---|---|
| Audience | You + other beginners who hate spreadsheets. Solo personal finance. |
| Job to be done | I don’t know where my money goes. |
| North star | Log a spend in under 20 seconds. |
| Platform | Android only. Kotlin + Jetpack Compose. |
| Input | Mix: manual-first, receipt photo + on-device OCR, CSV import. No bank sync. |
| Money model | One pot. No multiple accounts in v1–v2. |
| Identity | Google Sign-In. Single user per login. No household sharing in v1. |
| Data | Offline-first on device. Cloud sync. Amounts may live on server. |
| Tone | Playful, never preachy. Celebrate logging, not austerity. |
| Builder | Solo. Cost-effective. Ship when it’s good, in slices. |

---

## 0. Decisions locked (and one override)

Your answers had two tensions. They’re named here so the plan stays coherent.

### Tension A — “me and other people” vs “one person”

Read as: the product is public-facing, but each account is single-player. You are user #1 and the taste-maker. No partner sharing, no roles, no household ledger in v1. That feature is a different product (split bills, permissions, arguments about “who spent the groceries”). Park it.

### Tension B — “all features except two” vs solo + “when it’s good”

If v1 includes budgets, goals, OCR, multi-currency, net worth, recurring, reminders, PDF export, *and* playful polish, you will not ship. Beginners also bounce from apps that ask them to configure a life operating system on day one.

**Override:** everything you listed stays on the map. Almost none of it is day-one. The cut is sequenced, not deleted. Multiple accounts and bank/e-wallet sync stay out until a later generation — you already excluded them, and they fight the “one pot, 20 seconds” promise.

> **Build rule:** If a screen does not help someone answer “where did it go this week?” or log a spend in 20 seconds, it does not ship in v1.

---

## 1. Product thesis

Wherego is a daily capture app with a weekly story. Most finance apps are filing cabinets. This one is a pocket notebook that later turns into a comic-strip of your month.

The user is not trying to become a CFO. They feel a leak and want a sentence: “Most of it was eating out and online shopping. Groceries were fine.”

### 1.1 Positioning

- **Against spreadsheets:** no grid, no formulas, no “start a new tab for August.”
- **Against bank apps:** those show transactions, not meaning. We add category + story. We do not sync banks.
- **Against YNAB-class tools:** those win power users and lose beginners. We will look slightly “dumb” on purpose.
- **Against guilt apps:** no “you were bad today” copy. The mascot can look surprised, not disappointed.

### 1.2 Primary persona

Aria, 24–34. Pays rent, eats out, has GoPay/OVO/Shopee and a payroll account, never exported a CSV until last year. Opens finance apps for three days in January. Hates typing on a bus. Will take a photo of a receipt if it’s faster than typing. Wants IDR formatting that looks like Indonesia (`1.250.000`), not US (`1,250,000`). May travel and spend USD/SGD occasionally — hence multi-currency later, not first.

### 1.3 Jobs

| Job | When | Success |
|---|---|---|
| Capture | 30 seconds after paying | Amount + category saved, offline OK |
| Orient | Sunday night or payday | See this month vs last, by category |
| Steer | Mid-month unease | Budget bars: what is left in Food |
| Aim | After a bonus / before a trip | Goal: “Emergency 5jt” moving |
| Remember | Wifi bill week | Recurring posted or reminded |
| Leave | Tax time / breakup with the app | CSV/PDF out |

### 1.4 Explicit non-goals (until a later generation)

- Bank / e-wallet aggregation (Plaid, BCA scrape, GoPay official API). Cost, fragility, Play policy, and trust.
- Multiple accounts (cash vs bank vs card). One pot + optional “adjust balance.”
- Investments, gold, crypto lots, debt snowball engines.
- Shared wallets, split-with-roommate, ask-to-approve.
- Ads in the capture flow. Ever.

---

## 2. Feature map and what “good” means

Three layers. Only Layer 1 is the first public build.

### 2.1 Layer 1 — Capture & story (v1)

| Feature | v1 bar |
|---|---|
| Google sign-in | One tap, resume session, account picker |
| Onboarding | Name, base currency (IDR default), 6–10 starter categories, starting balance optional |
| Quick add expense/income | Numpad first. Category grid. Optional note. Save. &lt;20s |
| Edit / swipe delete | Undo snackbar 5s |
| Custom categories | Emoji + color + name. Archive, don’t hard-delete if used |
| Home | Today’s list, month spent, leftover-vs-income if any income logged |
| Month story | Category bars, “top 3 leaks”, compare to previous month |
| Offline-first | Full capture + browse with no network. Sync when back |
| Dark mode | Follow system + override |
| Playful layer | Mascot states, log streak (days you opened and saved), microcopy |

**v1 success test (your words):** you can record a spend in 20 seconds, on the train, with spotty data, without thinking.

### 2.2 Layer 2 — Steering (v1.5)

- Monthly budgets per category + one overall cap. Soft, not lock-out.
- Recurring templates (rent, Spotify, pulsa). Manual confirm first; auto-post later.
- Local notifications: “Wifi usually hits today. Log it?”
- Receipt photo attached to a transaction. OCR suggests amount + merchant note. User confirms.
- CSV export. CSV import mapped with a 3-column wizard.

### 2.3 Layer 3 — Depth (v2)

- Savings goals as earmarks of the same pot (not new accounts). “Trip 3jt — 1.2jt set aside.”
- Multi-currency transactions with rate on the day (manual or a free FX table cached weekly). Reports in base currency.
- Balance over time (“net worth lite”): starting balance + income − expense + adjustments. A line chart, not a brokerage view.
- Richer charts: weekday heatmap, merchant-ish notes cloud, “this category vs 3-month average.”
- PDF month report, pretty enough to screenshot.

### 2.4 Later generation (not scheduled)

- Household / roles.
- Multiple accounts.
- Bank sync.
- iOS / web companion.

---

## 3. UX

### 3.1 Design principles

1. Amount before everything. The first focused control is a big numpad, not a form.
2. Categories are pictures, not a spinner of 40 strings.
3. Defaults beat settings. IDR, local number format, this calendar month, last-used categories on top.
4. Playful ≠ childish clutter. One mascot, one accent motion, short copy. White space still wins.
5. Empty states teach the next tap, they do not lecture personal finance.
6. Mistakes are cheap: undo, edit, “this was income not expense.”

### 3.2 Tone

Voice: a slightly chaotic friend who is good with receipts.

- Log saved: “Parked. That one won’t vanish.”
- First of month: “New page. Old month is in Stories.”
- Over budget: “Food ate the week. Still time to coast.”
- Never: “You overspent again.” “Irresponsible.” “Only 3 days logged, do better.”

Streaks reward **capture consistency**, not frugality. A day you logged a 200rb lunch still counts. The product’s enemy is amnesia, not satay.

### 3.3 Information architecture

Four tabs. No hamburger.

| Tab | Job |
|---|---|
| Home | Today + this month pulse + FAB |
| Stories | Calendar month report, charts, comparisons |
| Plan | Budgets + goals (hidden or badged “soon” until Layer 2/3) |
| Me | Categories, recurring, export, currency, appearance, account, sign out |

Global: a fat FAB on Home (`+`). Long-press FAB = income vs expense vs scan. Default tap = expense.

### 3.4 The 20-second capture flow (sacred)

Target path, standing in a minimarket:

1. Tap FAB. Numpad is already up. Type `18000`. Big preview: **Rp 18.000**.
2. Hit a category chip. Recent 6 on row one. Full grid under “more.”
3. Optional: one-line note. Skip by default.
4. Checkmark. Haptic + tiny mascot hop. Back on Home with the row already there.

Allowed extra taps that still count as a win: flip to income; change date to yesterday; attach photo after save.

Disallowed in this path: account picker, tags, GPS, “was this necessary?”, budget lecture modal, forced note, forced category folder drill-down.

#### Scan path (Layer 2)

FAB long-press → camera. Crop. On-device OCR. If a number looks like money, prefill amount. If a header looks like a shop name, prefill note. User always confirms. Never auto-post a guessed `8.000.000`.

### 3.5 Home

- Header: mascot + “Tuesday · 8 logged this week”.
- Hero number: spent this month, in huge type. Subline: “of Rp X incoming” if income exists, else omit.
- If budgets exist: 3 thin bars (overall + top 2 categories).
- Section “Today” then “Earlier this week.” Anything older lives in Stories.
- Empty today: illustration + “First one is the hardest. Tap +”.

### 3.6 Stories

- Month switcher.
- Donut or horizontal bars by category (bars are kinder on small phones).
- Copy block generated from data: “Eating out 38% · Transport 16% · The rest is rent and quiet.”
- List of transactions grouped by day, filter chips by category.
- No 12-chart dashboard. One story, one list, one comparison.

### 3.7 Onboarding (4 screens, skippable after login)

1. Welcome + one sentence job. Google button already done on previous screen.
2. Currency + optional starting balance (“what’s in your pocket/bank together, roughly”).
3. Pick categories. Preset packs: Everyday ID, Minimal (5), Custom. Can add later.
4. Demo add: “Log something from today, even Rp 2.000 parking.” Then Home.

### 3.8 Playful system

- **Mascot:** a small round creature (working name “Go”). 6 faces: idle, happy-log, sleepy-empty, side-eye-big-spend (comic, not mean), party-under-budget, offline-scarf.
- **Motion:** 180–240ms, ease-out. No confetti cannons on every 5rb snack.
- **Color:** teal-ink + warm sand + category colors that stay accessible on dark.
- **Sound:** optional, off by default. Soft tick on save.

Do not gamify money saved. Gamify showing up.

---

## 4. Interaction details that save beginners

- Amount entry stores minor units (`Long`). UI formats with user locale. IDR has 0 decimals. JPY 0. USD 2.
- Quick amounts: chips under numpad after first use — 10rb, 15rb, 25rb, 50rb — derived from that user’s medians, not hardcoded forever.
- Yesterday / today / “pick date.” No time-of-day required. Store noon local if they only chose a date.
- Duplicate last: long-press a row → “same again.” Huge for coffee.
- Search notes in Stories, not a global search chrome on Home.
- Hard delete after undo window becomes soft-delete in DB so sync doesn’t resurrect zombies badly.

---

## 5. Data model

Room is the source of truth. Cloud is a replica. IDs are ULIDs generated on device so offline creates never collide.

### 5.1 Core tables

**users** (local profile + remote)

`id`, `googleSub`, `email`, `displayName`, `photoUrl`, `baseCurrency` (ISO 4217), `locale`, `onboardingDone`, `startingBalanceMinor`, `startingBalanceOn`, `createdAt`, `updatedAt`

**categories**

`id`, `userId`, `name`, `emoji`, `colorHex`, `kind` (`expense` | `income` | `both`), `isPreset`, `archived`, `sortOrder`, `updatedAt`, `deletedAt`

**transactions**

`id`, `userId`, `kind` (`expense` | `income` | `adjustment`), `amountMinor`, `currency`, `fxRateToBase` (BigDecimal as string), `amountBaseMinor`, `categoryId`, `note`, `occurredOn` (LocalDate), `occurredAt` (epoch optional), `recurringId` nullable, `receiptId` nullable, `createdAt`, `updatedAt`, `deletedAt`, `dirty` (pending sync)

**receipts**

`id`, `userId`, `transactionId`, `localUri`, `remotePath`, `ocrRaw`, `ocrAmountMinor`, `ocrMerchant`, `ocrConfidence`, `uploaded`

**budgets**

`id`, `userId`, `categoryId` nullable (null = monthly cap), `amountMinor`, `currency`, `yearMonth`, `rollover` (bool, default false), `updatedAt`

**recurring_rules**

`id`, `userId`, `kind`, `amountMinor`, `currency`, `categoryId`, `note`, `freq` (`weekly` | `monthly`), `interval`, `dayOfMonth` / `weekday`, `startOn`, `endOn`, `nextOn`, `remindDaysBefore`, `autoPost` (default false), `updatedAt`

**goals**

`id`, `userId`, `name`, `emoji`, `targetMinor`, `allocatedMinor`, `deadlineOn` nullable, `archived`, `updatedAt`

**fx_rates**

`base`, `quote`, `rate`, `asOf`, `source` — cached

**sync_state**

`collection`, `lastPullCursor`

### 5.2 Money rules

- Never store floating-point money. `amountMinor` is Integer/Long. Pick one convention and stick to it.
- **Recommendation:** store scale per currency (ISO 4217 minor units). IDR scale 0, USD scale 2. `amountMinor` is in that currency’s minor unit. `amountBaseMinor` always in base minor units.
- One pot. Goals allocate from this pot; they do not create a second balance unless you later add accounts.
- **The balance is anchored, not seeded.** A `reconcile` row asserts a total — "as of `occurredOn`, everything totalled `amountMinor`". Home balance = the latest anchor + every row dated after it. Rows dated *before* the anchor are already inside its number and must not be counted again: a backdated spend is history, not a new debit. Counting them twice is the bug `startingBalanceOn` was added to prevent and then never read.
- **An assertion, never a delta.** A delta is computed from one device's view (`targetMinor − currentBalance()`), so two devices replicating deltas stack them and land on a total neither one asked for. Two assertions are just two claims: the later `occurredOn` anchors, the earlier stays as history. Legacy `adjustment` rows keep their delta meaning and keep being summed — nothing new writes one.
- **Anchor collisions ask, but only when they must.** Two devices can both assert; parking before sign-in is the promise, so this is expected, not an error. The later anchor wins silently. Ask once when a pull hands the balance to a peer's anchor *and* the total moves — the arithmetic is right, but the figure the user was shown changed and the peer's claim could be the typo. Equal totals settle nothing worth asking about. Answering soft-deletes the rejected claim, so the decision syncs and every device lands on the same anchor.
- Pre-anchor rows stay in Stories and in every spend total — the point of the app is seeing where money went. Draw the anchor on the Balance sparkline instead, so the balance and the month's spend are allowed to disagree without reading as a bug.
- `user_profile.startingBalanceMinor` / `startingBalanceOn` are the pre-anchor form. Onboarding writes an anchor row instead, and migration 10→11 converts an existing scalar into one and zeroes it. The columns stay rather than being dropped: the synced profile document still carries the field for a device that has no anchor row yet, and zero is the right fallback for a profile that never set a balance.
- Month boundaries use the user’s zone (`Asia/Jakarta` default). A 23:40 transaction on the 31st does not fall into next month because the server is in UTC.
- Edits update `updatedAt` and set `dirty`. Sync is last-write-wins per row with `updatedAt`. Good enough for single-user. Do not invent CRDTs.

### 5.3 Preset categories (IDR everyday pack)

**Expense:** Food out, Groceries, Transport, Bills, Rent/Kos, Shopping, Health, Fun, Gifts, Other.

**Income:** Salary, Side hustle, Refund, Other in.

User can rename, recolor, archive. Preset ids stay stable so you can ship better icons later.

---

## 6. Tech plan

### 6.1 Stack (cost-effective, solo, Kotlin)

| Layer | Choice | Why |
|---|---|---|
| UI | Jetpack Compose + Material 3 | One UI toolkit. Animations for mascot without old XML soup. |
| Nav | Navigation Compose | Four tabs + capture sheet. |
| DI | Hilt | Boring and documented. |
| Local DB | Room + coroutines Flow | Offline source of truth. |
| Prefs | DataStore | Theme, onboarding, last category. |
| Async | Coroutines + WorkManager | Sync + reminder + receipt upload. |
| Auth | Credential Manager + Firebase Auth (Google) | You asked for Google. Free tier. |
| Cloud DB | Cloud Firestore | Offline cache exists, but Room remains SoT; Firestore is the mirror. |
| Files | Firebase Storage | Receipt images, compressed. |
| Push | FCM + local Alarm/WorkManager | Reminders work offline-ish via local first. |
| OCR | ML Kit Text Recognition v2 | On-device, free, no receipt pixels leave the phone until user saves. |
| Charts | Vico | Compose-native, no MPAndroidChart fight. |
| Images | Coil | Avatars, receipts. |
| Analytics | None at first, then Firebase Analytics only funnels | Privacy + focus. |
| Crash | Crashlytics | Solo lifesaver. |
| Build | Gradle KTS, minSdk 26, target 35+ | Covers the phones that will actually install this. |

**Module layout** so the app does not become one 8k-line God Activity:

- `:app` — nav host, theme, Application
- `:core:model` `:core:database` `:core:datastore` `:core:sync` `:core:designsystem` `:core:common`
- `:feature:auth` `:feature:capture` `:feature:home` `:feature:stories` `:feature:plan` `:feature:settings`

You do not need all modules on day one. Start `:app` + `:core:database` + `:feature:capture` + `:feature:home`. Split when a folder hurts.

### 6.2 Architecture

Unidirectional, simple: **Screen → ViewModel → UseCase/Repository → Room**. Sync engine observes dirty rows and a pull timestamp.

- `CaptureViewModel` holds draft amount, category, note. Save writes Room in one transaction, enqueues sync, pops back.
- `HomeViewModel` collects Room Flow (today, month aggregate). Never waits for network to paint.
- `SyncWorker`: push dirty → pull remote `updatedAt > lastPull` → apply. Backoff on 5xx. User-visible “cloud” dot: synced / pending / offline.
- Conflicts: single writer in practice. If two devices edit one row, `updatedAt` wins. Show “synced from your other phone” only if you later allow two devices — you will, because cloud.

### 6.3 Security & privacy (still cheap)

- Google Sign-In + Firebase Auth. Firestore rules: `request.auth.uid == resource.data.userId`.
- Storage rules: path `users/{uid}/receipts/{id}`.
- App Check when you leave internal testing.
- No need for SQLCipher in v1 given they accepted server-side amounts. Add if you ever offer local-only mode.
- Compress receipts (1080px edge, JPEG ~70) before upload.
- Privacy policy page (hosted on a free Firebase Hosting or Notion public page) before Play production.
- Play Data safety form: financial info = collected, encrypted in transit, not sold.

### 6.4 Cost envelope

Aim to live on free tiers until you have thousands of monthly actives.

| Item | When you pay |
|---|---|
| Play Console | USD 25 once |
| Firebase Auth / Firestore / Storage / FCM / Crashlytics | Spark free, then Blaze. Set a USD 5–10 budget alert day one. |
| ML Kit OCR | On-device = free |
| FX rates | A static weekly fetch from a free API, cached. Not per transaction. |
| Design | Compose + emoji categories. No paid illustration pack required. |
| Domain + policy page | Optional; Firebase Hosting free tier is enough |
| Bank APIs / Plaid | Never in this plan |

Biggest cost risk is Storage if users photograph every snack in 12MP. Cap and compress.

### 6.5 What I would not use

- KMP / Compose Multiplatform “just in case iOS.” You said Android. Don’t pay the tax.
- A custom backend on a VPS until Firestore rules or query bills actually hurt.
- Room as an afterthought with Firestore-only. Money apps that can’t add a coffee in airplane mode feel broken.
- WebView wrappers.

---

## 7. Analytics that matter (when you add them)

- `time_to_first_save` after install
- `capture_completed` (source: `manual` | `duplicate` | `scan` | `recurring`)
- `capture_abandoned_on_numpad`
- d1 / d7 open + at least one save
- `sync_fail_rate`

Do not optimize mascot clicks.

---

## 8. Roadmap

Cadence: one vertical slice you can use yourself every slice. You eat the dogfood nightly or the playful tone will be fake.

### Slice 0 — Project spine (a few evenings)

- Repo, modules, theme (light/dark), nav shell with empty tabs.
- Firebase project, SHA-1, Google Sign-In on a blank screen.
- Room empty DB + Hilt.

### Slice 1 — The 20-second loop

- Categories seeded.
- Capture sheet: numpad, format IDR, category grid, save to Room.
- Home lists today from Room.
- Edit + undo delete.

**Dogfood gate:** you log every spend for 7 days with no other tool. If you cheat to WhatsApp notes, the flow is still too heavy.

### Slice 2 — Story + identity

- Month aggregation + category bars.
- Generated one-liner.
- Onboarding + starting balance.
- Mascot idle/happy + log streak.
- Settings: theme, currency display, sign out.

### Slice 3 — Cloud

- Firestore schema + rules.
- Push/pull worker, dirty flags, conflict rule.
- Two-phone test: log offline on A, see on B after wifi.
- Crashlytics.

### Slice 4 — Layer 2 steering

- Budgets UI + home bars + stories warning copy.
- Recurring list + “due today” inbox (confirm to post).
- Local notifications.
- CSV export.

### Slice 5 — Camera

- Attach photo.
- ML Kit suggest amount/note.
- Upload worker + Storage.

### Slice 6 — Layer 3 depth

- Goals earmarks.
- Multi-currency + cached FX.
- Balance line chart.
- CSV import wizard + PDF month.

### Release channel plan

- **Internal:** you only, from Slice 1.
- **Closed testing:** 5–15 friends after Slice 3 (sync must work or you will support ghosts).
- **Open / production** after Slice 4 feels calm. Scan and goals can land as Play updates.

---

## 9. Play Store & “other people”

Shipping for others is not a feature, it is ops.

- Name + icon that reads at 48px. Avoid a generic wallet glyph.
- Short desc: “See where your money went. Log a spend in seconds.”
- Screenshots: capture sheet, home today, month story. Not a settings dump.
- Content rating, Data safety, privacy policy URL.
- Support email you actually read.
- Backup: Google cloud sync is the backup. Still offer CSV.

Do not put “bank-level security” in the listing. You are a notebook with a login.

---

## 10. Risks

| Risk | Move |
|---|---|
| Scope appetite vs solo time | Layer 1 dogfood gate is mandatory. No budgets until 7-day self-use. |
| Sync bugs = duplicated lunches | ULID + dirty flag + tests on pull merge. Never insert without client id. |
| OCR confident and wrong | Always confirm. Show the crop. Never auto-save. |
| Playful tone feels like a kids’ app | Adult palette, one mascot, copy edited cold the next morning. |
| IDR formatting bugs | Central `MoneyFormatter`. Snapshot tests. |
| Firestore bills from chatty snapshots | Room SoT; pull on interval + app start, not 12 listeners. |
| You get bored after auth + theme | Slice 1 first, pretty mascot second. |
| Users ask for BCA sync immediately | Say no in the listing. “You type or snap. That’s the product.” |

---

## 11. First week build order (concrete)

If you open Android Studio tomorrow, do this and nothing else:

1. Empty Compose app, Material 3 dynamic color off (lock a teal/sand theme so playful doesn’t become random).
2. Capture bottom sheet with a working numpad and Rp formatting. No DB yet. Log to logcat.
3. Room `Transaction` + `Category`. Save and list on Home.
4. Seed 10 categories with emoji.
5. Use it for your own week. Keep a note of every time the flow exceeded 20 seconds and why.

Auth can wait until the loop is addictive. A local “guest profile” that later attaches to Google is allowed if it keeps you moving — but design the User id from day one so you don’t rewrite the world.

---

## 12. Open choices (small, answer later)

- **Final name:** Wherego is a placeholder. Check Play for collisions.
- **Mascot:** blob vs tiny fish vs coin-with-legs. Pick after Slice 1.
- **Default language:** English UI with IDR defaults, or Indonesian UI first? Recommend: English resources + an `in-ID` copy file so you can add Bahasa without a rewrite. Ship the language you write faster.
- **Guest mode vs force Google before first save.** Recommend: try capture first, sign in when they hit a second device or a “backup” banner.
- **Adjustments:** a hidden “set balance to…” in Me, so the one-pot number can be reconciled to real life without fake income.

---

## 13. Definition of done

### v1 is done when

- You log a real minimarket spend in under 20 seconds, offline, and it appears on Home.
- A month of your own data produces a category story that you didn’t have to explain to yourself.
- A fresh install on another phone, Google Sign-In, data comes back.
- Dark mode is not broken on the capture sheet.
- No crash on first-run path (Crashlytics quiet for a week of your use).

### The product is “good” when

- You stop keeping a parallel note in WhatsApp or Photos.
- One friend who hates spreadsheets logs for 7 days without you standing over them.

---

*Next useful artifact if you want it: a Compose capture-sheet wireframe spec (every tap, every state) or a Room + Firestore schema file ready to paste into Android Studio.*
