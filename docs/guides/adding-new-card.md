# Developer Guide: Adding a New Card

This guide explains how to add a new card to the redesigned card system. The system follows the **Open-Closed Principle (OCP)**, meaning you can add new card types by extending existing registries without modifying core orchestration logic.

---

## Step 1: Define the Card Type
Add a new entry to the `CardType` enum. This acts as the unique identifier for the card system.

**File:** `app/src/main/java/com/osscabinet/ossWallet/domain/card/CardType.kt`

```kotlin
enum class CardType(val serialName: String, val displayName: String, val description: String) {
    // ... existing types
    DUMMY("dummy", "Dummy Card", "A demonstration card for documentation.")
}
```

## Step 2: Create the Configuration Model
Define the settings your card requires. This model must extend `CardConfig` and be `@Serializable`.

**File:** `app/src/main/java/com/osscabinet/ossWallet/domain/model/settings/CardConfig.kt`

```kotlin
@Serializable
@SerialName("dummy")
data class DummyCardConfig(
    override val cardId: String,
    override val title: String = "DUMMY",
    override val isEnabled: Boolean = true,
    override val order: Int = 0,
    override val screenContext: ScreenContext = ScreenContext.DASHBOARD,
    val customMessage: String = "Hello World" // Card-specific setting
) : CardConfig()
```

## Step 3: Define the Render Data
Define the output of the card's computation. This is the UI-ready data that the renderer will consume.

**File:** `app/src/main/java/com/osscabinet/ossWallet/domain/card/CardRenderData.kt`

```kotlin
sealed class CardRenderData {
    // ... existing data classes
    data class Dummy(
        val config: DummyCardConfig,
        val processedMessage: String
    ) : CardRenderData()
}
```

## Step 4: Implement the Computer Strategy
Create a pure function that transforms `CardInput` (raw financial data) into your `CardRenderData`.

**File:** `app/src/main/java/com/osscabinet/ossWallet/domain/card/computers/DummyCardComputer.kt`

```kotlin
class DummyCardComputer : CardDataComputer<DummyCardConfig, CardRenderData.Dummy> {
    override val supportedType = CardType.DUMMY

    override fun compute(input: CardInput, config: DummyCardConfig): CardRenderData.Dummy {
        // Perform heavy calculations here using input.transactions, input.accounts, etc.
        val message = "${config.customMessage} - Precision is ${input.precision}"
        return CardRenderData.Dummy(config, message)
    }
}
```

## Step 5: Register the Computer
Add your new computer implementation to the `CardComputerRegistry`.

**File:** `app/src/main/java/com/osscabinet/ossWallet/domain/card/CardComputerRegistry.kt`

```kotlin
val DEFAULT = CardComputerRegistry(
    listOf(
        // ... existing computers
        DummyCardComputer()
    )
)
```

## Step 6: Update the UI Bridge
Map your new configuration to the registry computation and define the callbacks.

**File:** `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/CardUiBridge.kt`

```kotlin
// Inside rememberCardWithCallbacks
val renderData = when (config) {
    // ...
    is DummyCardConfig -> registry.compute(CardType.DUMMY, input, config)
}

val callbacks = when (config) {
    // ...
    is DummyCardConfig -> CardCallbacks.Analytics( // Reuse or create a typed callback
        onUpdate = { /* update logic */ },
        onDelete = { onDelete(config.cardId) },
        onRecordsClick = onRecordsClick
    )
}
```

## Step 7: Define the Rendering Logic
Add the Composable representation of your card to the `UnifiedCardRenderer`.

**File:** `app/src/main/java/com/osscabinet/ossWallet/ui/components/cards/UnifiedCardRenderer.kt`

```kotlin
@Composable
fun UnifiedCardRenderer(...) {
    when (renderData) {
        // ...
        is CardRenderData.Dummy -> {
             Card(modifier = modifier) {
                 Text(
                     text = renderData.processedMessage, 
                     modifier = Modifier.padding(16.dp),
                     style = MaterialTheme.typography.bodyLarge
                 )
             }
        }
    }
}

```

---

## Benefits of this Architecture

1.  **Type Safety**: The `CardDataComputer` interface ensures that a specific configuration type always produces the expected render data type.
2.  **Automatic Caching**: New cards automatically benefit from the caching layer in `CardComputerRegistry` without any additional code.
3.  **ViewModel Decoupling**: Screens and ViewModels do not need to know about the internal logic of your card. Adding a card requires zero changes to `DashboardViewModel` or `AnalyticsViewModel`.
4.  **Testability**: Each `CardDataComputer` is a pure function, making it trivial to unit test with various `CardInput` scenarios.
