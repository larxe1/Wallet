---
name: Money Tracker
description: Dark obsidian-emerald personal finance workspace with sculpted jade surfaces, perceptual gradients, tabular numeric clarity, and tactile spring motion.
colors:
  obsidian-canvas: "#09110F"
  jade-surface: "#121E1B"
  jade-elevated: "#172622"
  jade-muted: "#1D2E2A"
  ambient-emerald-glow: "#133C2F"
  emerald-primary: "#28C991"
  emerald-bright: "#35D49A"
  emerald-mid: "#17A376"
  emerald-deep: "#0E6E52"
  electric-violet: "#8E7CF8"
  violet-deep: "#6A58E8"
  chartreuse-lime: "#C8F865"
  semantic-danger: "#F06478"
  semantic-warning: "#F0B44A"
  text-primary: "#F2F7F5"
  text-secondary: "#98ADA6"
  text-muted: "#6E857D"
  text-on-accent: "#051A13"
typography:
  display:
    fontFamily: "Manrope, system-ui, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 800
    lineHeight: 1.15
    letterSpacing: "-0.03em"
  headline:
    fontFamily: "Manrope, system-ui, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "-0.025em"
  title:
    fontFamily: "Manrope, system-ui, sans-serif"
    fontSize: "1.05rem"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "-0.02em"
  body:
    fontFamily: "Manrope, system-ui, sans-serif"
    fontSize: "0.9rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0em"
  mono:
    fontFamily: "JetBrains Mono, ui-monospace, monospace"
    fontSize: "0.875rem"
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: "-0.02em"
    fontFeature: "\"tnum\" 1"
rounded:
  sm: "10px"
  md: "14px"
  lg: "20px"
  xl: "24px"
  pill: "9999px"
spacing:
  xs: "6px"
  sm: "10px"
  md: "16px"
  lg: "24px"
  xl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.emerald-primary}"
    textColor: "{colors.text-on-accent}"
    rounded: "{rounded.sm}"
    padding: "0.575rem 1.05rem"
  button-secondary:
    backgroundColor: "{colors.jade-elevated}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.sm}"
    padding: "0.575rem 1.05rem"
  surface-card:
    backgroundColor: "{colors.jade-surface}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.lg}"
    padding: "20px"
  input-field:
    backgroundColor: "{colors.obsidian-canvas}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.sm}"
    padding: "0.625rem 0.875rem"
---

# Design System: Money Tracker

## Overview

**Creative North Star: "The Obsidian Ledger"**

Money Tracker is an executive dark-emerald personal finance workspace designed to feel calm, tactile, and immediately scannable across multi-wallet balances, cashflow trends, and daily transaction schedules. Instead of flat dark voids or decorative glassmorphism stacked inside glassmorphism, the system separates **sculpted solid dark-jade surfaces** for persistent data from **translucent frosted overlays** reserved strictly for floating navigation rails, top command search, modal dialogs, toast notifications, and the overlapping wallet transactions tray.

Motion and responsiveness operate as quiet structural feedback rather than spectacle. Route switches leverage the browser View Transitions API (`document.startViewTransition`) when available, interactive segmented controls and active rail tabs glide via Motion shared `layoutId` spring pills, numeric KPIs interpolate smoothly through `AnimatedNumber`, and every transition honors `prefers-reduced-motion: reduce` across CSS, Motion (`<MotionConfig reducedMotion="user">`), and `::view-transition-*` pseudo-elements.

**Key Characteristics:**
- **Color-True Gradients:** Progressive enhancement via `@supports (background: linear-gradient(in oklab, white, black))` sets `--in-oklab` / `--in-oklch` modifiers on gradient tokens so emerald and violet transitions interpolate without muddy gray midpoints.
- **Restrained Material Separation:** Solid `#121E1B` and `#172622` containers hold ledgers and charts; `backdrop-filter` blur is reserved exclusively for floating layers.
- **Numeric Precision:** `Manrope` handles structural hierarchy while `JetBrains Mono` with `tabular-nums` anchors currency (`₱`), dates, and ledger totals.
- **Unified Stroke Iconography:** `lucide-react` replaces legacy glyph/emoji UI chrome across all navigation, form controls, feedback banners, and table actions, while user-authored category emojis stay bounded inside tinted category pills.

## Colors

The palette pairs deep obsidian-emerald surfaces with luminous mint-emerald primary actions, electric violet analytical focus states, and chartreuse lime accents.

### Primary
- **Emerald Primary / Bright / Mid / Deep** (`emerald-primary`, `emerald-bright`, `emerald-mid`, `emerald-deep`): Drives primary CTAs, active rail and filter pills, positive income trends, focus rings (`:focus-visible` 2px outline and `accent-color`), and the front card mesh gradient of the Wallet Card Stack. Perceptual gradients (`--grad-emerald` and `--grad-emerald-pill`) transition cleanly across these stops using `var(--in-oklab)`.

### Secondary
- **Electric Violet & Violet Deep** (`electric-violet`, `violet-deep`): Reserved for analytical emphasis—the active/hovered bar highlight in `TrendChart`, expense-share pills on KPI cards, debit payment badges, and the top donut slice in `CategoryPieChart`.
- **Chartreuse Lime** (`chartreuse-lime`): High-contrast pinpoint highlight used on the brand emblem node, latest release badges, and secondary donut chart segments.

### Semantic
- **Semantic Danger** (`semantic-danger`): Expense outflow indicators, destructive confirmation buttons, error toasts, and `:user-invalid` form validation borders.
- **Semantic Warning** (`semantic-warning`): Cash payment badges, warning confirmation/toast icons, and interval scheduling alerts.

### Neutral
- **Obsidian Canvas & Ambient Glow** (`obsidian-canvas`, `ambient-emerald-glow`): Fixed deep canvas layered with subtle large-radius radial emerald ambient fields so cards sit inside a cohesive atmospheric room rather than a stark black void.
- **Jade Surface Ramp** (`jade-surface`, `jade-elevated`, `jade-muted`): Three-step elevation hierarchy providing crisp tonal separation between base containers, elevated modals/popovers, and interactive hover/muted wells.
- **Text Hierarchy** (`text-primary`, `text-secondary`, `text-muted`, `text-on-accent`): High-contrast warm-mint off-white for primary figures and headings, muted sage for labels and metadata, and deep forest ink (`text-on-accent`) on bright emerald pills to maintain strong readability.

### Named Rules
**The Specular Hairline Rule.** Structural boundaries rely on low-opacity white hairlines (`rgba(255, 255, 255, 0.07)` at rest, `0.14` on glass/hover) paired with a 1px top inner specular highlight (`inset 0 1px 0 rgba(255, 255, 255, 0.06)`), never thick colored `border-left` or `border-right` stripes.

**The Single-Emphasis Accent Rule.** Reserve vivid emerald and violet gradients for active states, primary actions, and focused data points; inactive controls stay neutral dark jade so active selections pop immediately.

## Typography

**Display & UI Font:** `Manrope` (`400, 500, 600, 700, 800`), Google Fonts (`sans-serif` fallback).
**Numeric & Code Font:** `JetBrains Mono` (`500, 600`), Google Fonts (`ui-monospace` fallback).

**Character:** Geometric, open apertures with tight negative tracking on headings give financial summaries modern fintech authority without decorative serif flourishes (`Fraunces` is retired completely).

### Hierarchy
- **Display** (`800`, `1.5rem–1.75rem`, line-height `1.15`, tracking `-0.03em`): Top-level page titles (`My Dashboard`, `Money Tracker` login heading).
- **Headline** (`700`, `1.25rem`, line-height `1.25`, tracking `-0.025em`): Section and modal headers (`Release Notes`, `ExpenseForm`, `IncomeForm`).
- **Title** (`700`, `1.05rem`, line-height `1.3`, tracking `-0.02em`): Card headers (`Cashflow & Spending`, `Category Breakdown`, `My Wallets`).
- **Body** (`400–600`, `0.875rem–0.925rem`, line-height `1.5`): Descriptions, form labels, ledger entries, and navigation tooltips (`text-wrap: balance` applied to headings).
- **Monospace / Tabular** (`500–600`, `0.8rem–1.35rem`, `font-variant-numeric: tabular-nums`): Currency amounts (`₱`), ledger columns, calculator display, axis ticks, and release version tags.

### Named Rules
**The Tabular Alignment Rule.** Every currency value, percentage, date badge, and numeric KPI uses `.amount` or `.tabular-nums` (`font-variant-numeric: tabular-nums; font-feature-settings: "tnum" 1;`) so columns and live `AnimatedNumber` transitions never jitter horizontally.

**The Heading Autonomy Rule.** Headings speak directly without eyebrow labels or kickers stacked above them.

## Layout

Money Tracker uses a persistent floating navigation shell enclosing responsive single- or two-column content grids.

- **Desktop Navigation Rail (`>= 900px`):** Fixed slim left floating icon rail (`76px` wide, inset `16px` from top/left/bottom) dividing primary workspace destinations (`Dashboard`, `Expenses`, `Income`, `Calendar`, `Recurring`) from utility controls (`Settings`, `Release Notes`) and bottom `Sign Out`. Content reserves `margin-left: 108px` and centers inside a `1480px` max-width container.
- **Top Header Bar:** Three-zone grid (`auto minmax(260px, 470px) auto`) anchored above `<Outlet />`:
  - *Left:* Geometric emerald SVG brand mark, `Money Tracker` wordmark, and clickable `v1.1.0` changelog trigger pill.
  - *Center:* Global Quick Search pill (`Ctrl K` / `/` shortcut) querying pages and current-month transactions.
  - *Right:* Email-derived greeting pill (`Hi, <Name>`) + gradient monogram avatar (plus compact mobile sign-out button below `900px`).
- **Dashboard Executive Grid:** Two-column desktop grid (`minmax(0, 1fr) 380px`, `24px` gap) pairing left analytical controls (header bar, wallet pill filter, primary chart card, and `1.15fr / 0.85fr` bottom split for donut chart + stacked Income/Expense KPIs) with the right-hand `My Wallets` 3D stacked deck, overlapping frosted Transactions tray, and 2-column mini insight widgets (`Recurring` + `Spending Pace`).
- **Mobile & Tablet Adaptation (`< 900px` / `< 768px`):**
  - Left rail transforms into a fixed bottom floating dock bar (`height: 64px`, `12px` inset) with `96px` bottom content padding so dock controls never obscure ledger totals.
  - Top bar wraps cleanly into two rows below `720px` (brand + user on row 1, full-width quick search on row 2).
  - Multi-column data tables on `/expenses` and `/income` switch to responsive card stacks below `768px` to eliminate horizontal scroll overflow.

## Elevation & Depth

Depth uses a strict two-tier separation: **solid tonal layering** for resting surfaces and **frosted backdrop translucency** strictly for floating overlays.

### Shadow Vocabulary
- **Subtle Rest (`--shadow-sm`):** `0 2px 6px rgba(0, 0, 0, 0.28)` — compact pills, top bar container, and chips.
- **Card Surface (`--shadow-md` + specular):** `0 10px 24px -6px rgba(0, 0, 0, 0.46), 0 4px 10px -2px rgba(0, 0, 0, 0.3)` plus `inset 0 1px 0 rgba(255, 255, 255, 0.06)` — analytical cards, tables, and KPI panels.
- **Floating & Top-Layer (`--shadow-lg` + glass blur):** `0 22px 48px -12px rgba(0, 0, 0, 0.62), 0 8px 18px -4px rgba(0, 0, 0, 0.36)` with `backdrop-filter: blur(20px–22px)` — left navigation rail, search dropdown, modals, native `<dialog>` prompts, toast stack, and the overlapping wallet transactions tray.

### Named Rules
**The Grounded Card Rule.** Resting cards never jump vertically (`translateY`) on hover or float indefinitely with idle keyframe loops; hover feedback adjusts border luminosity cleanly.

**The No-Nested-Glass Rule.** Solid dark-jade cards never contain inner `.glass-card` containers or stacked blur panels; charts, tables, and filters render directly inside their single parent surface.

## Shapes

- **Radius Ladder:** Inputs and standard buttons use `10px`; inner icon badges, calculator keys, and segmented thumbs use `10px–14px`; cards and floating panels step up to `20px–24px`; floating rail containers use `26px`; toggles, wallet filters, search bar, and status tags use full capsule pills (`9999px`).
- **Brand & Icon Geometry:** The `Money Tracker` mark is a geometric emerald-gradient rounded square carrying an ascending angular pulse line ending in a chartreuse-lime vertex node. All UI icons derive from `lucide-react` at consistent `1.85–2.25` stroke weights.
- **Data Visualization Shapes:** `TrendChart` bars feature rounded top caps (`topRadius: 10`, `bottomRadius: 4`) with diagonal SVG `<pattern id="hatched-bar">` fills for inactive periods and solid violet gradients for the focused period; `CategoryPieChart` uses rounded donut segments (`cornerRadius: 10`, `paddingAngle: 5`).

## Components

### Buttons
- **Primary:** Rounded rectangle (`10px`) or capsule (`9999px` for page CTAs), filled with `--grad-emerald-pill`, dark ink text (`#051A13`), subtle emerald elevation shadow, brightening (`brightness(1.06)`) on hover.
- **Secondary & Ghost:** Solid `--bg-surface-elevated` with subtle `1px` border (`Secondary`), or transparent hoverable `--bg-surface-muted` (`Ghost`).
- **Danger:** Solid `--accent-danger` with high-contrast white label for confirmed deletions.

### Chips, Toggles & Filters
- **Segmented Pill Switches:** Capsule wells (`--bg-canvas` or `--bg-surface`) housing animated `motion.span` `layoutId` indicator thumbs (Login Sign-In/Create Account, Dashboard Monthly/Annual & chart view tabs, Recurring type/frequency toggles, WalletFilter chips).
- **Accessible Switches:** Real `<button role="switch" aria-checked>` controls with animated thumb translation for Recurring active/paused state and Expense installment splitting.
- **Dropdown Popovers:** `MonthSelector` and `MultiSelectDropdown` render frosted elevated popovers with `AnimatePresence`, Escape/click-outside dismissal, and `aria-expanded` attributes.

### Cards & Signature Wallet Deck
- **Sculpted Surface Card (`.surface-card`):** `20px` radius, `#121E1B` solid fill, `1px` `rgba(255,255,255,0.07)` border, inner specular top highlight.
- **Wallet Card Stack (`WalletCardStack` + `BalanceCard`):** 3D fanned vertical deck (`224px` card height, `22px` radius) featuring a luminous radial/linear emerald mesh front card with decorative light-wave SVG, net balance (`AnimatedNumber` honoring default privacy masking `••••••`), circular white `+` quick-add trigger, and interactive stacked cards behind that spring to the front on click or prev/next cycling.
- **Overlapping Transactions Tray:** Positioned seamlessly over the lower margin of the front wallet card using `rgba(18, 32, 28, 0.72)` frosted glass (`blur(22px)`) and top specular highlight.

### Inputs & Native Browser Integration
- **Form Fields:** Deep `#09110F` well (`10px` radius), subtle border, crisp emerald focus ring (`box-shadow: 0 0 0 3px rgba(40, 201, 145, 0.16)` + `:focus-visible` `2px` outline), and native `:user-invalid` / `:user-valid` feedback states (explicitly excluding `range`, `checkbox`, and `radio` inputs).
- **Browser Surfaces:** Global `color-scheme: dark`, emerald `::selection` tint, themed `scrollbar-color`, `accent-color: #28C991`, and `@media (forced-colors: active)` system-color fallbacks (`Canvas`, `CanvasText`, `Highlight`, `ButtonBorder`).

### Navigation, Modals & Feedback
- **Icon Rail & Tooltips:** `44px × 44px` hit targets inside rounded dark-jade clusters; active route renders a luminous mint-emerald `layoutId="sidebar-active-pill"` capsule; hover and `:focus-visible` reveal positioned side tooltips.
- **Native `<dialog>` & Form Modals:** `FeedbackContext` confirmation dialogs and `ChangelogModal` use native top-layer `<dialog>` elements enhanced with `@starting-style` and `transition-behavior: allow-discrete` entry/exit animations; form modals (`ExpenseForm`, `IncomeForm`, `RecurringForm`, `CategoryForm`) enforce `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, `htmlFor` label associations, and `Escape` dismissal.
- **Toast Notifications:** Top-right frosted stack (`aria-live="polite"`) with uniform `1px` semantic borders and Lucide status icons (`CheckCircle2`, `AlertCircle`, `AlertTriangle`, `Info`).

## Do's and Don'ts

### Do:
- **Do** format every monetary amount via `formatCurrency()` (`en-PH`, `₱`) and every date string via `parseLocalDate()` / `formatDate()` with `tabular-nums`.
- **Do** use `lucide-react` for all interactive UI icons and reserve user-selected emojis strictly for category badges (`category.icon`).
- **Do** preserve `:focus-visible` `2px solid #28C991` outlines across all interactive buttons, links, switches, and inputs.
- **Do** route confirmations and feedback through `useFeedback()` (`confirm()` and `showToast()`) rather than native browser alerts.
- **Do** guard every motion effect and view transition behind `<MotionConfig reducedMotion="user">` and `@media (prefers-reduced-motion: reduce)`.

### Don't:
- **Don't** reintroduce `Fraunces` or decorative serif display typography anywhere in the app.
- **Don't** nest cards inside cards or place `.glass-card` inside `.glass-card`.
- **Don't** apply thick (`> 1px`) colored `border-left` or `border-right` stripes on cards, nav links, alerts, or toasts.
- **Don't** use gradient text (`background-clip: text`), hover card jumping (`translateY`), infinite floating keyframes (`@keyframes float`), decorative grid-line CSS backgrounds, or eyebrow kickers above headings.
- **Don't** suppress focus indicators with bare `outline: none` without a visible `:focus-visible` replacement.
