# Material 3 Expressive Color Architecture & Layering Specification
**File System Identity:** `design-colors.md`  
**Target Engine:** Material You / Monet Dynamic Color (Android 14+, API 34+)  
**Compliance Standard:** WCAG 2.1 AA (Minimum 4.5:1 text contrast / 3.0:1 structural contrast)

---

## 1. Architectural System Overview

In the Material 3 Expressive paradigm, color is semantic, functional, and strictly paired. Color tokens must follow a predictable architectural hierarchy based on two immutable principles:

1. **The Pairing Rule:** Any token prefixing with `On` (e.g., `OnSurface`) is engineered to sit *on top of* its corresponding container or core color block.
2. **The Alternation Principle:** To avoid elevation exhaustion (running out of increasingly lighter/darker tokens), deep nesting is achieved by alternating between solid structural containers, container variants, and outlined boundaries.

---

## 2. Efficient Component Nesting & Alternation Blueprint

Instead of infinitely deepening layouts using linear elevation (`Low → Medium → High → Highest`), modern best practices implement a **Tonal Alternation Loop**. This reuses a tight set of tokens efficiently while maintaining clean visual depth.

### 2.1 The Rules of Alternation
1. **The Parent Anchorage:** The primary structural card or wrapper defaults to a mid-tier surface container (`Surface Container` or `Surface Container Low`).
2. **The Inverted Child Nest:** Inside that parent, nested sub-components utilize a contrasting variant baseline (`Surface Variant`) or a step up (`Surface Container High`).
3. **The Recessed Structural Loop (High $\rightarrow$ Variant/Low):** To nest a 3rd or 4th level item efficiently without losing contrast, you do not use a brighter fill. Instead, you drop the background color to `Surface Variant` or `Surface` and bind the element with an explicit `Outline` stroke. This turns the deeper component into a crisp, recessed structural unit rather than a floating layer.

### 2.2 Alternation Layering ASCII Architecture


+--------------------------------------------------------------------------+
| LEVEL 0: Foundation Canvas                                               |
| Token: [Surface]                                                         |
| Copy/Glyphs: [OnSurface]                                                 |
|                                                                          |
|  +--------------------------------------------------------------------+  |
|  | LEVEL 1: Parent Grouping Card (Elevated Baseline)                   |  |
|  | Token: [Surface Container Low]                                     |  |
|  | Copy/Glyphs: [OnSurface]                                           |  |
|  |                                                                    |  |
|  |  +--------------------------------------------------------------+  |  |
|  |  | LEVEL 2: Nested Container / Active Input Rows (Step Up)      |  |  |
|  |  | Token: [Surface Container High]                              |  |  |
|  |  | Copy/Glyphs: [OnSurface]                                     |  |  |
|  |  |                                                              |  |  |
|  |  |  +--------------------------------------------------------+  |  |  |
|  |  |  | LEVEL 3: Deepest Unit / Recessed Information Module     |  |  |  |
|  |  |  | Token: [Surface Variant]                               |  |  |  |
|  |  |  | Border/Stroke Requirement: [Outline]                      |  |  |  |
|  |  |  | Copy/Glyphs: [OnSurface Variant]                       |  |  |  |
|  |  |  |                                                        |  |  |  |
|  |  |  |  +--------------------------------------------------+  |  |  |  |
|  |  |  |  | LEVEL 4: Core Interactive Action Nodes           |  |  |  |  |
|  |  |  |  | Token: [Primary] or [Secondary]                  |  |  |  |  |
|  |  |  |  | Content/Label: [OnPrimary] or [OnSecondary]      |  |  |  |  |
|  |  |  |  +--------------------------------------------------+  |  |  |  |
|  |  |  +--------------------------------------------------------+  |  |  |
|  |  +--------------------------------------------------------------+  |  |
|  +--------------------------------------------------------------------+  |
+--------------------------------------------------------------------------+

### 2.3 Layering Strategy Matrix

| Layout Depth Level | Architectural Target | Token Mapping Strategy |
| :--- | :--- | :--- |
| **Level 0** (Root Canvas) | System Activity Window Viewport | `SurfaceLight` / `SurfaceDark` |
| **Level 1** (Parent Group) | Sectional layout wrapper / Dashboard group | `SurfaceContainerLowLight` / `SurfaceContainerLowDark` |
| **Level 2** (Sub-Section Grid) | Content feed units / Unselected list modules | `SurfaceContainerHighLight` / `SurfaceContainerHighDark` |
| **Level 3** (Recessed Inner Item) | Embedded micro-metrics / Token feeds / Input chips | `SurfaceVariantLight` (with `Outline`) / `SurfaceVariantDark` (with `Outline`) |
| **Level 4** (Action Target) | Interactive conversion keys / Validation elements | `PrimaryLight` / `PrimaryDark` |

---

## 3. Comprehensive Color Slot Mapping

### 3.1 The Brand Accents (Primary, Secondary, Tertiary)
Use these slots to voice intent, brand elements, and interactive prioritization across your nested layers.

| Light Theme Token | Dark Theme Token | Design Role & Usage Guidelines |
| :--- | :--- | :--- |
| `PrimaryLight` | `PrimaryDark` | High-prominence interaction targets (Filled Buttons, FABs, active state switches). |
| `OnPrimaryLight` | `OnPrimaryDark` | Clearly legible text/glyphs sitting directly over `Primary` fills. |
| `PrimaryContainerLight` | `PrimaryContainerDark` | Soft accent padding backdrops. Used for extended structural headers or highlighted app states. |
| `OnPrimaryContainerLight` | `OnPrimaryContainerDark` | Informational typography and microcopy nested inside a `Primary Container`. |
| `SecondaryLight` | `SecondaryDark` | Secondary actions, selection chips, toggle bars, and filters. Less visually loud. |
| `OnSecondaryLight` | `OnSecondaryDark` | Labels/Glyphs applied over `Secondary` interactive blocks. |
| `SecondaryContainerLight` | `SecondaryContainerDark` | Structural clustering backgrounds (e.g., active navigation rail selectors). |
| `OnSecondaryContainerLight` | `OnSecondaryContainerDark` | Content embedded within standard secondary container modules. |
| `TertiaryLight` | `TertiaryDark` | Expressive accent structures. Ideal for notifications, tags, bookmarks, or balancing branding. |
| `OnTertiaryLight` | `OnTertiaryDark` | Layout labels embedded into a core `Tertiary` highlight element. |
| `TertiaryContainerLight` | `TertiaryContainerDark` | Background wrappers for contextual callouts, tips, or warning alerts. |
| `OnTertiaryContainerLight` | `OnTertiaryContainerDark` | Functional copy inside a tertiary warning alert banner. |

### 3.2 System Utilities & Safeguards (Error, Background, Outline)
These tokens govern structural spacing, input fields, boundaries, and validation feedback.

| Light Theme Token | Dark Theme Token | Design Role & Usage Guidelines |
| :--- | :--- | :--- |
| `ErrorLight` | `ErrorDark` | Destructive actions (Delete, Wipe) or hard application crash/failure alerts. |
| `OnErrorLight` | `OnErrorDark` | Clear glyph markings placed on a filled Error block. |
| `ErrorContainerLight` | `ErrorContainerDark` | Backdrop canvas layers for invalid text fields, error summaries, or system warnings. |
| `OnErrorContainerLight` | `OnErrorContainerDark` | Text messages detailing validation errors inside an error container. |
| `BackgroundLight` | `BackgroundDark` | Full bleed window foundation canvas layer sitting behind scrolling content blocks. |
| `OnBackgroundLight` | `OnBackgroundDark` | Global base typographical treatments requiring a direct draw on the window background. |
| `OutlineLight` | `OutlineDark` | High-visibility separation lines. Applied to borders of Outlined Buttons and active field inputs. |
| `OutlineVariantLight` | `OutlineVariantDark` | Lower contrast decorative bounds (e.g., subtle separators between items in a nested list view). |

### 3.3 The Core Canvas & Surface Container Matrix
Instead of absolute elevations driven by heavy gradients or drop shadows, Material 3 Expressive scales backgrounds continuously using 5 container depth categories.

| Light Theme Token | Dark Theme Token | Relative Luminance Position | Practical Mapping Rule |
| :--- | :--- | :--- | :--- |
| `SurfaceLight` | `SurfaceDark` | Baseline Zero Layer | Base viewport window color for views, settings canvases, and lists. |
| `OnSurfaceLight` | `OnSurfaceDark` | Primary Glyphs | Global body copy, dominant titles, and core iconography on a standard surface. |
| `SurfaceVariantLight` | `SurfaceVariantDark` | Secondary Variant | Structural headers, top app bars, unselected text-field fills. |
| `OnSurfaceVariantLight` | `OnSurfaceVariantDark` | Soft Copy | Secondary tags, timestamps, placeholder hints, and disabled labels. |
| `InverseSurfaceLight` | `InverseSurfaceDark` | Inverted Base | Used for short-lived overlays like Snackbars where high visual friction is needed. |
| `InverseOnSurfaceLight` | `InverseOnSurfaceDark` | Inverted Font | Interactive labels/action options within Snackbars or temporary banner elements. |
| `SurfaceContainerLowestLight`| `SurfaceContainerLowestDark`| Absolute Minimum Tonal State | *Light Theme:* Purest White. *Dark Theme:* Darkest black layer. Use for deep structural splits. |
| `SurfaceContainerLowLight` | `SurfaceContainerLowDark` | Minimal Elevation | Standard underlying card modules, settings layout rows. |
| `SurfaceContainerLight` | `SurfaceContainerDark` | Intermediate Baseline | Bottom navigation rails, elevated layout cards, sheet panels. |
| `SurfaceContainerHighLight` | `SurfaceContainerHighDark` | Highlighted Segment | Content dialog panels, overflow spinner modules, context dropdowns. |
| `SurfaceContainerHighestLight`| `SurfaceContainerHighestDark`| Maximum Contrast Layer | Switches, toggle track layers, active context-pinned fields. |

---

## 4. UI Contrast & Accessibility Rules

To maintain high visual quality across user-generated palettes, the color definitions must obey the standard W3C luminance contrast calculation:

$$\text{Contrast Ratio} = \frac{L_1 + 0.05}{L_2 + 0.05}$$

### 4.1 Contrast Mapping Checklist
* **4.5:1 Minimum Contrast Ratio:** Mandatory for all functional body layout text, secondary instructions, captions, and glyph forms using `On-` token pairs (`OnSurface`, `OnPrimaryContainer`, etc.) across **any** nesting depth level.
* **3.0:1 Minimum Contrast Ratio:** Permitted only for large typography displays ($\ge 18\text{pt}$ bold text or $\ge 24\text{pt}$ regular weight) and functional component bounding boxes (`Outline` borders against its underlying container token).
* **The Layer 3 Boundary Absolute Constraint:** When looping structural tracking back into lower luminance tokens (like `Surface Variant`), you **must** apply a 1dp stroke via the `Outline` token. The color difference alone is not high enough to indicate a physical boundary without an explicit border.

### 4.2 Handling Interactive State Overlays
When creating interactive UI feedback layers (Pressed, Focused, Hovered states), programmatically compound your base container token color with an explicit Alpha opacity overlay of the corresponding `On-` token.

* **Hover State:** Base Component Container Token + `8%` opacity of the designated `On-` counterpart.
* **Focus State:** Base Component Container Token + `10%` opacity of the matching `On-` counterpart, wrapped with an outer `2dp` stroke line drawing from `Outline`.
* **Pressed State:** Base Component Container Token + `12%` opacity of the designated `On-` counterpart.
* **Disabled States:** Flatten the base component block color down to `12%` opacity of the token, and lock all nested typography or structural icons to exactly `38%` absolute opacity over the top of the inactive block.