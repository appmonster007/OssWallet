# Dependency Graph

This file shows the dependency relationships between all Kotlin files in the app/src/main/java directory.

## Analysis Summary
- **Total Files Analyzed**: 243
- **Total Classes Found**: 343
- **Files with Dependencies**: 166
- **Total Dependency Relationships**: 593

## Verification Notes
The original analysis found only 166 dependency relationships. After a detailed verification by reading all 243 files and analyzing their actual imports, the accurate count is **593 dependency relationships**. The graph below reflects the complete, verified dependency mapping.

## Mermaid Graph

```mermaid
graph TD
    F0["MainActivity"]
    F1["OssWalletApp"]
    F2["AppDatabase"]
    F3["BackupModels"]
    F4["Converters"]
    F5["SettingsDataStore"]
    F6["AccountDao"]
    F7["BudgetDao"]
    F8["CategoryDao"]
    F9["CurrencyDao"]
    F10["FilterDao"]
    F11["LoanDao"]
    F12["ProfileDao"]
    F13["TagDao"]
    F14["TemplateDao"]
    F15["TransactionDao"]
    F16["TransactionLinkDao"]
    F17["AccountEntity"]
    F18["BudgetEntity"]
    F19["CategoryEntity"]
    F20["CurrencyRateEntity"]
    F21["FilterEntity"]
    F22["LoanEntity"]
    F23["ProfileEntity"]
    F24["TagEntity"]
    F25["TemplateEntity"]
    F26["TransactionEntity"]
    F27["TransactionLinkEntity"]
    F28["AccountMapper"]
    F29["BudgetMapper"]
    F30["FilterMapper"]
    F31["LoanMapper"]
    F32["TransactionMapper"]
    F33["CurrencyApi"]
    F34["RetrofitClient"]
    F35["AccountRepositoryImpl"]
    F36["BackupRepositoryImpl"]
    F37["BudgetRepositoryImpl"]
    F38["CurrencyRepositoryImpl"]
    F39["LoanRepositoryImpl"]
    F40["ProfileRepositoryImpl"]
    F41["RepositoryExtensions"]
    F42["SharedWalletRepositoryImpl"]
    F43["TagRepositoryImpl"]
    F44["TransactionRepositoryImpl"]
    F45["DatabaseSeeder"]
    F46["SeedingConfig"]
    F47["AppContainer"]
    F48["ExpressionEvaluator"]
    F49["ThemeHelper"]
    F50["TimelineFormatter"]
    F51["Account"]
    F52["Budget"]
    F53["CashFlowType"]
    F54["FilterManager"]
    F55["Loan"]
    F56["SharedWallet"]
    F57["Transaction"]
    F58["TransactionFilter"]
    F59["TransactionFilterExtensions"]
    F60["TransactionStatus"]
    F61["TransactionType"]
    F62["AnalyticsData"]
    F63["ComputationContext"]
    F64["FinancialModels"]
    F65["TransactionPrefillDTO"]
    F66["TransactionRequest"]
    F67["FilterOperator"]
    F68["LogicalOperator"]
    F69["BaseCardConfig"]
    F70["CardConfig"]
    F71["NetWorthSettings"]
    F72["TimelineRange"]
    F73["AccountRepository"]
    F74["BackupRepository"]
    F75["BudgetRepository"]
    F76["CurrencyRepository"]
    F77["LoanRepository"]
    F78["ProfileRepository"]
    F79["SharedWalletRepository"]
    F80["TagRepository"]
    F81["TransactionRepository"]
    F82["CardComputationEngine"]
    F83["CardComputationService"]
    F84["CurrencyService"]
    F85["FilterService"]
    F86["TimelineService"]
    F87["TransactionManager"]
    F88["TransactionsUtilityService"]
    F89["AccountUseCases"]
    F90["BudgetUseCases"]
    F91["CategoryUseCases"]
    F92["LoanUseCases"]
    F93["TransactionUseCases"]
    F94["CacheUtils"]
    F95["EnumExtensions"]
    F96["StringExtensions"]
    F97["AccountDetailScreen"]
    F98["AccountHeroCard"]
    F99["BalanceByAccountsCard"]
    F100["BalanceForecastCard"]
    F101["BalanceTrendCard"]
    F102["CardActions"]
    F103["CashFlowProgressCard"]
    F104["CashFlowTableCard"]
    F105["CashFlowTrendCard"]
    F106["CategoryBreakdownCard"]
    F107["CategoryExpenseTreemapCard"]
    F108["IncomeExpensesBookCard"]
    F109["IncomeExpensesSummaryCard"]
    F110["PeriodComparisonCard"]
    F111["PlannedPaymentsCard"]
    F112["PlannedPaymentsTimelineCard"]
    F113["TimelineTrajectoryCard"]
    F114["CalculatorState"]
    F115["AccountFormComponents"]
    F116["AccountIcons"]
    F117["AeonAlertDialog"]
    F118["AeonButtons"]
    F119["AeonCards"]
    F120["AeonComponents"]
    F121["AeonDateTimePicker"]
    F122["AeonExpandableFab"]
    F123["AeonFilterComponents"]
    F124["AeonFlowComponents"]
    F125["AeonFormComponents"]
    F126["AeonInputs"]
    F127["AeonLayouts"]
    F128["AeonLists"]
    F129["AeonPickers"]
    F130["AeonProgress"]
    F131["AeonTimelineFilter"]
    F132["AppDrawer"]
    F133["CalculatorDialog"]
    F134["CalculatorNumpad"]
    F135["CardManagementComponents"]
    F136["CardReorderableList"]
    F137["CategoryFormComponents"]
    F138["CategoryMultiSelectPicker"]
    F139["NavigationPill"]
    F140["OCRScanner"]
    F141["ReorderableList"]
    F142["AeonAccountCards"]
    F143["AeonCardSettingsSheet"]
    F144["CardCallbacks"]
    F145["CardCompute"]
    F146["CardFilter"]
    F147["CardRenderData"]
    F148["UnifiedCardRenderer"]
    F149["ExpressiveNavigationBar"]
    F150["AdvancedFilterSection"]
    F151["AeonPeriodSelector"]
    F152["AeonTransactionComponents"]
    F153["FilterComponents"]
    F154["FilterSheetContent"]
    F155["FilterUtils"]
    F156["RecentActivitiesCard"]
    F157["SwipeableTransactionItem"]
    F158["TransactionFilterSheet"]
    F159["TransactionHeader"]
    F160["TransactionListEmptyState"]
    F161["TransactionListTopAppBar"]
    F162["AeonProgressBar"]
    F163["AeonBarChart"]
    F164["AeonBarLineChart"]
    F165["AeonChartInteractionPopup"]
    F166["AeonChartModels"]
    F167["AeonLineChart"]
    F168["AeonPieChart"]
    F169["AeonTreemap"]
    F170["CategoryBreakdownPieChart"]
    F171["TimelineTrajectoryLineChart"]
    F172["DashboardScreen"]
    F173["DashboardViewModel"]
    F174["AccountsCard"]
    F175["BudgetsCard"]
    F176["CashFlowCard"]
    F177["CurrencyBalancesCard"]
    F178["LoansCard"]
    F179["NetWorthCard"]
    F180["AccountsCardViewState"]
    F181["BudgetsCardViewState"]
    F182["CardViewState"]
    F183["CashFlowCardViewState"]
    F184["CurrencyBalancesCardViewState"]
    F185["DashboardUiState"]
    F186["NetWorthCardViewState"]
    F187["AccountsFilterContext"]
    F188["DashboardViewContext"]
    F189["NavigationExtensions"]
    F190["OssWalletNavGraph"]
    F191["RecordsFilterContext"]
    F192["Screen"]
    F193["AccountManagementScreen"]
    F194["AccountSetupScreen"]
    F195["AccountsViewScreen"]
    F196["AddEditBudgetScreen"]
    F197["AddEditLoanScreen"]
    F198["AddTemplateScreen"]
    F199["AnalyticsScreen"]
    F200["BudgetDetailScreen"]
    F201["BudgetsScreen"]
    F202["CategoryManagementScreen"]
    F203["EditAccountScreen"]
    F204["EditBudgetScreen"]
    F205["EditFilterScreen"]
    F206["ExchangeRatesScreen"]
    F207["FiltersSettingsScreen"]
    F208["LoanDetailScreen"]
    F209["LoansScreen"]
    F210["ManageLabelsScreen"]
    F211["OnboardingScreen"]
    F212["SettingsScreen"]
    F213["SharedWalletScreen"]
    F214["TemplatesScreen"]
    F215["TransactionEntryScreen"]
    F216["TransactionListContent"]
    F217["TransactionListPreview"]
    F218["TransactionListScreen"]
    F219["Color"]
    F220["Theme"]
    F221["Type"]
    F222["ColorUtils"]
    F223["CurrencyUtils"]
    F224["TransactionTypeUtils"]
    F225["AccountDetailViewModel"]
    F226["AccountViewModel"]
    F227["AccountsViewViewModel"]
    F228["AnalyticsViewModel"]
    F229["BaseViewModel"]
    F230["BudgetViewModel"]
    F231["CardManagerViewModel"]
    F232["CardType"]
    F233["CurrencyViewModel"]
    F234["GenericViewModelFactory"]
    F235["LoanViewModel"]
    F236["NetWorthViewModel"]
    F237["SettingsViewModel"]
    F238["SharedWalletViewModel"]
    F239["TagViewModel"]
    F240["TemplateViewModel"]
    F241["TransactionViewModel"]
    F242["BiometricAuthenticator"]
    F0 --> F46
    F0 --> F47
    F1 --> F47
    F2 --> F27
    F4 --> F52
    F4 --> F58
    F4 --> F72
    F5 --> F71
    F5 --> F174
    F5 --> F176
    F5 --> F177
    F6 --> F17
    F7 --> F18
    F8 --> F19
    F9 --> F20
    F10 --> F21
    F11 --> F22
    F12 --> F23
    F13 --> F24
    F14 --> F25
    F15 --> F26
    F16 --> F26
    F16 --> F27
    F18 --> F52
    F18 --> F58
    F21 --> F72
    F28 --> F17
    F28 --> F51
    F29 --> F18
    F29 --> F52
    F29 --> F58
    F30 --> F21
    F30 --> F58
    F30 --> F60
    F31 --> F22
    F31 --> F51
    F31 --> F55
    F32 --> F26
    F32 --> F57
    F32 --> F60
    F35 --> F6
    F35 --> F51
    F35 --> F73
    F36 --> F74
    F37 --> F7
    F37 --> F15
    F37 --> F52
    F37 --> F75
    F37 --> F85
    F38 --> F9
    F38 --> F20
    F38 --> F33
    F38 --> F76
    F39 --> F6
    F39 --> F11
    F39 --> F55
    F39 --> F77
    F40 --> F5
    F40 --> F12
    F40 --> F23
    F40 --> F78
    F42 --> F15
    F42 --> F56
    F42 --> F57
    F42 --> F79
    F43 --> F13
    F43 --> F15
    F43 --> F24
    F43 --> F80
    F44 --> F2
    F44 --> F6
    F44 --> F15
    F44 --> F16
    F44 --> F57
    F44 --> F58
    F44 --> F60
    F44 --> F81
    F45 --> F5
    F45 --> F6
    F45 --> F8
    F45 --> F10
    F45 --> F15
    F45 --> F17
    F45 --> F19
    F45 --> F21
    F45 --> F26
    F46 --> F2
    F46 --> F5
    F47 --> F2
    F47 --> F5
    F47 --> F34
    F47 --> F82
    F47 --> F83
    F47 --> F87
    F48 --> F114
    F50 --> F72
    F58 --> F3
    F58 --> F72
    F59 --> F72
    F59 --> F86
    F59 --> F191
    F64 --> F51
    F64 --> F57
    F65 --> F60
    F66 --> F60
    F70 --> F58
    F70 --> F60
    F70 --> F72
    F71 --> F72
    F73 --> F51
    F75 --> F52
    F76 --> F33
    F77 --> F55
    F78 --> F23
    F79 --> F56
    F79 --> F57
    F80 --> F24
    F81 --> F57
    F81 --> F58
    F82 --> F51
    F82 --> F57
    F82 --> F63
    F82 --> F64
    F82 --> F70
    F83 --> F8
    F83 --> F51
    F83 --> F57
    F83 --> F58
    F83 --> F60
    F83 --> F62
    F83 --> F63
    F83 --> F64
    F83 --> F72
    F85 --> F57
    F85 --> F58
    F85 --> F60
    F86 --> F72
    F87 --> F57
    F87 --> F60
    F87 --> F66
    F87 --> F93
    F88 --> F51
    F88 --> F57
    F88 --> F58
    F88 --> F60
    F89 --> F73
    F90 --> F52
    F90 --> F75
    F90 --> F81
    F90 --> F85
    F90 --> F86
    F91 --> F8
    F91 --> F19
    F92 --> F55
    F92 --> F57
    F92 --> F77
    F92 --> F81
    F93 --> F51
    F93 --> F57
    F93 --> F58
    F93 --> F79
    F93 --> F81
    F93 --> F84
    F97 --> F57
    F97 --> F58
    F97 --> F144
    F97 --> F147
    F97 --> F148
    F97 --> F149
    F97 --> F225
    F97 --> F226
    F97 --> F228
    F97 --> F231
    F97 --> F237
    F97 --> F240
    F97 --> F241
    F98 --> F51
    F98 --> F84
    F99 --> F19
    F99 --> F21
    F99 --> F51
    F99 --> F70
    F100 --> F19
    F100 --> F21
    F100 --> F51
    F100 --> F70
    F100 --> F166
    F101 --> F19
    F101 --> F21
    F101 --> F51
    F101 --> F70
    F101 --> F166
    F102 --> F19
    F102 --> F21
    F102 --> F51
    F102 --> F191
    F103 --> F19
    F103 --> F21
    F103 --> F51
    F103 --> F70
    F104 --> F19
    F104 --> F21
    F104 --> F51
    F104 --> F70
    F105 --> F19
    F105 --> F21
    F105 --> F51
    F105 --> F70
    F106 --> F19
    F106 --> F21
    F106 --> F51
    F106 --> F62
    F106 --> F70
    F106 --> F166
    F107 --> F19
    F107 --> F21
    F107 --> F51
    F107 --> F62
    F107 --> F70
    F107 --> F166
    F108 --> F19
    F108 --> F21
    F108 --> F51
    F108 --> F70
    F109 --> F19
    F109 --> F21
    F109 --> F51
    F109 --> F70
    F110 --> F19
    F110 --> F21
    F110 --> F51
    F110 --> F70
    F110 --> F166
    F111 --> F19
    F111 --> F21
    F111 --> F51
    F111 --> F70
    F112 --> F19
    F112 --> F21
    F112 --> F51
    F112 --> F70
    F113 --> F19
    F113 --> F21
    F113 --> F51
    F113 --> F62
    F113 --> F70
    F113 --> F166
    F114 --> F48
    F122 --> F25
    F124 --> F53
    F124 --> F58
    F128 --> F19
    F129 --> F19
    F129 --> F24
    F129 --> F51
    F131 --> F72
    F132 --> F23
    F132 --> F25
    F132 --> F51
    F134 --> F48
    F137 --> F19
    F138 --> F19
    F142 --> F51
    F142 --> F53
    F143 --> F19
    F143 --> F21
    F143 --> F51
    F143 --> F58
    F144 --> F72
    F145 --> F51
    F145 --> F57
    F145 --> F63
    F145 --> F64
    F145 --> F82
    F146 --> F51
    F146 --> F57
    F147 --> F51
    F147 --> F62
    F147 --> F64
    F147 --> F70
    F147 --> F72
    F147 --> F99
    F147 --> F100
    F147 --> F101
    F147 --> F103
    F147 --> F104
    F147 --> F105
    F147 --> F108
    F147 --> F109
    F147 --> F110
    F147 --> F111
    F147 --> F112
    F147 --> F236
    F148 --> F19
    F148 --> F21
    F148 --> F51
    F148 --> F72
    F150 --> F19
    F150 --> F21
    F150 --> F51
    F150 --> F58
    F151 --> F58
    F151 --> F72
    F151 --> F86
    F152 --> F19
    F152 --> F53
    F152 --> F57
    F152 --> F84
    F153 --> F19
    F153 --> F51
    F153 --> F58
    F154 --> F19
    F154 --> F21
    F154 --> F51
    F154 --> F58
    F155 --> F58
    F155 --> F60
    F156 --> F19
    F156 --> F53
    F156 --> F57
    F156 --> F84
    F157 --> F57
    F157 --> F60
    F158 --> F19
    F158 --> F21
    F158 --> F51
    F158 --> F58
    F161 --> F5
    F161 --> F58
    F170 --> F62
    F171 --> F62
    F172 --> F25
    F172 --> F51
    F172 --> F52
    F172 --> F53
    F172 --> F57
    F172 --> F63
    F172 --> F64
    F172 --> F70
    F172 --> F72
    F172 --> F86
    F172 --> F90
    F172 --> F92
    F172 --> F135
    F172 --> F144
    F172 --> F147
    F172 --> F148
    F172 --> F149
    F172 --> F188
    F172 --> F226
    F172 --> F228
    F172 --> F230
    F172 --> F231
    F172 --> F235
    F172 --> F236
    F172 --> F237
    F172 --> F240
    F172 --> F241
    F173 --> F5
    F173 --> F51
    F173 --> F53
    F173 --> F57
    F173 --> F58
    F173 --> F62
    F173 --> F63
    F173 --> F72
    F173 --> F73
    F173 --> F83
    F173 --> F188
    F173 --> F228
    F173 --> F231
    F173 --> F237
    F174 --> F51
    F174 --> F64
    F174 --> F72
    F174 --> F86
    F174 --> F191
    F175 --> F72
    F175 --> F90
    F175 --> F191
    F176 --> F53
    F176 --> F64
    F176 --> F72
    F177 --> F64
    F177 --> F72
    F178 --> F92
    F178 --> F191
    F179 --> F71
    F179 --> F72
    F179 --> F86
    F179 --> F191
    F179 --> F236
    F180 --> F72
    F183 --> F72
    F184 --> F72
    F185 --> F182
    F186 --> F72
    F188 --> F173
    F190 --> F47
    F190 --> F173
    F190 --> F225
    F190 --> F226
    F190 --> F227
    F190 --> F228
    F190 --> F230
    F190 --> F231
    F190 --> F233
    F190 --> F234
    F190 --> F235
    F190 --> F237
    F190 --> F238
    F190 --> F239
    F190 --> F240
    F190 --> F241
    F193 --> F51
    F193 --> F226
    F194 --> F226
    F195 --> F51
    F195 --> F53
    F195 --> F57
    F195 --> F64
    F195 --> F72
    F195 --> F135
    F195 --> F144
    F195 --> F147
    F195 --> F148
    F195 --> F187
    F195 --> F226
    F195 --> F227
    F195 --> F228
    F195 --> F231
    F195 --> F236
    F195 --> F237
    F195 --> F241
    F196 --> F226
    F196 --> F230
    F196 --> F239
    F196 --> F241
    F197 --> F235
    F198 --> F24
    F198 --> F25
    F198 --> F226
    F198 --> F237
    F198 --> F239
    F198 --> F240
    F198 --> F241
    F199 --> F58
    F199 --> F99
    F199 --> F100
    F199 --> F101
    F199 --> F103
    F199 --> F104
    F199 --> F105
    F199 --> F108
    F199 --> F109
    F199 --> F110
    F199 --> F111
    F199 --> F112
    F199 --> F144
    F199 --> F147
    F199 --> F148
    F200 --> F85
    F200 --> F90
    F200 --> F230
    F200 --> F241
    F201 --> F90
    F201 --> F230
    F201 --> F237
    F202 --> F19
    F202 --> F241
    F203 --> F226
    F203 --> F241
    F204 --> F230
    F204 --> F237
    F205 --> F19
    F205 --> F21
    F205 --> F51
    F205 --> F58
    F206 --> F233
    F207 --> F21
    F207 --> F237
    F208 --> F92
    F208 --> F235
    F208 --> F241
    F209 --> F92
    F209 --> F235
    F209 --> F237
    F210 --> F24
    F210 --> F239
    F211 --> F226
    F211 --> F237
    F212 --> F237
    F213 --> F56
    F213 --> F238
    F214 --> F25
    F214 --> F226
    F214 --> F240
    F215 --> F19
    F215 --> F24
    F215 --> F25
    F215 --> F51
    F215 --> F53
    F215 --> F57
    F215 --> F65
    F215 --> F66
    F215 --> F84
    F216 --> F5
    F216 --> F19
    F216 --> F21
    F216 --> F25
    F216 --> F51
    F216 --> F57
    F216 --> F58
    F216 --> F228
    F217 --> F19
    F217 --> F51
    F217 --> F57
    F217 --> F58
    F217 --> F228
    F218 --> F57
    F218 --> F58
    F218 --> F191
    F218 --> F228
    F218 --> F240
    F218 --> F241
    F223 --> F84
    F225 --> F5
    F225 --> F51
    F225 --> F57
    F225 --> F63
    F225 --> F72
    F225 --> F73
    F225 --> F88
    F225 --> F94
    F226 --> F5
    F226 --> F51
    F226 --> F57
    F226 --> F73
    F226 --> F81
    F227 --> F5
    F227 --> F51
    F227 --> F57
    F227 --> F63
    F227 --> F64
    F227 --> F72
    F227 --> F73
    F227 --> F88
    F227 --> F94
    F227 --> F187
    F228 --> F5
    F228 --> F51
    F228 --> F57
    F228 --> F58
    F228 --> F73
    F228 --> F76
    F228 --> F83
    F228 --> F84
    F228 --> F85
    F228 --> F88
    F228 --> F93
    F228 --> F94
    F229 --> F5
    F230 --> F52
    F230 --> F90
    F231 --> F70
    F231 --> F72
    F233 --> F5
    F233 --> F9
    F233 --> F20
    F233 --> F76
    F235 --> F55
    F235 --> F92
    F236 --> F72
    F237 --> F5
    F237 --> F10
    F237 --> F21
    F237 --> F23
    F237 --> F74
    F237 --> F78
    F238 --> F56
    F238 --> F79
    F239 --> F24
    F239 --> F80
    F240 --> F14
    F240 --> F25
    F241 --> F5
    F241 --> F54
    F241 --> F57
    F241 --> F58
    F241 --> F65
    F241 --> F66
    F241 --> F93
    F241 --> F191
```
