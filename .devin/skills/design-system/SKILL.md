---
name: design-system
description: Audit "kunci dong" against its design system (BounceBox) — colour tokens (no raw hex or @android:color/white), the type/radius/spacing/elevation scales, component styles, hardcoded user-facing strings, light-only theme, and dead resources. Use when changing a layout, theme, colour, dimension or string resource, when adding a new screen or drawable, when asked "does this follow the design system", "is this on-brand", "check the tokens", "design review", or before shipping any UI change.
allowed-tools:
  - read
  - grep
  - glob
triggers:
  - user
  - model
---

# Design-system audit — kunci dong (BounceBox)

Audit UI changes against the app's design system and report what to fix. This skill is
**read-only**: it produces findings and proposed changes, it never edits files.

The app runs **BounceBox**, translated to Android: one light theme, one palette, six type roles, three
radii, one 8px-based spacing scale, one elevation scale, and a set of component styles. Most
regressions are a screen reaching past those tokens to a raw value. The job here is to catch that.

## Step 0 — Load the system (do this first)

Read [references/bouncebox.md](references/bouncebox.md) (the source design system) and
[references/design-tokens.md](references/design-tokens.md) (the Android translation — the canonical
token tables, component styles, and documented exceptions). Do not skip this even if you know the app:
the exception list is what keeps the audit from flagging the gradient, the launcher icon or the fifth
category hue.

## Step 1 — Scope

Decide what to audit, and say so in the report:

- **A change** — the files the user names, or the files a described change touches.
- **A screen** — the layout plus the Kotlin that inflates it, plus any drawable it references.
- **Nothing given** — audit the whole `app/src/main/res/` tree and the Kotlin under
  `app/src/main/java/com/rendy/customlockscreen/`.

## Step 2 — Run the checks

Go through every check and mark it **Pass**, **Fail**, or **N/A** — never skip one silently. Cite the
file and line for each finding.

**Colour tokens**
- No raw `#RRGGBB` in `res/layout/**` or `res/drawable/**`. Colour lives in `values/colors.xml` only.
- No `@android:color/white` in layouts — use `@color/surface_base`. In vector `fillColor` it is
  acceptable when the vector is tinted (it is masked by `android:tint`).
- Every `@color/<name>` and `R.color.<name>` reference resolves to a `<color>` in `values/colors.xml`.
- No duplicate values under different names. A colour with no reference **and no BounceBox role** is a
  finding; palette roles the app has not used yet (e.g. `info`, `*_container`) are allowed.
- Text on a brand-coloured fill uses dark `content_primary`, and coloured status copy on white uses a
  `*_text` token (`success_text`, `warning_text`, `error_text`) — never coral/teal/yellow as small text
  on white.

**Typography**
- Readable text uses a `TextAppearance.Customlockscreen.*` role or a `@dimen/text_*` token — never an
  ad-hoc `textSize="13sp"`.
- `TextView`/`MaterialTextView` use `android:textAppearance`; buttons/`EditText`/`RadioButton` use
  `android:textSize` (Material buttons own their text appearance).
- Nothing a user reads is below `text_xs` (14sp) — BounceBox forbids sub-14px text.
- `Display`/`Title`/`Heading` (Titan One) are for headings and titles; body copy and labels are Poppins.

**Icons**
- Icons are Material Symbols vectors (`res/drawable/ic_*.xml`) — never an emoji, a glyph `TextView`,
  or an icon font. An emoji left in a layout or a string is a finding.
- An icon is sized with `@dimen/icon_*` (dp), never a `textSize` or a `glyph_*` token.
- An icon is tinted from a colour token (`app:tint` / `app:drawableTint` / `app:iconTint`) — never a
  raw hex, and never left on the drawable's default `?attr/colorControlNormal` where the surface
  needs a specific colour.
- A button icon uses `app:icon` + `app:iconGravity`; a titled row uses `android:drawableStart` +
  `android:drawablePadding`. A `drawableStart` with no `drawablePadding` is a finding.

**Shape, spacing and elevation**
- `app:cardCornerRadius` uses `@dimen/radius_md|lg|pill`. Drawable `<corners android:radius>` does too.
  Buttons, chips and badges are `radius_pill`.
- Paddings and margins use `@dimen/space_*`. Values that are off the scale are a finding; component
  sizes (`layout_height`, icon sizes) are not spacing and are exempt.
- `cardElevation`/`android:elevation` uses `@dimen/elevation_sm|md|lg` rather than a literal dp.
- Buttons, cards, inputs, checkbox/radio use the `Widget.Customlockscreen.*` styles rather than ad-hoc
  `Widget.Material3.*` overrides.

**Strings**
- No hardcoded `android:text`, `android:hint` or `android:contentDescription` literals in layouts —
  all user-facing copy is in `values/strings.xml`.
- No hardcoded user-facing strings in Kotlin (`Toast`, `setTitle`, `setMessage`, `.text =`,
  notification titles/channels). Log tags, channel ids, intent actions and domain keys
  (`"MATEMATIKA"`) are exempt.
- Every icon and image has a `contentDescription`, or `importantForAccessibility="no"` when it is
  decorative and adjacent text already carries the meaning.

**Theme**
- The app is **light-only by design**. There must be no `res/values-night/` directory, and the base
  theme must be a `Theme.Material3.Light.*` parent with `android:forceDarkAllowed=false`.
- `SmartLockApplication.onCreate()` calls `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`.

**Dead resources**
- Every layout, drawable, font and string is referenced by at least one other file. An unreferenced
  resource is a finding (propose deletion, do not delete).

## Step 3 — Report

Write findings as a short ranked list, most impactful first, capped at 8. For each:

- **Where** — file and line.
- **Evidence** — an observable fact, not an opinion. "`activity_pairing.xml:71` uses
  `textSize="44sp"`" not "the code looks inconsistent".
- **Severity** — **Blocker** (a raw value that breaks the theme or renders unreadable),
  **Major** (a screen bypasses the system), **Minor** (polish).
- **Fix** — a concrete change someone could make in under an hour.

Close with **Top 3 fixes**, ranked by impact. If nothing clears the bar, say the change is in good
shape and name the 2-3 strongest polish items instead — never pad the list.

### Output format

```
## <scope> — design-system audit

### 1. <Short title>
- **Where:** <file>:<line>
- **Evidence:** <observable fact>
- **Severity:** Blocker | Major | Minor
- **Fix:** <concrete change>

...

**Top 3 fixes:**
1. ...
2. ...
3. ...
```

## Do not flag

- The pastel gradient background (`gradient_background.xml`) — BounceBox specifies a flat white page,
  but the gradient is the app's deliberate kid identity.
- The rounded white cards, the coral (`primary`) accent and the pill-shaped buttons — these are
  BounceBox's identity.
- The Material Symbols icon set — monochrome, flat and Rounded by choice; the requirement is only
  that each icon is on the `@dimen/icon_*` scale, tinted from a token, and described for TalkBack.
- `@android:color/white` as a vector `fillColor` under a `android:tint`.
- The fifth category hue (`category_language`) — the app has five categories, BounceBox has four brands.
- The absence of BounceBox's coloured glow shadows and dashed list dividers — neither has a faithful
  Android equivalent; see the translation notes.
- `Widget.Customlockscreen.Button.Destructive` and the unused palette roles — the system defines the
  full BounceBox set; a variant is only a finding if it has no BounceBox counterpart.
- Indonesian copy, and the parent/kid dual-persona split.
