# kunci dong — audience map for kid-UX reviews

Which screens are for a child, which are for a parent, and which are ambiguous. Use this to decide
the review rules before you start (Step 1 of the skill).

The app has **two personas** chosen at role selection and stored in `SmartLockApplication`:
`ROLE_KIDS` and `ROLE_PARENT`. `MainActivity` is only a router; it is not a screen to review.

## Child-facing (apply the child rules)

| Screen | Layout | Notes |
|---|---|---|
| Lock-screen quiz | `app/src/main/res/layout/activity_lock_screen.xml` | The main child surface. A question + 4 answer cards the child taps to unlock. Also inflated as an overlay by `DeviceAdminLockScreenService`. |
| Parent-message lock screen | `app/src/main/res/layout/activity_lock_screen_text.xml` | Shows a message from the parent and a PIN field the child uses to unlock text mode. |

That is the whole list — only two layouts are genuinely child-facing. **`kids/` is not a
child-facing folder**: `activity_kids_home.xml` and `activity_kids_settings.xml` live there but are
operated by an adult (see below).

## Parent-facing (apply adult rules, not the child checklist)

| Screen | Layout | Notes |
|---|---|---|
| Kids settings | `app/src/main/res/layout/activity_kids_settings.xml` | Lives under `kids/`, but is admin setup: switch role, manage question packs, set both PINs. An adult operates it. |
| Parent home | `app/src/main/res/layout/activity_parent_home.xml` | Finds the child's phone; issues lock/unlock. |
| Message templates | `app/src/main/res/layout/activity_message_templates.xml` | Parent composes lock messages. |
| Question manager | `app/src/main/res/layout/activity_question_manager.xml` | Parent manages question packs. |
| Pairing | `app/src/main/res/layout/activity_pairing.xml` | Parent enters the child's 6-digit pairing code. |
| Role selection | `app/src/main/res/layout/activity_role_selection.xml` | Adult chooses the phone's role. |
| App PIN gate | `app/src/main/res/layout/activity_app_lock.xml` | The parent's PIN gate that protects the app. |

## Ambiguous — decide and state your call

| Screen | Layout | Why it is ambiguous |
|---|---|---|
| Kids home | `app/src/main/res/layout/activity_kids_home.xml` | Titled "HP Anak" but it is really **permission setup** (device admin, overlay, accessibility) and a service toggle. In practice a parent does this, even though it lives under `kids/`. Treat it as adult setup unless the user says otherwise, and say so in the report. |

## Not layouts

Services and receivers have no UI to review: `DeviceAdminLockScreenService`, `LockScreenService`,
`OverlayLockScreenService`, `LockScreenTimerService`, `RemoteControlService`, `BootReceiver`,
`DeviceAdminReceiver`, `LockScreenAccessibilityService`. They inflate the lock-screen layouts, so a
review of those layouts covers their visible output too.

## The dual-persona rule

Never apply the child checklist to a parent screen, and never apply adult density conventions to a
child screen. If a finding only makes sense for one persona, name the persona in the finding.

## Known dead resources

Do not review these — nothing references them:
`activity_lock_screen_enhanced.xml`, `drawable/gradient_background_enhanced.xml`.

Three lock-screen layouts exist; only two are live (`activity_lock_screen.xml`,
`activity_lock_screen_text.xml`). `activity_lock_screen_enhanced.xml` is the orphan.
