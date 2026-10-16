First Step styles are token-driven and component-scoped.

# CSS Architecture

*Where a style belongs, and why. See `references/decisions.md` Decision 039 for
the reasoning and the migration that produced this.*

## Why this exists

Two silent layout bugs were caused by the same thing: a **deleted component's CSS
outliving it**. The class name looked free — grep the components, find nothing —
so a new component claimed it and silently inherited the corpse's styles. Nothing
errored; every unit test passed. Only measuring rendered output caught it.

Global CSS makes that possible. This architecture removes the possibility for new
code and shrinks the surface for old code.

## The file layout

```
src/
  index.css              QUARANTINE — legacy globals, shrinking only
  styles/
    tokens.css           design tokens (CSS custom properties)
    base.css             element resets, typography, a11y utilities
    themes.css           default + high contrast
  components/
    ContentCard/
      ContentCard.tsx
      ContentCard.module.css
  pages/
    HomePage/
      HomePage.tsx
      HomePage.module.css
```

**The governing rule: a class that styles a component appears in exactly one
place — that component's `.module.css`.** Never in `index.css`, never in the
three `styles/` files.

## `tokens.css` — where tokens live

**Contains `:root` and nothing else.** No selectors, no component classes.

The 16 tokens are colour, surface, shadow, radius and transition primitives. They are
**deliberately CSS custom properties rather than a framework config**, because
the palette is shared with `backend/src/main/resources/static/styles.css` — the
legacy static app. Two stylesheets in two build systems can both read custom
properties; neither can read a Tailwind config.

**Add a token when a literal value appears in more than one rule, or when a theme
needs to flip it.** `--surface` exists because six panel rules each hard-coded
`white`, and high contrast has to turn all six black.

## `base.css` — element defaults and utilities

**Contains:** element selectors (`*`, `body`), resets, typography, and
**documented global a11y utilities** — today only `.visually-hidden`.

**Never contains:** component classes or layout classes.

The bar for adding a global utility here is high: it must be genuinely
cross-cutting and accessibility- or reset-related. "Several components use it" is
**not** sufficient — `.section-placeholder` is used by seven components and still
lives in the quarantine, because it is a shared *component* style, not a base
utility. Shared component styles become a shared component, not a global class.

## `themes.css` — how theming works

**Contains:** theme-level selectors only — `:root` defaults and
`body.high-contrast`.

### High contrast is expressed as token redefinition

`hooks/useHighContrast.ts` toggles a single `high-contrast` class on `<body>`.
That block **redefines the tokens**, and the whole UI follows:

```css
body.high-contrast {
  --surface: #000;
  --bg-lighter: #000;
  --bg-light: #222;
  --border-color: #ff0;
  --text-primary: #fff;
  --text-secondary: #fff;
  --text-light: #fff;
}
```

**This replaced 30 per-component override rules.** The per-component approach
required remembering every element and demonstrably failed to: `.flyer-card` and
`.flyer-card-title` had overrides while `.flyer-card-image` (a **cream** block on
a black page) and `.flyer-card-meta` (**gray text on black**) were forgotten for
months. Token redefinition covers them by construction.

**Write components against tokens and they are themed for free.** A component
whose colours are all `var(--…)` needs no high-contrast rule at all.

### What still needs an explicit rule, and why

A token cannot express two things, so these stay:

1. **Inversions** — yellow ground with black ink, where the rest of the page is
   the reverse: `.utility-button[aria-pressed='true']`, `.primary-nav-item.active`,
   the `.category-badge` family.
2. **Divergent roles of one token.** `--primary-color` is *overloaded*: it is the
   utility bar's **background** (wants `#000`) and heading **text** (wants
   `#ff0`). No single value satisfies both, so both stay explicit. **Splitting it
   into surface/ink roles is tracked tech debt** — until then, that overload is
   the main reason `themes.css` still has per-component rules.
3. **Shape, not colour** — `body.high-contrast .hero-logo { border-radius: 6px }`
   is the only one.
4. **Specificity work** — see the trap below.

`--warning-color` is deliberately *not* flipped: it is only ever a background
behind white text, so `#ff0` would make `.update-urgency` yellow-on-white.

## CSS Modules — naming and convention

**Every component built from now on uses a co-located module.** One folder per
component, module named after it:

```
components/ContentCard/ContentCard.tsx
components/ContentCard/ContentCard.module.css
```

- **Class names inside a module are local, so keep them short and semantic** —
  `.card`, `.title`, `.meta`, not `.contentCardTitle`. The filename already
  provides the namespace; repeating it is noise.
- **Vite needs no configuration** — `*.module.css` works out of the box.
- **Existing components stay where they are until a slice touches them.** Moving
  a component into a folder changes its import path, so the move rides along with
  work that was editing it anyway rather than creating a churn-only diff. The
  Front Door redesign rewrites most pages; migrating them early would be wasted.
- Tests assert roles and text, **never class names**, so scoped hashes cannot
  break them. Keep it that way.

## `index.css` — the quarantine rule

**End state: an import manifest and nothing else.**

```css
@import './styles/tokens.css';
@import './styles/base.css';
@import './styles/themes.css';
```

**Today it also holds component rules that have not yet moved. It is a
QUARANTINE, not a home: rules only ever leave it.**

> **Never add a rule to `index.css` for a component built after this convention
> was adopted.** New component styles belong in that component's `.module.css`.

This is the likeliest regression, because it is always the path of least
resistance in the moment — the file is already open, the class already works.

**Enforced as a ratchet: the selector count may only ever decrease.**

```sh
grep -cE "^[.#a-zA-Z].*\{" frontend/src/index.css
```

It was 167 before the cleanup and is **91** now. A change that increases it has
done something wrong.

## Typography roles

**Components do not choose a typeface.** They declare what kind of text something
is, and the role decides. Seven roles live in `styles/typography.module.css`.

```css
.title {
  composes: displayPage from '../../styles/typography.module.css';
  font-size: clamp(2rem, 5vw, 2.75rem);   /* the component still owns size */
}
```

**Why `composes` and not a global utility class.** A global `.display-page`
would have to live in `index.css` — a quarantine whose selector count may only
ever DECREASE. Seven role classes there would break the ratchet and reopen the
global namespace this architecture exists to close. `composes` keeps every class
locally scoped while the declarations live in one file.

**Roles carry family and weight only** — never `font-size`, never
`line-height`. A composed class lands in a different place in the stylesheet
than the component composing it, so a role that also set leading could silently
win or lose against a component that sets its own. Restricting roles to
properties no component currently varies makes composition predictable.

**The dividing line:**

| | |
| --- | --- |
| **Montserrat** | hierarchy, destination, brand |
| **Open Sans** | the working interface, reading, compact content hierarchy |

A new Montserrat use requires a display or brand rationale. If a role is
"a title, but small and functional", it is Open Sans — an A/B on card titles
settled that, and the reasoning is in `typography.module.css`.

**Apply a role to the element that carries the TEXT, not to a layout wrapper.**
Font properties inherit, so composing `brand` onto a flex container put
Montserrat on the wordmark *and* the tagline beside it. The role belongs on
`.appName`, not on the `<Link>` that wraps it.

## When `:global()` is allowed

**Reserved for top-level theme selectors: `:root`, `html`, `body.high-contrast`.**

**Never use it to style a component class** — that re-opens exactly the global
namespace this architecture closes.

In practice it should be rare to the point of absent. Because high contrast is
token-driven, a component written against tokens needs **no** escape hatch. If a
module seems to need `:global(body.high-contrast) &`, the likely real answer is
that a value should have become a token.

## Traps that have actually bitten

**A rule can look redundant by VALUE while doing SPECIFICITY work.**
`body.high-contrast .category-update-title { color: #fff }` looked redundant once
`--text-primary` was `#fff`. It was not: an update title is an `<a>` when its
source has a URL, and `body.high-contrast a` (0,1,2) outranks
`.category-update-title` (0,1,0). Removing it turned every linked title yellow
and collapsed its hover state into a no-op. **Before deleting a theme rule, ask
what else could match the same element.**

**Dynamically built classes look unused.** `badge-resource|law|news|flyer|expert`
are constructed as `` `badge-${contentType.toLowerCase()}` `` and never appear as
literals. A grep-the-class-name sweep deletes all five.

**Hyphens are word boundaries.** `\blogo\b` matches *inside* `hero-logo`. Use
`(?<![\w-])name(?![\w-])` when auditing.

**Only rendered output catches these.** The DOM was identical and every test
passed in all three cases. Any change to shared styles needs a before/after
comparison of the actual pixels, in **both** themes and **both** viewport widths.

## Definition of done for a style change

- [ ] New component styles are in a co-located `.module.css`.
- [ ] Colours reference tokens, not literals.
- [ ] `index.css` selector count did not increase.
- [ ] No new `:global()` outside `:root` / `html` / `body.high-contrast`.
- [ ] Verified in light **and** high contrast, at desktop **and** 375px.
- [ ] Every colour comes from a **defined** token — no `var(--name, #literal)`
      fallbacks on colour, and no raw hex in a component rule.
- [ ] If the component uses `--primary-color` as INK, it declares its own
      `:global(body.high-contrast)` rule. That token is deliberately not flipped.

### Why those last two were added (Slice K)

`ArticlePage` shipped its title and headings as:

```css
color: var(--brand-green, #1a5c38);   /* --brand-green does not exist */
```

Every rule fell silently through to the literal, which cannot participate in a
theme. Rendered dark green on black: **2.63:1**, on the most important text on
the page.

Switching to `--primary-color` fixed nothing — the measurement still said
2.63:1 — because that token is **deliberately not flipped** (it is overloaded as
both a surface and an ink colour; see `styles/themes.css`). The real convention
is that each component declares its own high-contrast rules, and that block had
been omitted entirely. Measured after the real fix: **19.56:1**.

**The lesson is not that a rule was missing. Line 206 above already required
verifying both themes, and it was skipped.** What the toolchain could see was
all green: 87 frontend tests passed, `tsc` was clean, and the light theme looked
correct. **No test, type check or default-theme screenshot can observe a colour
contrast failure.** Only rendering the alternate theme and *measuring* it can —
which is why contrast on this project is measured, never eyeballed.

A fallback literal is the specific trap: it exists to make a missing token
harmless, and its actual effect is to make a missing token **invisible**.
