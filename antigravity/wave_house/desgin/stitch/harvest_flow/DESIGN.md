# Design System Strategy: The Organic Grid

This design system is a bespoke framework crafted for the high-velocity environment of fresh produce logistics. Moving beyond the "standard" warehouse dashboard, it utilizes a philosophy of **Organic Editorialism**. We treat inventory management not as a series of data points, but as a living ecosystem. By leveraging tonal depth, sophisticated typography scales, and a strict "No-Line" rule, we create an interface that is both highly functional for fast-paced operations and aesthetically premium.

---

## 1. Creative North Star: "The Digital Orchard"
The system is built on the concept of **The Digital Orchard**. In a warehouse, clarity is paramount, but "clean" shouldn't mean "cold." 

*   **Intentional Asymmetry:** Avoid rigid, perfectly centered grids. Use off-balance layouts to guide the eye toward primary actions (like "Add Stock").
*   **Tonal Depth:** We replace 1px borders with "atmospheric layering." The UI should feel like overlapping sheets of vellum or frosted glass.
*   **Editorial Utility:** Using high-contrast typography (Work Sans) for data ensures that even in a dimly lit warehouse or on a moving tablet, information is undeniable.

---

## 2. Color & Surface Architecture

Our palette is rooted in the vitality of agriculture. We use greens for growth (primary) and oranges for harvest/energy (tertiary).

### The "No-Line" Rule
**Strict Mandate:** Designers are prohibited from using 1px solid borders to section content. Boundaries must be defined through:
1.  **Background Shifts:** Placing a `surface-container-highest` element on a `surface` background.
2.  **Tonal Transitions:** Using the `surface-container` tiers to create hierarchy.
3.  **Negative Space:** Using the spacing scale to create "invisible" gutters.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack.
*   **Base Layer:** `surface` (#f4fbf1) - The canvas.
*   **Section Layer:** `surface-container-low` (#eff6ec) - Large grouped content areas.
*   **Action Layer:** `surface-container-highest` (#dde5db) - Interactive cards and modal surfaces.
*   **The "Glass" Rule:** For floating menus or status overlays, use `surface-container-lowest` (#ffffff) at 80% opacity with a 20px backdrop-blur.

### Signature Textures
Avoid flat primary blocks. For main CTAs and "Add Stock" buttons, apply a subtle linear gradient from `primary` (#006d37) to `primary_container` (#27ae60) at a 135-degree angle to provide a "ripe" visual depth.

---

## 3. Typography: Authority Meets Utility

We pair **Work Sans** (Display/Headlines) for an authoritative, editorial feel with **Inter** (Body/Labels) for maximum legibility in high-stress environments.

*   **Display & Headline (Work Sans):** Used for warehouse totals, revenue metrics, and section titles. The wide character set ensures numbers are easy to scan.
*   **Body & Title (Inter):** Used for inventory names, SKU details, and system feedback.
*   **The "Data Weight" Principle:** Use `title-lg` for numeric values in cards to ensure they are the most prominent element, even over the item name.

---

## 4. Elevation & Depth: Tonal Layering

We reject the standard Material Design shadow. Depth is achieved through "Tonal Layering."

*   **The Layering Principle:** Place a `surface-container-lowest` card on a `surface-container-low` section to create a soft, natural lift.
*   **Ambient Shadows:** If an element must float (e.g., a "Sell" action sheet), use a shadow color of `#171d17` at 6% opacity with a 32px blur and 8px Y-offset. This mimics natural light filtered through a warehouse skylight.
*   **Ghost Borders:** If accessibility requires a stroke (e.g., in high-glare environments), use `outline-variant` (#bccabc) at 20% opacity. Never use 100% opacity borders.

---

## 5. Signature Components

### Inventory Cards
*   **Structure:** No borders. Use `surface-container-highest` for the background.
*   **Visuals:** Use `xl` (1.5rem) rounded corners.
*   **Layout:** The inventory count (e.g., "450 kg") should be in `headline-sm` (Work Sans) in the top right, while the product name sits in `title-md` (Inter) at the bottom left. This diagonal tension creates a premium, custom feel.

### Action Buttons (Add Stock / Sell)
*   **Primary (Add Stock):** Use the signature `primary` gradient. `full` (9999px) roundedness.
*   **Secondary (Sell):** Use `tertiary_container` (#d58700) with `on_tertiary_container` text. This warm orange highlight signals a commercial transaction.
*   **States:** On hover/press, do not change color; instead, increase the `surface-tint` overlay by 8% to "deepen" the color.

### Inventory List Items
*   **Rule:** Forbid the use of divider lines. 
*   **Implementation:** Use a 12px vertical gap between items. Apply a `surface-container-low` background to every second item (zebra-striping) to maintain row-tracking without visual clutter.

### Revenue Charts
*   **Visual Style:** Use `primary` (#006d37) for growth trends and `tertiary` (#865300) for highlights. 
*   **Area Fills:** Use semi-transparent gradients (from 40% opacity to 0%) for area charts to maintain the "Glassmorphism" aesthetic.

---

## 6. Do's and Don'ts

### Do
*   **Do** use `tertiary_fixed` (#ffddb9) for warning labels or items close to expiry; it provides high visibility without the "alarmism" of red.
*   **Do** use asymmetrical margins (e.g., 24px left, 40px right) on dashboard headers to create an editorial, high-end feel.
*   **Do** embrace white space. If a screen feels crowded, increase the spacing rather than adding a border.

### Don't
*   **Don't** use 100% black text. Use `on_surface` (#171d17) to maintain a soft, organic contrast.
*   **Don't** use "Drop Shadows" on cards. Use background color shifts.
*   **Don't** use standard system icons. Use custom, thin-stroke (1.5px) icons that match the `outline` token.

---

## 7. Global Spacing & Radius
*   **Radius:** Cards use `xl` (1.5rem). Inputs use `md` (0.75rem). Buttons use `full`.
*   **Spacing:** Use a strict 8px base grid. However, for section separation, skip intervals (e.g., jump from 16px to 40px) to create dramatic, intentional breathing room.