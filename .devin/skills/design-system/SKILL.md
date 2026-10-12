---
name: design-system
description: Audit "kunci dong" against its design system — day/night theme parity, colour tokens (no raw hex or @android:color/white), the type/radius/spacing scales, hardcoded user-facing strings, and dead resources. Use when changing a layout, theme, colour, dimension or string resource, when adding a new screen or drawable, when asked "does this follow the design system", "is this on-brand", "check the tokens", "design review", or before shipping any UI change.
allowed-tools:
  - read
  - grep
  - glob
triggers:
  - user
  - model
---

# Design-system audit — kunci dong

Audit UI changes against the app's design system and report what to fix. This skill is
**read-only**: it produces findings and proposed changes, it never edits files.

The design system is intentionally small: one light theme, one palette, five type roles, three radii,
one spacing scale. Most regressions are a screen reaching past those tokens to a raw value. The job
here is to catch that.

## Step 0 — Load the tokens (do this first)

Read [references/design-tokens.md](references/design-tokens.md) before writing any finding. It lists the
canonical colour tokens, the type/glyph roles, the radii and spacing scale, and the documented
exceptions. Do not skip this even if you know the app — the exception list is what keeps the audit
from flagging the launcher icon.

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
- No `@android:color/white` in layouts — use `@color/surface_card`. In vector `fillColor` it is
  acceptable when the vector is tinted (it is masked by `android:tint`).
- Every `@color/<name>` and `R.color.<name>` reference resolves to a `<color>` in `values/colors.xml`.
- No duplicate values under different names; no zero-reference tokens.

**Typography**
- Readable text uses a `TextAppearance.Customlockscreen.*` role or a `@dimen/text_*` token — never an
  ad-hoc `textSize="13sp"`.
- `TextView`/`MaterialTextView` use `android:textAppearance`; buttons/`EditText`/`RadioButton` use
  `android:textSize` (Material buttons own their text appearance).
- Emoji used as an icon uses a `@dimen/glyph_*` token, never a text role.
- `Display` (Fredoka One) is reserved for hero/brand titles; everything else is Nunito.

**Shape and spacing**
- `app:cardCornerRadius` uses `@dimen/radius_sm|md|lg`. Drawable `<corners android:radius>` does too.
- Paddings and margins use `@dimen/space_*`. Values that are off the scale are a finding; component
  sizes (`layout_height`, icon sizes) are not spacing and are exempt.

**Strings**
- No hardcoded `android:text`, `android:hint` or `android:contentDescription` literals in layouts —
  all user-facing copy is in `values/strings.xml`.
- No hardcoded user-facing strings in Kotlin (`Toast`, `setTitle`, `setMessage`, `.text =`,
  notification titles/channels). Log tags, channel ids, intent actions and domain keys
  (`"MATEMATIKA"`) are exempt.
- Every emoji-as-icon and image has a `contentDescription`, or `importantForAccessibility="no"` when
  it is decorative and adjacent text already carries the meaning.

**Theme**
- The app is **light-only by design**. There must be no `res/values-night/` directory, and the base
  theme must be a `Theme.Material3.Light.*` parent with `android:forceDarkAllowed=false`.
- `SmartLockApplication.onCreate()` calls `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)`.

**Dead resources**
- Every layout, drawable and string is referenced by at least one other file. An unreferenced
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

- The pastel gradient background, the rounded white cards, and the coral (`kid_primary`) accent —
  these are the app's identity.
- Emoji as the icon language; the requirement is only that it is sized on the glyph scale and
  described for TalkBack.
- `@android:color/white` as a vector `fillColor` under a `android:tint`.
- Indonesian copy, and the parent/kid dual-persona split.
