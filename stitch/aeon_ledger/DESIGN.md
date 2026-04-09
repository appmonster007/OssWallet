# Design System Specification: Editorial Financial Intelligence

## 1. Overview & Creative North Star
**Creative North Star: "The Architectural Ledger"**

This design system moves away from the "utility-first" clutter of traditional banking apps toward a "High-End Editorial" experience. The goal is to treat financial data with the same reverence as a premium fashion magazine or an architectural portfolio. 

We achieve this through **Organic Asymmetry** and **Atmospheric Depth**. By leveraging Material 3’s dynamic logic but applying a strict "No-Line" rule, we create an interface that feels carved rather than constructed. The layout prioritizes breathing room (whitespace) as a functional tool to reduce cognitive load, making complex financial data feel serene and manageable.

---

## 2. Color Philosophy: Tonal Depth & The "No-Line" Rule
The palette is rooted in the "Deep Sea & Slate" spectrum, using `primary (#004d5f)` and `tertiary (#005143)` to establish authority and growth.

### The "No-Line" Rule
**Explicit Instruction:** 1px solid borders are prohibited for sectioning. 
Structure must be defined through **Background Color Shifts** or **Tonal Transitions**. To separate a transaction list from a header, transition from `surface` to `surface-container-low`.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers—stacked sheets of frosted glass.
*   **Base Layer:** `surface` (#f7fafa)
*   **Sectional Shifts:** `surface-container-low` (#f1f4f4) for secondary content areas.
*   **Interactive Cards:** `surface-container-lowest` (#ffffff) to provide a "pop" of clarity.
*   **High-Emphasis Containers:** `surface-container-highest` (#e0e3e3) for inactive or "sunken" states.

### The "Glass & Gradient" Rule
For floating action buttons or hero balance cards, utilize **Glassmorphism**:
*   **Style:** Background `surface_variant` at 60% opacity + `backdrop-filter: blur(20px)`.
*   **Signature Textures:** Use a linear gradient from `primary` (#004d5f) to `primary_container` (#00677d) at a 135° angle for high-value CTA backgrounds to add "soul" and dimension.

---

## 3. Typography: The Editorial Scale
We utilize a pairing of **Manrope** (Display/Headline) for a modern, geometric character and **Inter** (Body/Labels) for technical legibility.

| Role | Token | Font | Size | Weight/Note |
| :--- | :--- | :--- | :--- | :--- |
| **Hero Balance** | `display-lg` | Manrope | 3.5rem | Tight tracking (-0.02em) |
| **Section Header** | `headline-sm` | Manrope | 1.5rem | Medium; sentence case only |
| **Transaction Title**| `title-md` | Inter | 1.125rem | Semi-bold |
| **Financial Data** | `body-lg` | Inter | 1rem | Tabular Numeral alignment |
| **Micro-Data** | `label-sm` | Inter | 0.6875rem | Uppercase with +0.05em tracking |

**Hierarchy Note:** Use `on-surface-variant` (#3f484c) for secondary labels to create a sophisticated grey-scale contrast against the high-contrast `on-surface` (#181c1d) headings.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are largely replaced by **Tonal Layering**. Depth is achieved by "stacking" container tiers.

*   **The Layering Principle:** Place a `surface-container-lowest` card on a `surface-container-low` background. This creates a soft, natural lift.
*   **Ambient Shadows:** For elevated modals, use a "Tinted Ambient Shadow": 
    *   `box-shadow: 0px 24px 48px rgba(0, 77, 95, 0.08);` (Shadow color derived from `primary`).
*   **The Ghost Border:** If containment is required for accessibility, use a `outline-variant` (#bec8cc) at **15% opacity**. Never 100%.

---

## 5. Components & Intentional Patterns

### Transactions & Status
Instead of heavy icons, use color-coded typography and "soft-glow" indicators.
*   **Income:** `tertiary` (#005143) text with a `tertiary-fixed` (#7cf8da) subtle background glow.
*   **Expense:** `on-surface` (#181c1d) with a simple minus prefix.
*   **Transfer:** `secondary` (#516164) with `surface-variant` containers.

### Cards & Lists
*   **Radius:** Cards must use `lg` (2rem) or `xl` (3rem) corner radius to feel organic.
*   **No Dividers:** Forbid the use of divider lines between transactions. Use `0.5rem` of vertical white space and subtle alternating background tints (`surface` vs `surface-container-low`).

### Input Fields
*   **Style:** "Borderless" approach. Use a `surface-container-high` background with a `md` (1.5rem) radius.
*   **Focus State:** Transition background to `primary-fixed` (#b3ebff) with a 2px "Ghost Border" of `primary`.

### Navigation (The Floating Dock)
Instead of a standard bottom nav, use a floating "Dock" with a `surface-container-lowest` background, 80% opacity, and a heavy `backdrop-filter: blur(12px)`.

---

## 6. Do’s and Don’ts

### Do
*   **Do** use asymmetrical margins (e.g., 24pt left, 16pt right) for a custom editorial feel.
*   **Do** use "Tabular Numbers" for financial figures to ensure decimals align vertically.
*   **Do** use `primary-fixed-dim` (#86d1ea) for icons to give them a soft, modern glow.

### Don’t
*   **Don’t** use pure black (#000000) for text; always use `on-surface` (#181c1d).
*   **Don’t** use "Alert Red" for standard expenses; reserve `error` (#ba1a1a) strictly for critical failures or overdrafts.
*   **Don’t** use 1px dividers to separate content. Use whitespace or `surface` shifts.