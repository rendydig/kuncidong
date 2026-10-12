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
- Use colour tokens from `app/src/main/res/values/colors.xml` — do not hardcode hex in layouts.
- Put user-facing text in `strings.xml`; do not hardcode `android:text` literals.
- Every emoji used as an icon and every image needs a `contentDescription`.
- Keep day and night themes (`values/` and `values-night/`) in sync.

## Skills

- For any screen a child sees, or any change to one, run the **`/kid-ux`** skill first.
