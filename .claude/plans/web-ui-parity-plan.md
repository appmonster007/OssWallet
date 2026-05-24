# Plan: Web UI — Unified Computation + Full Filter Parity

## Context
The web UI dashboard fetches card data from `GET /dashboard` which uses the same `CardComputerRegistry` pipeline as the Android app. However:
1. There is **no `PUT /dashboard/cards/{cardId}`** endpoint, so filter changes from the web UI cannot be persisted to the server — overrides are session-only.
2. `CARD_FILTER_CAPS` in `index.html` is incomplete and inaccurate vs. actual `CardFilterRules` per card type.
3. Several server-returned fields are fetched but never rendered (`transactionCount`, `nature`, tag `income`/`expense` breakdowns).
4. `refreshCardContent()` only supports 3 of 14 card types for live re-fetch.

The goal is to: persist card filter changes server-side, expose correct filter dimensions per card matching Android's `CardFilterRules`, render all missing data fields, and extend live re-fetch to all cards.

---

## Files to Modify

| File | Change |
|------|--------|
| `app/src/main/java/com/osscabinet/ossWallet/data/web/WebApiRoutes.kt` | Add `PUT /dashboard/cards/{cardId}` |
| `app/src/main/java/com/osscabinet/ossWallet/data/web/dto/CardDataDto.kt` | Add `CardFilterUpdateDto` request body DTO |
| `app/src/main/assets/web/index.html` | Fix CARD_FILTER_CAPS, expand filter sheet, render missing fields, extend refreshCardContent |

---

## Step 1 — Add `PUT /dashboard/cards/{cardId}` (Kotlin)

**File**: `WebApiRoutes.kt`

Add after `DELETE /dashboard/cards/{cardId}`:
```kotlin
put("/dashboard/cards/{cardId}") {
    val cardId = call.parameters["cardId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
    val update = call.receive<CardFilterUpdateDto>()
    // Build TransactionFilter from DTO and call cardRepository.updateCardFilter(profileId, cardId, filter)
    call.respond(HttpStatusCode.OK)
}
```

**File**: `CardDataDto.kt` — add:
```kotlin
@Serializable
data class CardFilterUpdateDto(
    val timelineType: String? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val accountIds: List<String> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val transactionTypes: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val currencies: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val queryNote: String? = null,
    val queryPayee: String? = null,
    val excludeFuture: Boolean? = null
)
```

The route handler maps this DTO to a `TransactionFilter` update via the existing `cardRepository.updateCardFilter()` (or equivalent method — inspect `CardRepository` interface; add if missing).

---

## Step 2 — Fix `CARD_FILTER_CAPS` in `index.html`

Current caps are too restrictive. Replace with values derived from actual `CardFilterRules` per card type:

| payloadType | timeline | txType | accounts | categories | currencies | tags | statuses | methods | amount | search | future |
|---|---|---|---|---|---|---|---|---|---|---|---|
| NetWorth | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| CashFlow | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| CurrencyBalances | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Accounts | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Budgets | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| PlannedPayments | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Loans | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| RecentActivities | ✓ | — | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| CategoryBreakdown | ✓ | income\|expense (forced one) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| CategoryTreemap | ✓ | income\|expense (forced one) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| CashFlowComparison | ✓ | income+expense (forced both) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| IncomeExpenseBar | ✓ | income+expense (forced both) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |
| CategorySpendingTrend | ✓ | income\|expense (forced one) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| TagUpSetPlot | ✓ | income\|expense (forced one) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

Add `refetch: true` for all types (now that PUT endpoint exists).

---

## Step 3 — Expand `openCardFilterSheet()` in `index.html`

Replace the simplified filter controls with 6 sections matching Android's `FilterSheetContent.kt`, gated by caps:

1. **Timeline section** (if `caps.timeline`): dropdown with all `TimelineType` values + date pickers for custom range
2. **Transaction type** (if `caps.txType`): radio (income/expense) for forced-one types; hidden for forced-both
3. **Accounts** (if `caps.accounts`): fetch `/api/accounts`, render checkbox list
4. **Categories** (if `caps.categories`): fetch `/api/categories`, render nested checkbox list
5. **Currencies / Status / Payment Methods** (if respective cap): multi-select chips
6. **Amount range** (if `caps.amount`): min/max number inputs
7. **Search** (if `caps.search`): note and payee text fields
8. **Future transactions toggle** (if `caps.future`): checkbox

On **Apply**: call `PUT /dashboard/cards/{cardId}` with filter payload, then call `refreshCardContent()`.
On **Reset**: call `PUT /dashboard/cards/{cardId}` with empty filter (defaults), then reload.

---

## Step 4 — Render Missing Server Data Fields

| Card | Missing field | Fix |
|------|--------------|-----|
| CategoryBreakdown | `transactionCount` per item | Show "(N txns)" after percentage |
| CategorySpendingTrend | `nature` per category amount | Color-code by nature (MUST/NEED/WANT) |
| TagUpSetPlot | `income`, `expense` in `TagSetDto` | Show income/expense breakdown below total |
| TagUpSetPlot | `income`, `expense` in `TagIntersectionDto` | Show in intersection tooltip/detail |

---

## Step 5 — Extend `refreshCardContent()` for All Card Types

Once PUT endpoint persists filters server-side, `GET /dashboard` will return updated data for all cards. Change `refreshCardContent()` to always call `GET /dashboard` and re-render only the target card's body — removing the need for card-specific analytics endpoint logic.

Simplified approach:
```javascript
async function refreshCardContent(cardId, payloadType, cardType) {
  const resp = await apiFetch('/api/dashboard');
  const cards = await resp.json();
  const card = cards.find(c => c.cardId === cardId);
  if (card) {
    const d = card.data;
    const pt = d.type ? d.type.split('.').pop() : payloadType;
    document.getElementById(`card-body-${cardId}`).innerHTML = dispatchCardBody(card, d, pt);
  }
}
```

This eliminates duplicated analytics endpoint logic and ensures all 14 card types can re-fetch.

---

## Verification

1. Build debug APK: `./gradlew assembleDebug`
2. Install on device/emulator: `./gradlew installDebug`
3. Open web UI in browser at `http://<device-ip>:8080`
4. For each of the 14 cards:
   - Verify card renders with all data fields
   - Open filter sheet → confirm all sections appear per caps table
   - Apply a filter → confirm PUT call succeeds, card re-renders with filtered data
   - Reload page → confirm filter persists (server-side)
5. Test card add/remove/reorder still works
