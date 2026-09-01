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

Name these in `core/designsystem`. Use exact hex. Components read **roles**, never hex; a mode is one
`WheregoColors` instance swap in `WheregoTheme`.

Family: cobalt + cool off-white. One accent (`teal` token, hex is cobalt). Categories collapse to that accent plus an alarm. Emoji and label distinguish categories, not hue.

### 3.1 Light (the reference)

| Token | Hex | Use |
|---|---|---|
| `paper` | `#F2F4F8` | App background |
| `white` | `#FFFFFF` | Card plane. A **surface**, never an on-accent colour |
| `sheet` | `#FFFFFF` | Capture sheet background |
| `noteChip` | `#F6F8FB` | Add-note chip fill |
| `key` | `#EEF2F7` | Numpad keys |
| `chipIdle` | `#E8EDF4` | Quick-amount chips |
| `mascotFill` | `#E2EAF8` | Go avatar + streak pill fill |
| `track` | `#E1E7F0` | Budget track, hairline borders |
| `divider` | `#E8EDF4` | Setting-row divider |
| `ink` | `#121826` | Text and icons **only** |
| `muted` | `#5A6A80` | Secondary text, inactive chips |
| `outline` | `#121826` | 2.5dp contour on paper / card / soft-tint surfaces |
| `outlineStrong` | `#121826` | Contour on **bright** fills: accent buttons, FAB, category swatches |
| `shadow` | `#121826` | Hard-shadow slab. Equals `outline` by design |
| `teal` | `#2157C7` | Accent **fill**: primary CTA, FAB, selected tab/chip, budget bar |
| `accentText` | `#2157C7` | Accent **content**: links, accent icons, chart strokes |
| `onAccent` | `#FFFFFF` | Text/icons on a `teal` fill |
| `tealDeep` | `#163A8A` | Text on `tealSoft`: “left” amount, Today label, active tab |
| `tealSoft` | `#D7E3F8` | Left-amount pill, idle chips, badges |
| `coral` | `#E24B4B` | Over-budget, flame, alarm |
| `onAlarm` | `#FFFFFF` | Text/icons on a `coral` fill (swipe-to-delete) |
| `peach` | `#F4D6D6` | Alarm-soft (more-spend pill, archive) |
| `capFill` / `capTrack` / `capLabel` | `#163A8A` / `#102A66` / `#D7E3F8` | Plan cap hero slab |
| `blue` `blueSoft` `green` `greenSoft` `onGreenSoft` `violet` `violetSoft` `amber` `amberSoft` `pink` `pinkSoft` | accent / `tealSoft` / `tealDeep` / `coral` | Legacy hue aliases |

**Border recipe:** almost every “important” shape uses `2.5dp solid outline`. Bright-filled shapes
use `outlineStrong`. Idle chips use `2dp solid` in their own fill color (looks borderless) or
`2dp solid track`.

**`ink` is text, `outline` is the contour.** They share one hex in light mode. Do not collapse them
back into one token: the split is the only reason dark mode can lift the contour without lifting
body text.

### 3.2 Dark

Built in OKLCH at hue ~262, chroma-clamped into sRGB. Every text pair clears WCAG AA (worst case
4.65:1); every contour clears 3:1 against `paper` and `card`. The surface ladder mirrors light
mode's six steps away from the card plane, scaled 1.35x because a dark surface has no ambient
shading cue to help it separate.

| Token | Hex | Note |
|---|---|---|
| `paper` | `#060B14` | Cool off-black, not `#000` |
| `white` / `sheet` | `#1C232F` | Card plane, lifted above paper |
| `noteChip` | `#242A36` | ↑ ladder step 1 |
| `key` | `#29303C` | ↑ step 2 |
| `chipIdle` | `#2E3643` | ↑ step 3 |
| `mascotFill` | `#243450` | ↑ step 4, accent-tinted |
| `track` / `divider` | `#343C49` | ↑ step 5 |
| `ink` | `#E5ECF5` | 13.3:1 on card |
| `muted` | `#9BA9BB` | 6.2:1 on card, 4.7:1 on track |
| `outline` | `#727B8A` | Mid-slate. 3.7:1 on card, 4.6:1 on paper |
| `outlineStrong` | `#010E32` | Deep navy rim, 4.0:1 on the accent fill |
| `shadow` | `#727B8A` | Same as `outline`, as in light mode |
| `teal` | `#386FDC` | Accent fill; holds `onAccent` at 4.7:1 |
| `accentText` | `#71A3FF` | 7.4:1 on paper, 6.0:1 on card |
| `onAccent` | `#FFFFFF` | |
| `tealDeep` | `#91B7FE` | 5.8:1 on `tealSoft` |
| `tealSoft` | `#25385D` | |
| `coral` | `#FC746F` | 5.9:1 on card |
| `onAlarm` | `#240204` | Near-black on the bright coral, 7.3:1 |
| `peach` | `#5B2126` | |
| `capFill` / `capTrack` / `capLabel` | `#092866` / `#000B34` / `#A4BEEF` | |

Three rules keep the sticker look intact on a dark paper:

1. **The contour goes mid-slate, not near-white.** Inverting `ink` to `#E5ECF5` for the border turns
   every card into a glowing wireframe. A mid-tone stroke reads as a drawn edge in both modes.
2. **The hard slab follows the contour.** A shadow cannot be darker than a near-black paper, so it
   becomes a visible offset silhouette at the contour's tone. Do not tint it near-black: at 1.07:1
   against the paper the depth cue disappears entirely.
3. **The accent splits by role.** `teal` is a fill dark enough to hold white text; `accentText` is
   a brighter tone for links and icons. One token cannot do both above 4.5:1.

Do not ship an auto-generated M3 dark, and do not build the dark scheme from light constants —
reading `WheregoColors.Light` inside `darkColorScheme` is what put `#2157C7` primary (2.5:1) and
`#5A6A80` on-surface-variant (2.9:1) on a near-black surface.

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

No Material shadows on cards. Depth = **contour + hard slab**, never `Modifier.shadow()`.

- Cards, avatar, streak, selected chip, sheet, month pill: `BorderStroke(2.5.dp, outline)`
- Accent-filled shapes (FAB, save button, selected kind tab, category swatch): `BorderStroke(2.5.dp, outlineStrong)`
- Idle category chips: fill only, `BorderStroke(2.dp, same as fill)`
- Note chip: `BorderStroke(2.dp, track)`
- Grabber: fill `track`, no stroke
- Hard slab: `Modifier.wheregoHardShadow(shape, colors.shadow, offsetY = 4–5.dp)`, applied **before**
  `clip`/`background`. Solid offset copy of the shape, no blur, no spread.

Scrim behind sheet: black ~40% (`#000000` alpha 0.4). Home stays visible and slightly dimmed.

---

## 7. Iconography

- Lucide-style stroke icons at 15–22dp, tinted ink / teal / muted / coral
- Categories = emoji, not vector sets
- Go mascot mark = Waypoint Coin (minted token with 45° directional compass rose & coral pivot pin).
- Flame in streak pill is coral `#E24B4B`

---

## 8. Component specs

### 8.1 Go avatar

- 54×54, fill `mascotFill`, stroke 2.5 `outline`, circle
- Content: `WheregoWaypointMark` (or `😄` reaction for 800ms after save)
- After successful save: swap to a grin emoji for 800ms then back (happy state). No Lottie required in S1.

### 8.2 Streak pill

- Padding 7×12, gap 5
- Fill `mascotFill`, stroke 2 `outline`, pill
- Flame 16 + Fredoka 15 number
- Number = distinct days logged (see playbook). Mock shows `12`.

### 8.3 Hero

- Label 13 muted bold + 0.6 tracking, **sentence case in mockup:** `Spent this month` (not all-caps visually except tracking). Match mockup: `Spent this month`.
- Amount 44/53 Fredoka, one line. Shrink to 36 if overflow.
- Meta row: `of Rp X in` + tealSoft pill `Rp Y left`
- If no income logged this month: hide `of … in` and the left pill.
- Trailing the label row, an 18 `muted` eye (`Visibility` / `VisibilityOff`) when the caller passes
  `onToggleAmounts`. Flips the device-local `Hide amounts` guard, which renders every amount on the
  browsing surfaces as `••••••`. Amounts arrive already masked — the caller wraps them in
  `displayAmount(...)`; the hero only picks the icon. Live numpad entry is never masked.

### 8.4 Budget card

- `white` fill, 28 radius, 2.5 `outline`, padding 18, gap 15
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

Selected kind = `teal` fill + `onAccent` Fredoka text + 2.5 `outlineStrong`. Unselected = `sheet` fill, `ink` label, no extra chrome.

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

- Selected: `teal` fill + 2.5 `outlineStrong` + **`onAccent`** label
- Idle: `tealSoft` fill, `ink` label, 2dp same-as-fill border
- Height ~40, padding 8×14, gap 8
- Trailing `⋯` more button 44×40, 2dp track border, opens full grid sheet

Numpad:

- Key 120×50 on 412 frame ≈ `weight(1f)` height 50, radius 18, fill `key`, **no border**
- Gap ~9 vertical, space-between horizontal
- Keys: 1–9, `000`, `0`, backspace icon 22 ink

Save:

- Height 56, radius 20, fill `teal`, stroke 2.5 `outlineStrong`
- Label `Park it` + check 21 `onAccent`
- Disabled (amount 0 or no category): fill `tealSoft` at 60% alpha, text `muted`, unstroked — not a toast

Long-press FAB is not in this mockup because the sheet *is* the add path. Home still needs a way to open the sheet: **a 64dp `teal` FAB stroked 2.5 `outlineStrong`** bottom-end above system nav, `+`. Not drawn in the cropped mock (sheet is open). Add it on Home when sheet is closed.

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
3. Fat 2.5 `outline` borders on cards, avatar, selected chip, save (`outlineStrong` on accent fills)
4. Hero 44sp amount
5. Capture: kind toggle → huge amount → chips → category scroller → numpad with **000** → `Park it`
6. `Park it` accent button with an `outlineStrong` contour
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
