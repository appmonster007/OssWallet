# Floating "Pill-Only" Glass Header Architecture

## Overview
This architecture replaces the traditional solid/full-width blurred Top Bar with a floating system of independent "Glass Pills". It creates a modern, spacious aesthetic where content either flows seamlessly under the header (standard mode) or respects the header boundary (sticky mode).

## ASCII Architecture Diagram

### Front View (Layered)
```text
+-----------------------------------------------------------+ <--- Status Bar (100% Transparent)
                                                           |
    /------------------\            /------------------\   |
    | [=] Dashboard    |            |       [Search]   |   | <--- Layer 3: Floating Glass Pills
    \------------------/            \------------------/   |      (HazeLayer.Overlay)
                                                           |
+~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~+ <--- Layer 2 boundary (Header Offset)
 [ STICKY HEADER: TODAY ]                                  | <--- Layer 2: Sticks at offset
+-----------------------------------------------------------+      (In Sticky Mode: List container padded)
 [ Transaction Item 1 ]                                    | 
 [ Transaction Item 2 ]                                    | <--- Layer 1: Content Layer
 [ Transaction Item 3 ]                                    |      (Flow-Under Mode: Flows up to Y=0)
+-----------------------------------------------------------+
```

### Side View (Z-Stack)
```text
(Top) Layer 3: AeonTopAppBar (Floating Pills only, no background strip)

(Mid) Layer 2: Sticky Headers (only in sticky mode, where the transaction list content starts below/after the AeonTopAppBar, such that the transaction header stick correctly to the top of the list which is always below the AeonTopAppBar)

(Bot) Layer 1: Transaction List Items (Full screen height in flow-under; Padded in sticky)

(Base) Layer 0: Background Layer (Solid color + Topographic Contours)
```

## Key Components & Behaviors

### 1. AeonTopAppBar (Floating Pills)
*   **Decoupled from Scaffold**: DO NOT use `Scaffold(topBar = { AeonTopAppBar(...) })`. This pattern reserves vertical space and prevents the "Flow-Under" aesthetic.
*   **Floating Overlay**: Treat `AeonTopAppBar` as a floating overlay within a root `Box`. It sits on the highest Z-index (Layer 3).
*   **Background**: 100% Transparent.
*   **Pills**: Navigation/Title and Actions are contained in separate `GlassSurface` components with `CircleShape`.
*   **Depth**: Uses `HazeLayer.Overlay` (Depth 1) to blur content.

### 2. Dual-Mode Layout Logic
The application supports two layout behaviors based on the `stickyHeaders` setting:

#### A. Sticky Mode (OFF) - "Flow-Under" Aesthetic
*   **Container**: The `LazyColumn` container is **not** padded at the top (`Modifier.fillMaxSize()`).
*   **Padding**: `contentPadding` is used to apply a top offset (`Status Bar + 64dp`) so the first items are not obscured when the list is at rest.
*   **Scrolling**: As the user scrolls, content flows **under** the status bar and floating pills.
*   **Haze**: The content layer registers itself as a source for `HazeLayer.Overlay` (Depth 1), allowing the glass pills to blur the scrolling content.

#### B. Sticky Mode (ON) - "Boundary-Respected" Aesthetic
*   **Container**: The `LazyColumn` container is **padded** from the top using `Modifier.padding(top = totalHeaderHeight)`.
*   **Padding**: `contentPadding.top` is set to `0.dp` because the container itself provides the necessary clearance.
*   **Scrolling**: Content is strictly confined to the area below the floating pills. Sticky headers "stick" at the very top of the list container, which aligns perfectly with the bottom of the pill zone.
*   **Haze**: The content layer **must NOT** register itself as a source for `HazeLayer.Overlay` (Depth 1). Since content never flows under the pills, registering it would cause incorrect blur artifacts or unnecessary computation.

### 3. Header Height Constants
*   **Standard Top Bar**: 64dp (`AeonSize.D64`).
*   **Total Offset**: `WindowInsets.statusBars + 64dp`. This value is used for either `Modifier.padding` or `contentPadding` depending on the mode.

## Acceptance Criteria
1.  **Non-Scaffold implementation**: The header is an overlay in a `Box`, not a `Scaffold` slot.
2.  **Mode Awareness**: The layout correctly toggles between container-padding (Sticky ON) and content-padding (Sticky OFF).
3.  **Haze Logic**: Content registers for Depth 1 blur **only** when `stickyHeaders` is OFF.
4.  **Floating Pills**: Separate glass pills for navigation/title and actions.
5.  **Sticky Alignment**: Headers stick exactly at the bottom boundary of the 64dp + status bar zone.
6.  **Visual Continuity**: In Flow-Under mode, content is visible through the gaps between pills and the status bar area.
