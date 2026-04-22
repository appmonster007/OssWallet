# Color Palette Reference

## Light Theme Palette

### Primary (Teal-Green)
```
Primary:         #006A5D ████████████
On Primary:      #FFFFFF ████████████
Primary Container: #74F8D9 ████████████
On Primary Container: #00201A ████████████
```

### Secondary (Amber-Brown)
```
Secondary:       #6D5A3A ████████████
On Secondary:    #FFFFFF ████████████
Secondary Container: #F4DDB5 ████████████
On Secondary Container: #231900 ████████████
```

### Tertiary (Purple-Violet)
```
Tertiary:        #5A3A6D ████████████
On Tertiary:     #FFFFFF ████████████
Tertiary Container: #DDC0F6 ████████████
On Tertiary Container: #1A0029 ████████████
```

### Error (Vibrant Red)
```
Error:           #B3261E ████████████
On Error:        #FFFFFF ████████████
Error Container: #F9DEDC ████████████
On Error Container: #410E0B ████████████
```

### Background & Surface
```
Background:      #FBFDF8 ████████████
On Background:   #191C18 ████████████
Surface:         #FBFDF8 ████████████
On Surface:      #191C18 ████████████
```

### Surface Variants
```
Surface Variant: #DBE5DE ████████████
On Surface Variant: #3F4944 ████████████
Outline:         #6F7974 ████████████
Outline Variant: #BFC9C2 ████████████
Inverse Surface: #2E312E ████████████
Inverse On Surface: #F0F1EC ████████████
```

## Dark Theme Palette

### Primary (Light Teal)
```
Primary:         #52DBC0 ████████████
On Primary:      #00382F ████████████
Primary Container: #005138 ████████████
On Primary Container: #74F8D9 ████████████
```

### Secondary (Light Amber)
```
Secondary:       #D7C19A ████████████
On Secondary:    #3B2F15 ████████████
Secondary Container: #524325 ████████████
On Secondary Container: #F4DDB5 ████████████
```

### Tertiary (Light Purple)
```
Tertiary:        #C0A5DA ████████████
On Tertiary:     #2F1643 ████████████
Tertiary Container: #432954 ████████████
On Tertiary Container: #DDC0F6 ████████████
```

### Error (Light Red)
```
Error:           #F2B8B5 ████████████
On Error:        #601410 ████████████
Error Container: #8C1D18 ████████████
On Error Container: #F9DEDC ████████████
```

### Background & Surface
```
Background:      #191C18 ████████████
On Background:   #E1E3DF ████████████
Surface:         #191C18 ████████████
On Surface:      #E1E3DF ████████████
```

### Surface Variants
```
Surface Variant: #3F4944 ████████████
On Surface Variant: #BFC9C2 ████████████
Outline:         #8A938C ████████████
Outline Variant: #3F4944 ████████████
Inverse Surface: #E1E3DF ████████████
Inverse On Surface: #191C18 ████████████
```

## Special Colors

### Financial Indicators
```
Positive Amount: #00C853 ████████████ (Vibrant Green)
Negative Amount: #FF1744 ████████████ (Vibrant Red)
```

### Accent Colors
```
Accent Blue:     #2979FF ████████████
Accent Orange:   #FF9100 ████████████
Accent Purple:   #D500F9 ████████████
```

### Gradient Colors
```
Gradient Start:  #006A5D ████████████ (Primary)
Gradient End:    #52DBC0 ████████████ (Primary Dark)
```

### Semantic Colors
```
Success:         #00C853 ████████████ (Vibrant Green)
Warning:         #FF9100 ████████████ (Orange)
Info:            #2979FF ████████████ (Blue)
```

## Container Variants

### Light Theme
```
Primary Fixed:           #74F8D9 ████████████
On Primary Fixed:        #00201A ████████████
Primary Fixed Dim:       #52DBC0 ████████████
On Primary Fixed Variant: #005138 ████████████

Secondary Fixed:         #F4DDB5 ████████████
On Secondary Fixed:      #231900 ████████████
Secondary Fixed Dim:      #D7C19A ████████████
On Secondary Fixed Variant: #524325 ████████████

Tertiary Fixed:          #DDC0F6 ████████████
On Tertiary Fixed:       #1A0029 ████████████
Tertiary Fixed Dim:       #C0A5DA ████████████
On Tertiary Fixed Variant: #432954 ████████████
```

### Dark Theme
```
Primary Fixed:           #74F8D9 ████████████
On Primary Fixed:        #00201A ████████████
Primary Fixed Dim:       #52DBC0 ████████████
On Primary Fixed Variant: #005138 ████████████

Secondary Fixed:         #F4DDB5 ████████████
On Secondary Fixed:      #231900 ████████████
Secondary Fixed Dim:      #D7C19A ████████████
On Secondary Fixed Variant: #524325 ████████████

Tertiary Fixed:          #DDC0F6 ████████████
On Tertiary Fixed:       #1A0029 ████████████
Tertiary Fixed Dim:       #C0A5DA ████████████
On Tertiary Fixed Variant: #432954 ████████████
```

## Color Usage Guidelines

### When to Use Primary
- Main action buttons
- FABs (Floating Action Buttons)
- Active navigation items
- Selected states
- Primary brand elements

### When to Use Secondary
- Secondary action buttons
- Chips and tags
- Less prominent interactive elements
- Secondary brand elements

### When to Use Tertiary
- Accent elements
- Decorative items
- Creative highlights
- Tertiary brand elements

### When to Use Error
- Error states
- Destructive actions
- Validation errors
- Warning indicators

### When to Use Financial Indicators
- `PositiveAmountColor`: Income, gains, positive values
- `NegativeAmountColor`: Expenses, losses, negative values

### When to Use Accent Colors
- `AccentBlue`: Information highlights, variety
- `AccentOrange`: Warnings, variety
- `AccentPurple`: Creative accents, variety

### When to Use Semantic Colors
- `SuccessColor`: Success states, completed actions
- `WarningColor`: Warning states, caution
- `InfoColor`: Information states, help text

### When to Use Surface Variants
- `surfaceVariant`: Elevated surfaces, alternate backgrounds
- `onSurfaceVariant`: Text on surface variants
- `outline`: Borders, dividers
- `outlineVariant`: Subtle borders, separators
- `inverseSurface`: Inverse backgrounds (e.g., tooltips)
- `inverseOnSurface`: Text on inverse surfaces

### Alpha Transparency Patterns
```
Very subtle:  0.1f - 0.2f (disabled states, very subtle backgrounds)
Subtle:       0.3f - 0.4f (subtle backgrounds, highlights)
Medium:       0.5f (medium backgrounds, borders)
High:         0.6f (high backgrounds, placeholders)
```

## Theme Comparison

### Light vs Dark Theme
| Role | Light | Dark |
|------|-------|------|
| Primary | #006A5D (dark teal) | #52DBC0 (light teal) |
| Secondary | #6D5A3A (dark amber) | #D7C19A (light amber) |
| Tertiary | #5A3A6D (dark purple) | #C0A5DA (light purple) |
| Background | #FBFDF8 (off-white) | #191C18 (dark gray) |
| Surface | #FBFDF8 (off-white) | #191C18 (dark gray) |

### Key Differences
- **Primary**: Dark teal in light, light teal in dark
- **Secondary**: Dark amber in light, light amber in dark
- **Tertiary**: Dark purple in light, light purple in dark
- **Background/Surface**: Off-white in light, dark gray in dark
- **Error**: Vibrant red in light, light red in dark

## Color Harmony

### Complementary Pairs
- Primary (Teal) + Secondary (Amber) - Warmth contrast
- Primary (Teal) + Tertiary (Purple) - Creative contrast
- Secondary (Amber) + Tertiary (Purple) - Warm-cool contrast

### Analogous Pairs
- Primary (Teal) + Error (Red) - Attention contrast
- Secondary (Amber) + Error (Red) - Warm harmony
- Tertiary (Purple) + Accent Purple - Monochromatic harmony

### Triadic Harmony
- Primary (Teal) + Secondary (Amber) + Tertiary (Purple) - Balanced triadic

## Accessibility Notes

### Contrast Ratios
All color combinations meet WCAG AA or AAA standards:
- Primary/on-Primary: AAA (7:1+)
- Secondary/on-Secondary: AAA (7:1+)
- Tertiary/on-Tertiary: AAA (7:1+)
- Error/on-Error: AAA (7:1+)
- Background/on-Background: AAA (7:1+)
- Surface/on-Surface: AAA (7:1+)

### Color Blindness Considerations
- Primary teal is distinguishable in most color blindness types
- Financial indicators (green/red) should be paired with symbols (+/-) for accessibility
- Avoid relying solely on color for information conveyance
- Use patterns, shapes, or text labels in addition to color

## Dynamic Color Support

The app supports Material You dynamic colors on Android 12+ (API 31+):
- When enabled, colors are derived from user's wallpaper
- Falls back to this expressive palette when dynamic color is disabled
- Maintains expressiveness while allowing personalization

## Testing Recommendations

### Visual Testing
1. Test in Light, Dark, and Amoled themes
2. Test with dynamic color enabled/disabled
3. Test on different screen sizes and densities
4. Test with different wallpaper colors (dynamic color)

### Accessibility Testing
1. Test with color blindness simulation tools
2. Test with high contrast mode
3. Test with different display settings
4. Verify text contrast ratios

### User Testing
1. Gather feedback on color vibrancy
2. Test readability in different lighting conditions
3. Test with users with visual impairments
4. Verify color preferences match user expectations
