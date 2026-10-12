# Children's UX guidelines (ages 3-12) — reference

Distilled for kunci dong from Nielsen Norman Group's public research. This is a paraphrase written
for this app, not a copy of the NN/g report. Cite it as "NN/g" in findings.

Sources:
- Katie Sherwin & Jakob Nielsen, *Children's UX: Usability Issues in Designing for Young People*, 2019
  (three rounds of usability studies, 125 children aged 3-12, sites and apps).
- Feifei Liu, *Designing for Kids: Cognitive Considerations*, 2018.
- Feifei Liu, *Design for Kids Based on Their Stage of Physical Development*, 2018.

The full **156-guideline** report, *UX Design for Children (Ages 3-12)* (4th ed.), is a paid NN/g
publication. This file only captures the public articles.

---

## 1. Age bands — the single most important rule

NN/g: "There's no such thing as designing for children, defined as everybody aged 3-12." You must
target a narrow band. At minimum: **3-5**, **6-8**, **9-12**. Even two years apart behaves very
differently on the same task.

If the app does not declare a band, judge against the **youngest** band (the strictest).

### Physical development (motor skills)

| | 3-5 | 6-8 | 9-12 |
|---|---|---|---|
| Gross motor | Limited | Partially developed | Well developed |
| Fine motor | Very limited | Limited | Well developed |
| Motor coordination | Very limited | Limited | Partially developed |
| Device | Touchscreen | Touchscreen + trackpad | Mouse + trackpad |
| Mastered gestures | Tap, swipe, drag (touch) | Click, simple keyboard | Drag/scroll with mouse, complex coordination |

- Gross motor skills (big arm/hand movements) develop before fine motor skills (precise finger
  work). That is why tap/swipe/drag on a big target is easy even for the youngest, while precise
  dragging to a small spot is hard.
- Around **11** children reach the adult range of physical interaction — but their reasoning is still
  developing, so the UI should stay simple regardless.
- Under **5**: avoid anything needing two-handed coordination or a quick manual reaction to a visual
  cue. Under **8**: keep coordination demands limited.

### Cognitive development (Piaget)

- **Preoperational (2-7)**: can think in symbols, but cannot reliably take another person's
  perspective; language still developing.
- **Concrete operational (7-11)**: can use logic and make inferences.
- Immature across both: **theory of mind**, **cognitive flexibility**, and **executive function**
  (planning/monitoring their own behaviour).

---

## 2. Physical design recommendations (NN/g)

1. **Touchscreen designs for under-9s should emphasise swiping, tapping, and dragging.** These need
   big movements of arms and hands, which are better developed than fine motor skills.
2. **Design big, noticeable buttons.** NN/g recommends **at least 2 cm x 2 cm touch targets for
   young children — 4x the area of the 1 cm x 1 cm adult target.** Small targets cause failures; in
   one study 7-year-olds could not hit a 5 mm close button and kept triggering an ad instead.
   - Android note: 48dp is the adult *floor* (roughly 1 cm). For young children, size primary
     controls well above it. Do not treat 48dp as the goal for a child.
3. **Make designs flexible.** If a target must be moved, support both dragging it and tapping the
   destination. Do not require a single precise gesture.
4. **Reduce navigation.** Recommend related content instead of forcing a trip back to a home screen.
5. **Avoid complex coordination.** Under 5: no two-handed use or quick reactions. Under 8: limited
   coordination. Games requiring gestures beyond a child's skillset are frustrating, not fun.
6. **Desktop (if ever): clicks and simple keyboard only.** Dragging, scrolling, and precise cursor
   work are hard under 9, and harder still on a trackpad.
7. **Even on touchscreens, precise dragging is hard.** If a child misses a drop target by a little,
   make it forgiving — do not discard their input.

---

## 3. Cognitive design recommendations (NN/g)

1. **State the goal and how to achieve it.** Show the objective *and* the operation, with visuals (and
   audio where useful). A game that shows only the controls leaves children confused about what they
   are trying to do.
2. **Tailor instructions to the child's level.** Under 6 miss subtle cues (a small face, a quiet
   noise). Use explicit, exaggerated cues, and watch for cultural assumptions in sounds/expressions.
3. **Use existing mental models.** Lean on what children already know from daily life (a colouring
   book, cooking, a household object). Familiar metaphors let even 3-year-olds succeed unaided.
4. **Reduce cognitive load; prevent errors.** A child's working memory is smaller than an adult's.
   Make the interface self-explanatory. A bare number or symbol without a picture is confusing (in
   one study a 7-year-old kept asking what a "2" meant). Be tolerant of input mistakes — children
   misspell and mistap; correct them rather than fail them.
5. **Be clear and specific, but not over-prescriptive.** Children take instructions literally. Naming
   a specific device ("use the mouse") makes a child switch away from a tool they are better with.
   Say "tap the bucket", not "use the mouse to click the bucket".

---

## 4. Children vs adults (NN/g)

Same for both: follow UI conventions, prefer user control, judge fast and leave if it is not good.

| Dimension | Children | Adults |
|---|---|---|
| Willingness to wait | Want instant gratification | Limited patience |
| Redundant/multiple navigation | Very confusing | Slightly confusing |
| Back button | In-app back used if prominent; browser back not used (young); relied on (older) | Relied on |
| Reading | None (youngest) / tentative (young) / scanning (older) | Scanning |
| Readability level | Each user's grade level | 8th-10th grade for broad audiences |
| Font size | **14 pt (young children)**, 12 pt (older) | 12 pt (up to 14 pt for seniors) |
| Scrolling | **Avoid (young children)**, some (older) | Some |
| Touch gestures | Large, simple actions (young); easy and liked (older) | Easy and well-liked |
| Goal in visiting | Entertainment | Getting things done |
| Exploratory behaviour | Like to try many options; "mine-sweep" the screen | Stick to the main path |
| Real-life metaphors | Very helpful for pre-readers | Often distracting/clunky |
| Animation and sound | **Liked** | Usually disliked |
| Ads and promotions | Cannot distinguish from real content | Ad-blind; sceptical of promos |

---

## 5. Ethical considerations (NN/g)

- Children's ability to plan and execute is limited, so they are **more vulnerable to attractive
  games and can develop addictions easily**.
- NN/g highlights apps that give **parents** the control: a Parent Center where an adult can set a
  maximum continuous play time, enforced rest periods, and a sleep window.
- A **content wall** can prevent a child from changing settings: make it easy for an adult and hard
  for a child (e.g. an adult-only task). This is a deliberate, legitimate use of children's limited
  cognitive development.
- Designers should consider offering such parental controls, given the ethics of marketing to
  children and increasing screen time.

**Relevance to kunci dong:** the app is itself a parental-control tool (a parent locks the child's
phone, remotely or with a PIN gate). This aligns with the ethics guidance — the review should treat
the grown-up gate and remote control as strengths, and check that a child cannot defeat them.

---

## 6. Quick numbers to cite

- Touch target, young children: **>= 2 cm x 2 cm** (4x the 1 cm adult target).
- Touch target, Android adult floor: **48dp**.
- Body text, young children: **>= 14 pt/sp**; older children 12 pt.
- Scrolling: **avoid** for young children.
- Target bands: **3-5 / 6-8 / 9-12**, judge against the youngest unless told otherwise.
