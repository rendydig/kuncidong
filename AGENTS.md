# kunci dong — project rules

Educational Android lock screen for kids: a child answers a question (or a parent sends a message)
to unlock, and a parent can lock/unlock the child's phone from another device. UI copy is in
Indonesian.

## Layout

- `app/` — the Android app (Kotlin, XML Views, package `com.rendy.customlockscreen`).
- `parent-go/` — Go CLI (`kuncidong-parent`) that speaks the same TCP-JSON protocol as the app's
  "Anak" role, for testing remote lock without the Android parent app.

## Build & test

```bash
./gradlew test            # unit tests
./gradlew assembleDebug   # build the debug APK
./gradlew installDebug    # install on a connected device
```

## Conventions

- Kotlin/Android with Material 3. App themes are named `Theme.Customlockscreen.*`.
- **The app is light-only by design.** There is no `values-night/`; the base theme parents
  `Theme.Material3.Light` with `android:forceDarkAllowed=false`, and `SmartLockApplication` pins
  `AppCompatDelegate.MODE_NIGHT_NO`. Do not reintroduce a dark variant.
- The app follows the **BounceBox** design system
  (`.devin/skills/design-system/references/bouncebox.md`), translated to Android tokens in
  `.devin/skills/design-system/references/design-tokens.md`. Use those tokens and the
  `Widget.Customlockscreen.*` component styles; do not invent ad-hoc Material styles.
- Use colour tokens from `app/src/main/res/values/colors.xml` — never hardcode hex in layouts or
  drawables, and never use `@android:color/white` in a layout.
- **Type:** use `TextAppearance.Customlockscreen.*` roles (`Display`/`Title`/`Heading`/`Body`/`Label`)
  or the `@dimen/text_*` tokens; emoji-as-icon uses `@dimen/glyph_*`. No ad-hoc `textSize`, and
  nothing a user reads below `text_xs` (14sp). Headings/titles are Titan One; body copy is Poppins.
- **Shape/spacing/elevation:** `@dimen/radius_md|lg|pill` for corners, `@dimen/space_*` for
  paddings/margins, `@dimen/elevation_sm|md|lg` for elevation. Buttons, chips and badges are pill-shaped.
- Put user-facing text in `strings.xml`; never hardcode `android:text`/`android:hint` literals or
  Kotlin UI strings (Toasts, dialog titles, notification text).
- Every emoji used as an icon and every image needs a `contentDescription`, or
  `importantForAccessibility="no"` when decorative.
- Fonts: `res/font/poppins.xml` + TTFs and `res/font/titan_one.xml` + `titan_one_regular.ttf`
  (both OFL — see `Poppins-OFL.txt` and `TitanOne-OFL.txt`).

## Skills

- For any screen a child sees, or any change to one, run the **`/kid-ux`** skill first.
- For any UI/theme/token/string change, run the **`/design-system`** skill to check it against the
  design system. Full token reference: `.devin/skills/design-system/references/design-tokens.md`.
