# Material 3 Expressive Color System Implementation

## Overview

OssWallet implements the Material 3 Expressive Color Scheme, which is designed to move beyond simple brand matching. It utilizes a more courageous palette that allows for higher contrast, more vibrant secondary colors, and a dynamic range that adapts to user preferences while maintaining accessibility.

## Core Philosophies

The transition from Material 2 to Material 3 introduced Dynamic Color, which uses a seed color to generate a full tonal palette. The Expressive scheme specifically focuses on:

- **Vibrancy**: Utilizing more saturated tones than the "Tonal Spot" or "Neutral" schemes
- **Achromatic Balance**: Ensuring high-energy colors are balanced by grounded neutrals
- **Accessibility by Default**: Leveraging the HCT (Hue, Chroma, Tone) color space to ensure contrast ratios meet WCAG standards regardless of the hue

## Tonal Palettes and the HCT System

Unlike traditional RGB or Hex systems, Material 3 uses HCT to define colors. This ensures that a "60" tone in Blue has the same perceived lightness as a "60" tone in Red.

| Role | Description | Tone Range |
|------|-------------|------------|
| Primary | The most prominent color, used for key components like FABs and active states | Tone 80 (Light), Tone 80 (Dark) |
| Secondary | Used for less prominent components, often providing a complementary contrast | Tone 70 (Light), Tone 70 (Dark) |
| Tertiary | An accent color for "expressive" elements, often used to create a distinct visual hierarchy | Tone 60 (Light), Tone 60 (Dark) |
| Error | Reserved for warnings and error states, typically a high-chroma red | Tone 30 (Light), Tone 80 (Dark) |
| Neutral | Used for surfaces and backgrounds | Tone 98/10 (Light), Tone 10/90 (Dark) |

## Expressive Guidelines

### The 60-30-10 Rule

To maintain visual balance when using the expressive scheme:

- **60% Neutral/Surface**: Keep the interface breathable
- **30% Primary/Secondary**: Use for main interactive elements
- **10% Tertiary/Accents**: Use the most "expressive" colors for moments of delight or emphasis

### Dynamic Color Mapping

When implementing expressive colors, the system generates 13 tones for each color group (0 to 100).

- **Containers**: Use lower-tonal-value containers (e.g., Tone 90) for backgrounds of elements
- **On-Colors**: Always pair a "Container" color with its corresponding "On" color (e.g., On-Primary Container) to guarantee legibility

### Meaningful Color Usage

Expression should not sacrifice usability. Avoid using expressive tertiary colors for critical "Success" or "Error" states if they conflict with established mental models.

**Example**: Using a bright purple for a "Delete" button would confuse users who expect red for destructive actions.

## Implementation in OssWallet

### Dynamic Color Priority

1. **Android 12+ with Dynamic Color**: Uses Material You dynamic color schemes derived from user's wallpaper
2. **Amoled Theme**: Pure black background (Tone 0) with expressive colors
3. **Dark/Light Themes**: Custom expressive color schemes with proper HCT tones

### Color Usage by Component

#### FAB (Floating Action Button)
- Uses: Primary Container (Tone 90 in Light, Tone 20 in Dark)
- On-color: On-Primary Container

#### Navigation Bar
- Background: Surface Variant (Tone 92 in Light, Tone 30 in Dark)
- Indicator: Primary (Tone 80 in Light, Tone 80 in Dark)
- Unselected: On-Surface Variant (Tone 30 in Light, Tone 80 in Dark)

#### Cards
- Background: Surface Variant or Outline
- Content: On-Surface Variant

#### Dark Theme Considerations
- Expressive vibrancy scales down in dark mode
- Use Tones 80-90 for primary elements in dark mode to avoid "neon" eye strain
- Background uses Tone 10 (deep neutral) instead of pure black for better visual hierarchy

### Financial Indicators

Following meaningful color usage guidelines:

- **Positive Amount**: High-chroma green (Tone 60) - NOT using tertiary color
- **Negative Amount**: Error color (Tone 30) for consistency with destructive actions
- **Success**: Green (standard mental model)
- **Warning**: Orange (standard mental model)
- **Info**: Blue (standard mental model)

### Accent Colors (10% Usage)

Used for expressive moments of delight:

- Accent Blue: Tone 60
- Accent Orange: Tone 60
- Accent Purple: Tone 60

These should be used sparingly for:
- Highlighting special features
- Creating visual interest in otherwise neutral areas
- Providing variety without overwhelming the interface

## Accessibility Check

All color combinations in OssWallet maintain:

- **Primary action buttons**: Contrast ratio of at least 3.0:1 against the background for graphical elements
- **Text**: Contrast ratio of 4.5:1 for text (WCAG AA standard)
- **Container/On-Container pairs**: Automatically guaranteed legibility through HCT color space

## Key Components Checklist

- [x] **FAB**: Uses Primary Container
- [x] **Navigation Bar**: Uses Surface with Primary indicators
- [x] **Cards**: Uses Surface Variant
- [x] **Dark Theme**: Expressive vibrancy scales down (Tones 80-90 for primary elements)
- [x] **Financial Indicators**: Uses high-contrast colors, not tertiary
- [x] **Semantic Colors**: Follows standard mental models (green for success, red for error)
- [x] **Dynamic Color**: Enabled by default on Android 12+ with Material You

## Future Enhancements

Potential improvements to further align with Material 3 Expressive guidelines:

1. **Surface Tints**: Add more primary color tinting to surface variants for cohesive mood
2. **Container Variants**: Implement more use of Primary/Secondary/Tertiary Fixed colors
3. **Tonal Range**: Expand tonal palette usage for more subtle variations
4. **Animation**: Add color-based animations that transition through tonal ranges
5. **Customization**: Allow users to select their own seed color for dynamic color generation

## Resources

- [Material 3 Color System](https://m3.material.io/styles/color/the-color-system)
- [Material 3 Color Roles](https://m3.material.io/styles/color/color-roles)
- [HCT Color Space](https://material.io/blog/science-of-color-design-part-2)
- [WCAG Contrast Guidelines](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html)
