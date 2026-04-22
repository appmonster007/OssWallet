# Developer Color Guide - Material 3 Expressive Colors

## Quick Reference

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

// Surface Variants (NEW)
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

## Usage Patterns

### 1. Buttons
```kotlin
// Primary action
Button(
    onClick = { /* */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
) { /* */ }

// Secondary action
Button(
    onClick = { /* */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
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

### 2. Cards
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

// Elevated card
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
) { /* */ }
```

### 3. Financial Indicators
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

// Badge
Surface(
    color = PositiveAmountColor.copy(alpha = 0.1f),
    shape = RoundedCornerShape(8.dp)
) {
    Text(
        text = "INCOME",
        color = PositiveAmountColor,
        style = MaterialTheme.typography.labelSmall
    )
}
```

### 4. State Colors
```kotlin
// Success
Icon(
    imageVector = Icons.Default.Check,
    tint = SuccessColor,
    contentDescription = "Success"
)

// Warning
Icon(
    imageVector = Icons.Default.Warning,
    tint = WarningColor,
    contentDescription = "Warning"
)

// Info
Icon(
    imageVector = Icons.Default.Info,
    tint = InfoColor,
    contentDescription = "Info"
)
```

### 5. Surface Variants
```kotlin
// Subtle background
Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    shape = RoundedCornerShape(16.dp)
) { /* */ }

// Border
BorderStroke(
    1.dp,
    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
)

// Placeholder text
TextField(
    value = text,
    onValueChange = { /* */ },
    placeholder = {
        Text(
            text = "Enter value",
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    }
)
```

### 6. Accent Colors
```kotlin
// Use accent colors for variety in UI elements
Icon(
    imageVector = Icons.Default.Star,
    tint = AccentBlue,
    contentDescription = "Featured"
)

// Use in chips
AssistChip(
    onClick = { /* */ },
    colors = AssistChipDefaults.assistChipColors(
        containerColor = AccentOrange.copy(alpha = 0.1f),
        labelColor = AccentOrange
    ),
    label = { Text("Featured") }
)
```

## Alpha Transparency Patterns

### Backgrounds
```kotlin
// Very subtle (disabled states, very subtle backgrounds)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

// Subtle (subtle backgrounds, highlights)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

// Medium (medium backgrounds, borders)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

// High (high backgrounds, placeholders)
MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
```

### Container Colors
```kotlin
// Subtle highlights
MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)

// Medium highlights
MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
```

### Outline Colors
```kotlin
// Disabled states
MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
MaterialTheme.colorScheme.error.copy(alpha = 0.1f)

// Enabled states
MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
MaterialTheme.colorScheme.error.copy(alpha = 0.5f)

// Borders
MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

// Placeholders
MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
```

## Common Mistakes to Avoid

### ❌ Don't use hardcoded colors
```kotlin
// BAD
Color(0xFF004D56)
Color(0xFFF44336)

// GOOD
MaterialTheme.colorScheme.primary
NegativeAmountColor
```

### ❌ Don't forget alpha transparency
```kotlin
// BAD - too opaque
Surface(
    color = MaterialTheme.colorScheme.primaryContainer
) { /* */ }

// GOOD - proper transparency
Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
) { /* */ }
```

### ❌ Don't use wrong color for purpose
```kotlin
// BAD - using primary for error
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary  // Wrong!
    )
) { Text("Delete") }

// GOOD - using error for destructive action
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error
    )
) { Text("Delete") }
```

### ❌ Don't ignore contrast
```kotlin
// BAD - poor contrast
Text(
    text = "Important",
    color = MaterialTheme.colorScheme.surfaceVariant,
    style = MaterialTheme.colorScheme.onSurfaceVariant
)

// GOOD - proper contrast
Text(
    text = "Important",
    color = MaterialTheme.colorScheme.primary,
    style = MaterialTheme.colorScheme.onPrimary
)
```

## Migration Checklist

When updating existing components:

- [ ] Replace hardcoded colors with `MaterialTheme.colorScheme.*`
- [ ] Use `PositiveAmountColor` and `NegativeAmountColor` for financial indicators
- [ ] Add appropriate alpha transparency for backgrounds
- [ ] Test in Light, Dark, and Amoled themes
- [ ] Verify contrast ratios meet accessibility standards
- [ ] Use semantic colors for state representation
- [ ] Consider accent colors for variety

## Testing

### Visual Testing
1. Test in all three themes (Light, Dark, Amoled)
2. Test with dynamic color enabled/disabled
3. Test on different screen sizes
4. Test in different lighting conditions

### Accessibility Testing
1. Use color blindness simulation tools
2. Test with high contrast mode
3. Verify text contrast ratios (should be 4.5:1 or higher)
4. Ensure color is not the only indicator of information

## Resources

- **Material 3 Color System**: https://m3.material.io/styles/color/the-color-system
- **Material 3 Color Roles**: https://m3.material.io/styles/color/color-roles
- **WCAG Contrast Guidelines**: https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html
- **Color Blindness Simulator**: https://www.toptal.com/designers/colorfilter

## Support

For questions or issues with the color system:
1. Check this guide first
2. Refer to COLOR_PALETTE_REFERENCE.md for visual reference
3. Refer to MATERIAL3_EXPRESSIVE_COLOR_REVISION.md for design rationale
4. Consult the team design system documentation
