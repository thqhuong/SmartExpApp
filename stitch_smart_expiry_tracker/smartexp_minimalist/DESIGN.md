---
name: SmartExp Minimalist
colors:
  surface: '#f9f9f9'
  surface-dim: '#dadada'
  surface-bright: '#f9f9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f4'
  surface-container: '#eeeeee'
  surface-container-high: '#e8e8e8'
  surface-container-highest: '#e2e2e2'
  on-surface: '#1a1c1c'
  on-surface-variant: '#564334'
  inverse-surface: '#2f3131'
  inverse-on-surface: '#f0f1f1'
  outline: '#897362'
  outline-variant: '#ddc1ae'
  surface-tint: '#904d00'
  primary: '#904d00'
  on-primary: '#ffffff'
  primary-container: '#ff8c00'
  on-primary-container: '#623200'
  inverse-primary: '#ffb77d'
  secondary: '#5f5e5e'
  on-secondary: '#ffffff'
  secondary-container: '#e2dfde'
  on-secondary-container: '#636262'
  tertiary: '#5d5f5f'
  on-tertiary: '#ffffff'
  tertiary-container: '#a9aaaa'
  on-tertiary-container: '#3d3f3f'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdcc3'
  primary-fixed-dim: '#ffb77d'
  on-primary-fixed: '#2f1500'
  on-primary-fixed-variant: '#6e3900'
  secondary-fixed: '#e5e2e1'
  secondary-fixed-dim: '#c8c6c5'
  on-secondary-fixed: '#1c1b1b'
  on-secondary-fixed-variant: '#474746'
  tertiary-fixed: '#e2e2e2'
  tertiary-fixed-dim: '#c6c6c7'
  on-tertiary-fixed: '#1a1c1c'
  on-tertiary-fixed-variant: '#454747'
  background: '#f9f9f9'
  on-background: '#1a1c1c'
  surface-variant: '#e2e2e2'
typography:
  h1:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  h2:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  h3:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: '0'
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: '0'
  label-caps:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  cta:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  container-padding: 20px
  gutter: 16px
---

## Brand & Style
The brand personality is efficient, reliable, and alert. It targets users seeking to reduce waste and manage inventory with zero friction. The UI evokes a sense of organized calm, utilizing a **Minimalist** style that prioritizes content clarity over decorative elements. By leveraging high-contrast typography against a vast white canvas, the design system ensures that the primary utility—tracking expiration dates—remains the focal point. The aesthetic is professional and "utility-first," using the bold orange accent sparingly to signal importance without causing visual fatigue.

## Colors
The color strategy follows a strict 60-30-10 distribution to maintain a high-end, gallery-like feel.
- **Base (60%):** #FFFFFF is used for the primary background and all container surfaces to maximize the "breathability" of the interface.
- **Contrast (30%):** #1A1A1A is reserved for all text, iconography, and structural borders, ensuring AAA accessibility and a modern, bold punch.
- **Action (10%):** #FF8C00 (International Orange) is the surgical strike color. It is used exclusively for primary CTAs, critical expiry alerts, and active states. 
- **Subtle Surface:** #F5F5F5 is introduced as a subtle neutral for off-white backgrounds in secondary list items or input fields to provide soft differentiation from the main canvas.

## Typography
This design system utilizes **Inter** for its exceptional readability on mobile screens and its neutral, systematic character. Headlines use a tighter letter-spacing and heavier weights to create a strong hierarchy against the white space. Body text is optimized for legibility with generous line heights. Small labels use uppercase styling with increased tracking to differentiate them from interactive body elements.

## Layout & Spacing
The layout relies on a **Fluid Grid** with a 4-column structure for mobile. A 20px horizontal "safe zone" is maintained on all screens. The spacing rhythm is based on an 8px baseline grid to ensure mathematical harmony. Generous vertical whitespace (the "lg" and "xl" tokens) is encouraged between distinct sections to prevent the UI from feeling cluttered, emphasizing a premium, minimalist experience.

## Elevation & Depth
Depth is conveyed through **Ambient Shadows** and **Tonal Layers**. Instead of heavy shadows, this design system uses extremely soft, high-blur shadows with low opacity (e.g., 4-6% Black) to lift cards slightly off the white background.
- **Level 0:** Main background (#FFFFFF).
- **Level 1:** Cards and Modals. Use a subtle 0px 4px 20px rgba(0,0,0,0.05) shadow.
- **Level 2:** Floating Action Buttons or active Overlays. Use a slightly more pronounced shadow to indicate interactivity.
- **Interaction:** On press, elements should visually "sink" by reducing the shadow spread, mimicking a physical press.

## Shapes
The shape language is defined by a "Rounded" philosophy to soften the high-contrast color palette.
- **Standard components** (Buttons, Inputs): 0.5rem (8px) radius.
- **Large containers** (Cards, Bottom Sheets): 1rem (16px) radius.
- **Icons & Badges:** Use a full pill-shape (circular ends) for status indicators to contrast against the more structured rectangular cards.

## Components
- **Buttons:** Primary CTAs are solid #FF8C00 with #FFFFFF text. Secondary buttons use #1A1A1A text with a thin 1px border or a light grey fill.
- **Cards:** White background with a 1px #F5F5F5 border and the Level 1 shadow. Content within cards should have a minimum of 16px internal padding.
- **Input Fields:** Minimalist design with a 1px #1A1A1A border only on focus; otherwise, a light #F5F5F5 background with a soft 8px corner radius.
- **Chips:** Used for categories (e.g., "Dairy", "Pantry"). These use #F5F5F5 backgrounds with #1A1A1A text. If an item is "Near Expiry," the chip border or text turns #FF8C00.
- **Lists:** Clean rows separated by subtle 1px dividers (#F5F5F5). Use high-contrast #1A1A1A for item names and a muted grey for secondary metadata.
- **Progress Bars:** For expiry tracking, use a thin grey track with #FF8C00 filling the "danger" zone as the date approaches.
- **Icons:** 24px minimalist line icons with a 1.5px or 2px stroke weight to match the weight of the typography.