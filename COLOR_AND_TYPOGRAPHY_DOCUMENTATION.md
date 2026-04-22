# Color and Typography System Documentation

## ⚠️ IMPORTANT UPDATE: Material 3 Expressive Color Revision

This documentation has been updated to reflect the **Material 3 Expressive Color Revision** implemented on April 22, 2026. The color system has been revised to follow Material 3 expressive color guidelines with more vibrant, saturated colors and better tonal palettes.

### Key Changes
- **More vibrant primary color**: Teal-green is more saturated and expressive
- **Warmer secondary color**: Changed to amber-brown for better complementary contrast
- **More expressive tertiary**: Changed to purple-violet for creative accent
- **Better tonal palettes**: Improved surface variants with better contrast
- **More vibrant financial indicators**: Green and red are more saturated
- **New color tokens**: Added surface variants, accent colors, semantic colors

### Documentation Files
- **MATERIAL3_EXPRESSIVE_COLOR_REVISION.md**: Detailed revision documentation
- **COLOR_PALETTE_REFERENCE.md**: Visual color palette reference

### Legacy Information
The color definitions and usage patterns documented below reflect the **new expressive color system**. For comparison with the previous system, refer to the revision documentation.

---

## Table of Contents
1. [Color System](#color-system)
2. [Typography System](#typography-system)
3. [Usage Patterns](#usage-patterns)
4. [Color Flowchart](#color-flowchart)
5. [Typography Flowchart](#typography-flowchart)

---

## Color System

### 1. Color Definitions (`ui/theme/Color.kt`)

#### Light Theme Colors
```kotlin
Primary System (Teal-Green - Vibrant)
- PrimaryLight:         #006A5D (Vibrant Teal)
- OnPrimaryLight:       #FFFFFF (White)
- PrimaryContainerLight:#74F8D9 (Vibrant Light Teal)
- OnPrimaryContainerLight: #00201A (Very Dark Teal)

Secondary System (Amber-Brown - Warm)
- SecondaryLight:       #6D5A3A (Warm Amber-Brown)
- OnSecondaryLight:     #FFFFFF (White)
- SecondaryContainerLight: #F4DDB5 (Warm Amber)
- OnSecondaryContainerLight: #231900 (Dark Amber-Brown)

Tertiary System (Purple-Violet - Expressive)
- TertiaryLight:        #5A3A6D (Vibrant Purple-Violet)
- OnTertiaryLight:      #FFFFFF (White)
- TertiaryContainerLight: #DDC0F6 (Vibrant Light Violet)
- OnTertiaryContainerLight: #1A0029 (Dark Purple)

Error System (Vibrant Red)
- ErrorLight:           #B3261E (Vibrant Red)
- OnErrorLight:         #FFFFFF (White)
- ErrorContainerLight:  #F9DEDC (Light Red)
- OnErrorContainerLight: #410E0B (Dark Red)

Background/Surface
- BackgroundLight:      #FBFDF8 (Warm Off-White)
- OnBackgroundLight:    #191C18 (Dark Gray)
- SurfaceLight:         #FBFDF8 (Warm Off-White)
- OnSurfaceLight:       #191C18 (Dark Gray)

Surface Variants (New)
- SurfaceVariantLight:  #DBE5DE (Elevated Surface)
- OnSurfaceVariantLight: #3F4944 (Text on Surface Variant)
- OutlineLight:         #6F7974 (Borders)
- OutlineVariantLight:  #BFC9C2 (Subtle Borders)
- InverseSurfaceLight:  #2E312E (Inverse Background)
- InverseOnSurfaceLight: #F0F1EC (Inverse Text)
```

#### Dark Theme Colors
```kotlin
Primary System (Light Teal - Vibrant)
- PrimaryDark:          #52DBC0 (Vibrant Light Teal)
- OnPrimaryDark:        #00382F (Dark Teal)
- PrimaryContainerDark: #005138 (Dark Teal)
- OnPrimaryContainerDark: #74F8D9 (Vibrant Light Teal)

Secondary System (Light Amber - Warm)
- SecondaryDark:        #D7C19A (Warm Light Amber)
- OnSecondaryDark:      #3B2F15 (Dark Amber-Brown)
- SecondaryContainerDark: #524325 (Dark Amber-Brown)
- OnSecondaryContainerDark: #F4DDB5 (Warm Light Amber)

Tertiary System (Light Purple - Expressive)
- TertiaryDark:         #C0A5DA (Vibrant Light Purple)
- OnTertiaryDark:       #2F1643 (Dark Purple)
- TertiaryContainerDark: #432954 (Dark Purple)
- OnTertiaryContainerDark: #DDC0F6 (Vibrant Light Violet)

Error System (Light Red - Vibrant)
- ErrorDark:            #F2B8B5 (Vibrant Light Red)
- OnErrorDark:          #601410 (Dark Red)
- ErrorContainerDark:   #8C1D18 (Dark Red)
- OnErrorContainerDark: #F9DEDC (Light Red)

Background/Surface
- BackgroundDark:       #191C18 (Rich Dark Gray)
- OnBackgroundDark:     #E1E3DF (Light Gray)
- SurfaceDark:          #191C18 (Rich Dark Gray)
- OnSurfaceDark:        #E1E3DF (Light Gray)

Surface Variants (New)
- SurfaceVariantDark:   #3F4944 (Elevated Surface)
- OnSurfaceVariantDark: #BFC9C2 (Text on Surface Variant)
- OutlineDark:          #8A938C (Borders)
- OutlineVariantDark:   #3F4944 (Subtle Borders)
- InverseSurfaceDark:   #E1E3DF (Inverse Background)
- InverseOnSurfaceDark: #191C18 (Inverse Text)
```

#### Special Colors
```kotlin
Financial Indicators (More Vibrant)
- PositiveAmountColor:  #00C853 (Vibrant Green - for income)
- NegativeAmountColor:  #FF1744 (Vibrant Red - for expenses)

Accent Colors (New)
- AccentBlue:          #2979FF (Blue accent for variety)
- AccentOrange:        #FF9100 (Orange accent for variety)
- AccentPurple:        #D500F9 (Purple accent for variety)

Gradient Colors (New)
- GradientStart:       #006A5D (Primary - gradient start)
- GradientEnd:         #52DBC0 (Primary Dark - gradient end)

Semantic Colors (New)
- SuccessColor:        #00C853 (Success states)
- WarningColor:        #FF9100 (Warning states)
- InfoColor:           #2979FF (Information states)
```

### 2. Theme Schemes (`ui/theme/Theme.kt`)

#### Light Color Scheme
```kotlin
lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,        // NEW
    onSurfaceVariant = OnSurfaceVariantLight,    // NEW
    outline = OutlineLight,                      // NEW
    outlineVariant = OutlineVariantLight,        // NEW
    inverseSurface = InverseSurfaceLight,        // NEW
    inverseOnSurface = InverseOnSurfaceLight,    // NEW
    scrim = ScrimLight                           // NEW
)
```

#### Dark Color Scheme
```kotlin
darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,        // NEW
    onSurfaceVariant = OnSurfaceVariantDark,    // NEW
    outline = OutlineDark,                      // NEW
    outlineVariant = OutlineVariantDark,        // NEW
    inverseSurface = InverseSurfaceDark,        // NEW
    inverseOnSurface = InverseOnSurfaceDark,    // NEW
    scrim = ScrimDark                           // NEW
)
```

#### Amoled Color Scheme
```kotlin
darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color.Black,
    onBackground = OnBackgroundDark,
    surface = Color.Black,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1A1D1A),        // UPDATED
    onSurfaceVariant = OnSurfaceVariantDark,    // NEW
    outline = OutlineDark,                      // NEW
    outlineVariant = Color(0xFF1A1D1A),         // NEW
    inverseSurface = InverseSurfaceDark,        // NEW
    inverseOnSurface = InverseOnSurfaceDark,    // NEW
    scrim = ScrimDark                           // NEW
)
```

### 3. Dynamic Color Support
- Supports Material You dynamic colors on Android 12+ (API 31+)
- Falls back to custom color schemes on older devices
- Theme selection: "System", "Light", "Dark", "Amoled"

### 4. Color Utilities (`ui/util/ColorUtils.kt`)

#### `Color.isDark()`
Determines if a color is dark based on luminance calculation.
```kotlin
val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
return luminance < 0.5
```

#### `Color.pastel()`
Creates a pastel version of a color by adjusting saturation and lightness.
- Saturation: 15-35%
- Lightness: 92%

#### `Color.toContainerColor(isDarkTheme, isAmoled)`
Creates a container color from a seed color following Material You logic.
- **Amoled**: Very dark surface (5% lightness)
- **Dark**: Dark surface (15% lightness)
- **Light**: Very light surface (92% lightness)

#### `Color.toOnContainerColor(isDarkTheme, isAmoled)`
Creates an on-container color with high contrast.
- **Amoled/Dark**: Light color (90% lightness)
- **Light**: Dark color (25% lightness)

### 5. XML Color Resources (`res/values/colors.xml`)

Legacy XML colors (mostly unused in Compose):
```xml
- purple_200:  #FFBB86FC
- purple_500:  #FF6200EE
- purple_700:  #FF3700B3
- teal_200:    #FF03DAC5
- teal_700:    #FF018786
- black:       #FF000000
- white:       #FFFFFFFF
```

XML Drawable Colors:
```xml
badge_background_green: #1A4CAF50 (10% opacity green)
badge_background_red:   #1AF44336 (10% opacity red)
```

### 6. Material Design Color Tokens Usage

#### Primary Usage
- **Primary**: Main action buttons, FABs, active navigation items
- **PrimaryContainer**: Cards, highlighted sections, badges
- **OnPrimary/OnPrimaryContainer**: Text/icons on primary surfaces

#### Secondary Usage
- **Secondary**: Secondary actions, less prominent elements
- **SecondaryContainer**: Subtle backgrounds, chips
- **OnSecondary/OnSecondaryContainer**: Text/icons on secondary surfaces

#### Tertiary Usage
- **Tertiary**: Accent elements, decorative items
- **TertiaryContainer**: Tinted backgrounds
- **OnTertiary/OnTertiaryContainer**: Text/icons on tertiary surfaces

#### Error Usage
- **Error**: Error states, destructive actions
- **ErrorContainer**: Error backgrounds, warning cards
- **OnError/OnErrorContainer**: Text/icons on error surfaces

#### Background/Surface Usage
- **Background**: Main app background
- **Surface**: Card backgrounds, dialogs, sheets
- **SurfaceVariant**: Elevated surfaces, alternate backgrounds
- **SurfaceContainer**: Container-like surfaces (cards with borders)
- **OnBackground/OnSurface**: Primary text
- **OnSurfaceVariant**: Secondary text, icons

#### Outline Usage
- **Outline**: Borders, dividers, inactive states
- **OutlineVariant**: Subtle borders, separators

### 7. Color Usage Patterns

#### Alpha Transparency Patterns
```kotlin
// Surface variants for backgrounds
surfaceVariant.copy(alpha = 0.2f)  // Very subtle
surfaceVariant.copy(alpha = 0.3f)  // Subtle
surfaceVariant.copy(alpha = 0.4f)  // Medium
surfaceVariant.copy(alpha = 0.5f)  // Medium-high
surfaceVariant.copy(alpha = 0.6f)  // High

// Container colors
primaryContainer.copy(alpha = 0.3f)  // Subtle highlight
primaryContainer.copy(alpha = 0.4f)  // Medium highlight
secondaryContainer.copy(alpha = 0.4f)  // Medium highlight
secondaryContainer.copy(alpha = 0.5f)  // Medium-high highlight

// Outline colors
outline.copy(alpha = 0.1f)  // Very subtle (disabled)
outline.copy(alpha = 0.4f)  // Subtle (enabled)
outline.copy(alpha = 0.5f)  // Medium (borders)
outline.copy(alpha = 0.6f)  // High (placeholders)

// Error colors
error.copy(alpha = 0.1f)  // Very subtle (disabled)
error.copy(alpha = 0.3f)  // Subtle (disabled)
error.copy(alpha = 0.5f)  // Medium (borders)
```

#### Hardcoded Colors (Legacy - Updated)
```kotlin
// CategoryItem (AeonCards.kt) - NOW USES THEME COLORS
// Previously: Color(0xFF004D56), Color(0xFFF1F3F4), Color.White, Color(0xFF546E7A)
// Now: MaterialTheme.colorScheme.primary, surfaceVariant, onPrimary, onSurfaceVariant

// AeonFlowComponents.kt - NOW USES THEME COLORS
// Previously: Color(0xFF4CAF50), Color(0xFFF44336)
// Now: PositiveAmountColor, NegativeAmountColor (from theme)

// NetWorthCard.kt - NOW USES THEME COLORS
// Previously: Color(0xFF4CAF50), Color(0xFFF44336)
// Now: PositiveAmountColor, NegativeAmountColor (from theme)

// Theme.kt (Amoled)
Color(0xFF1A1D1A)  // Surface variant (updated)
Color.Black        # Background/Surface
```

**Note**: Most hardcoded colors have been migrated to theme colors for better consistency and theme support.

---

## Typography System

### 1. Typography Definitions (`ui/theme/Type.kt`)

#### Font Families
```kotlin
val Manrope = FontFamily.SansSerif  // Currently using default (prevents crashes)
val Inter = FontFamily.Default       // Currently using default
```
**Note**: Custom fonts (Manrope/Inter) are temporarily disabled due to resource loading issues. Using system defaults instead.

### 2. Typography Scale

#### Display Styles (Headlines, Hero Text)
```kotlin
displayLarge:
  - Font: Manrope
  - Weight: Bold
  - Size: 57sp
  - Line Height: 64sp
  - Letter Spacing: -0.25sp
  - Feature: tnum (tabular numbers)

displayMedium:
  - Font: Manrope
  - Weight: Bold
  - Size: 45sp
  - Line Height: 52sp
  - Letter Spacing: 0sp
  - Feature: tnum

displaySmall:
  - Font: Manrope
  - Weight: Bold
  - Size: 36sp
  - Line Height: 44sp
  - Letter Spacing: 0sp
  - Feature: tnum
```

#### Headline Styles (Section Headers)
```kotlin
headlineLarge:
  - Font: Manrope
  - Weight: Normal
  - Size: 32sp
  - Line Height: 40sp
  - Letter Spacing: 0sp
  - Feature: tnum

headlineMedium:
  - Font: Manrope
  - Weight: Normal
  - Size: 28sp
  - Line Height: 36sp
  - Letter Spacing: 0sp
  - Feature: tnum

headlineSmall:
  - Font: Manrope
  - Weight: Normal
  - Size: 24sp
  - Line Height: 32sp
  - Letter Spacing: 0sp
  - Feature: tnum
```

#### Title Styles (Card Headers, Subsections)
```kotlin
titleLarge:
  - Font: Manrope
  - Weight: Bold
  - Size: 22sp
  - Line Height: 28sp
  - Letter Spacing: 0sp
  - Feature: tnum

titleMedium:
  - Font: Manrope
  - Weight: Bold
  - Size: 18sp
  - Line Height: 24sp
  - Letter Spacing: 0.15sp
  - Feature: tnum

titleSmall:
  - Font: Manrope
  - Weight: Medium
  - Size: 14sp
  - Line Height: 20sp
  - Letter Spacing: 0.1sp
  - Feature: tnum
```

#### Body Styles (Primary Text Content)
```kotlin
bodyLarge:
  - Font: Inter
  - Weight: Normal
  - Size: 16sp
  - Line Height: 24sp
  - Letter Spacing: 0.5sp
  - Feature: tnum

bodyMedium:
  - Font: Inter
  - Weight: Normal
  - Size: 14sp
  - Line Height: 20sp
  - Letter Spacing: 0.25sp
  - Feature: tnum

bodySmall:
  - Font: Inter
  - Weight: Normal
  - Size: 12sp
  - Line Height: 16sp
  - Letter Spacing: 0.4sp
  - Feature: tnum
```

#### Label Styles (Buttons, Chips, Captions)
```kotlin
labelLarge:
  - Font: Inter
  - Weight: Medium
  - Size: 14sp
  - Line Height: 20sp
  - Letter Spacing: 0.1sp
  - Feature: tnum

labelMedium:
  - Font: Inter
  - Weight: Medium
  - Size: 12sp
  - Line Height: 16sp
  - Letter Spacing: 0.5sp
  - Feature: tnum

labelSmall:
  - Font: Inter
  - Weight: Medium
  - Size: 11sp
  - Line Height: 16sp
  - Letter Spacing: 0.5sp
  - Feature: tnum
```

### 3. Typography Usage Patterns

#### Display Styles Usage
- **displayLarge**: Extra large numbers (Net Worth - X-Large setting)
- **displayMedium**: Large numbers (Net Worth - Large/Medium setting)
- **displaySmall**: Medium-large numbers (Net Worth - Small setting)

#### Headline Styles Usage
- **headlineLarge**: Page titles, hero sections
- **headlineMedium**: Section headers, dialog titles
- **headlineSmall**: Card headers, subsection titles

#### Title Styles Usage
- **titleLarge**: Card titles, main headers
- **titleMedium**: Subsection headers, list item titles
- **titleSmall**: Small headers, chip labels

#### Body Styles Usage
- **bodyLarge**: Primary content, descriptions
- **bodyMedium**: Secondary content, list items
- **bodySmall**: Tertiary content, captions

#### Label Styles Usage
- **labelLarge**: Button text, chip labels
- **labelMedium**: Small buttons, tag labels
- **labelSmall**: Captions, badges, tiny labels

### 4. Font Weight Usage

#### Bold
- Display styles (all)
- Title styles (Large, Medium)
- Form headers
- Selected items
- Emphasized text

#### Medium
- Label styles (all)
- TitleSmall
- Form labels
- Secondary emphasis

#### Normal
- Headline styles (all)
- Body styles (all)
- Default text

#### Black
- Extra emphasis (numbers, special cases)
- HEX code prefixes
- Special highlighted text

#### ExtraBold
- Currency badges
- Special emphasis

#### Light
- (Not currently used in codebase)

### 5. Font Size Customizations

#### Custom Font Sizes (from .copy())
```kotlin
// NetWorthCard
fontSize = 64.sp  // X-Large display

// Account cards, transaction components
fontSize = 9.sp   // Tiny labels
fontSize = 10.sp  // Small labels
fontSize = 11.sp  // Medium-small labels

// Timeline labels
fontSize = 10.sp  // Timeline badges
```

#### Letter Spacing Customizations
```kotlin
// Tight spacing (negative)
letterSpacing = (-1).sp  // Large numbers (Net Worth)

// Standard spacing
letterSpacing = 0sp      // Most text

// Wide spacing
letterSpacing = 0.1sp   // Label styles
letterSpacing = 0.15sp  // TitleMedium
letterSpacing = 0.25sp  // BodyMedium
letterSpacing = 0.4sp   // BodySmall, LabelMedium/Small
letterSpacing = 0.5sp   // BodyLarge, LabelMedium/Small
letterSpacing = 1.sp    // Section labels

// Extra wide spacing
letterSpacing = 0.5sp   // Button text, badges
```

### 6. Typography Copy Patterns

#### Common Copy Patterns
```kotlin
// Weight modifications
.copy(fontWeight = FontWeight.Bold)
.copy(fontWeight = FontWeight.Medium)
.copy(fontWeight = FontWeight.Black)
.copy(fontWeight = FontWeight.ExtraBold)

// Size modifications
.copy(fontSize = 64.sp)  // X-Large
.copy(fontSize = 10.sp)  // Small
.copy(fontSize = 9.sp)   // Tiny

// Letter spacing modifications
.copy(letterSpacing = (-1).sp)  // Tight
.copy(letterSpacing = 0.5.sp)   // Wide
.copy(letterSpacing = 1.sp)     // Extra wide

// Combined modifications
.copy(
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.5.sp,
    fontSize = 10.sp
)
```

---

## Usage Patterns

### 1. Component-Level Color Usage

#### Navigation Components
- **ExpressiveNavigationBar**: Uses `surfaceVariant`, `onPrimary`, `primary`, `onSurfaceVariant`
- **NavigationPill**: Uses `surfaceVariant`, `primary`, `onPrimary`, `onSurfaceVariant`

#### Card Components
- **AeonCards**: Uses hardcoded colors (legacy)
- **NetWorthCard**: Uses `surfaceContainer`, `outlineVariant`, `primary`, hardcoded flow colors
- **AccountsCard**: Uses `surfaceContainer`, `outlineVariant`, `primary`, `error`
- **CashFlowCard**: Uses `surfaceContainer`, `primaryContainer`
- **BudgetsCard**: Uses `surfaceContainer`, `primaryContainer`
- **CurrencyBalancesCard**: Uses `surfaceContainer`

#### Form Components
- **AeonInputs**: Uses `surfaceVariant`, `primaryContainer`, `outline`
- **AeonFormComponents**: Uses `surfaceVariant`, `outline`, `secondaryContainer`
- **CategoryFormComponents**: Uses `surface`, `outline`, `secondaryContainer`, `primary`
- **AccountFormComponents**: Uses `surfaceVariant`, `outline`

#### Button Components
- **AeonButtons**: Uses `error`, `onSurface`, `outline`, `primary`, `onPrimary`
- **AeonExpandableFab**: Uses `surface`, `secondaryContainer`, `onSecondaryContainer`

#### Transaction Components
- **AeonTransactionComponents**: Uses `surfaceVariant`, `primary`, `error`
- **TransactionListTopAppBar**: Uses `surface`, `onSurface`
- **RecentActivitiesCard**: Uses `surfaceContainer`

#### Progress Components
- **AeonProgress**: Uses `primaryContainer`, `secondaryContainer`

#### Timeline Components
- **AeonTimelineFilter**: Uses `surfaceVariant`, `primaryContainer`, `secondaryContainer`
- **AeonPeriodSelector**: Uses `surfaceVariant`, `primary`

### 2. Screen-Level Color Usage

#### Dashboard Screen
- Background: `background`
- Cards: `surfaceContainer`, `surfaceVariant`
- Icons: `primary`, `outline`
- Text: `onSurface`, `outline`

#### Account Detail Screen
- Background: `background`
- Cards: `surfaceContainer`
- Text: `onSurface`, `outline`
- Icons: `outline`

#### Transaction Entry Screen
- Background: `background`
- Cards: `surfaceContainer`
- Inputs: `surfaceVariant`
- Text: `onSurface`, `outline`

#### Settings Screen
- Background: `background`
- Cards: `surfaceContainer`, `surfaceVariant`
- Text: `onSurface`, `outline`

### 3. Component-Level Typography Usage

#### Headers
- **Page Titles**: `headlineLarge` (Bold)
- **Section Headers**: `headlineMedium` (Normal), `titleMedium` (Bold)
- **Card Headers**: `titleLarge` (Bold), `titleMedium` (Bold)

#### Body Text
- **Primary Content**: `bodyLarge` (Normal)
- **Secondary Content**: `bodyMedium` (Normal)
- **Descriptions**: `bodySmall` (Normal)

#### Labels
- **Button Text**: `bodyLarge` (Bold), `labelLarge` (Bold)
- **Chip Labels**: `labelSmall` (Medium)
- **Captions**: `labelMedium` (Medium), `labelSmall` (Medium)
- **Badges**: `labelSmall` (Bold, custom size 9-11sp)

#### Numbers
- **Large Numbers**: `displayLarge/Medium/Small` (Bold/Black)
- **Currency Amounts**: `bodyLarge` (Bold)
- **Small Numbers**: `labelSmall` (Bold)

---

## Color Flowchart

```
┌─────────────────────────────────────────────────────────────────┐
│                     COLOR SYSTEM ARCHITECTURE                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  1. COLOR DEFINITIONS (ui/theme/Color.kt)                       │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Light Theme Colors                                          │
│  │  ├─ Primary: #006C4C, #FFFFFF, #89F8C7, #002114              │
│  │  ├─ Secondary: #4D6357, #FFFFFF, #CFE9D8, #092016            │
│  │  ├─ Tertiary: #3D6373, #FFFFFF, #C1E8FB, #001F29            │
│  │  ├─ Error: #BA1A1A, #FFFFFF, #FFDAD6, #410002               │
│  │  └─ Background: #FBFDF9, #191C1A                           │
│  │                                                              │
│  ├─ Dark Theme Colors                                           │
│  │  ├─ Primary: #6CDBAC, #003826, #005138, #89F8C7             │
│  │  ├─ Secondary: #B3CCBD, #1F352A, #354B40, #CFE9D8            │
│  │  ├─ Tertiary: #A5CCE0, #073543, #244C5A, #C1E8FB            │
│  │  ├─ Error: #FFB4AB, #690005, #93000A, #FFDAD6               │
│  │  └─ Background: #191C1A, #E1E3DF                           │
│  │                                                              │
│  └─ Special Colors                                             │
│     ├─ PositiveAmountColor: #4CAF50                            │
│     └─ NegativeAmountColor: #BA1A1A                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. THEME SCHEMES (ui/theme/Theme.kt)                           │
├─────────────────────────────────────────────────────────────────┤
│  ├─ LightColorScheme                                            │
│  │  └─ Maps Light Colors to Material3 Tokens                    │
│  │                                                              │
│  ├─ DarkColorScheme                                             │
│  │  └─ Maps Dark Colors to Material3 Tokens                     │
│  │                                                              │
│  ├─ AmoledColorScheme                                           │
│  │  └─ Dark Scheme + Black Background (#000000)                │
│  │                                                              │
│  └─ Dynamic Color (Android 12+)                                 │
│     └─ dynamicLightColorScheme() / dynamicDarkColorScheme()     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. COLOR UTILITIES (ui/util/ColorUtils.kt)                      │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Color.isDark()                                              │
│  │  └─ Calculates luminance to determine dark/light            │
│  │                                                              │
│  ├─ Color.pastel()                                              │
│  │  └─ Creates pastel colors (low saturation, high lightness)   │
│  │                                                              │
│  ├─ Color.toContainerColor(isDarkTheme, isAmoled)               │
│  │  └─ Generates container colors from seed colors              │
│  │     ├─ Amoled: 5% lightness                                  │
│  │     ├─ Dark: 15% lightness                                   │
│  │     └─ Light: 92% lightness                                  │
│  │                                                              │
│  └─ Color.toOnContainerColor(isDarkTheme, isAmoled)             │
│     └─ Generates high-contrast on-container colors              │
│        ├─ Amoled/Dark: 90% lightness                             │
│        └─ Light: 25% lightness                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. MATERIAL3 COLOR TOKENS                                      │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Primary System                                              │
│  │  ├─ primary, onPrimary                                       │
│  │  ├─ primaryContainer, onPrimaryContainer                    │
│  │  └─ Usage: Actions, FABs, navigation                        │
│  │                                                              │
│  ├─ Secondary System                                            │
│  │  ├─ secondary, onSecondary                                   │
│  │  ├─ secondaryContainer, onSecondaryContainer                │
│  │  └─ Usage: Secondary actions, chips, badges                  │
│  │                                                              │
│  ├─ Tertiary System                                              │
│  │  ├─ tertiary, onTertiary                                     │
│  │  ├─ tertiaryContainer, onTertiaryContainer                 │
│  │  └─ Usage: Accents, decorative elements                     │
│  │                                                              │
│  ├─ Error System                                                │
│  │  ├─ error, onError                                           │
│  │  ├─ errorContainer, onErrorContainer                         │
│  │  └─ Usage: Error states, destructive actions                │
│  │                                                              │
│  └─ Background/Surface System                                   │
│     ├─ background, onBackground                                 │
│     ├─ surface, onSurface                                       │
│     ├─ surfaceVariant, onSurfaceVariant                         │
│     ├─ surfaceContainer                                          │
│     ├─ outline, outlineVariant                                   │
│     └─ Usage: Backgrounds, cards, text, borders                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. COMPONENT USAGE PATTERNS                                    │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Navigation Components                                      │
│  │  └─ surfaceVariant, primary, onPrimary, onSurfaceVariant    │
│  │                                                              │
│  ├─ Card Components                                             │
│  │  └─ surfaceContainer, outlineVariant, primary, error        │
│  │                                                              │
│  ├─ Form Components                                             │
│  │  └─ surfaceVariant, primaryContainer, outline              │
│  │                                                              │
│  ├─ Button Components                                           │
│  │  └─ error, onSurface, outline, primary, onPrimary            │
│  │                                                              │
│  └─ Transaction Components                                      │
│     └─ surfaceVariant, primary, error                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. ALPHA TRANSPARENCY PATTERNS                                 │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Surface Variants                                            │
│  │  ├─ 0.2f: Very subtle backgrounds                            │
│  │  ├─ 0.3f: Subtle backgrounds                                 │
│  │  ├─ 0.4f: Medium backgrounds                                 │
│  │  ├─ 0.5f: Medium-high backgrounds                            │
│  │  └─ 0.6f: High backgrounds                                  │
│  │                                                              │
│  ├─ Container Colors                                            │
│  │  ├─ 0.3f: Subtle highlights                                 │
│  │  └─ 0.4f-0.5f: Medium highlights                            │
│  │                                                              │
│  ├─ Outline Colors                                               │
│  │  ├─ 0.1f: Disabled states                                    │
│  │  ├─ 0.4f: Enabled states                                     │
│  │  ├─ 0.5f: Borders                                           │
│  │  └─ 0.6f: Placeholders                                      │
│  │                                                              │
│  └─ Error Colors                                                │
│     ├─ 0.1f: Disabled error                                     │
│     ├─ 0.3f: Subtle error                                       │
│     └─ 0.5f: Error borders                                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  7. HARDCODED COLORS (Legacy)                                   │
├─────────────────────────────────────────────────────────────────┤
│  ├─ CategoryItem                                                │
│  │  ├─ #004D56: Selected category                              │
│  │  ├─ #F1F3F4: Unselected category                             │
│  │  ├─ #FFFFFF: Selected icon                                   │
│  │  └─ #546E7A: Unselected icon/text                            │
│  │                                                              │
│  ├─ Flow Components                                             │
│  │  ├─ #4CAF50: Income color                                   │
│  │  └─ #F44336: Expense color                                  │
│  │                                                              │
│  └─ Amoled Theme                                                │
│     ├─ #000000: Pure black background                          │
│     └─ #121212: Surface variant                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Typography Flowchart

```
┌─────────────────────────────────────────────────────────────────┐
│                   TYPOGRAPHY SYSTEM ARCHITECTURE                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  1. FONT FAMILIES (ui/theme/Type.kt)                            │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Manrope                                                      │
│  │  └─ Currently: FontFamily.SansSerif (temporary fallback)    │
│  │     └─ Planned: Custom font file (when resource stable)     │
│  │                                                              │
│  └─ Inter                                                       │
│     └─ Currently: FontFamily.Default (temporary fallback)       │
│        └─ Planned: Custom font file (when resource stable)      │
│                                                              │
│  Note: Custom fonts disabled due to Resources$NotFoundException │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. TYPOGRAPHY SCALE (Material3)                                │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Display Styles (Hero Text)                                  │
│  │  ├─ displayLarge: 57sp, Bold, -0.25sp                        │
│  │  ├─ displayMedium: 45sp, Bold, 0sp                           │
│  │  └─ displaySmall: 36sp, Bold, 0sp                           │
│  │                                                              │
│  ├─ Headline Styles (Section Headers)                           │
│  │  ├─ headlineLarge: 32sp, Normal, 0sp                         │
│  │  ├─ headlineMedium: 28sp, Normal, 0sp                        │
│  │  └─ headlineSmall: 24sp, Normal, 0sp                         │
│  │                                                              │
│  ├─ Title Styles (Card Headers)                                 │
│  │  ├─ titleLarge: 22sp, Bold, 0sp                             │
│  │  ├─ titleMedium: 18sp, Bold, 0.15sp                        │
│  │  └─ titleSmall: 14sp, Medium, 0.1sp                        │
│  │                                                              │
│  ├─ Body Styles (Content Text)                                  │
│  │  ├─ bodyLarge: 16sp, Normal, 0.5sp                          │
│  │  ├─ bodyMedium: 14sp, Normal, 0.25sp                        │
│  │  └─ bodySmall: 12sp, Normal, 0.4sp                          │
│  │                                                              │
│  └─ Label Styles (Buttons, Captions)                             │
│     ├─ labelLarge: 14sp, Medium, 0.1sp                         │
│     ├─ labelMedium: 12sp, Medium, 0.5sp                         │
│     └─ labelSmall: 11sp, Medium, 0.5sp                         │
│                                                              │
│  All styles use: fontFeatureSettings = "tnum" (tabular nums)   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. FONT WEIGHT HIERARCHY                                       │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Black (900)                                                 │
│  │  └─ Extra emphasis (Net Worth numbers, HEX prefixes)         │
│  │                                                              │
│  ├─ ExtraBold (800)                                             │
│  │  └─ Currency badges, special emphasis                        │
│  │                                                              │
│  ├─ Bold (700)                                                  │
│  │  ├─ Display styles (all)                                     │
│  │  ├─ TitleLarge, TitleMedium                                  │
│  │  ├─ Form headers                                             │
│  │  ├─ Selected items                                           │
│  │  └─ Emphasized text                                          │
│  │                                                              │
│  ├─ Medium (500)                                                │
│  │  ├─ Label styles (all)                                       │
│  │  ├─ TitleSmall                                               │
│  │  ├─ Form labels                                              │
│  │  └─ Secondary emphasis                                       │
│  │                                                              │
│  └─ Normal (400)                                                │
│     ├─ Headline styles (all)                                    │
│     ├─ Body styles (all)                                        │
│     └─ Default text                                             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. CUSTOM FONT SIZES                                           │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Extra Large                                                  │
│  │  └─ 64.sp: X-Large Net Worth display                         │
│  │                                                              │
│  ├─ Tiny                                                        │
│  │  └─ 9.sp: Badges, captions                                   │
│  │                                                              │
│  ├─ Small                                                       │
│  │  └─ 10.sp: Small labels, timeline badges                     │
│  │                                                              │
│  └─ Medium-Small                                                │
│     └─ 11.sp: Account card labels                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. LETTER SPACING PATTERNS                                    │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Tight (Negative)                                            │
│  │  └─ -1.sp: Large numbers (Net Worth)                        │
│  │                                                              │
│  ├─ Standard (Zero)                                             │
│  │  └─ 0.sp: Most text (headlines, titles)                      │
│  │                                                              │
│  ├─ Narrow                                                      │
│  │  └─ 0.1sp: Label styles, TitleSmall                          │
│  │                                                              │
│  ├─ Normal                                                      │
│  │  ├─ 0.15sp: TitleMedium                                     │
│  │  ├─ 0.25sp: BodyMedium                                      │
│  │  └─ 0.4sp: BodySmall                                        │
│  │                                                              │
│  ├─ Wide                                                        │
│  │  ├─ 0.5sp: BodyLarge, LabelMedium/Small, buttons, badges    │
│  │  └─ 1.sp: Section labels (settings dialogs)                 │
│  │                                                              │
│  └─ Extra Wide                                                  │
│     └─ Custom: Button text, special emphasis                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. TYPOGRAPHY USAGE BY COMPONENT TYPE                          │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Navigation                                                  │
│  │  └─ labelSmall, labelMedium                                  │
│  │                                                              │
│  ├─ Headers                                                     │
│  │  ├─ Page titles: headlineLarge (Bold)                        │
│  │  ├─ Section headers: headlineMedium (Normal)               │
│  │  └─ Card headers: titleLarge (Bold), titleMedium (Bold)     │
│  │                                                              │
│  ├─ Body Text                                                   │
│  │  ├─ Primary: bodyLarge (Normal)                              │
│  │  ├─ Secondary: bodyMedium (Normal)                           │
│  │  └─ Tertiary: bodySmall (Normal)                            │
│  │                                                              │
│  ├─ Buttons                                                     │
│  │  └─ bodyLarge (Bold), labelLarge (Bold)                     │
│  │                                                              │
│  ├─ Forms                                                       │
│  │  ├─ Labels: labelMedium, labelSmall                          │
│  │  ├─ Inputs: bodyLarge (Normal, Bold)                        │
│  │  └─ Placeholders: bodyLarge (outline color)                  │
│  │                                                              │
│  ├─ Cards                                                       │
│  │  ├─ Titles: titleLarge, titleMedium (Bold)                  │
│  │  ├─ Content: bodyMedium, bodySmall                           │
│  │  └─ Badges: labelSmall (Bold, custom size 9-11sp)            │
│  │                                                              │
│  ├─ Numbers                                                     │
│  │  ├─ Large: displayLarge/Medium/Small (Bold/Black)           │
│  │  ├─ Currency: bodyLarge (Bold)                               │
│  │  └─ Small: labelSmall (Bold)                                 │
│  │                                                              │
│  └─ Transactions                                                │
│     ├─ Amounts: bodyLarge (Bold)                                │
│     ├─ Descriptions: bodyMedium                                  │
│     └─ Metadata: bodySmall, labelSmall                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  7. TYPOGRAPHY COPY PATTERNS                                   │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Weight Modifications                                        │
│  │  ├─ .copy(fontWeight = FontWeight.Bold)                     │
│  │  ├─ .copy(fontWeight = FontWeight.Medium)                   │
│  │  ├─ .copy(fontWeight = FontWeight.Black)                    │
│  │  └─ .copy(fontWeight = FontWeight.ExtraBold)                │
│  │                                                              │
│  ├─ Size Modifications                                           │
│  │  ├─ .copy(fontSize = 64.sp)  // X-Large                     │
│  │  ├─ .copy(fontSize = 10.sp)  // Small                       │
│  │  └─ .copy(fontSize = 9.sp)   // Tiny                        │
│  │                                                              │
│  ├─ Letter Spacing Modifications                                │
│  │  ├─ .copy(letterSpacing = (-1).sp)  // Tight                │
│  │  ├─ .copy(letterSpacing = 0.5.sp)   // Wide                 │
│  │  └─ .copy(letterSpacing = 1.sp)     // Extra wide            │
│  │                                                              │
│  └─ Combined Modifications                                       │
│     └─ .copy(                                                   │
│          fontWeight = FontWeight.Bold,                          │
│          letterSpacing = 0.5.sp,                                │
│          fontSize = 10.sp                                        │
│        )                                                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  8. TYPOGRAPHY BY SCREEN                                        │
├─────────────────────────────────────────────────────────────────┤
│  ├─ Dashboard                                                   │
│  │  ├─ Net Worth: displayLarge/Medium/Small (Bold/Black)         │
│  │  ├─ Card titles: titleMedium (Bold)                          │
│  │  ├─ Card content: bodyMedium, bodySmall                      │
│  │  └─ Badges: labelSmall (Bold, custom size)                   │
│  │                                                              │
│  ├─ Account Detail                                              │
│  │  ├─ Page title: titleMedium (Bold)                           │
│  │  ├─ Balance: displayMedium (Bold)                           │
│  │  ├─ Labels: bodyLarge, bodySmall                             │
│  │  └─ Settings: bodyLarge, bodySmall                           │
│  │                                                              │
│  ├─ Transaction Entry                                            │
│  │  ├─ Form headers: headlineSmall (Bold)                       │
│  │  ├─ Labels: labelMedium, labelSmall                          │
│  │  ├─ Inputs: bodyLarge (Normal, Bold)                         │
│  │  └─ Amount: displaySmall (Bold)                              │
│  │                                                              │
│  ├─ Settings                                                    │
│  │  ├─ Section headers: labelSmall (Bold, 1sp spacing)          │
│  │  ├─ Items: bodyMedium                                       │
│  │  └─ Descriptions: bodySmall                                  │
│  │                                                              │
│  └─ Transaction List                                            │
│     ├─ Amounts: bodyLarge (Bold)                                │
│     ├─ Descriptions: bodyMedium                                  │
│     ├─ Metadata: bodySmall, labelSmall                          │
│     └─ Dates: labelSmall                                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Summary

### Color System Key Points
1. **Three Theme Modes**: Light, Dark, Amoled (with dynamic color support on Android 12+)
2. **Material Design 3 Color System**: Primary, Secondary, Tertiary, Error, Background/Surface
3. **Custom Color Utilities**: `pastel()`, `toContainerColor()`, `toOnContainerColor()` for dynamic theming
4. **Alpha Transparency Patterns**: Consistent use of 0.1f-0.6f for various states
5. **Special Colors**: `PositiveAmountColor` (#4CAF50) and `NegativeAmountColor` (#BA1A1A) for financial indicators
6. **Legacy Hardcoded Colors**: Some components still use hardcoded colors (CategoryItem, FlowComponents)

### Typography System Key Points
1. **Font Families**: Manrope (headlines/titles) and Inter (body/labels) - currently using system defaults
2. **Material 3 Typography Scale**: 15 predefined styles (Display, Headline, Title, Body, Label)
3. **Font Weight Hierarchy**: Black > ExtraBold > Bold > Medium > Normal
4. **Custom Font Sizes**: 9sp-11sp for small labels, 64sp for extra-large displays
5. **Letter Spacing Patterns**: -1sp (tight) to 1sp (wide) for different emphasis levels
6. **Tabular Numbers**: All styles use `tnum` feature for aligned numbers

### Best Practices
1. **Always use `MaterialTheme.colorScheme.*`** for colors to ensure theme consistency
2. **Use `MaterialTheme.typography.*`** for typography to maintain design system
3. **Apply alpha transparency** with `.copy(alpha = Xf)` for subtle variations
4. **Use color utilities** (`toContainerColor`, `toOnContainerColor`) for dynamic theming
5. **Maintain font weight hierarchy** for clear visual hierarchy
6. **Use tabular numbers** (`tnum`) for financial data alignment
7. **Minimize hardcoded colors** - migrate to theme tokens where possible
8. **Test across all themes** (Light, Dark, Amoled) for color contrast
