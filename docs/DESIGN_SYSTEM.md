# Design System & Visual Identity — OssWallet

OssWallet utilizes a custom design language called **Aeon**, characterized by the "H — Editorial Instrument" aesthetic. This document outlines the typography, color layering, and glassmorphism implementation.

## 1. Typography (Editorial Serif)
We use variable fonts (SDK 26+) for a premium editorial feel:
- **Serif**: **Fraunces** (headings, amounts, and hero text).
- **Sans**: **Inter** (body text, labels, and UI controls).
- **Mono**: **JetBrains Mono** (tabular numbers, micro-readouts, and metadata).

**Rule**: Use `AppTextStyles` for all Compose text; avoid direct `MaterialTheme.typography` access to preserve the custom "H" identity.

---

## 2. Color Architecture (Material 3 Expressive)
OssWallet follows the **Tonal Alternation Loop** to achieve depth without "elevation exhaustion."

### 2.1 Layering Strategy
| Level | Component | Token Mapping |
| :--- | :--- | :--- |
| **0** | Root Canvas | `Surface` |
| **1** | Parent Card | `Surface Container Low` |
| **2** | Nested Grid | `Surface Container High` |
| **3** | Recessed Item | `Surface Variant` (+ 1dp `Outline` border) |
| **4** | Action Node | `Primary` / `Secondary` |

### 2.2 UI Identity Palettes
- **Dark Theme**: Warm-black, ivory, and champagne neutrals.
- **Light Theme**: Ivory, ink, and bronze neutrals.

---

## 3. Glassmorphism (Selective Policy)

The effect is powered by **Haze**. We follow a strict "selective glass" policy to maintain readability and performance.

### 3.1 Three-Tier Architecture
1. **Tier 1 (`AeonHazeProvider`)**: Wraps screens to capture the background (contours/colors).
2. **Tier 2 (`AeonGlassHost`)**: Wraps glass containers (like cards) so children can blur the container surface instead of the screen background.
3. **Tier 3 (`GlassSurface`)**: The core consumer composable that resolves which layer to blur via a priority chain.

### 3.2 Glass Rules
- **Opaque Cards**: Dashboard cards (`AeonExpressiveCard`) have opaque hairline panels. Glass is reserved for **floating chrome**:
    - `AeonTopAppBar` (pills)
    - `TransactionFilterPill`
    - Navigation sheets and drawers.
- **Degradation**: If `LocalGlassmorphismEnabled` is `false`, `GlassSurface` automatically falls back to a solid `fallbackColor`.

---

## 4. Reusable Components (Aeon System)
All generic components live in `ui/components/` with the `Aeon` prefix:
- `AeonButtons.kt`, `AeonCards.kt`, `AeonInputs.kt`, `AeonProgress.kt`.
- **Card Rendering**: Use `UnifiedCardRenderer` for all cards to ensure consistency.
