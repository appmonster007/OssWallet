# Task: Reimplement AeonExpressiveCard Optimally

- [x] Research and Planning
    - [x] List relevant files
    - [x] Read and analyze core components
    - [x] Create implementation plan
- [x] Optimization: AeonExpressiveCard.kt
    - [x] Refactor to single-surface architecture (lift GlassSurface/AeonGlassHost)
    - [x] Gate graphicsLayer math for static states
    - [x] Optimize 3D rotation distance and elevation curves
- [x] Cleanup and Refactoring
    - [x] Delete unused `AeonCardFilterButton`
    - [x] Merge `CardSettingsSheet` legacy wrapper into `AeonExpressiveCardConfig`
    - [x] Remove redundant layout modifiers in `AeonCardList.kt` (verified none)
- [x] Verification
    - [x] Verify card flip animation performance
    - [x] Verify all 15+ card types render correctly in both states
    - [x] Run compose previews
