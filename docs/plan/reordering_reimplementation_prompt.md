# Task: Re-implement the Dashboard Card Reordering System

**Objective**: Replace the current internal reordering logic with a high-performance system inspired by `sh.calvin.reorderable` (for animations) and the **KanOne** reference (for coordinate mapping).

## Core Requirements

### 1. State Management (Buffered Writes)
- Refactor `DashboardViewModel` to use a `MutableStateFlow<List<CardConfig>>` for "ephemeral" reordering.
- Update the local list immediately during `onMove`.
- **CRITICAL**: Only trigger the `cardRepository.reorderCards` (disk write) when the drag gesture **ends** (`onDragStop`).

### 2. Animation & Interaction (Calvin-Style)
- Utilize `Modifier.animateItem()` for smooth sliding of non-dragged cards.
- Implement a "Lift & Scale" effect: When a card is grabbed, it should scale to `1.05f` and increase `shadowElevation` using `animateFloatAsState`.
- Add haptic feedback: Use `HapticFeedbackType.LongPress` at the start and `TextHandleMove` on every successful index swap.

### 3. Coordinate Mapping (KanOne "Radar")
- Ensure the `ReorderableLazyListState` uses `onGloballyPositioned` and `positionInRoot()` to track card bounds. This is essential for the future transition to a multi-column Kanban layout where cards must "jump" between different `LazyColumn` containers.

### 4. Visual Polish
- **Floating Card**: High `zIndex`, full opacity, slight scale up.
- **Ghost Placeholder**: A dimmed (`alpha = 0.2f`) version of the card should remain in the list at the current target index to show the user exactly where the card will land.

## Refactor Target Files
- `ui/reorderable/ReorderableLazyListState.kt`: Refactor the swap engine for more "forgiving" collision detection (use a `swapTravelRatio` of ~0.4f).
- `ui/components/cards/AeonCardList.kt`: Integrate the new buffered persistence logic.
- `ui/reorderable/ReorderableItem.kt`: Enhance the `graphicsLayer` transitions for the lift effect.

## Verification
- Ensure the Dashboard remains responsive even during rapid, long-distance drags.
- Verify that TalkBack accessibility actions ("Move Up", "Move Down") are preserved and function correctly.
