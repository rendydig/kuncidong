# Design tokens — kunci dong (BounceBox on Android)

The single source of truth for the app's visual language, translated from
[BounceBox](bouncebox.md) to Android. Everything here lives in `app/src/main/res/values/`
(`colors.xml`, `dimens.xml`, `themes.xml`) plus `res/font/` and `res/drawable/`. If a screen needs
a value that is not on this page, add a token here rather than hardcoding it.

## Theme

- **Light-only by design.** `Base.Theme.Customlockscreen` parents `Theme.Material3.Light` and sets
  `android:forceDarkAllowed=false`; `SmartLockApplication.onCreate()` calls
  `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`.
- There is deliberately **no** `res/values-night/` directory. The app never renders a dark variant.
- The base theme sets `fontFamily=@font/poppins`, so every widget inherits Poppins without per-view work.
- `Theme.Customlockscreen.LockScreen` is the fullscreen lock-screen theme.
- The page background is the **pastel gradient** (`@drawable/gradient_background`), not BounceBox's flat
  white. This is a deliberate, kid-identity exception — see "Documented exceptions".

## Colour tokens (`colors.xml`)

| Token | Value | BounceBox role | Use |
| --- | --- | --- | --- |
| `white` | `#FFFFFFFF` | — | Text/icons on a coloured surface |
| `content_primary` | `#2D2D2D` | content-primary | Body text, headings |
| `content_secondary` | `#6B7280` | content-secondary | Supporting text |
| `content_hint` | `#A1A1AA` | content-hint | Placeholder / meta text |
| `primary` | `#FF6B6B` | Color Primary (coral) | Primary actions, brand accents |
| `primary_dark` | `#E85D5D` | Primary hover | Pressed primary |
| `primary_container` | `#FFE3E3` | coral ~20% | Primary tint fills |
| `primary_text` | `#B03A3A` | coral darkened | Brand copy on a coral tint (4.94:1 on `primary_container`) |
| `primary_alpha_15` / `_20` / `_40` | `#26/33/66FF6B6B` | coral 15/20/40% | Focus rings, chip borders |
| `secondary` | `#4ECDC4` | Color Secondary (teal) | Navigation, supporting accents, checked state |
| `secondary_dark` | `#3DBEB5` | Secondary hover | Pressed secondary |
| `secondary_container` | `#E4F8F6` | teal ~15% | Secondary tint fills |
| `secondary_text` | `#1F7A72` | teal darkened | Brand copy on a teal tint (4.66:1 on `secondary_container`) |
| `tertiary` | `#FFE66D` | Color Tertiary (sunshine) | Rewards, stars, celebration |
| `tertiary_dark` | `#F5D84A` | — | Pressed tertiary (derived) |
| `tertiary_container` | `#FFF9DB` | — | Tertiary tint fills (derived) |
| `success` | `#4ECDC4` | Color Success | Correct / completed fills |
| `success_container` / `success_text` | `#D1FAE5` / `#047857` | Status chip success | Success status chip |
| `warning` | `#FFE66D` | Color Warning | Hints, gentle alerts (as a fill) |
| `warning_container` / `warning_text` | `#FEF9C3` / `#854D0E` | Status chip warning | Warning chip + warning copy |
| `error` | `#FF6B6B` | Color Error | Wrong answer fills |
| `error_container` / `error_text` | `#FEE2E2` / `#991B1B` | Status chip error | Error chip + error copy |
| `info` / `info_container` | `#60A5FA` / `#E3F0FF` | Color Info | New / tips |
| `destructive` / `destructive_dark` | `#EF4444` / `#DC2626` | Destructive button | Delete / remove actions |
| `surface_base` | `#FFFFFFFF` | Surface Base | Card and sheet backgrounds |
| `surface_muted` | `#F9FAFB` | Disabled bg | Inset panels, question box |
| `surface_error` | `#FFF5F5` | Error input bg | Error input background |
| `list_hover` | `#FFF5F5` | List hover | List row hover tint |
| `answer_tile` | `#FFE3E3` | coral 20% | Quiz answer tiles, pairing tile |
| `lock_screen_background` | `#FFF5F5` | — | Lock-screen window background |
| `card_border` | `#E5E7EB` | Card border | Card and input outlines |
| `input_border` / `input_border_hover` | `#E5E7EB` / `#A1A1AA` | Input border | Input outlines |
| `tooltip_bg` | `#2D2D2D` | Tooltip bg | Tooltip surface |
| `gradient_start` / `_center` / `_end` | `#FFE9E9` / `#E4F8F6` / `#FFF9DB` | — | Pastel coral→teal→yellow page gradient |
| `category_math` | `#FFE66D` | tertiary | Category badge |
| `category_science` | `#4ECDC4` | secondary | Category badge |
| `category_geography` | `#60A5FA` | info | Category badge |
| `category_history` | `#FF6B6B` | primary | Category badge |
| `category_language` | `#A78BFA` | — | Category badge (added hue) |

Rules: colour is defined only here. Layouts and drawables reference tokens; they never contain a raw
`#RRGGBB`. `@android:color/white` is not used in layouts.

The palette is the **complete** BounceBox set, so a few roles (`info`, `info_container`,
`warning_container`, `error_container`, `tertiary_container`, `list_hover`, `primary_alpha_15/_40`)
are defined for the system but not referenced by any screen yet. That is expected — a token is only a
finding if it has neither a reference nor a BounceBox role.

**Text on colour.** Coral, teal and yellow fills are light, so white text on them fails contrast. Use
dark `content_primary` text on brand-coloured fills (answer tiles, category badges), and the `*_text`
tokens (`success_text`, `warning_text`, `error_text`) for coloured status copy on white.

When copy has to stay brand-coloured on a brand tint — a card subtitle, say — use `primary_text` /
`secondary_text`. The raw `primary` / `secondary` on their own container is only ~2:1 and reads as
invisible; the `*_dark` variants are ~2.8:1 / ~2.1:1, still short of AA. `primary_text` / `secondary_text`
keep the coral and teal hues and clear 4.5:1 on `primary_container` / `secondary_container`.

## Type scale (`dimens.xml` → `TextAppearance.Customlockscreen.*`)

| Role | Size | Font | BounceBox | Use |
| --- | --- | --- | --- | --- |
| `text_display` | 48sp | Titan One | h1 | Hero/brand titles only |
| `text_title` | 28sp | Titan One | h3 | Screen headers, card titles, question text |
| `text_heading` | 22sp | Titan One | h4 | Section headings, answer text, list-row titles |
| `text_body` | 18sp | Poppins | body | Body copy, descriptions |
| `text_label` | 16sp | Poppins | small | Captions, meta, status lines |
| `text_xs` | 14sp | Poppins (bold) | xs | Badges |

Headlines are **Titan One**; body copy is **Poppins** (bundled TTFs, weights 400/600/700). Roboto Mono
is part of BounceBox but unused in this app.

**Icon scale** — Material Symbols vector drawables, sized in dp:

| Token | Size |
| --- | --- |
| `icon_hero` | 48dp |
| `icon_xl` | 40dp |
| `icon_lg` | 24dp |
| `icon_md` | 20dp |
| `icon_sm` | 16dp |

How to apply: `android:textAppearance="@style/TextAppearance.Customlockscreen.<Role>"` on
`TextView`/`MaterialTextView`; `android:textSize="@dimen/text_*"` on `MaterialButton`, `Button`,
`EditText`, `RadioButton` and `CheckBox` (Material buttons own their text appearance). Programmatic
views use `setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_*))`.

## Icons

Google **Material Symbols (Rounded)** ship as vector drawables in `res/drawable/ic_*.xml`
(`ic_<symbol>.xml`), sourced from `github.com/google/material-design-icons`
(`symbols/android/<name>/materialsymbolsrounded/<name>_24px.xml`) and licensed Apache-2.0 — see
`MaterialSymbols-Apache-2.0.txt`. The app has **no icon font and no icon-library dependency**.

- Size with `@dimen/icon_*`. The vectors are 24dp on a 960 viewport, so they scale cleanly.
- Tint from a colour token: `app:tint` on an `ImageView`, `app:drawableTint` on a labelled view's
  `drawableStart`, or `app:iconTint` on a button's `app:icon`.
- Placement: a standalone `ImageView` for a lone icon; `app:icon` + `app:iconGravity="textStart"` for
  a button; `android:drawableStart` + `android:drawablePadding` for a titled row.
- Category icons live in `CategoryVisuals` (`data/CategoryVisuals.kt`), shared by the adapters.
- Every icon is either described (`contentDescription`) or `android:importantForAccessibility="no"`
  when decorative.

## Shape and spacing (`dimens.xml`)

Radii — BounceBox's three steps:

| Token | Value | BounceBox | Use |
| --- | --- | --- | --- |
| `radius_md` | 16dp | radius-md | Inputs, badges, answer tiles, smaller elements |
| `radius_lg` | 24dp | radius-lg | Cards, panels, dialogs, lock-screen panels |
| `radius_pill` | 100dp | radius-pill | Buttons, chips, badges, pills |

Spacing — BounceBox's 8px-based scale: `space_xs` 4dp, `space_sm` 8dp, `space_md` 16dp, `space_lg` 24dp,
`space_xl` 32dp, `space_2xl` 48dp, `space_3xl` 64dp. Paddings and margins come from this scale.
Component sizes (`layout_height`, icon dimensions) are not spacing and may stay literal.

Elevation — `elevation_sm` 2dp, `elevation_md` 4dp, `elevation_lg` 8dp, mapping BounceBox shadow-sm/md/lg.

## Component styles (`themes.xml`)

- **Buttons** — `Widget.Customlockscreen.Button.Primary` (coral), `.Secondary` (teal), `.Ghost`
  (transparent + 3dp coral border), `.Destructive` (#EF4444), and `.Text` (borderless text button).
  All are pill-shaped with a 48dp minimum height. `.Destructive` is used by the kids-settings
  "Ganti Peran HP ini" action, which sits isolated below the settings cards.
- **Cards** — `Widget.Customlockscreen.Card` (white, 2dp `card_border`, 24dp radius, elevation_sm) and
  `.Card.Elevated` (no border, elevation_md).
- **Inputs** — `Widget.Customlockscreen.EditText`, backed by `@drawable/edittext_background` (default
  2dp `input_border`, focused 3dp coral + coral ring) and `@drawable/edittext_background_error`
  (3dp `destructive` over `surface_error`).
- **Checkbox / Radio** — `Widget.Customlockscreen.CheckBox` / `.RadioButton`, tinted teal.
- **Tooltip** — `Widget.Customlockscreen.Tooltip`, backed by `@drawable/tooltip_background`
  (`tooltip_bg`, 16dp radius).
- **Badges** — `@drawable/category_badge` (pill, tinted per category by the adapters) and
  `@drawable/number_badge` (pill, `primary_container` + `primary` border).

## Settings screen pattern

Settings and utility screens (`activity_kids_settings.xml`, `activity_kids_home.xml`) follow one
recurring structure — inset cards grouped under quiet section headers, chevron rows for anything that
navigates, and destructive/system actions isolated below the cards. Section headers are
`TextAppearance.Customlockscreen.Label` in `content_secondary`; rows are whole clickable cards with a
trailing `ic_chevron_right`; separation is card boundaries plus `space_md`, never dividers. Full
anatomy, including the deliberate exception that keeps inline `Spinner`/`RadioGroup` pickers inside
their own card, is in [patterns.md](patterns.md).

## Strings

All user-facing copy lives in `values/strings.xml`. Layouts reference `@string/...`; Kotlin uses
`getString(...)`/`R.string...`. Format strings (`%1$d`, `%1$s`) are used for interpolated copy.
There are no emoji-as-icon strings — icons are drawables (see "Icons"). Content descriptions are
named `cd_*`.

## Web → Android translation notes

BounceBox is a web system; these are the honest translations:

- **px → sp/dp.** Type sizes map 1:1 to `sp`; spacing/radii/elevation to `dp`.
- **Hover → pressed.** Android has no hover on touch. BounceBox "hover" states (card border→teal,
  button darken, list tint) map to `pressed`/`focused` state-list drawables and Material ripple.
- **Coloured glow shadows** (`shadow-coral`/`shadow-teal`/`shadow-sunny`) have **no Android elevation
  equivalent** — `android:elevation` only casts a single neutral shadow. They are approximated with
  elevation plus a tinted stroke, and are a known gap.
- **Dashed list dividers** are not used: this app's lists are card-based (`item_*` cards in a
  RecyclerView), so there is no divider to style.
- **Tooltips** are defined and wired via `tooltipStyle`, but no screen currently shows one.

## Documented exceptions

- The **pastel gradient background** (`gradient_background.xml`, coral→teal→yellow tints) replaces
  BounceBox's flat white page background. It is the app's kid identity and is kept deliberately.
- The `ic_*.xml` Material Symbols vectors use `@android:color/white` as `android:fillColor` under an
  `android:tint`; the tint masks it, so they never render as white.
- The adaptive launcher icon uses the raster art in `res/mipmap-*/ic_launcher_*.webp`.
- `success_text` (`#047857`, emerald-700) rather than BounceBox's status-chip green `#166534`: the
  forest green read as generic "corporate success" against the coral/teal/sunshine palette, so
  success copy is pulled toward BounceBox's teal success identity (`secondary` / `success`) while
  keeping a green reading. Contrast on white is 5.5:1 (WCAG AA).
- `category_language` (`#A78BFA`) is a fifth hue added beyond BounceBox's four brand colours, because
  the app has five question categories.
- `OverlayLockScreenService` builds its view in code and uses pixel margins rather than the dp scale;
  it predates the system and is queued for a rewrite.
