---
name: kid-ux
description: Review the child-facing screens of "kunci dong" against children's UX research (ages 3-12) — touch targets, reading level, cognitive load, age-band fit, and parental-control ethics. Use when reviewing or changing any screen a child sees (the lock-screen quiz, the parent-message lock screen, kids settings), when adding a new kid-facing layout, or when asked "is this good for kids", "kid-friendly", "child UX", "age appropriate", or "review the lock screen".
allowed-tools:
  - read
  - grep
  - glob
triggers:
  - user
  - model
---

# Kid UX review — kunci dong (ages 3-12)

Audit a child-facing screen against children's UX research and report what to fix. This skill is
**read-only**: it produces findings and proposed changes, it never edits files.

The app has two audiences with opposite needs. A child (3-12) is locked out and must answer a
question to get back in; a parent (an adult) sets the lock. Do not judge a child's screen by adult
standards, and do not judge a parent's screen by child standards.

## Step 0 — Load the guidelines (do this first)

Read [references/nng-kid-ux-guidelines.md](references/nng-kid-ux-guidelines.md) and
[references/app-audience.md](references/app-audience.md) before writing any finding. The guideline
file has the age-band tables and the physical/cognitive/ethical rules you cite from; the audience
file says which screens are child-facing in this repo. Do not skip this even if you know the
research — the exact age bands and the "children vs adults" contrasts are what keep the review
consistent across runs.

## Step 1 — Classify the screen

Decide who the screen is actually for, and say so in the report:

- **Child-facing** — the child sees and operates it. In this app: the lock-screen quiz
  (`activity_lock_screen.xml`) and the parent-message lock screen (`activity_lock_screen_text.xml`).
  Use the list in [references/app-audience.md](references/app-audience.md) — do **not** treat
  everything under `kids/` as child-facing.
- **Parent-facing** — an adult sets things up. In this app: `parent/`, `QuestionManagerActivity`,
  `PairingActivity`, `RoleSelectionActivity`, `activity_kids_settings.xml`, and most of
  `activity_kids_home.xml` (both are permission/settings screens that live under `kids/` but are
  operated by an adult — say so if they are mislabelled).

If the user names a screen, review that. If they name a change, review the screens it touches. If
they give you nothing, review the child-facing screens above. Never apply the child rules to a
parent screen, and never apply adult density rules to a child screen.

## Step 2 — Pick the age band

kunci dong is used by roughly 3-12, and NN/g is explicit that "designing for children" is not one
thing. State the band you are judging against — **3-5**, **6-8**, or **9-12** — and if the app does
not say which child it targets, treat the youngest band as the constraint (the strictest one). The
band changes the answer: a 12sp label is fine for a 10-year-old and too small for a 4-year-old.

## Step 3 — Run the checklist

Go through every item and mark it **Pass**, **Fail**, or **N/A** — never skip one silently. Cite the
screen, file, and line for each finding.

**Physical (motor skills)**
- Primary controls are comfortably above the 48dp Android floor. For young children, aim for
  NN/g's ~2 cm target (roughly 2x the 1 cm adult target); 48dp is a floor, not a goal.
- Only tap, swipe, and drag are required — no precise dragging to a small spot, no two-handed
  coordination, no quick reactions to a stimulus (under 5), and only limited coordination (under 8).
- Interactions are forgiving: a near-miss still works, and a wrong tap is recoverable.
- Where a target can be moved, both "drag it" and "tap the destination" work.

**Cognitive (developing reasoning)**
- The goal is stated, not just the controls — the child can tell what they are trying to do and how.
- Instructions are tailored to the child's level; subtle cues (a small face, a quiet noise) do not
  carry the meaning.
- The screen leans on real-world mental models (a familiar object, a familiar action) rather than
  abstract app conventions.
- Working memory is not overloaded: few choices at once, no number or symbol the child must decode
  on their own (a bare "2" means nothing — pair it with a picture).
- Instructions are clear but not over-prescriptive — do not tell a child to use a specific device.

**Text and reading**
- The screen does not depend on reading to be usable by the youngest band; icons, colour, and shape
  carry the meaning and text is a reinforcement.
- Body text is at least 14pt (sp) for young children; nothing a child must read is at 12-13sp.
- Copy is short, in the child's words, and matches their reading level.

**Feedback and motivation**
- Every action gets immediate, obvious feedback (children have little patience).
- Feedback is positive and non-punishing; a wrong answer invites another try rather than scolding.
- Animation and sound are used where they help — children like them, unlike adults (see the
  intentional list below for what this app already does deliberately).

**Accessibility**
- Every icon and image has a `contentDescription`, or is marked decorative; a monochrome icon read
  aloud by TalkBack is noise.
- State is never signalled by colour alone.

**Ethics and parental control**
- The child cannot reach or change settings, purchases, or the lock itself.
- A grown-up gate exists where a child could otherwise change something that matters.
- The design does not exploit children's weak impulse control (no streak-guilt, no dark patterns,
  no countdown that pressures a wrong answer).

## Step 4 — Report

Write findings as a short ranked list, most impactful first, capped at 6. For each:

- **Where** — file and line (or screen name).
- **Evidence** — an observable fact, not an opinion. "Answer cards are 60dp tall" not "targets feel
  small".
- **Severity** — **Blocker** (a child cannot complete the task), **Major** (hurts usability),
  **Minor** (polish).
- **Fix** — a concrete change someone could make in under an hour.

Close with **Top 3 fixes**, ranked by impact. If nothing clears the bar, say the screen is in good
shape and name the 2-3 strongest polish items instead — never pad the list.

### Output format

```
## <Screen> — kid UX review (<age band>)

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

## Intentional — do not flag

These are deliberate parts of kunci dong's identity, not defects. Judge them on whether they serve
the child, not on whether they match a generic clean-UI checklist:

- The soft pastel gradient background, the rounded white cards, and the coral primary accent — these
  are the app's deliberate BounceBox kid identity (see the `design-system` skill).
- The monochrome Material Symbols icons — they are not the pre-reader aid emoji were, so check that
  the text beside an icon still carries the meaning on its own for the youngest band.
- Lottie and list animations — children like motion and sound; the question is whether the motion is
  meaningful and whether it can be reduced, not whether it exists.
- Indonesian copy, and the parent/kid dual-persona split.
- The playful, high-contrast kid palette, pill-shaped buttons and oversized type.

## Attribution

Guidelines distilled from Nielsen Norman Group's public research on children's UX (ages 3-12):
*Children's UX: Usability Issues in Designing for Young People* (Sherwin & Nielsen, 2019),
*Designing for Kids: Cognitive Considerations* (Liu, 2018), and *Design for Kids Based on Their Stage
of Physical Development* (Liu, 2018). The full 156-guideline report is a paid NN/g publication; this
skill paraphrases the public articles rather than copying the report.
