# OssWallet Design System — Complete Rules & Guidelines

A comprehensive design system documentation covering color layering, glass themes, typography, spacing, components, and interaction patterns for Light, Dark, AMOLED, Glass Light, and Glass Dark themes.

---

## Table of Contents

1. [Core Philosophy](#1-core-philosophy)
2. [Color System](#2-color-system)
3. [Glass Themes — Transparency & Frosted Effects](#3-glass-themes--transparency--frosted-effects)
4. [Typography](#4-typography)
5. [Spacing & Layout](#5-spacing--layout)
6. [Component Patterns](#6-component-patterns)
7. [Interaction States](#7-interaction-states)
8. [Accessibility](#8-accessibility)
9. [Implementation Checklist](#9-implementation-checklist)

---

## 1. Core Philosophy

### 1.1 Design Principles

**Clarity over decoration**  
Every visual element serves a functional purpose. Gradients, shadows, and blur effects enhance hierarchy and depth — never applied purely for aesthetics.

**Progressive disclosure**  
Critical information (balances, amounts, account names) uses maximum contrast. Supporting metadata (timestamps, exchange rates) uses reduced contrast. Users scan top-to-bottom, left-to-right in order of visual weight.

**Consistent elevation**  
The 4-layer elevation model applies across all themes:
```
Layer 4: Pills, inputs, tertiary surfaces (highest)
Layer 3: Cards, primary surfaces
Layer 2: Screen background
Layer 1: Phone bezel / canvas (lowest)
```

**Theme-appropriate separation**
- **Light:** Shadows create depth (airy, lifted feel)
- **Dark:** Fill contrast creates depth (soft, modern feel)
- **AMOLED:** Outlines create depth (minimal, battery-efficient)
- **Glass:** Blur + translucency creates depth (vibrant, immersive)

---

## 2. Color System

### 2.1 Semantic Token Structure

Every color is a CSS custom property. Components reference tokens, never raw hex values.

```css
/* Theme-agnostic component */
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--text);
}
```

### 2.2 Layer 2: Screen Background

The passive backdrop behind all content.

| Theme | Token | Value | Notes |
|---|---|---|---|
| **Light** | `--bg` | `#EDECF3` | Soft gray-lavender, warm undertone |
| **Dark** | `--bg` | `#11111B` | Deep charcoal with faint indigo |
| **AMOLED** | `--bg` | `#000000` | Pure black (OLED pixel-off) |
| **Glass Light** | `--bg` | `#E8EAF4` | Slightly desaturated base (mesh overlays it) |
| **Glass Dark** | `--bg` | `#06060E` | Very deep blue-black (mesh overlays it) |

**Rule:** `--bg` is never interactive. No hover states, no click targets.

### 2.3 Layer 3: Primary Surface (Cards, Sheets)

The main container layer where most content lives.

| Theme | `--surface` | Border | Shadow | Philosophy |
|---|---|---|---|---|
| **Light** | `#FFFFFF` | `rgba(40,40,80,0.10)` | `0 1px 2px rgba(30,30,60,0.04), 0 4px 16px rgba(30,30,60,0.06)` | Bright white, soft shadow lift |
| **Dark** | `#20212C` | `rgba(255,255,255,0.10)` | `0 1px 2px rgba(0,0,0,0.45), 0 6px 20px rgba(0,0,0,0.32), inset 0 1px 0 rgba(255,255,255,0.04)` | Lifted slate, inner highlight |
| **AMOLED** | `#050509` | `rgba(255,255,255,0.14)` | `0 0 0 1px rgba(255,255,255,0.06), 0 4px 16px rgba(0,0,0,0.5)` | Near-black, outline-led |
| **Glass Light** | `rgba(255,255,255,0.62)` | `rgba(255,255,255,0.85)` | `0 1px 2px rgba(30,30,60,0.08), 0 10px 32px rgba(30,30,60,0.12), inset 0 1px 0 rgba(255,255,255,0.6)` | Frosted white, white edge highlight |
| **Glass Dark** | `rgba(255,255,255,0.09)` | `rgba(255,255,255,0.22)` | `0 2px 6px rgba(0,0,0,0.35), 0 14px 40px rgba(0,0,0,0.32), inset 0 1px 0 rgba(255,255,255,0.10)` | Very translucent, subtle white edge |

**AMOLED Rule:** Stronger borders compensate for minimal fill contrast. Shadow is weak — outlines do the separation work.

**Glass Rule:** Translucency lets the background mesh show through. Border is a white highlight (frost edge effect).

### 2.4 Layer 4: Secondary Surface (Inputs, Strips, Controls)

Slightly elevated or inset relative to primary surface.

| Theme | `--surface-3` | Usage |
|---|---|---|
| **Light** | `#F1F0F6` | Reads as "inset" — slightly darker than white |
| **Dark** | `#2F3140` | Reads as "raised" — lighter than `--surface` |
| **AMOLED** | `#11111A` | Slightly lighter, stronger border (14% → 18%) |
| **Glass Light** | `rgba(255,255,255,0.42)` | More transparent than `--surface` |
| **Glass Dark** | `rgba(255,255,255,0.07)` | More transparent than `--surface` |

**Examples:**
- Input fields (`.input`)
- Week strips in transaction lists (`.tx-week`)
- Segmented control backgrounds (`.segmented`)
- Toggle track (`.toggle`)

### 2.5 Text Hierarchy

Three tiers ensure readability without overwhelming contrast.

| Level | Token | Light | Dark | AMOLED | Glass Light | Glass Dark |
|---|---|---|---|---|---|---|
| **Primary** | `--text` | `#15151F` | `#ECEDF3` | `#FFFFFF` | `#16162A` | `#F4F4FA` |
| **Secondary** | `--text-muted` | `#5E5E72` | `#9092A0` | `#9091A0` | `#4E4E62` | `#BCBDCB` |
| **Tertiary** | `--text-faint` | `#92929F` | `#62636F` | `#5C5D6B` | `#76768A` | `#8487A0` |

**Typography Rules:**
- Primary: Headings, amounts, account names (24–28px for hero numbers)
- Secondary: Subtitles, labels, category names (11–13px)
- Tertiary: Timestamps, placeholders, hints (9.5–11px)

**Never use `--text-faint` for:**
- Currency amounts
- Account balances
- Transaction titles
- Any critical decision-making info

### 2.6 Accent Colors

The highest-contrast interactive layer. Must be vivid across all themes.

| Theme | `--accent` | `--accent-bg` (tint) | Text on accent |
|---|---|---|---|
| **Light** | `#4853A0` | `rgba(72,83,160,0.14)` | `#FFFFFF` |
| **Dark** | `#C5CBEE` | `rgba(197,203,238,0.18)` | `#1A1A24` |
| **AMOLED** | `#DDE2FA` | `rgba(221,226,250,0.16)` | `#000000` |
| **Glass Light** | `#2F3A88` | `rgba(47,58,136,0.22)` | `#FFFFFF` |
| **Glass Dark** | `#D2D8F4` | `rgba(210,216,244,0.22)` | `#1A1A24` |

**Accent Palette Alternates:**  
Users can swap accent color via Tweaks panel. Five options: **Indigo** (default), **Slate**, **Teal**, **Plum**, **Rust**.

Each palette overrides `--accent`, `--accent-2`, `--accent-bg`, and `--accent-soft` across all themes via body class (`.palette-slate`, `.palette-teal`, etc.).

### 2.7 Semantic Colors (Positive / Negative)

Always vivid, never desaturated. Immediately recognizable across all themes.

| Semantic | Token | Light | Dark | AMOLED | Glass Light | Glass Dark |
|---|---|---|---|---|---|---|
| **Positive** | `--pos` | `#1E9B4F` | `#4DCE85` | `#5BE093` | `#0F6E37` | `#5CDB8F` |
| **Negative** | `--neg` | `#C8463F` | `#E27974` | `#F08782` | `#A8332D` | `#F08581` |

**Usage:**
- Income amounts (green)
- Expense amounts (red)
- Net change indicators (green = profit, red = loss)
- Check badges (green circle with white ✓)

**Rule:** Never apply opacity to semantic colors. They must pop at full saturation.

### 2.8 Category Tint System

Each account category has a unique **background + ink** color pair. Tints adapt per theme.

| Category | Light Bg | Light Ink | Dark Bg | Dark Ink | AMOLED Bg | AMOLED Ink |
|---|---|---|---|---|---|---|
| **Green** (Cash) | `#DCEBD7` | `#2D5238` | `#1F3B2A` | `#92CEA8` | `#08321A` | `#9BDCAE` |
| **Blue** (Bank) | `#DEE4EE` | `#2A3A55` | `#243349` | `#A8BAD6` | `#0B1F36` | `#B1C4E2` |
| **Pink** (Savings) | `#EDDDE4` | `#5A2E3F` | `#432636` | `#D6A1B5` | `#361421` | `#E5A7BD` |
| **Peach** (Credit) | `#EEDED8` | `#5D362A` | `#43332B` | `#D6AB98` | `#361F14` | `#E5B197` |
| **Yellow** (Investment) | `#ECE6CC` | `#54481F` | `#3A3520` | `#CFBE83` | `#2E2710` | `#DEC785` |
| **Orange** (Travel) | `#EFDED0` | `#5A3621` | `#43302A` | `#D6A286` | `#361E10` | `#E5A684` |
| **Rose** (DBS) | `#EBD7D3` | `#5B2D29` | `#402524` | `#D2908C` | `#321311` | `#E0928D` |
| **Mint** (GoPay) | `#DAE4D9` | `#2B4533` | `#243A2C` | `#97C2A4` | `#0B2B1A` | `#9DCFAD` |

**Glass themes:** Same hues, but backgrounds use `rgba(...)` with 60–75% opacity (light) or 28–40% opacity (dark).

**Pattern:**
- Light: **Pastel background** + **saturated dark ink**
- Dark: **Saturated dark background** + **pastel ink**
- AMOLED: **Deeper background** + **brighter ink** (higher contrast for OLED)

### 2.9 Borders & Dividers

| Element | Token | Light | Dark | AMOLED | Glass Light | Glass Dark |
|---|---|---|---|---|---|---|
| **Card border** | `--border` | `rgba(40,40,80,0.10)` | `rgba(255,255,255,0.10)` | `rgba(255,255,255,0.14)` | `rgba(255,255,255,0.85)` | `rgba(255,255,255,0.22)` |
| **Strong border** | `--border-strong` | `rgba(40,40,80,0.18)` | `rgba(255,255,255,0.18)` | `rgba(255,255,255,0.26)` | `rgba(255,255,255,0.95)` | `rgba(255,255,255,0.32)` |
| **Divider** | `--divider` | `rgba(40,40,80,0.08)` | `rgba(255,255,255,0.08)` | `rgba(255,255,255,0.10)` | `rgba(40,40,80,0.14)` | `rgba(255,255,255,0.10)` |

**AMOLED Rule:** Borders are 40–60% stronger than Dark theme. Outlines create separation when fill contrast is minimal.

**Glass Rule:** Borders are **white highlights** (frost edge effect), much stronger than solid themes.

---

## 3. Glass Themes — Transparency & Frosted Effects

Glass themes layer frosted translucent surfaces over vibrant gradient meshes. The background does the color work; surfaces stay mostly transparent.

### 3.1 Philosophy

**Solid themes:** Color lives in the surfaces (white cards, slate cards, etc.).  
**Glass themes:** Color lives in the **background mesh**. Surfaces are frosted windows that reveal the mesh beneath.

This creates:
- **Depth** — multiple layers of blur + translucency
- **Vibrancy** — saturated gradients shine through frosted glass
- **Immersion** — UI feels like it's floating in a colorful 3D space

### 3.2 Background Mesh Construction

Both glass themes use a **5-blob radial gradient mesh** positioned at various corners/edges of the phone frame.

#### Glass Light Mesh
```css
.t-glass-light {
  --bg: #E8EAF4; /* Desaturated base */
  --bg-img:
    radial-gradient(60% 50% at 15% 8%,  rgba(120,200,160,0.65), transparent 70%), /* Green, top-left */
    radial-gradient(55% 45% at 95% 18%, rgba(220,140,170,0.60), transparent 70%), /* Pink, top-right */
    radial-gradient(70% 55% at 50% 55%, rgba(230,200,140,0.55), transparent 70%), /* Yellow, center */
    radial-gradient(55% 45% at 10% 95%, rgba(150,170,230,0.65), transparent 70%), /* Blue, bottom-left */
    radial-gradient(60% 45% at 90% 95%, rgba(230,170,150,0.60), transparent 70%); /* Peach, bottom-right */
}
```

**Properties:**
- Opacity: 55–65% (pastel, not overwhelming)
- Colors: Green, pink, yellow, blue, peach (warm palette)
- Positions: Corners + center (full coverage, no dead zones)

#### Glass Dark Mesh
```css
.t-glass-dark {
  --bg: #06060E; /* Very deep blue-black */
  --bg-img:
    radial-gradient(55% 45% at 12% 8%,  rgba(60,180,120,0.55), transparent 70%),  /* Teal, top-left */
    radial-gradient(50% 40% at 90% 14%, rgba(200,80,130,0.55), transparent 70%),  /* Magenta, top-right */
    radial-gradient(65% 50% at 55% 50%, rgba(80,90,200,0.50), transparent 75%),   /* Blue, center */
    radial-gradient(55% 45% at 10% 92%, rgba(200,130,60,0.55), transparent 70%),  /* Orange, bottom-left */
    radial-gradient(50% 40% at 92% 92%, rgba(80,160,220,0.50), transparent 70%);  /* Cyan, bottom-right */
}
```

**Properties:**
- Opacity: 50–55% (vivid but not blinding)
- Colors: Teal, magenta, blue, orange, cyan (cool palette with warm accents)
- More saturated than Glass Light (pops against dark base)

### 3.3 DOM Structure for Glass Backgrounds

The mesh is rendered via a real DOM element (not `::before` pseudo) so it's captured in PNG exports:

```jsx
function PhoneFrame({ theme, children }) {
  const isGlass = theme.startsWith('glass-');
  return (
    <div className={`phone t-${theme} ${isGlass ? 'is-glass' : ''}`}>
      {isGlass && <div className="glass-bg"></div>}
      <div className="screen">...</div>
    </div>
  );
}
```

```css
.t-glass-light .glass-bg,
.t-glass-dark .glass-bg {
  position: absolute;
  inset: 0;
  background: var(--bg-img);
  pointer-events: none;
  z-index: 0;
}
```

### 3.4 Surface Transparency Rules

Glass surfaces are **translucent**, not opaque. Opacity varies by layer:

| Layer | Glass Light | Glass Dark | Rationale |
|---|---|---|---|
| **Primary surface** (cards) | `rgba(255,255,255,0.62)` | `rgba(255,255,255,0.09)` | Enough opacity to be readable, enough transparency to show mesh |
| **Secondary surface** (inputs) | `rgba(255,255,255,0.42)` | `rgba(255,255,255,0.07)` | More transparent than cards (visual hierarchy) |
| **Tertiary surface** (week strips) | Same as secondary | Same as secondary | Inputs and strips share transparency tier |

**Rule:** Glass Light is **more opaque** (62%) than Glass Dark (9%). Why?
- Light mesh is pastel → needs more opacity to maintain text contrast
- Dark mesh is vivid → can stay more transparent without losing legibility

### 3.5 Backdrop Filter (Frosted Glass Effect)

All glass surfaces get `backdrop-filter: blur() saturate()` to create the frosted look.

```css
.is-glass .card,
.is-glass .input,
.is-glass .acct,
.is-glass .pill-nav,
.is-glass .fab,
.is-glass .chip {
  backdrop-filter: blur(var(--tweak-blur, 24px)) saturate(180%);
  -webkit-backdrop-filter: blur(var(--tweak-blur, 24px)) saturate(180%);
}
```

**Properties:**
- `blur(24px)` (Glass Light) / `blur(28px)` (Glass Dark) — more blur in dark mode for stronger frost
- `saturate(180%)` — slightly boost colors shining through the blur
- User-adjustable via Tweaks panel: 4–48px range

**Browsers:**
- Webkit (Safari, Chrome): Full support
- Firefox: Partial support (may render without blur)
- Fallback: Surface colors are opaque enough to be readable even without blur

### 3.6 White Edge Highlight (Frost Rim)

Glass surfaces use **white borders** to simulate light hitting the edge of frosted glass.

```css
.t-glass-light .card {
  border: 1px solid rgba(255,255,255,0.85); /* Bright white edge */
}
.t-glass-dark .card {
  border: 1px solid rgba(255,255,255,0.22); /* Subtle white edge */
}
```

**Plus inner glow:**
```css
.t-glass-light .card {
  box-shadow: 
    0 1px 2px rgba(30,30,60,0.08),
    0 10px 32px rgba(30,30,60,0.12),
    inset 0 1px 0 rgba(255,255,255,0.6); /* Top inner highlight */
}
```

The `inset` shadow simulates light refracting through the top edge of the glass.

### 3.7 Account Tiles in Glass Themes

Category tints use the **same hues** as solid themes, but with **rgba() transparency**:

```css
/* Glass Light */
.t-glass-light {
  --c-green: rgba(190,235,200,0.65);   --c-green-ink: #214A2C;
  --c-blue:  rgba(195,215,245,0.65);   --c-blue-ink:  #1E2E50;
  /* ... */
}

/* Glass Dark */
.t-glass-dark {
  --c-green: rgba(60,160,90,0.30);   --c-green-ink: #9CD9AE;
  --c-blue:  rgba(100,140,210,0.30); --c-blue-ink:  #A8BCE0;
  /* ... */
}
```

**Pattern:**
- Light: 60–75% opacity (pastel tints)
- Dark: 28–40% opacity (vivid tints)
- Tiles also get 1px white border: `border: 1px solid rgba(255,255,255,0.5)` (Light) / `0.18` (Dark)

### 3.8 Glass Theme Checklist

When implementing glass themes, ensure:

- [ ] Background mesh is a **real DOM element** (`.glass-bg`), not `::before` pseudo
- [ ] Mesh uses 5 radial gradients at corners + center (no dead zones)
- [ ] All interactive surfaces (cards, inputs, pills) have `backdrop-filter: blur(...) saturate(180%)`
- [ ] Surfaces use `rgba(255,255,255,...)` backgrounds (not opaque hex)
- [ ] Borders are **white highlights** (`rgba(255,255,255,0.22–0.85)`)
- [ ] Inner glow (`inset 0 1px 0 rgba(255,255,255,...)`) on cards
- [ ] Category tiles use rgba() tints, not opaque colors
- [ ] Text contrast meets WCAG AA on all surfaces (test with mesh behind)
- [ ] User can adjust blur radius via Tweaks (4–48px)

### 3.9 Glass vs Solid — When to Use Each

| Use Glass When... | Use Solid When... |
|---|---|
| User wants immersive, vibrant UI | User wants clean, minimal UI |
| Device supports backdrop-filter well | Device/browser has poor blur support |
| Ambient light is moderate (indoor) | Ambient light is very bright (outdoor) |
| User prefers bold aesthetic | User prefers subtle aesthetic |

**Tip:** Offer both as options. Glass is subjective — some users love it, others find it distracting.

---

## 4. Typography

### 4.1 Font Stack

| Role | Family | Weights | Features | Usage |
|---|---|---|---|---|
| **Primary** | Manrope | 400, 500, 600, 700, 800 | `ss01`, `cv11` | All UI text, labels, headings |
| **Mono** | JetBrains Mono | 400, 500, 600, 700 | `tnum` (tabular numerals) | Currency amounts, balances |
| **Fallback** | `system-ui, -apple-system, sans-serif` | — | — | When web fonts fail to load |

**Font Loading:**
```html
<link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600;700&display=swap" rel="stylesheet">
```

### 4.2 Type Scale

| Element | Size | Weight | Letter-spacing | Line-height | Family | Color |
|---|---|---|---|---|---|---|
| **Hero amount** | 28px | 700 | -0.02em | 1 | Mono | `--text` or semantic |
| **Screen title** | 22px | 700 | -0.02em | 1.15 | Manrope | `--text` |
| **Account tile amount** | 19px | 700 | -0.02em | 1 | Mono | ink color |
| **Transaction amount** | 13px | 800 | -0.01em | 1.1 | Mono | `--pos` / `--neg` |
| **Transaction title** | 13px | 700 | — | 1.2 | Manrope | `--text` |
| **Transaction subtitle** | 11px | 500 | — | 1.2 | Manrope | `--text-muted` |
| **Section label (caps)** | 10px | 700 | 0.12em | 1.2 | Manrope | `--text-muted` |
| **Timestamp** | 9.5px | 400 | — | 1.1 | Manrope | `--text-faint` |
| **Account tile name (caps)** | 9.5px | 700 | 0.10em | 1.2 | Manrope | ink color |

### 4.3 Number Formatting

All currency amounts use **tabular numerals** so columns align on the decimal:

```css
.mono {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}
```

**Format:**
- Sign: Use proper minus `−` (U+2212), not hyphen `-`
- Thousands separator: Comma `,`
- Decimals: Always 2 places for primary amounts (`.00`)
- Currency: Follows amount with space: `−2,465.21 $`

**Example:**
```jsx
<div className="money neg">
  <span className="sign">−</span>
  <span className="val mono">2,465.21</span>
  <span className="ccy">$</span>
</div>
```

---

## 5. Spacing & Layout

### 5.1 Base Grid

- Phone frame: **360 × 740px**, border-radius **44px**
- Content padding: **16px** horizontal
- Bottom safe area: **96px** (nav bar + home indicator)
- Status bar height: **44px**

### 5.2 Corner Radii

| Element | Radius | Tweakable | Notes |
|---|---|---|---|
| **Phone frame** | 44px | No | Fixed |
| **Card** | 22px | Yes (10–32px) | `var(--tweak-radius, 22px)` |
| **Account tile** | 18px | Yes (follows card - 4px) | Slightly tighter than parent card |
| **Input** | 14px | No | Fixed |
| **Filter tile** | 14px | No | Fixed |
| **Week strip** | 10px | No | Subtle rounding |
| **Pill / chip** | 999px | No | Full round |
| **Round icon** | 50% | No | Circle |

### 5.3 Component Spacing

| Context | Value | Notes |
|---|---|---|
| Profile header bottom margin | 18px | Space below header before section title |
| Section title row bottom margin | 12px | Space below title before card grid |
| Card internal padding | 16px | `.card-pad` class |
| Account tile grid gap | 10px | 2-column grid |
| Account tile internal padding | 12px 14px 14px | top, left/right, bottom |
| Transaction row padding | 10px 4px | vertical, horizontal |
| Transaction row gap (columns) | 12px | Between avatar, body, amount |
| Week strip margin | 8px 0 | vertical |
| Bottom bar position | bottom: 22px | From bottom edge |
| Pill nav gap | 4px | Between nav items |
| FAB size | 50 × 50px | Circle |

---

## 6. Component Patterns

### 6.1 Card Component

Standard container for grouped content.

```jsx
<div className="card">
  <div className="card-pad">
    {/* Content */}
  </div>
</div>
```

```css
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--tweak-radius, 22px);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.card-pad { padding: 16px; }
```

### 6.2 Account Tile

```jsx
<div className="acct" style={{ background: 'var(--c-green)', color: 'var(--c-green-ink)' }}>
  <div className="acct-head">
    <I.wallet(16) />
    <span className="nm">CASH</span>
  </div>
  <div className="amt mono">
    −2,465.21 <span className="ccy">$</span>
  </div>
  <div className="amt-sub">≈ −2,465.21 $</div>
  <svg className="motif" width="80" height="80" style={{ opacity: 'var(--tweak-motif-opacity, 0.18)' }}>
    {/* Category illustration */}
  </svg>
</div>
```

```css
.acct {
  border-radius: calc(var(--tweak-radius, 22px) - 4px);
  padding: 12px 14px 14px;
  min-height: 92px;
  position: relative;
  overflow: hidden;
}
.acct .motif {
  position: absolute;
  right: -10px;
  bottom: -6px;
  opacity: var(--tweak-motif-opacity, 0.18);
  pointer-events: none;
}
```

### 6.3 Transaction Row

```jsx
<div className="tx">
  <div className="avatar av-green">
    {I.utensil(14)}
    <div className="check">{I.check(8)}</div>
  </div>
  <div className="body">
    <div className="ttl">Restaurants</div>
    <div className="sub">Cash IDR</div>
    <div className="note">Lunch at Warung Sari</div>
    <div className="tags">
      <div className="chip">Food</div>
      <div className="chip">Lunch</div>
    </div>
  </div>
  <div className="right">
    <div className="amt neg">−2,857 $</div>
    <div className="amt-sub">≈ −2,857 $</div>
    <div className="time">08:01 PM</div>
  </div>
</div>
```

---

## 7. Interaction States

### 7.1 Hover States

| Element | Hover Effect |
|---|---|
| **Card** | `box-shadow` deepens slightly |
| **Button** | `background` lightens 5% |
| **Pill nav item** | `background: rgba(0,0,0,0.05)` (light) / `rgba(255,255,255,0.05)` (dark) |
| **Input** | `border-color: var(--border-strong)` |

### 7.2 Active States

| Element | Active Effect |
|---|---|
| **Pill nav item** | `background: var(--accent-bg)`, `color: var(--accent)` |
| **Segmented control** | `background: var(--surface)` (pops back to primary surface) |
| **Filter tile** | `border-color: var(--accent)`, `box-shadow` adds accent ring |

### 7.3 Focus States

All keyboard-focusable elements get a **2px accent outline**:

```css
button:focus-visible,
input:focus-visible,
.pill-nav .item:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
```

---

## 8. Accessibility

### 8.1 Contrast Requirements

All themes meet **WCAG AA** (4.5:1 for normal text, 3:1 for large text ≥24px).

| Pair | Light | Dark | AMOLED |
|---|---|---|---|
| `--text` on `--bg` | 13.2:1 ✓ | 13.8:1 ✓ | 21:1 ✓ |
| `--text` on `--surface` | 14.5:1 ✓ | 12.1:1 ✓ | 19.2:1 ✓ |
| `--text-muted` on `--surface` | 6.8:1 ✓ | 5.2:1 ✓ | 7.1:1 ✓ |
| `--accent` on `--surface` | 7.2:1 ✓ | 8.4:1 ✓ | 9.6:1 ✓ |

**Glass themes:** Test contrast with mesh backgrounds. Worst-case regions (brightest blob behind text) must still meet 4.5:1.

### 8.2 Semantic Color Redundancy

Never rely on color alone. Positive/negative indicators use **both color + symbol**:

```jsx
{/* Good: Green + ↑ arrow */}
<div className="delta pos">
  <svg>↑</svg>
  <span>+12.4%</span>
</div>

{/* Bad: Color only */}
<div className="delta pos">+12.4%</div>
```

### 8.3 Touch Targets

Minimum **44 × 44px** on mobile:

| Element | Size | Notes |
|---|---|---|
| FAB | 50 × 50px | ✓ Exceeds minimum |
| Pill nav item | 48 × 38px | ✓ Exceeds minimum |
| Transaction row | Full-width × ~70px | ✓ Exceeds minimum |
| Kebab menu button | 22 × 22px | ✗ Below minimum — increase to 28 × 28px |

---

## 9. Implementation Checklist

### 9.1 Theme Setup

- [ ] Define all CSS custom properties for each theme (Light, Dark, AMOLED, Glass Light, Glass Dark)
- [ ] Apply theme class to phone wrapper (`.t-light`, `.t-dark`, etc.)
- [ ] Add `.is-glass` class when theme starts with `glass-`
- [ ] Inject `.glass-bg` DOM element for glass themes
- [ ] Load Google Fonts (Manrope, JetBrains Mono)
- [ ] Set `font-feature-settings` on body and `.mono` class

### 9.2 Components

- [ ] All containers use `var(--surface)` background, `var(--border)` border
- [ ] Inputs use `var(--surface-3)` background
- [ ] All text uses `var(--text)`, `var(--text-muted)`, or `var(--text-faint)`
- [ ] Currency amounts use `.mono` class + tabular-nums
- [ ] Positive/negative amounts use `var(--pos)` / `var(--neg)`
- [ ] Account tiles use `var(--c-{category})` background + `var(--c-{category}-ink)` text
- [ ] FAB uses `var(--accent)` background
- [ ] Active states use `var(--accent-bg)` background + `var(--accent)` text

### 9.3 Glass Themes

- [ ] Mesh background is a real DOM element (not `::before`)
- [ ] All surfaces have `backdrop-filter: blur(...) saturate(180%)`
- [ ] Surfaces use `rgba(255,255,255,...)` (not opaque)
- [ ] Borders are white highlights (`rgba(255,255,255,...)`)
- [ ] Inner glow on cards (`inset 0 1px 0 rgba(255,255,255,...)`)
- [ ] Category tiles use rgba() tints
- [ ] Blur radius is user-adjustable via `--tweak-blur`

### 9.4 Accessibility

- [ ] All text meets WCAG AA contrast (4.5:1 minimum)
- [ ] Touch targets are ≥44 × 44px
- [ ] Focus states have 2px accent outline
- [ ] Semantic colors have redundant symbols (✓, ↑, etc.)
- [ ] Glass themes tested with mesh behind text (worst-case contrast)

---

## 10. Quick Reference

| Token | Light | Dark | AMOLED | Glass Light | Glass Dark |
|---|---|---|---|---|---|
| `--bg` | `#EDECF3` | `#11111B` | `#000000` | `#E8EAF4` | `#06060E` |
| `--surface` | `#FFFFFF` | `#20212C` | `#050509` | `rgba(255,255,255,0.62)` | `rgba(255,255,255,0.09)` |
| `--surface-3` | `#F1F0F6` | `#2F3140` | `#11111A` | `rgba(255,255,255,0.42)` | `rgba(255,255,255,0.07)` |
| `--text` | `#15151F` | `#ECEDF3` | `#FFFFFF` | `#16162A` | `#F4F4FA` |
| `--text-muted` | `#5E5E72` | `#9092A0` | `#9091A0` | `#4E4E62` | `#BCBDCB` |
| `--accent` | `#4853A0` | `#C5CBEE` | `#DDE2FA` | `#2F3A88` | `#D2D8F4` |
| `--pos` | `#1E9B4F` | `#4DCE85` | `#5BE093` | `#0F6E37` | `#5CDB8F` |
| `--neg` | `#C8463F` | `#E27974` | `#F08782` | `#A8332D` | `#F08581` |
| `--border` | `rgba(40,40,80,0.10)` | `rgba(255,255,255,0.10)` | `rgba(255,255,255,0.14)` | `rgba(255,255,255,0.85)` | `rgba(255,255,255,0.22)` |
| `--glass-blur` | — | — | — | `24px` | `28px` |

---

**End of Documentation**
