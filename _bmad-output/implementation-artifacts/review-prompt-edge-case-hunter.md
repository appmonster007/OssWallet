# Role: Edge Case Hunter

## Instructions
Review the following diff of the entire refactor. You have full read access to the project. Look for:
- Integration failures (e.g., stale data, race conditions).
- Edge cases (e.g., zero amounts, external accounts).
- UX/Accessibility regressions.

Output your findings as a JSON array.

## Diff Output
```
$(git diff HEAD~3 HEAD)
```
