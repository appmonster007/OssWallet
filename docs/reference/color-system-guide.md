# Color System Guide

## Overview

OssWallet implements the Material 3 Expressive Color Scheme, designed to move beyond simple brand matching with more vibrant, saturated colors while maintaining accessibility and professional appearance suitable for a finance application.

## Quick Reference for Developers

### Theme Colors (Always use these first)
```kotlin
// Primary - Main actions, FABs, active states
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.onPrimary
MaterialTheme.colorScheme.primaryContainer
MaterialTheme.colorScheme.onPrimaryContainer

// Secondary - Secondary actions, chips, badges
MaterialTheme.colorScheme.secondary
MaterialTheme.colorScheme.onSecondary
MaterialTheme.colorScheme.secondaryContainer
MaterialTheme.colorScheme.onSecondaryContainer

// Tertiary - Accents, decorative elements
MaterialTheme.colorScheme.tertiary
MaterialTheme.colorScheme.onTertiary
MaterialTheme.colorScheme.tertiaryContainer
MaterialTheme.colorScheme.onTertiaryContainer

// Error - Error states, destructive actions
MaterialTheme.colorScheme.error
MaterialTheme.colorScheme.onError
MaterialTheme.colorScheme.errorContainer
MaterialTheme.colorScheme.onErrorContainer

// Background & Surface
MaterialTheme.colorScheme.background
MaterialTheme.colorScheme.onBackground
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.onSurface

// Surface Variants
MaterialTheme.colorScheme.surfaceVariant
MaterialTheme.colorScheme.onSurfaceVariant
MaterialTheme.colorScheme.outline
MaterialTheme.colorScheme.outlineVariant
MaterialTheme.colorScheme.inverseSurface
MaterialTheme.colorScheme.inverseOnSurface
```

### Special Colors (Use for specific purposes)
```kotlin
import com.osscabinet.ossWallet.ui.theme.*

// Financial Indicators
PositiveAmountColor  // Income, gains, positive values
NegativeAmountColor  // Expenses, losses, negative values

// Accent Colors (for variety)
AccentBlue
AccentOrange
AccentPurple

// Semantic Colors (for states)
SuccessColor  // Success states
WarningColor  // Warning states
InfoColor     // Information states

// Gradient Colors
GradientStart  // Primary color
GradientEnd    // Primary dark color
```

## Color Palette

### Light Theme

#### Primary (Teal-Green)
```
Primary:         #006A5D
On Primary:      #FFFFFF
Primary Container: #74F8D9
On Primary Container: #00201A
```

#### Secondary (Amber-Brown)
```
Secondary:       #6D5A3A
On Secondary:    #FFFFFF
Secondary Container: #F4DDB5
On Secondary Container: #231900
```

#### Tertiary (Purple-Violet)
```
Tertiary:        #5A3A6D
On Tertiary:     #FFFFFF
Tertiary Container: #DDC0F6
On Tertiary Container: #1A0029
```

#### Error (Vibrant Red)
```
Error:           #B3261E
On Error:        #FFFFFF
Error Container:  #F9DEDC
On Error Container: #410E0B
```

#### Background & Surface
```
Background:      #FBFDF8
On Background:   #191C18
Surface:         #FBFDF8
On Surface:      #191C18
```

#### Surface Variants
```
Surface Variant: #DBE5DE
On Surface Variant: #3F4944
Outline:         #6F7974
Outline Variant: #BFC9C2
Inverse Surface: #2E312E
Inverse On Surface: #F0F1EC
```

### Dark Theme

#### Primary (Light Teal)
```
Primary:         #52DBC0
On Primary:      #00382F
Primary Container: #005138
On Primary Container: #74F8D9
```

#### Secondary (Light Amber)
```
Secondary:       #D7C19A
On Secondary:    #3B2F15
Secondary Container: #524325
On Secondary Container: #F4DDB5
```

#### Tertiary (Light Purple)
```
Tertiary:        #C0A5DA
On Tertiary:     #2F1643
Tertiary Container: #432954
On Tertiary Container: #DDC0F6
```

#### Error (Light Red)
```
Error:           #F2B8B5
On Error:        #601410
Error Container:  #8C1D18
On Error Container: #F9DEDC
```

#### Background & Surface
```
Background:      #191C18
On Background:   #E1E3DF
Surface:         #191C18
On Surface:      #E1E3DF
```

#### Surface Variants
```
Surface Variant: #3F4944
On Surface Variant: #BFC9C2
Outline:         #8A938C
Outline Variant: #3F4944
Inverse Surface: #E1E3DF
Inverse On Surface: #191C18
```

### Special Colors
```
Positive Amount: #00C853 (Vibrant Green)
Negative Amount: #FF1744 (Vibrant Red)
Accent Blue:     #2979FF
Accent Orange:   #FF9100
Accent Purple:   #D500F9
Gradient Start:  #006A5D
Gradient End:    #52DBC0
Success:         #00C853
Warning:         #FF9100
Info:            #2979FF
```

## Material 3 Expressive Color Guidelines

### Core Philosophies

- **Vibrancy**: More saturated tones for visual interest
- **Achromatic Balance**: High-energy colors balanced by grounded neutrals
- **Accessibility by Default**: HCT color space ensures contrast ratios meet WCAG standards

### The 60-30-10 Rule

- **60% Neutral/Surface**: Keep the interface breathable
- **30% Primary/Secondary**: Use for main interactive elements
- **10% Tertiary/Accents**: Use for moments of delight or emphasis

### Color Usage by Component

#### FAB (Floating Action Button)
- Uses: Primary Container
- On-color: On-Primary Container

#### Navigation Bar
- Background: Surface Variant
- Indicator: Primary
- Unselected: On-Surface Variant

#### Cards
- Background: Surface Variant or Outline
- Content: On-Surface Variant

## Usage Patterns

### Buttons
```kotlin
// Primary action
Button(
    onClick = { /* */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
) { /* */ }

// Destructive action
Button(
    onClick = { /* */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    )
) { /* */ }
```

### Cards
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    border = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
) { /* */ }
```

### Financial Indicators
```kotlin
// Income (positive)
Text(
    text = "+$1,000",
    color = PositiveAmountColor,
    style = MaterialTheme.typography.bodyLarge
)

// Expense (negative)
Text(
    text = "-$500",
    color = NegativeAmountColor,
    style = MaterialTheme.typography.bodyLarge
)
```

### Alpha Transparency Patterns
```kotlin
// Very subtle (disabled states)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)

// Subtle (subtle backgrounds, highlights)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

// Medium (medium backgrounds, borders)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

// High (placeholders)
MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
```

## Common Mistakes to Avoid

### ❌ Don't use hardcoded colors
```kotlin
// BAD
Color(0xFF004D56)

// GOOD
MaterialTheme.colorScheme.primary
```

### ❌ Don't forget alpha transparency
```kotlin
// BAD - too opaque
Surface(color = MaterialTheme.colorScheme.primaryContainer) { /* */ }

// GOOD - proper transparency
Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) { /* */ }
```

### ❌ Don't use wrong color for purpose
```kotlin
// BAD - using primary for error
Button(colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary
)) { Text("Delete") }

// GOOD - using error for destructive action
Button(colors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.error
)) { Text("Delete") }
```

## Accessibility

### Contrast Ratios
All color combinations meet WCAG AA or AAA standards:
- Primary/on-Primary: AAA (7:1+)
- Secondary/on-Secondary: AAA (7:1+)
- Tertiary/on-Tertiary: AAA (7:1+)
- Error/on-Error: AAA (7:1+)
- Background/on-Background: AAA (7:1+)

### Color Blindness Considerations
- Primary teal is distinguishable in most color blindness types
- Financial indicators (green/red) should be paired with symbols (+/-) for accessibility
- Avoid relying solely on color for information conveyance

## Dynamic Color Support

The app supports Material You dynamic colors on Android 12+ (API 31+):
- When enabled, colors are derived from user's wallpaper
- Falls back to this expressive palette when dynamic color is disabled
- Theme selection: "System", "Light", "Dark", "Amoled"

## Testing

### Visual Testing
1. Test in Light, Dark, and Amoled themes
2. Test with dynamic color enabled/disabled
3. Test on different screen sizes and densities

### Accessibility Testing
1. Test with color blindness simulation tools
2. Test with high contrast mode
3. Verify text contrast ratios

## Migration Checklist

When updating existing components:
- [ ] Replace hardcoded colors with `MaterialTheme.colorScheme.*`
- [ ] Use `PositiveAmountColor` and `NegativeAmountColor` for financial indicators
- [ ] Add appropriate alpha transparency for backgrounds
- [ ] Test in Light, Dark, and Amoled themes
- [ ] Verify contrast ratios meet accessibility standards

## Related Documentation

- **[Color Revision History](material3-expressive-color-revision.md)** - Detailed documentation of the April 2026 color system revision
- **[Typography System](color-and-typography-documentation.md#typography-system)** - Typography definitions and usage patterns

## Resources

- [Material 3 Color System](https://m3.material.io/styles/color/the-color-system)
- [Material 3 Color Roles](https://m3.material.io/styles/color/color-roles)
- [WCAG Contrast Guidelines](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)
- [Color Blindness Simulator](https://www.toptal.com/designers/colorfilter)
