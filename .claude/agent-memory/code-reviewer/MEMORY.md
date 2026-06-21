# Memory Index

- [FilterEvent Data Layer Import](feedback_filter_event_data_import.md) — FilterEvent.kt in ui/ imports TransactionSectionSettings from data.local; this is a recurring architectural tension to watch
- [Staged-but-Deleted Files Pattern](project_staged_deleted_files.md) — Four nav event files and TransactionListDisplayConfig staged as new but deleted from disk; need explicit git rm before merge
- [Feature Audit May 2026](project_feature_audit_2026_05.md) — Full feature completeness audit: key bugs in PP delete logic, filterUsageCount never loaded, AccountStructureType domain violation, SharedWallet is a mock
- [Progressive Dashboard Loading](project_progressive_dashboard_loading.md) — channelFlow-based card loading in DashboardViewModel; CardAggregationService expanded; account filter not propagated into recursive budget rollover DB queries
