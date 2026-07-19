---
name: Staged-but-deleted files on feature/tagv22
description: Four files are staged as new additions but subsequently deleted from disk — they appear in git status as unresolved and must be git rm'd before merge
type: project
---

On `feature/tagv22`, the following files were staged (`git add`) as new files but then physically deleted from the working tree, leaving them in an inconsistent state:

- `app/src/main/java/com/osscabinet/osswallet/ui/navigation/DashboardNavEvent.kt`
- `app/src/main/java/com/osscabinet/osswallet/ui/navigation/AnalyticsNavEvent.kt`
- `app/src/main/java/com/osscabinet/osswallet/ui/navigation/SettingsNavEvent.kt`
- `app/src/main/java/com/osscabinet/osswallet/ui/components/transactions/TransactionListDisplayConfig.kt`

None of these files are referenced anywhere in the codebase; they appear to be dead/vestigial sealed interfaces that were created and then abandoned. They need `git rm --cached <path>` to clear the staging area, or they should simply be left out of the commit.

**Why:** These files were part of an earlier attempt at a nav-event refactor that was not carried through. They have zero callers and would compile as dead code.

**How to apply:** When reviewing this branch, remind the author to run `git rm` on these four paths before opening the PR.
