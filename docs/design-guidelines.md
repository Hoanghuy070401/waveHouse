# Design Guidelines: The Organic Ledger

## Documentation Maintenance
**Last Updated:** 2026-04-20  
**Document Version:** 1.1  
**Maintained By:** Development Team

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Living Ledger."** 

In produce logistics, we sit at the intersection of raw nature and industrial precision. We are moving away from the rigid, "boxed-in" feel of traditional ERP software toward an editorial experience that feels as fresh as the produce it tracks. This system leverages **Organic Minimalism**—prioritizing breathability, tonal depth, and a sophisticated typographic scale to transform logistical data into a premium narrative. 

By utilizing intentional asymmetry and a "borderless" philosophy, we create a tool that feels less like a spreadsheet and more like a high-end botanical journal—efficient enough for a warehouse, refined enough for the boardroom.

---

## 2. Colors & Surface Philosophy
Color is not just decorative; it is our primary structural tool. Since we are moving beyond traditional borders, color shifts define our architecture.

### The "No-Line" Rule
**Explicit Instruction:** Prohibit 1px solid borders for sectioning. Boundaries must be defined solely through background color shifts or subtle tonal transitions. 
- Use `surface_container_low` for large section backgrounds sitting on a `surface` base. 
- Use `surface_container_highest` only for the most critical interactive elements or distinct floating panels.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. 
- **Base Layer:** `surface` (#f7fbf0) – The "paper" on which everything sits.
- **Mid Layer:** `surface_container` (#ebefe5) – Used for grouping related content blocks.
- **Top Layer:** `surface_container_lowest` (#ffffff) – Used for interactive cards to provide a "pop" of clean white against the organic greens and drabs.

### The "Glass & Gradient" Rule
To elevate the "Harvest" aesthetic, main CTAs and Hero sections should utilize a subtle linear gradient: 
- **Primary Gradient:** From `primary` (#0d631b) to `primary_container` (#2e7d32) at a 135-degree angle. This adds "soul" and depth to buttons, preventing them from feeling flat and "web-standard."

---

## 3. Typography: Editorial Authority
Our type system pairs the geometric precision of **Manrope** with the approachable clarity of **Work Sans**.

- **Display & Headlines (Manrope):** These are our "Editorial" moments. Use `display-lg` and `headline-md` with generous tracking adjustments to anchor pages. The high-contrast size difference between a headline and the sub-text creates an authoritative, modern hierarchy.
- **Body & Labels (Work Sans):** Designed for maximum legibility in high-velocity logistics environments. `body-md` is the workhorse, while `label-md` provides the "metadata" layer for SKUs, weights, and timestamps.
- **The Visual Rhythm:** Always pair a `headline-sm` with a `body-md` using a 4px or 8px vertical gap to create tight, cohesive "information clusters."

---

## 4. Elevation & Depth
In this system, depth is a whisper, not a shout. We replace structural lines with **Tonal Layering**.

### The Layering Principle
Achieve depth by stacking surface tiers. A `surface_container_lowest` card placed on a `surface_container_low` section creates a natural lift. This mimics the way light hits stacked sheets of fine vellum.

### Ambient Shadows
Shadows are reserved for "Floating" elements (e.g., Modals, Tooltips). 
- **Specification:** Use an extra-diffused blur (20px - 40px) at 6% opacity. 
- **Shadow Tint:** The shadow must use a tinted version of `on_surface` (#181d17) rather than pure black, ensuring the shadow feels like a natural extension of the environment.

### The "Ghost Border" Fallback
If a border is required for accessibility (e.g., in high-glare outdoor environments), use the **Ghost Border**: `outline_variant` at 20% opacity. Never use 100% opaque lines.

---

## 5. Iconography Integration
The new icon set is integrated into a strict **24x24px grid**. To maintain the "Harvest Flow" personality:
- **Stroke & Weight:** Maintain a consistent 1.5px or 2px stroke weight to match the sturdiness of the `Work Sans` typeface.
- **Coloration:** Icons should default to `on_surface_variant` (#40493d). Active or "Success" states must transition to `primary` (#0d631b).
- **Organic Corners:** Avoid sharp 90-degree joins in custom iconography. Use a 1px-2px radius to echo the "Roundedness Scale."

---

## 6. Components

### Buttons
- **Primary:** Gradient fill (`primary` to `primary_container`), `xl` (0.75rem) roundedness. No border.
- **Secondary:** `surface_container_highest` fill with `on_surface` text.
- **Tertiary:** Ghost style. No fill, `on_surface` text.

### Cards & Lists
- **The Forbid Rule:** No horizontal dividers between list items. 
- **The Alternative:** Use an 8px vertical gap and a subtle background shift on hover (`surface_container_high`). For complex data, use "Zebra Striping" with `surface` and `surface_container_low`.

### Input Fields
- **Styling:** Use `surface_container_low` as the field fill. 
- **State:** On focus, transition the background to `surface_container_lowest` and apply a 1px `primary` Ghost Border (20% opacity).

### Logistics Chips
- Use for "Status" (e.g., *In Transit*, *Quality Check*). 
- Use `tertiary_container` for neutral/warning and `primary_fixed` for success. Roundedness must be set to `full`.

---

## 7. Do's and Don'ts

### Do
- **Do** use negative space as a separator. If a layout feels cluttered, increase the white space rather than adding a line.
- **Do** use `Glassmorphism` (backdrop-blur: 12px) on top-navigation bars to allow produce imagery to bleed through subtly.
- **Do** prioritize the `manrope` font for all numerical data in "Display" or "Headline" sizes to emphasize volume and scale.

### Don't
- **Don't** use `#000000` for text. Use `on_surface` (#181d17) to maintain the organic, "ink-on-paper" feel.
- **Don't** use the `none` (0px) roundedness setting unless designing a technical edge-to-edge data grid. Everything else should feel "grown," not "manufactured."
- **Don't** drop a standard shadow on a card. Use tonal shifts first. Shadow is the last resort for extreme hierarchy.
