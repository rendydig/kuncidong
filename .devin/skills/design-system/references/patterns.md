# Screen patterns — kunci dong (BounceBox on Android)

The recurring layout patterns this app builds screens from. Token names in `@dimen/…`,
`@color/…` and `@style/…` refer to [design-tokens.md](design-tokens.md), which stays the
single source of truth for values. A screen that invents its own structure instead of
reusing one of these is a finding.

---

## 1. Settings screen — inset groups, section headers, chevron rows

The shape of a settings/utility screen (`activity_kids_settings.xml`,
`activity_kids_home.xml`). Five properties, in the order a reader meets them:

### Inset groups

Every card is inset from the screen edge — the scroll container owns the page gutter
(`android:padding="@dimen/space_md"`) and no card ever touches the left or right edge.
A group is one card; related settings are grouped into one card rather than spread down
the page. Do not add a second gutter or a full-bleed band.

### Categorized modularity

Sections are separated by a quiet header `TextView` placed directly above the first card
of the section:

```xml
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="@string/<section_key>"
    android:textAppearance="@style/TextAppearance.Customlockscreen.Label"
    android:textColor="@color/content_secondary"
    android:layout_marginTop="@dimen/space_sm"
    android:layout_marginBottom="@dimen/space_sm" />
```

- `TextAppearance.Customlockscreen.Label` (16sp Poppins) in `content_secondary` — quiet
  meta text, normal case, no icon, no accent colour.
- The first header of a screen omits `layout_marginTop` (the header block above already
  contributes `space_md`); later headers keep it so a section break reads wider than the
  `space_md` gap between cards.
- The header is a `@string/...` key like every other user-facing label.

### Row anatomy (progressive disclosure)

A setting that navigates somewhere — a dialog, a drill-down, another activity — is a
whole card that is tappable, with a trailing chevron:

```
[ icon_md ]  Title                          >
             Status / current value
```

- Card: `style="@style/Widget.Customlockscreen.Card"`, the section accent
  `app:strokeColor` (coral `@color/primary` for kid/parent screens),
  `android:clickable="true"`, `android:focusable="true"`,
  `android:foreground="?android:attr/selectableItemBackground"`.
- Leading icon: `@dimen/icon_md` `ImageView`, tinted from a token
  (`app:tint="@color/primary"`), `android:contentDescription="@null"` +
  `android:importantForAccessibility="no"` — the row's text carries the meaning.
- Title: `TextAppearance.Customlockscreen.Label`, `content_primary`.
- Optional status line: `TextAppearance.Customlockscreen.Label`, `content_secondary`,
  set from Kotlin (`tvPinStatus`, `tvAppPinStatus`).
- Trailing chevron: `@drawable/ic_chevron_right`, `@dimen/icon_lg`, tinted with the row
  accent, `importantForAccessibility="no"`.
- Icon, title block and chevron sit in one horizontal `LinearLayout`
  (`gravity="center_vertical"`, `padding="@dimen/space_md"`); the title block takes
  `layout_weight="1"`.
- The chevron is a promise: tapping the row opens something. Do not put a chevron on a
  card that does not navigate.

### Action isolation

System-level and destructive actions live **outside** every card, at the bottom of the
screen, full width:

- `style="@style/Widget.Customlockscreen.Button.Destructive"` (red `#EF4444`), with
  `layout_marginTop="@dimen/space_lg"` and a closing `layout_marginBottom`.
- The switch-role action (`btnSwitchRole`, "Ganti Peran HP ini") is the app's
  log-out-equivalent and is the current example.
- Isolating them separates them from ordinary navigation and communicates that they are
  not another setting. Never nest one inside a settings card.

### Minimalist dividers

Separation comes from **card boundaries plus `@dimen/space_md`** — not from rules, lines
or heavy borders. The app's lists are card-based, so there is no divider drawable to
style; the deliberate absence of BounceBox's dashed dividers (see the translation notes
in design-tokens.md) is the same decision seen from the other side.

### Deliberate deviation: inline controls stay inline

Single-value pickers that a parent adjusts in place — the auto-lock interval `Spinner`
and the lock-mode `RadioGroup` — keep their controls **inside** their own card instead of
drilling down behind a chevron. This is a conscious exception: the screen is a parental
utility, the controls are small and unambiguous, and a one-tap value change beats a
modal. The card still follows the section rhythm (Heading title with a `drawableStart`
icon, `space_md` padding, section accent stroke) — it just has no chevron.

---

## 2. Header block

Both kid screens open with the same header: an icon on the left, a `Title` in the accent
colour, and (on the home screens) a borderless gear/text button flush to the right edge
(`android:insetRight="0dp"`, `paddingEnd="0dp"`). A settings screen swaps the icon for a
back `Widget.Customlockscreen.Button.Text` button. Keep the header outside any section —
it is chrome, not a group.

## 3. Empty state

A card that can be empty (paired parents, device list) keeps a `Body` `TextView` in
`content_secondary` that Kotlin toggles between `VISIBLE` and `GONE`
(`tvNoParentsSettings`, `tvEmpty`), rather than showing an empty container.
