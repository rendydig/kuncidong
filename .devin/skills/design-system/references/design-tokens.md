# Design tokens — kunci dong

The single source of truth for the app's visual language. Everything here lives in
`app/src/main/res/values/` (`colors.xml`, `dimens.xml`, `themes.xml`). If a screen needs a value that
is not on this page, add a token here rather than hardcoding it.

## Theme

- **Light-only by design.** `Base.Theme.Customlockscreen` parents `Theme.Material3.Light` and sets
  `android:forceDarkAllowed=false`; `SmartLockApplication.onCreate()` calls
  `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`.
- There is deliberately **no** `res/values-night/` directory. The app never renders a dark variant.
- Font family is set once on the base theme (`fontFamily=@font/nunito`), so every widget inherits
  Nunito without per-view work.
- `Theme.Customlockscreen.LockScreen` is the fullscreen lock-screen theme (AppCompat.Light.NoActionBar
  plus the window flags).

## Colour tokens (`colors.xml`)

| Token | Value | Use |
| --- | --- | --- |
| `white` | `#FFFFFFFF` | Text/icons on a coloured or brand surface |
| `surface_card` | `#FFFFFFFF` | Card and sheet backgrounds |
| `answer_card_bg` | `#E3F2FD` | Answer options, info tiles |
| `background_light` | `#F8F9FA` | Inset panels (question box, permission rows) |
| `lock_screen_background` | `#E3F2FD` | Lock-screen window background |
| `card_stroke` | `#E0E0E0` | Card and input outlines |
| `primary_blue` | `#2196F3` | Primary brand, links, category accents |
| `primary_blue_dark` | `#1976D2` | Primary pressed/emphasis |
| `primary_blue_light` | `#BBDEFB` | Primary container fill |
| `success` | `#4CAF50` | Positive state, download action, paired status |
| `success_dark` | `#388E3C` | Positive emphasis |
| `success_light` | `#E8F5E8` | Positive container fill |
| `success_container` | `#C8E6C9` | Category badge fill |
| `error_red` | `#F44336` | Errors, wrong answer, destructive icon |
| `warning_orange` | `#FF9800` | Warnings, pending state |
| `text_primary` | `#212121` | Body text |
| `text_secondary` | `#757575` | Supporting text |
| `text_hint` | `#BDBDBD` | Placeholder/meta text |
| `question_text` | `#1565C0` | Lock-screen question text |
| `gradient_start` / `gradient_center` / `gradient_end` | `#E3F2FD` / `#F3E5F5` / `#FFF8E1` | The page gradient in `gradient_background.xml` |
| `category_math` / `_science` / `_geography` / `_history` / `_language` | `#FF9800` / `#4CAF50` / `#2196F3` / `#9C27B0` / `#F44336` | Category badge colours |
| `kid_primary` | `#FF6B6B` | Coral accent — kid-facing headings |

Rules: colour is defined only here. Layouts and drawables reference tokens; they never contain a raw
`#RRGGBB`. `@android:color/white` is not used in layouts.

## Type scale (`dimens.xml` → `TextAppearance.Customlockscreen.*`)

| Role | Size | Weight | Font | Use |
| --- | --- | --- | --- | --- |
| `text_display` | 28sp | bold | Fredoka One | Hero/brand titles only |
| `text_title` | 20sp | bold | Nunito | Screen headers, card titles, question and answer text |
| `text_heading` | 16sp | bold | Nunito | Section headings, list-row titles |
| `text_body` | 14sp | normal | Nunito | Body copy, descriptions |
| `text_label` | 12sp | normal | Nunito | Captions, meta, status lines |

**Glyph scale** — for emoji used as an icon, sized for visual weight rather than reading:

| Token | Size |
| --- | --- |
| `glyph_hero` | 48sp |
| `glyph_xl` | 40sp |
| `glyph_lg` | 24sp |
| `glyph_md` | 20sp |
| `glyph_sm` | 16sp |

How to apply: `android:textAppearance="@style/TextAppearance.Customlockscreen.<Role>"` on
`TextView`/`MaterialTextView`; `android:textSize="@dimen/text_*"` on `MaterialButton`, `Button`,
`EditText`, `RadioButton` and `CheckBox` (Material buttons own their text appearance). Programmatic
views use `setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_*))`.

## Shape and spacing (`dimens.xml`)

Radii — three steps only:

| Token | Value | Use |
| --- | --- | --- |
| `radius_sm` | 12dp | Badges, list items, buttons, inputs |
| `radius_md` | 16dp | Standard cards |
| `radius_lg` | 20dp | Hero cards, dialogs, lock-screen panels |

Spacing — `space_1` 4dp, `space_2` 8dp, `space_3` 12dp, `space_4` 16dp, `space_5` 20dp, `space_6` 24dp.
Paddings and margins come from this scale. Component sizes (`layout_height`, icon dimensions) are not
spacing and may stay literal.

## Strings

All user-facing copy lives in `values/strings.xml`. Layouts reference `@string/...`; Kotlin uses
`getString(...)`/`R.string...`. Format strings (`%1$d`, `%1$s`) are used for interpolated copy.
Emoji-as-icon strings are named `icon_*`; content descriptions are named `cd_*`.

## Documented exceptions

- `ic_list.xml` uses `@android:color/white` as `android:fillColor` under an `android:tint`; the tint
  masks it, so it never renders as white.
- The adaptive launcher icon uses the raster art in `res/mipmap-*/ic_launcher_*.webp`; the legacy
  vector `ic_launcher_background.xml` / `ic_launcher_foreground.xml` were removed as unreferenced.
- `OverlayLockScreenService` builds its view in code and uses pixel margins rather than the dp scale;
  it predates the system and is queued for a rewrite.
