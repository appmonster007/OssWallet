# Records search

Search lives on the Records screen (search icon in the top bar). The old drawer "Search" screen, which also
looked up accounts, loans, goals, templates and planned payments, was removed; those have no search of their own.

## Behaviour

- Opening search swaps the top bar for a back button and a search field, hides the filter pill, and stops
  showing planned-payment rows. Back (or the arrow) closes search and restores the previous list untouched.
- **Search ignores the filter pill.** On the main Records view it looks at every transaction: all time,
  future-dated entries and archived accounts included.
- **Exception:** a Records view opened with its own filter (a category or tag from elsewhere, `isTopLevel = false`)
  searches inside that filter only.
- Syntax (`SearchQueryParser`): words, `#tag` (prefix match), amounts (`49.99`, `>100`, `<50`, `20-50`).
  Every word must appear in the name, payee, note or category name; a lone number matches as an amount or as text.
- Group opening/closing balances are hidden while searching, since matches span months and accounts.
  Per-item balances stay correct (history is not cut at the pill's end date).

## Where things are

| Piece | File |
|-------|------|
| Matching rules | `core/domain/.../service/search/TransactionSearchMatcher.kt` |
| Query parsing | `core/domain/.../service/search/SearchQuery.kt` |
| State and list wiring | `TransactionViewModel` (`isSearching`, `searchQuery`, `searchScope`) |
| Top bar | `TransactionListTopAppBar` (reuses `AeonTopBarSearchPill`) |

The search text is deliberately **not** part of `TransactionFilter`: it must not be saved, merged by
`FilterManager`, or counted as an active filter in the pill.

`TransactionSearcher` / `searchForGlobal` (database full-text search) is still used by the assistant functions;
Records does not use it because it already holds every transaction in memory.

## Not done

- Saving a search as a filter (needs an "any text" field on `TransactionFilter`, plus entity, mapper, backup and
  SQL changes).
- Match highlighting in rows.
