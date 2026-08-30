# Wherego Design System

**Source of truth for layout, type, shape, copy:** the Pencil HTML mockup `Home + Capture` (412×915).
**Source of truth for color:** this spec (section 3) and `WheregoColors`. Do not follow the HTML mockup for hex.
Do not invent a different look. Do not use default Material 3 purple/teal.

If HTML and this spec disagree on a pixel value that is not color, **follow the HTML**.
If this spec and the product plan disagree on visuals, **follow this spec**.
If this spec and the product plan disagree on *behavior scope*, follow the playbook slices (budget bars may be drawn as a static shell in S1, wired in S4).

---

## 1. Personality

Chunky notebook. Cool paper. Fat ink outlines. Soft pills. Rounded like a sticker sheet.

- Playful, not babyish
- High contrast ink on paper
- One accent motion at a time
- Copy is short and human (`Park it`, not `Submit`)

---

## 2. Frame

| Token | Value |
|---|---|
| Device frame in mockup | 412 × 915 |
| Horizontal page padding | 18dp |
| Content stack gap | 12dp |
| Status bar height (ignore in-app) | 46dp — use system bars |
| Bottom capture sheet | overlay + scrim, not a new Activity |

Compose: draw Home full-bleed paper. Capture is a `ModalBottomSheet` / custom sheet anchored to the bottom. No extra top app bar on Home.

---

## 3. Color tokens

Name these in `core/designsystem`. Use exact hex.

Family: cobalt + cool off-white. One accent (`teal` token, hex is cobalt). Categories collapse to that accent plus an alarm. Emoji and label distinguish categories, not hue.

| Token | Hex | Use |
|---|---|---|
| `paper` | `#F2F4F8` | App background |
| `ink` | `#121826` | Text, icons, borders |
| `muted` | `#5A6A80` | Secondary text, inactive chips |
| `white` | `#FFFFFF` | Cards, selected income tab, sheet |
| `mascotFill` | `#E2EAF8` | Go avatar + streak pill fill |
| `teal` | `#2157C7` | Primary CTA, expense tab selected, Today icon, selected chip |
| `tealDeep` | `#163A8A` | “left” text, Today label |
| `tealSoft` | `#D7E3F8` | Left-amount pill, idle chips, badges |
| `coral` | `#E24B4B` | Over-budget, flame, alarm |
| `peach` | `#F4D6D6` | Alarm-soft (more-spend pill) |
| `blue` | `#2157C7` | Alias of accent (legacy name) |
| `blueSoft` | `#D7E3F8` | Alias of tealSoft |
| `greenSoft` | `#D7E3F8` | Alias of tealSoft |
| `violet` | `#2157C7` | Alias of accent |
| `violetSoft` | `#D7E3F8` | Alias of tealSoft |
| `pinkSoft` | `#D7E3F8` | Alias of tealSoft |
| `track` | `#E1E7F0` | Budget track, hairline borders |
| `chipIdle` | `#E8EDF4` | Quick-amount chips |
| `key` | `#EEF2F7` | Numpad keys |
| `noteChip` | `#F6F8FB` | Add-note chip fill |
| `sheet` | `#FFFFFF` | Capture sheet background |

**Border recipe:** almost every “important” shape uses `2.5dp solid ink` (`#121826`). Idle chips use `2dp solid` in their own fill color (looks borderless) or `2dp solid #E1E7F0`.

**Dark mode:** paper `#10141C`, cards `#1A2230`, ink `#E8EEF6`, muted `#8B9BB0`, track `#2A3444`, accent `#4B86FF`, tealDeep `#8FB0FF`, tealSoft `#1A2F55`, coral `#FF6B6B`. Do not ship an auto-generated M3 dark that breaks the sticker look.

---

## 4. Type

Bundle or download:

- **Fredoka** (wght 500–700) — display, numbers, headings, keypad
- **Nunito Sans** (wght 600–800) — UI labels, meta, chips

| Style | Font | Size | Weight | Color | Use |
|---|---|---|---|---|---|
| Greeting | Fredoka | 19 | Semibold 600 | ink | `Hey Aria 👋` |
| Meta | Nunito Sans | 13 | Semibold 600 | muted | `Tuesday · 8 logged this week` |
| Streak num | Fredoka | 15 | Semibold 600 | ink | `12` |
| Eyebrow | Nunito Sans | 13 | Bold 700 | muted | `SPENT THIS MONTH` tracking +0.6 |
| Hero amount | Fredoka | 44 / line 53 | Semibold 600 | ink | `Rp 3.482.500` |
| Hero meta | Nunito Sans | 13 | Semibold 600 | muted | `of Rp 6.500.000 in` |
| Left pill | Nunito Sans | 12 | Bold 700 | tealDeep | `Rp 3.017.500 left` |
| Card title | Fredoka | 17 | Semibold 600 | ink | `Budget check`, `Today` |
| Link | Nunito Sans | 13 | Bold 700 | teal | `Plan →` |
| Cat name | Nunito Sans | 14 | Bold 700 | ink | Budget row |
| Budget note | Nunito Sans | 13 | Bold 700 | coral / tealDeep | `Rp 190rb over` |
| Tx title | Nunito Sans | 15 | Bold 800 | ink | `Warteg Bahari` |
| Tx sub | Nunito Sans | 12 | Semibold 600 | muted | `12:40 · Food out` |
| Tx amount | Fredoka | 16 | Semibold 600 | ink | `Rp 28.000` |
| Tab label | Fredoka | 15 | Semibold 600 | white on selected | Expense / Income |
| Amount huge | Fredoka | 52 | Semibold 600 | ink | `18.000` |
| Currency prefix | Fredoka | 28 | Semibold 600 | muted | `Rp` |
| Key | Fredoka | 24 (000 = 20) | Medium 500 | ink | Numpad |
| CTA | Fredoka | 19 | Semibold 600 | white | `Park it` |

Android: add both families via Downloadable Fonts *or* `res/font`. Do not substitute Roboto / default M3 Display.

IDR format in UI: `Rp 3.482.500` (dot thousands, no decimals). Compact budget notes may use `190rb`.

---

## 5. Shape

| Shape | Radius |
|---|---|
| Go avatar | 27dp (circle, 54×54) |
| Pills (streak, chips, left amount) | 99dp |
| Budget / Today cards | 28dp |
| Capture sheet top | 36dp |
| Kind toggle container | 18dp |
| Numpad key | 18dp |
| Save button | 20dp |
| Tx emoji badge | 16dp (44×44) |
| Grabber | 3×42, radius 99 |

---

## 6. Elevation & outline

No Material shadows on cards. Depth = **ink outline**.

- Cards, avatar, streak, selected chip, sheet, save button: `BorderStroke(2.5.dp, ink)`
- Idle category chips: fill only, `BorderStroke(2.dp, same as fill)`
- Note chip: `BorderStroke(2.dp, track)`
- Grabber: fill `#E1E7F0`, no stroke

Scrim behind sheet: black ~40% (`#000000` alpha 0.4). Home stays visible and slightly dimmed.

---

## 7. Iconography

- Lucide-style stroke icons at 15–22dp, tinted ink / teal / muted / coral
- Categories = emoji, not vector sets
- Go mascot in this mockup = coin emoji `🪙` inside the 54dp circle. Keep that until custom art exists.
- Flame in streak pill is coral `#E24B4B`

---

## 8. Component specs

### 8.1 Go avatar

- 54×54, fill `mascotFill`, stroke 2.5 ink, circle
- Content: `🪙` 26sp
- After successful save: swap to a grin emoji for 800ms then back (happy state). No Lottie required in S1.

### 8.2 Streak pill

- Padding 7×12, gap 5
- Fill `mascotFill`, stroke 2 ink, pill
- Flame 16 + Fredoka 15 number
- Number = distinct days logged (see playbook). Mock shows `12`.

### 8.3 Hero

- Label 13 muted bold + 0.6 tracking, **sentence case in mockup:** `Spent this month` (not all-caps visually except tracking). Match mockup: `Spent this month`.
- Amount 44/53 Fredoka, one line. Shrink to 36 if overflow.
- Meta row: `of Rp X in` + tealSoft pill `Rp Y left`
- If no income logged this month: hide `of … in` and the left pill.

### 8.4 Budget card

- White, 28 radius, 2.5 ink, padding 18, gap 15
- Header: `Budget check` + `Plan →` (navigates to Plan tab)
- Max **3** category rows
- Track height 13, radius 99, fill `track`
- Fill bar uses accent; if over budget, coral and width = 100%
- Right label: `Rp Xrb left` (tealDeep) or `Rp Xrb over` (coral)

S1/S2: may hardcode empty state `No budgets yet · set them in Plan` inside the same card chrome. Do not skip the chrome if you want visual parity; do not fake over-budget data.

### 8.5 Today list

- Header: `Today` Fredoka 17 + `Rp 148.000` muted 13 (sum of today expenses)
- Rows: 44 emoji badge + title + `HH:mm · Category` + amount
- Badge fill = tealSoft
- Divider: none in mockup; use 12–14 vertical gap, no hairlines
- Mock rows:
  - 🍜 Warteg Bahari · 12:40 · Food out · Rp 28.000
  - 🚕 Gojek to office · 07:55 · Transport · Rp 22.000

Tap row → edit in capture sheet. Swipe delete + undo still required (not drawn; keep).

### 8.6 Capture sheet

Structure top → bottom, padding 16–18, sheet white, top corners 36:

1. Grabber
2. Kind toggle (Expense | Income)
3. Amount display (`Rp` + digits + coral caret)
4. Quick row (Today, 10rb, 15rb, 25rb, note)
5. Category row (horizontal chips + more)
6. Numpad 4×3
7. Save `Park it`

Selected kind = teal fill + white Fredoka text + 2.5 ink. Unselected = white text-ink, no extra chrome.

Amount:

- Prefix `Rp` 28 muted
- Digits 52 ink
- Caret 3×44 coral, blink 600ms
- Raw digit buffer; `000` appends three zeros
- Max 12 digits

Quick chips:

- Today = selected state: tealSoft fill, tealDeep label + calendar icon
- 10rb / 15rb / 25rb set amount to 10000 / 15000 / 25000 (replace, don’t append)
- After the user has history, you may swap these for their median chips — visual stays identical
- `note` chip opens a single-line field above the numpad (not in mockup; slide a 44dp field in)

Category chips:

- Selected: accent `#2157C7` + 2.5 ink + **white** label
- Idle: tealSoft fill, ink label, 2dp same-as-fill border
- Height ~40, padding 8×14, gap 8
- Trailing `⋯` more button 44×40, 2dp track border, opens full grid sheet

Numpad:

- Key 120×50 on 412 frame ≈ `weight(1f)` height 50, radius 18, fill `key`, **no border**
- Gap ~9 vertical, space-between horizontal
- Keys: 1–9, `000`, `0`, backspace icon 22 ink

Save:

- Height 56, radius 20, fill teal `#2157C7`, stroke 2.5 ink
- Label `Park it` + check 21 white
- Disabled (amount 0 or no category): fill `#D7E3F8`, text `#5A6A80`, still stroked or unstroked — prefer unstroked + 60% alpha, not a toast

Long-press FAB is not in this mockup because the sheet *is* the add path. Home still needs a way to open the sheet: **a 64dp teal ink-outlined FAB** bottom-end above system nav, `+`. Not drawn in the cropped mock (sheet is open). Add it on Home when sheet is closed.

---

## 9. Home layout when sheet is closed

Same header + hero + budget card + Today list.
Then:

- Optional “Earlier this week” using the same row component
- FAB `+`
- No bottom tab bar in the mockup. **Product still has 4 destinations.** Resolve:

**Decision (locked):** use a floating 4-tab bar *or* a simple bottom bar that does **not** look like M3 NavigationBar.

Implement a custom bar:

- Height 64, paper, no shadow
- 4 items: Home, Stories, Plan, Me
- Active: accent dot + Fredoka 11 tealDeep
- Inactive: muted
- Icons Lucide-style, 22dp
- FAB sits 12dp above the bar, end-aligned (18dp from side)

Do not cover Today rows. List is the scroll surface; header + hero + budget can scroll away.

---

## 10. Motion

| Event | Motion |
|---|---|
| Open capture | Sheet spring, 280ms, from bottom |
| Save | Sheet dismiss + home row insert 180ms + Go hop 200ms |
| Key press | Scale 0.96, 80ms |
| Caret | Blink |
| Over-budget bar | Fill width animate 300ms |

No confetti on `Park it`.

---

## 11. Copy locked by this mock

| Place | Copy |
|---|---|
| Greeting | `Hey {firstName} 👋` fallback `Hey you 👋` |
| Sub | `{Weekday} · {n} logged this week` |
| Hero label | `Spent this month` |
| Income sub | `of Rp {income} in` |
| Remainder pill | `Rp {left} left` |
| Budget title | `Budget check` |
| Budget link | `Plan →` |
| Over | `Rp {n}rb over` |
| Under | `Rp {n}rb left` |
| Today header | `Today` |
| Expense tab | `Expense` |
| Income tab | `Income` |
| Date chip | `Today` |
| Note chip | `note` |
| Save | `Park it` |

Do not use `Submit`, `Save transaction`, `Add expense`.

---

## 12. Category colors

Idle = `tealSoft` `#D7E3F8`. Selected / meter = `teal` `#2157C7`. Over / alarm = `coral` `#E24B4B`.
Categories are distinguished by emoji + label, not by hue.

| Category | Emoji (mock / pack) | Soft | Strong |
|---|---|---|---|
| Food out | 🍜 | `#D7E3F8` | `#2157C7` |
| Transport | 🚕 (mock wins over 🛵) | `#D7E3F8` | `#2157C7` |
| Groceries | 🛒 | `#D7E3F8` | `#2157C7` |
| Fun | 🎬 | `#D7E3F8` | `#2157C7` |
| Shopping | 🛍️ | `#D7E3F8` | `#2157C7` |
| Bills | 📄 | `#D7E3F8` | `#2157C7` |
| Rent & bills | 🏠 | `#D7E3F8` | `#2157C7` |
| Health | 💊 | `#D7E3F8` | `#2157C7` |
| Gifts | 🎁 | `#D7E3F8` | `#2157C7` |
| Other | ✨ | `#D7E3F8` | `#2157C7` |
| Salary | 💼 | `#D7E3F8` | `#2157C7` |
| Side hustle | 🛠️ | `#D7E3F8` | `#2157C7` |
| Refund | ↩️ | `#D7E3F8` | `#2157C7` |
| Other in | ✨ | `#D7E3F8` | `#2157C7` |

Update seed pack: Transport emoji `🚕`, Fun `🎬` to match HTML. `pencil-new.pen` →
`Onboarding 3 · Categories` renames Rent/Kos to **Rent & bills** (amber soft, `🏠`) and
swaps Other's `📦` for `✨`; `sortOrder` follows the chip order on that screen, with
`Bills` last since the pack no longer preselects it.

---

## 13. What an implementer must copy exactly

From the HTML, these are non-negotiable (except color, which follows section 3):

1. Paper page `#F2F4F8`, ink `#121826`, accent `#2157C7`
2. Fredoka + Nunito Sans
3. Fat 2.5 ink borders on cards, avatar, selected chip, save
4. Hero 44sp amount
5. Capture: kind toggle → huge amount → chips → category scroller → numpad with **000** → `Park it`
6. `Park it` accent button with ink outline
7. Go = coin-in-circle, not a 3D character

---

## 14. Agent freeze additions

Add to the playbook freeze list:

- Design source = this file + the HTML mockup
- Theme is **custom**, not stock M3
- Dynamic color **off**
- Fonts Fredoka + Nunito Sans
- Primary CTA copy = `Park it`
- Numpad includes `000`
- Transport emoji `🚕`, Fun `🎬`
- No extra capture fields beyond mock + the hidden note field

`core/designsystem` must expose:

```text
WheregoTheme
WheregoColors
WheregoType
WheregoShapes
WheregoButtons.ParkIt
WheregoChips.Category
WheregoChips.Quick
WheregoNumpad
WheregoHero
WheregoGoAvatar
WheregoStreakPill
WheregoBudgetCard
WheregoTxRow
```

S0 builds theme + empty Home chrome (header + hero placeholders + tab bar).
S1 fills capture + today rows using these components.
